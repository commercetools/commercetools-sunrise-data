package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraftDsl;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.commands.TypeCreateCommand;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static io.sphere.sdk.client.SphereClientUtils.blockingWait;
import static io.sphere.sdk.client.SphereClientUtils.blockingWaitForEachCollector;

@Component
@Lazy
public class AvailabilityJobConfiguration extends DefaultCommercetoolsJobConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityJobConfiguration.class);
    private int productsImportStepChunkSize = 1;
    private final List<String> channelsIds = new ArrayList<>();

    @Bean
    public Job productsSuggestionsCopyJob(final Step channelImportStep,
                                          final Step customTypeImportStep,
                                          final Step createInventoryEntryStep) {
        return jobBuilderFactory.get("importJoyrideAvailabilityJob")
                .start(customTypeImportStep)
                .next(channelImportStep)
                .next(createInventoryEntryStep)
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
    @StepScope
    private ItemReader<ChannelDraftDsl> channelsDraftReader(@Value("#{jobParameters['resource']}") final Resource channelsJsonResource) throws IOException {
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
    @StepScope
    private ItemReader<TypeDraft> customTypeReader(@Value("#{jobParameters['resource_types']}") final Resource typesJsonResource) throws IOException {
        logger.info("URL_Types: " + typesJsonResource);
        final ObjectReader reader = SphereJsonUtils.newObjectMapper().readerFor(new TypeReference<List<TypeDraft>>() {
        });
        final InputStream inputStream = typesJsonResource.getInputStream();
        final List<TypeDraft> list = reader.readValue(inputStream);
        return new ListItemReader<>(list);
    }

    @Bean
    protected ItemWriter<TypeDraft> customTypeWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(customType -> sphereClient.executeBlocking(TypeCreateCommand.of(customType)));
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
    public ItemWriter<List<InventoryEntryDraft>> inventoryEntryWriter(final BlockingSphereClient sphereClient) {
        return entryLists -> entryLists.forEach(items -> items.forEach(inventoryEntry -> sphereClient.executeBlocking(InventoryEntryCreateCommand.of(inventoryEntry))));
    }

    @Bean
    protected ItemProcessor<Product, List<InventoryEntryDraft>> inventoryEntryProcessor(final BlockingSphereClient sphereClient) {
        return new ItemProcessor<Product, List<InventoryEntryDraft>>() {
            @Override
            public List<InventoryEntryDraft> process(final Product item) throws Exception {
                getChannelsIds(sphereClient);
                return inventoryEntryListByChannel(item);
            }
        };
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

}
