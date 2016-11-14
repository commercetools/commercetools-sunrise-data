package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClientUtils;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.*;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.products.search.PriceSelection;
import io.sphere.sdk.queries.PagedQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.sphere.sdk.client.SphereClientUtils.blockingWait;
import static io.sphere.sdk.queries.QueryExecutionUtils.queryAll;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class AvailabilityPricesImportJobConfiguration extends DefaultCommercetoolsJobConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityPricesImportJobConfiguration.class);

    @Bean
    public Job availabilityPricesImportJob(final Step availabilityPricesImportStep) {
        return jobBuilderFactory.get("availabilityPricesImportJob")
                .start(availabilityPricesImportStep)
                .build();
    }

    @Bean
    public Step availabilityPricesImportStep(final BlockingSphereClient sphereClient,
                                             final ItemReader<ProductProjection> productReader,
                                             final ItemWriter<ProductUpdateCommand> productPriceWriter,
                                             final ChannelListHolder channelListHolder) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("availabilityPricesImportStep");
        return stepBuilder
                .<ProductProjection, ProductUpdateCommand>chunk(20)
                .reader(productReader)
                .processor(priceCreationProcessor(sphereClient, channelListHolder))
                .listener(CustomItemProcessorListener.class)
                .writer(productPriceWriter)
                .faultTolerant()
                .skip(ErrorResponseException.class)
                .skipLimit(1)
                .build();
    }


    @Bean
    @StepScope
    public ItemReader<ProductProjection> productReader(final BlockingSphereClient sphereClient) {
        return createReader(sphereClient);
    }

    static ItemReader<ProductProjection> createReader(final BlockingSphereClient sphereClient) {
        final ProductProjectionQuery productProjectionQuery1 = ProductProjectionQuery.ofCurrent().withSort(m -> m.id().sort().asc());
        final Long productsCount = sphereClient.executeBlocking(productProjectionQuery1).getTotal();
        final Optional<ProductProjection> lastProductWithJoyrideChannel = findLastProductWithJoyrideChannel(sphereClient, 0L, productsCount - 1);
        final ProductProjectionQuery baseQuery = ProductProjectionQuery.ofStaged();
        final ProductProjectionQuery productProjectionQuery =
                lastProductWithJoyrideChannel
                        .map(product -> baseQuery.plusPredicates(m -> m.id().isGreaterThan(product.getId())))
                        .orElse(baseQuery);
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, productProjectionQuery, productProjection -> productProjection.getId());
    }

    @Bean
    protected ItemProcessor<ProductProjection, ProductUpdateCommand> priceCreationProcessor(final BlockingSphereClient sphereClient, final ChannelListHolder channelListHolder) {
        return createProcessor(sphereClient, channelListHolder);
    }

    private ItemProcessor<ProductProjection, ProductUpdateCommand> createProcessor(final BlockingSphereClient sphereClient, final ChannelListHolder channelListHolder) {
        return productProjection -> {
            final List<AddPrice> productPriceAddUpdateActions = channelListHolder.getChannels().stream()
                    .peek(m -> logger.info("attempting to process product {} in channel {}", productProjection.getId(), m.getId()))
                    .flatMap(channel -> createAddPriceStream(productProjection, channel, sphereClient))
                    .collect(Collectors.toList());
            final List<UpdateAction<Product>> allUpdateActions = new ArrayList<>(productPriceAddUpdateActions.size() + 1);
            allUpdateActions.addAll(productPriceAddUpdateActions);
            allUpdateActions.add(Publish.of());
            return ProductUpdateCommand.of(productProjection, allUpdateActions);
        };
    }

    private ProductProjection fetchProjectionWithPriceSelection(final BlockingSphereClient sphereClient, final ProductProjection productProjection, final Channel channel) {
        final CountryCode country = channel.getAddress().getCountry();
        final CurrencyUnit currency = Monetary.getCurrency(country.toLocale());
        final PriceSelection priceSelection = PriceSelection.of(currency).withPriceCountry(country);
        final ProductProjectionQuery productProjectionQuery = ProductProjectionQuery.ofCurrent().withPredicates(m -> m.id().is(productProjection.getId())).withPriceSelection(priceSelection);
        final PagedQueryResult<ProductProjection> result = sphereClient.executeBlocking(productProjectionQuery);
        return result.getResults().get(0);
    }

    private Stream<AddPrice> createAddPriceStream(final ProductProjection productProjection, final Channel channel, final BlockingSphereClient sphereClient) {
        final ProductProjection productProjectionWithSelectedPrice = fetchProjectionWithPriceSelection(sphereClient, productProjection, channel);
        return productProjectionWithSelectedPrice.getAllVariants().stream()
                .flatMap(productVariant -> calculateAddPrice(productVariant, channel));
    }

    private Stream<AddPrice> calculateAddPrice(final ProductVariant productVariant, final Channel channel) {
        return Optional.ofNullable(productVariant.getPrice())
                .filter(price -> price.getValue() != null)
                .map(price -> {
                    final PriceDraftDsl priceDraft = randomPriceDraft(productVariant, channel, price);
                    return Stream.of(AddPrice.of(productVariant.getId(), priceDraft));
                })
                .orElseGet(Stream::empty);
    }

    static PriceDraftDsl randomPriceDraft(final ProductVariant productVariant, final Channel channel, final Price price) {
        final Random random = new Random(productVariant.getSku().hashCode() + channel.getKey().hashCode());
        final double factor = randInt(random, 90, 110) * 0.01;
        final MonetaryAmount newAmount = price.getValue().multiply(factor);
        return PriceDraft.of(price).withValue(newAmount).withChannel(channel);
    }

    @Bean
    @JobScope
    public ChannelListHolder channelListHolder(final BlockingSphereClient sphereClient) {
        final ChannelQuery channelQuery = ChannelQuery.of()
                .withPredicates(m -> m.key().isIn(PreferredChannels.CHANNEL_KEYS));
        final List<Channel> channels = blockingWait(queryAll(sphereClient, channelQuery), 5, TimeUnit.MINUTES);
        return new ChannelListHolder(channels);
    }

    @Bean
    public ItemWriter<ProductUpdateCommand> setPriceWriter(final BlockingSphereClient sphereClient) {
        return updates -> updates.stream()
                .map(sphereClient::execute)
                .collect(SphereClientUtils.blockingWaitForEachCollector(5, TimeUnit.MINUTES));
    }

    static int randInt(final Random random, final int min, final int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    public static boolean productIsProcessed(final BlockingSphereClient sphereClient, final Long productIndex) {
        final ProductProjection product = searchProductByIndex(sphereClient, productIndex);
        return productContainJoyridePrice(product);
    }

    private static ProductProjection searchProductByIndex(final BlockingSphereClient sphereClient, final Long index) {
        final ProductProjectionQuery baseQuery = ProductProjectionQuery.ofCurrent()
                .withSort(m -> m.id().sort().asc())
                .withExpansionPaths(m -> m.allVariants().prices().channel());
        final ProductProjectionQuery productQuery = baseQuery.withOffset(index).withLimit(1L);
        return sphereClient.executeBlocking(productQuery).head().get();
    }

    public static Optional<ProductProjection> findLastProductWithJoyrideChannel(final BlockingSphereClient sphereClient, final Long start, final Long end) {
        final boolean firstProductIsProcessed = productIsProcessed(sphereClient, start);
        final boolean lastProductIsProcessed = productIsProcessed(sphereClient, end);
        if (!firstProductIsProcessed && !lastProductIsProcessed) {
            return Optional.empty();
        } else if (firstProductIsProcessed && lastProductIsProcessed) {
            return Optional.of(searchProductByIndex(sphereClient, end));
        } else {
            final Long mid = (start + end) / 2;
            final Optional<ProductProjection> optionalLeftResult = findLastProductWithJoyrideChannel(sphereClient, start, mid);
            final Optional<ProductProjection> optionalRightResult = findLastProductWithJoyrideChannel(sphereClient, mid + 1, end);
            final ProductProjection leftResult = optionalLeftResult.isPresent() ? optionalLeftResult.get() : null;
            final ProductProjection rightResult = optionalRightResult.isPresent() ? optionalRightResult.get() : null;
            return maxBetweenProducts(leftResult, rightResult);
        }
    }

    private static Optional<ProductProjection> maxBetweenProducts(@Nullable final ProductProjection product1, @Nullable final ProductProjection product2) {
        if (product1 == null) {
            return Optional.of(product2);
        } else if (product2 == null) {
            return Optional.of(product1);
        } else {
            final String productId1 = product1 != null ? product1.getId() : "";
            final String productId2 = product2 != null ? product1.getId() : "";
            final ProductProjection productProjection = productId1.compareTo(productId2) == 1 ? product1 : product2;
            return Optional.of(productProjection);
        }
    }

    public static boolean productContainJoyridePrice(@Nullable final ProductProjection product) {
        final List<String> joyrideChannels = PreferredChannels.CHANNEL_KEYS;
        final List<ProductVariant> productVariants = product != null ? product.getAllVariants() : Collections.emptyList();
        final List<Price> productPrices = productVariants
                .stream()
                .flatMap(variant -> variant.getPrices().stream())
                .collect(Collectors.toList());
        final Set<String> channelsKeys = productPrices.stream()
                .filter(price -> price.getChannel() != null)
                .map(price -> price.getChannel().getObj().getKey())
                .collect(Collectors.toSet());
        return CollectionUtils.containsAny(channelsKeys, joyrideChannels);
    }

}
