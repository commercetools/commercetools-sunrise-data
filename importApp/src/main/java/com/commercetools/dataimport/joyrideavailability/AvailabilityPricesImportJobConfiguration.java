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
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.dataimport.joyrideavailability.PreferredChannels.CHANNEL_KEYS;
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
                                             final ItemWriter<ProductUpdateCommand> productPriceWriter) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("availabilityPricesImportStep");
        return stepBuilder
                .<ProductProjection, ProductUpdateCommand>chunk(20)
                .reader(productReader)
                .processor(priceCreationProcessor(sphereClient))
                .writer(productPriceWriter)
                .faultTolerant()
                .skip(ErrorResponseException.class)
                .skipLimit(1)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<ProductProjection> productReader(final BlockingSphereClient sphereClient) {
        return createProductProjectionReader(sphereClient);
    }

    @Bean
    protected ItemProcessor<ProductProjection, ProductUpdateCommand> priceCreationProcessor(final BlockingSphereClient sphereClient) {
        return createProcessor(sphereClient);
    }

    @Bean
    public ItemWriter<ProductUpdateCommand> productPriceWriter(final BlockingSphereClient sphereClient) {
        return updates -> updates.stream()
                .map(sphereClient::execute)
                .collect(SphereClientUtils.blockingWaitForEachCollector(5, TimeUnit.MINUTES));
    }

    public ChannelListHolder channelListHolder(final BlockingSphereClient sphereClient) {
        final ChannelQuery channelQuery = ChannelQuery.of()
                .withPredicates(m -> m.key().isIn(CHANNEL_KEYS));
        final List<Channel> channels = blockingWait(queryAll(sphereClient, channelQuery), 5, TimeUnit.MINUTES);
        return new ChannelListHolder(channels);
    }

    static ItemReader<ProductProjection> createProductProjectionReader(final BlockingSphereClient sphereClient) {
        final ProductProjectionQuery baseQuery = ProductProjectionQuery.ofCurrent();
        final Long productsCount = sphereClient.executeBlocking(baseQuery.withLimit(0)).getTotal();
        final Optional<ProductProjection> lastProductWithJoyrideChannel = findLastProductWithJoyrideChannel(sphereClient, 0L, productsCount - 1);
        final ProductProjectionQuery productProjectionQuery =
                lastProductWithJoyrideChannel
                        .map(product -> baseQuery.plusPredicates(m -> m.id().isGreaterThan(product.getId())))
                        .orElse(baseQuery);
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, productProjectionQuery, productProjection -> productProjection.getId());
    }

    private ItemProcessor<ProductProjection, ProductUpdateCommand> createProcessor(final BlockingSphereClient sphereClient) {
        return productProjection -> {
            final List<AddPrice> productPriceAddUpdateActions = channelListHolder(sphereClient).getChannels().stream()
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

    static int randInt(final Random random, final int min, final int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    public static boolean productIsProcessed(final BlockingSphereClient sphereClient, final Long productIndex) {
        final Optional<ProductProjection> optionalProduct = searchProductByIndex(sphereClient, productIndex);
        return optionalProduct.map(product -> productContainJoyridePrice(product)).orElse(false);
    }

    private static Optional<ProductProjection> searchProductByIndex(final BlockingSphereClient sphereClient, final Long productIndex) {
        final ProductProjectionQuery baseQuery = ProductProjectionQuery.ofCurrent()
                .withSort(m -> m.id().sort().asc())
                .withExpansionPaths(m -> m.allVariants().prices().channel());
        final ProductProjectionQuery productQuery = baseQuery.withOffset(productIndex).withLimit(1L);
        return sphereClient.executeBlocking(productQuery).head();
    }

    public static Optional<ProductProjection> findLastProductWithJoyrideChannel(final BlockingSphereClient sphereClient, final Long startProductIndex, final Long lastProductIndex) {
        final boolean startProductIsProcessed = productIsProcessed(sphereClient, startProductIndex);
        final boolean lastProductIsProcessed = productIsProcessed(sphereClient, lastProductIndex);
        final boolean allProductsProcessed = startProductIsProcessed && lastProductIsProcessed;
        if (allProductsProcessed) {
            return searchProductByIndex(sphereClient, lastProductIndex);
        } else if (startProductIsProcessed) {
            return findLastProcessedProduct(sphereClient, startProductIndex, lastProductIndex);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<ProductProjection> findLastProcessedProduct(final BlockingSphereClient sphereClient, final Long startProductIndex, final Long lastProductIndex) {
        final Long mid = (startProductIndex + lastProductIndex) / 2;
        final Optional<ProductProjection> optionalLeftResult = findLastProductWithJoyrideChannel(sphereClient, startProductIndex, mid);
        final Optional<ProductProjection> optionalRightResult = findLastProductWithJoyrideChannel(sphereClient, mid + 1, lastProductIndex);
        return optionalRightResult.isPresent() ? optionalRightResult : optionalLeftResult;
    }

    public static boolean productContainJoyridePrice(final ProductProjection product) {
        final List<Price> productPrices = product.getAllVariants()
                .stream()
                .flatMap(variant -> variant.getPrices().stream())
                .collect(Collectors.toList());
        final Set<String> productPricesChannels = productPrices.stream()
                .filter(price -> price.getChannel() != null)
                .map(price -> price.getChannel().getObj().getKey())
                .collect(Collectors.toSet());
        return CollectionUtils.containsAny(productPricesChannels, CHANNEL_KEYS);
    }

}