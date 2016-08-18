package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraftDsl;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.*;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.products.search.PriceSelection;
import io.sphere.sdk.products.search.ProductProjectionSearch;
import io.sphere.sdk.search.PagedSearchResult;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.utils.MoneyImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.sphere.sdk.client.SphereClientUtils.blockingWait;
import static io.sphere.sdk.client.SphereClientUtils.blockingWaitForEachCollector;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;

@Component
@Lazy
public class AvailabilityJobConfiguration extends DefaultCommercetoolsJobConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityJobConfiguration.class);
    private int productsImportStepChunkSize = 1;
    private final List<String> channelsIds = new ArrayList<>();
    private final List<String> channelsToAddPrice = new ArrayList<>();

    @Bean
    public Job productsSuggestionsCopyJob(final Step channelImportStep,
                                          final Step customTypeImportStep,
                                          final Step createInventoryEntryStep,
                                          final Step setPricesStep) {
        return jobBuilderFactory.get("importJoyrideAvailabilityJob")
                .start(customTypeImportStep)
                .next(channelImportStep)
                .next(createInventoryEntryStep)
                .next(setPricesStep)
                .build();
    }

    @Bean
    public Step customTypeImportStep(final BlockingSphereClient sphereClient,
                                     final ItemReader<TypeDraft> customTypeReader,
                                     final ItemWriter<TypeDraft> customTypeItemWriter) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("customTypeImportStep");
        return stepBuilder
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(customTypeReader)
                .writer(customTypeWriter(sphereClient))
                .build();
    }

    @Bean
    public Step channelImportStep(final BlockingSphereClient sphereClient,
                                  final ItemReader<ChannelDraftDsl> channelsDraftReader,
                                  final ItemWriter<ChannelDraftDsl> channelsDraftItemWriter) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("channelImportStep");
        return stepBuilder
                .<ChannelDraftDsl, ChannelDraftDsl>chunk(1)
                .reader(channelsDraftReader)
                .writer(channelsDraftWriter(sphereClient))
                .build();
    }

    @Bean
    public Step createInventoryEntryStep(final BlockingSphereClient sphereClient,
                                         final ItemProcessor<Product, List<InventoryEntryDraft>> inventoryEntryProcessor,
                                         final ItemWriter<List<InventoryEntryDraft>> inventoryEntryWriter
    ) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("createInventoryEntryStep");
        return stepBuilder
                .<Product, List<InventoryEntryDraft>>chunk(productsImportStepChunkSize)
                .reader(ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductQuery.of()))
                .processor(inventoryEntryProcessor(sphereClient))
                .writer(inventoryEntryWriter)
                .build();
    }

    @Bean
    public Step setPricesStep(final BlockingSphereClient sphereClient,
                              final ItemWriter<ProductUpdateCommand> setPriceWriter
    ) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("createInventoryEntryStep");
        return stepBuilder
                .<Product, ProductUpdateCommand>chunk(productsImportStepChunkSize)
                .reader(ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductQuery.of()))
                .processor(setPriceProcessor(sphereClient))
                .writer(setPriceWriter)
                .build();
    }

    @Bean
    @StepScope
    private ItemReader<TypeDraft> customTypeReader(@Value("#{jobParameters['resourceTypes']}") final Resource typesJsonResource) throws IOException {
        logger.info("URL_Types: " + typesJsonResource);
        final ObjectReader reader = SphereJsonUtils.newObjectMapper().readerFor(new TypeReference<List<TypeDraft>>() { });
        final InputStream inputStream = typesJsonResource.getInputStream();
        final List<TypeDraft> list = reader.readValue(inputStream);
        return new ListItemReader<>(list);
    }

    @Bean
    protected ItemWriter<TypeDraft> customTypeWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(customType -> sphereClient.executeBlocking(TypeCreateCommand.of(customType)));
    }

    @Bean
    @StepScope
    private ItemReader<ChannelDraftDsl> channelsDraftReader(@Value("#{jobParameters['resourceChannels']}") final Resource channelsJsonResource) throws IOException {
        logger.info("URL_Channels: " + channelsJsonResource);
        final ObjectReader reader = SphereJsonUtils.newObjectMapper().readerFor(new TypeReference<List<ChannelDraftDsl>>() {
        });
        final InputStream inputStream = channelsJsonResource.getInputStream();
        final List<ChannelDraftDsl> list = reader.readValue(inputStream);
        return new ListItemReader<>(list);
    }

    @Bean
    protected ItemWriter<ChannelDraftDsl> channelsDraftWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(channelDraft -> sphereClient.executeBlocking(ChannelCreateCommand.of(channelDraft)));
    }

    @Bean
    protected ItemProcessor<Product, List<InventoryEntryDraft>> inventoryEntryProcessor(final BlockingSphereClient sphereClient) {
        return item -> {
            getChannelsIds(sphereClient);
            return inventoryEntryListByChannel(item);
        };
    }

    @Bean
    public ItemWriter<List<InventoryEntryDraft>> inventoryEntryWriter(final BlockingSphereClient sphereClient) {
        return entryLists -> entryLists.forEach(items -> items.forEach(inventoryEntry -> sphereClient.executeBlocking(InventoryEntryCreateCommand.of(inventoryEntry))));
    }

    @Bean
    protected ItemProcessor<Product, ProductUpdateCommand> setPriceProcessor(final BlockingSphereClient sphereClient) {
        return item -> {
            final ProductProjectionSearch searchRequest = ProductProjectionSearch.ofStaged()
                    .withQueryFilters(m -> m.id().is(item.getId()))
                    .withPriceSelection(PriceSelection.of(EUR));
            final PagedSearchResult<ProductProjection> result = sphereClient.executeBlocking(searchRequest);
            final ProductVariant masterVariant = result.getResults().get(0).getMasterVariant();
            final Price priceProduct = masterVariant.getPrice();
            final MonetaryAmount newAmount = priceProduct.getValue().subtract(MoneyImpl.ofCents(60, EUR));
            getChannelsByCountry(sphereClient);
            final List<AddPrice> productAndPricesList= new ArrayList<>();
            for (String channelId : channelsToAddPrice) {
                final Reference<Channel> channelReference = Channel.referenceOfId(channelId);
                final PriceDraft pricePerChannel = PriceDraft.of(newAmount).withChannel(channelReference).withCountry(CountryCode.DE);
                for ( ProductVariant productVariant : item.getMasterData().getCurrent().getAllVariants()) {
                    productAndPricesList.add(AddPrice.of(productVariant.getId(), pricePerChannel));
                }
            }
            return ProductUpdateCommand.of(item, productAndPricesList);
        };
    }

    @Bean
    public ItemWriter<ProductUpdateCommand> setPriceWriter(final BlockingSphereClient sphereClient) {
        return list -> list.forEach(cmd -> sphereClient.executeBlocking(cmd));
    }

    private List<InventoryEntryDraft> inventoryEntryListByChannel(final Product item) {
        final List<InventoryEntryDraft> listInvetoryEntryDraft = new ArrayList<>();
        for (String channelId : channelsIds ) {
            final Reference<Channel> channelReference = Channel.referenceOfId(channelId);
            final long quantityOnStock = 1L;
            final String sku = item.getMasterData().getCurrent().getMasterVariant().getSku();
            final InventoryEntryDraft inventoryEntryDraft = InventoryEntryDraft.of(sku, quantityOnStock)
                    .withSupplyChannel(channelReference);
            listInvetoryEntryDraft.add(inventoryEntryDraft);
        }
        return listInvetoryEntryDraft;
    }

    private void getChannelsIds(final BlockingSphereClient sphereClient) {
        if ( channelsIds.isEmpty() ){
            final ChannelQuery query = ChannelQuery.of();
            final List<Channel> results = sphereClient.executeBlocking(query).getResults();
            for (Channel result: results) {
                channelsIds.add(result.getId());
            }
        }
    }

    private void getChannelsByCountry(final BlockingSphereClient sphereClient){
        if ( channelsToAddPrice.isEmpty() ){
            final ChannelQuery query = ChannelQuery.of();
            final List<Channel> results = sphereClient.executeBlocking(query).getResults();
            for (Channel result: results) {
                if ( result.getAddress().getCountry().equals(CountryCode.DE) && channelsToAddPrice.size() < 5 ){
                    channelsToAddPrice.add(result.getId());
                }
            }
        }
    }

}
