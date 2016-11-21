package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClientUtils;
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
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.sphere.sdk.client.SphereClientUtils.blockingWait;
import static io.sphere.sdk.queries.QueryExecutionUtils.queryAll;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class InventoryEntryCreationJobConfiguration extends DefaultCommercetoolsJobConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(InventoryEntryCreationJobConfiguration.class);

    @Bean
    public Job inventoryEntryCreationJob(final Step createInventoryEntryStep) {
        return jobBuilderFactory.get("inventoryEntryCreationJob")
                .start(createInventoryEntryStep)
                .build();
    }

    @Bean
    public Step createInventoryEntryStep(final BlockingSphereClient sphereClient,
                                         final ItemReader<ProductProjection> inventoryEntryReader,
                                         final ItemProcessor<ProductProjection, List<InventoryEntryDraft>> inventoryEntryProcessor,
                                         final ItemWriter<List<InventoryEntryDraft>> inventoryEntryWriter) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("createInventoryEntryStep");
        return stepBuilder
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
    protected ItemReader<ProductProjection> inventoryEntryReader(final BlockingSphereClient sphereClient) {
        return createReader(sphereClient);
    }

    static ItemReader<ProductProjection> createReader(final BlockingSphereClient sphereClient) {
        final Optional<InventoryEntry> lastInventoryEntry = findLastInventoryEntry(sphereClient);
        final ProductProjectionQuery baseQuery = ProductProjectionQuery.ofCurrent();
        final ProductProjectionQuery productProjectionQuery =
                lastInventoryEntry
                        .map(inventoryEntry -> {
                            final PagedQueryResult<ProductProjection> queryResult = sphereClient.executeBlocking(ProductProjectionQuery.ofStaged().plusPredicates(product -> product.allVariants().where(m -> m.sku().is(inventoryEntry.getSku()))));
                            return queryResult.head().map(product -> baseQuery.plusPredicates(m -> m.id().isGreaterThanOrEqualTo(product.getId()))).orElse(baseQuery);
                        })
                        .orElse(baseQuery);
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, productProjectionQuery, productProjection -> productProjection.getId());
    }

    @Bean
    protected ItemProcessor<ProductProjection, List<InventoryEntryDraft>> inventoryEntryProcessor(final BlockingSphereClient sphereClient) {
        return product -> inventoryEntryListByChannel(product, channelListHolder(sphereClient).getChannels());
    }

    @Bean
    public ItemWriter<List<InventoryEntryDraft>> inventoryEntryWriter(final BlockingSphereClient sphereClient) {
        return entries -> {
            final Stream<InventoryEntryDraft> inventoryEntryDraftStream = entries.stream().flatMap(list -> list.stream());
            final List<InventoryEntry> collect = inventoryEntryDraftStream
                    .peek(draft -> logger.info("attempting to create inventory entry sku {}, channel {}", draft.getSku(), draft.getSupplyChannel().getId()))
                    .map(draft -> sphereClient.execute(InventoryEntryCreateCommand.of(draft)))
                    .collect(SphereClientUtils.blockingWaitForEachCollector(5, TimeUnit.MINUTES));
        };
    }

    public ChannelListHolder channelListHolder(final BlockingSphereClient sphereClient) {
        final ChannelQuery channelQuery = ChannelQuery.of()
                .withPredicates(m -> m.key().isIn(PreferredChannels.CHANNEL_KEYS));
        final List<Channel> channels = blockingWait(queryAll(sphereClient, channelQuery), 5, TimeUnit.MINUTES);
        return new ChannelListHolder(channels);
    }

    private List<InventoryEntryDraft> inventoryEntryListByChannel(final ProductProjection product, final List<Channel> channels) {
        logger.info("Processing product {}", Optional.ofNullable(product).map(ResourceView::getId).orElse("product was null"));
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
        final InventoryEntryDraft inventoryEntryDraft = InventoryEntryDraft.of(sku, quantityOnStock)
                .withSupplyChannel(channel);
        return inventoryEntryDraft;
    }

    static int randomInt(final Random random, final int min, final int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    public static Optional<InventoryEntry> findLastInventoryEntry(final BlockingSphereClient sphereClient) {
        final InventoryEntryQuery inventoryEntryQuery = InventoryEntryQuery.of().withSort(m -> m.lastModifiedAt().sort().desc()).withLimit(1L);
        return sphereClient.execute(inventoryEntryQuery).toCompletableFuture().join().head();
    }
}
