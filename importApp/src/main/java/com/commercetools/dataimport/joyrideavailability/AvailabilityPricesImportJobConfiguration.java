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
import io.sphere.sdk.products.search.ProductProjectionSearch;
import io.sphere.sdk.search.PagedSearchResult;
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
                .<ProductProjection, ProductUpdateCommand>chunk(1)
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
        final ProductProjectionSearch searchRequest = ProductProjectionSearch.ofCurrent()
                .withQueryFilters(m -> m.id().is(productProjection.getId()))
                .withPriceSelection(priceSelection);
        final PagedSearchResult<ProductProjection> result = sphereClient.executeBlocking(searchRequest);
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
                    final Random random = new Random(productVariant.getSku().hashCode() + channel.getKey().hashCode());
                    final double factor = randInt(random, 90, 110) * 0.01;
                    final MonetaryAmount newAmount = price.getValue().multiply(factor);
                    final PriceDraftDsl priceDraft = PriceDraft.of(price).withValue(newAmount).withChannel(channel);
                    return Stream.of(AddPrice.of(productVariant.getId(), priceDraft));
                })
                .orElseGet(Stream::empty);
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

    public int randInt(final Random random, final int min, final int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    public Optional<ProductProjection> findLastProductWithJoyrideChannel(final BlockingSphereClient sphereClient, final Long start, final Long end) {
        final ProductProjectionQuery baseQuery = ProductProjectionQuery.ofCurrent().withSort(m -> m.id().sort().asc());
        final ProductProjectionQuery firstProductQuery = baseQuery.withOffset(start).withLimit(1L);
        final Optional<ProductProjection> optionalFirstProduct = sphereClient.executeBlocking(firstProductQuery).head();
        final ProductProjectionQuery lastProductQuery = baseQuery.withOffset(end).withLimit(1L);
        final Optional<ProductProjection> optionalLastProduct = sphereClient.executeBlocking(lastProductQuery).head();
        final boolean firstProductIsProcessed = containsJoyrideChannel(sphereClient, optionalFirstProduct);
        final boolean lastProductIsProcessed = containsJoyrideChannel(sphereClient, optionalLastProduct);
        if ( !firstProductIsProcessed && !lastProductIsProcessed ) {
            return Optional.empty();
        } else if (firstProductIsProcessed && lastProductIsProcessed) {
            return optionalLastProduct;
        } else {
            final Long mid = (start+end)/2;
            final Optional<ProductProjection> leftResult = findLastProductWithJoyrideChannel(sphereClient, start, mid);
            final Optional<ProductProjection> rightResult = findLastProductWithJoyrideChannel(sphereClient, mid+1, end);
            return maxBetweenProducts(leftResult, rightResult);
        }
    }

    private Optional<ProductProjection> maxBetweenProducts(final Optional<ProductProjection> product1, final Optional<ProductProjection> product2) {
        if ( !product1.isPresent() ) {
            return product2;
        } else if ( !product2.isPresent() ){
            return product1;
        } else {
            final String productId1 = product1.map(m -> m.getId()).orElse("");
            final String productId2 = product2.map(m -> m.getId()).orElse("");
            final Optional<ProductProjection> productProjection = productId1.compareTo(productId2) == 1 ? product1 : product2;
            return productProjection;
        }
    }

    private boolean containsJoyrideChannel(final BlockingSphereClient sphereClient, Optional<ProductProjection> productVariantOptional) {
        final List<String> joyrideChannels = channelListHolder(sphereClient).getChannels()
                .stream()
                .map(m -> m.getId())
                .collect(Collectors.toList());
        final List<ProductVariant> firstProductVariants = productVariantOptional
                .map(firstProduct -> firstProduct.getAllVariants())
                .orElse(Collections.emptyList());
        final List<Price> firstProductPrices = firstProductVariants
                .stream()
                .flatMap(variant -> variant.getPrices().stream())
                .collect(Collectors.toList());
        final List<String> priceChannelsIds = firstProductPrices.stream()
                .filter(price -> price.getChannel() != null)
                .map(price -> price.getChannel().getId())
                .collect(Collectors.toList());
        return CollectionUtils.containsAny(priceChannelsIds, joyrideChannels);
    }

    public static Optional<ProductProjection> findLastModifiedProduct(final BlockingSphereClient sphereClient) {
        final ProductProjectionQuery productProjectionQuery = ProductProjectionQuery.ofCurrent().withSort(m -> m.lastModifiedAt().sort().desc()).withLimit(1L);
        return sphereClient.execute(productProjectionQuery).toCompletableFuture().join().head();
    }

}
