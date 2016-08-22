package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientUtils;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.ResourceView;
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
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.sphere.sdk.client.SphereClientUtils.blockingWait;
import static io.sphere.sdk.queries.QueryExecutionUtils.queryAll;

@Component
@Lazy
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
                                             final ItemWriter<ProductUpdateCommand> productPriceWriter,
                                             final ChannelListHolder channelListHolder) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("availabilityPricesImportStep");
        return stepBuilder
                .<ProductProjection, ProductUpdateCommand>chunk(50)
                .reader(ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductProjectionQuery.ofCurrent(), ResourceView::getId))
                .processor(priceCreationProcessor(sphereClient, channelListHolder))
                .writer(productPriceWriter)
                .build();
    }

    @Bean
    protected ItemProcessor<ProductProjection, ProductUpdateCommand> priceCreationProcessor(final BlockingSphereClient sphereClient, final ChannelListHolder channelListHolder) {
        return productProjection -> {
            final List<AddPrice> productPriceAddUpdateActions = channelListHolder.getChannels().stream()
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

    @Bean @JobScope
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

}
