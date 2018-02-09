package com.commercetools.dataimport.inventoryentries;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.dataimport.channels.ChannelListHolder;
import com.commercetools.dataimport.channels.PreferredChannels;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.ResourceView;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.sphere.sdk.client.SphereClientUtils.blockingWait;
import static io.sphere.sdk.queries.QueryExecutionUtils.queryAll;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class InventoryEntryCreationJobConfiguration extends CommercetoolsJobConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryEntryCreationJobConfiguration.class);

    @Bean
    public Job inventoryEntryCreationJob(final Step inventoryEntryCreationStep) {
        return jobBuilderFactory.get("inventoryEntryCreationJob")
                .start(inventoryEntryCreationStep)
                .build();
    }

    @Bean
    public Step inventoryEntryCreationStep(final ItemReader<ProductProjection> inventoryEntryReader,
                                           final ItemProcessor<ProductProjection, List<InventoryEntryDraft>> inventoryEntryProcessor,
                                           final ItemWriter<List<InventoryEntryDraft>> inventoryEntryWriter) {
        return stepBuilderFactory.get("inventoryEntryCreationStep")
                .<ProductProjection, List<InventoryEntryDraft>>chunk(1)
                .reader(inventoryEntryReader)
                .processor(inventoryEntryProcessor)
                .writer(inventoryEntryWriter)
                .faultTolerant()
                .skip(ErrorResponseException.class)
                .skipLimit(1)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<ProductProjection> inventoryEntryReader(final BlockingSphereClient sphereClient) {
        return createProductReader(sphereClient);
    }

    @Bean
    public ItemProcessor<ProductProjection, List<InventoryEntryDraft>> inventoryEntryProcessor(final BlockingSphereClient sphereClient) {
        return product -> inventoryEntryListByChannel(product, channelListHolder(sphereClient).getChannels());
    }

    @Bean
    public ItemWriter<List<InventoryEntryDraft>> inventoryEntryWriter(final BlockingSphereClient sphereClient) {
        return entries -> entries.stream()
                .flatMap(Collection::stream)
                .peek(draft -> LOGGER.info("attempting to create inventory entry sku {}, channel {}", draft.getSku(), draft.getSupplyChannel().getId()))
                .map(InventoryEntryCreateCommand::of)
                .forEach(sphereClient::execute);
    }

    static ItemReader<ProductProjection> createProductReader(final BlockingSphereClient sphereClient) {
        final Optional<ProductProjection> lastProductWithInventory = findLastProductWithInventory(sphereClient);
        final ProductProjectionQuery baseQuery = ProductProjectionQuery.ofCurrent();
        final ProductProjectionQuery productProjectionQuery = lastProductWithInventory
                .map(productProjection -> baseQuery.withPredicates(product -> product.id().isGreaterThan(productProjection.getId())))
                .orElse(baseQuery);
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, productProjectionQuery, ResourceView::getId);
    }

    private ChannelListHolder channelListHolder(final BlockingSphereClient sphereClient) {
        final ChannelQuery channelQuery = ChannelQuery.of()
                .withPredicates(m -> m.key().isIn(PreferredChannels.CHANNEL_KEYS));
        final List<Channel> channels = blockingWait(queryAll(sphereClient, channelQuery), 5, TimeUnit.MINUTES);
        return new ChannelListHolder(channels);
    }

    private List<InventoryEntryDraft> inventoryEntryListByChannel(final ProductProjection product, final List<Channel> channels) {
        LOGGER.info("Processing product {}", Optional.ofNullable(product).map(ResourceView::getId).orElse("product was null"));
        return channels.stream()
                .flatMap(channel -> product.getAllVariants().stream()
                        .map(productVariant -> createInventoryEntryDraftForProductVariant(channel, productVariant)))
                .collect(Collectors.toList());
    }

    static InventoryEntryDraft createInventoryEntryDraftForProductVariant(final Channel channel, final ProductVariant productVariant) {
        final Random random = new Random(productVariant.getSku().hashCode() + channel.getKey().hashCode());
        final int bucket = randomInt(random, 0, 99);
        final long quantityOnStock;
        if (bucket > 70) {
            quantityOnStock = randomInt(random, 11, 1000);
        } else if (bucket > 10) {
            quantityOnStock = randomInt(random, 1, 10);
        } else {
            quantityOnStock = 0;
        }
        final String sku = productVariant.getSku();
        return InventoryEntryDraft.of(sku, quantityOnStock).withSupplyChannel(channel);
    }

    private static int randomInt(final Random random, final int min, final int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    static Optional<ProductProjection> findLastProductWithInventory(final BlockingSphereClient sphereClient) {
        final InventoryEntryQuery inventoryEntryQuery = InventoryEntryQuery.of().withSort(m -> m.lastModifiedAt().sort().desc()).withLimit(1L);
        final Optional<InventoryEntry> inventoryEntryOptional = sphereClient.execute(inventoryEntryQuery).toCompletableFuture().join().head();
        return inventoryEntryOptional.map(inventoryEntry -> {
            final PagedQueryResult<ProductProjection> productProjectionPagedQueryResult
                    = sphereClient.executeBlocking(ProductProjectionQuery.ofCurrent().plusPredicates(product -> product.allVariants().where(m -> m.sku().is(inventoryEntry.getSku()))));
            return productProjectionPagedQueryResult.head();
        }).orElse(Optional.empty());
    }

}
