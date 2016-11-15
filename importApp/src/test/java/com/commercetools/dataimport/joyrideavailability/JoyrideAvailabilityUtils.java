package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.ExceptionalConsumer;
import com.commercetools.ExceptionalUnaryOperator;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.*;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.Query;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.batch.core.JobParameter;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static com.commercetools.dataimport.joyrideavailability.PreferredChannels.CHANNEL_KEYS;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class JoyrideAvailabilityUtils {

    public static void withProduct(final BlockingSphereClient sphereClient, final UnaryOperator<Product> op) {
        final Product product = createProduct(sphereClient);
        final Product updatedProduct = op.apply(product);
        final Product productToDelete = sphereClient.executeBlocking(ProductUpdateCommand.of(updatedProduct, Unpublish.of()));
        sphereClient.executeBlocking(ProductDeleteCommand.of(productToDelete));
    }

    public static void withListOfProductProjections(final BlockingSphereClient sphereClient, final int amountOfProducts, final ExceptionalUnaryOperator<List<ProductProjection>> op) throws Exception {
        for (int i = 0; i < amountOfProducts; i++) {
            createProduct(sphereClient);
        }
        final List<ProductProjection> productProjection = sphereClient.executeBlocking(ProductProjectionQuery.ofCurrent().withSort(m -> m.id().sort().asc())).getResults();
        final List<ProductProjection> productsToDelete = op.apply(productProjection);
        productsToDelete.forEach(m -> {
            final Product productToDelete = sphereClient.executeBlocking(ProductUpdateCommand.of(m, Unpublish.of()));
            sphereClient.executeBlocking(ProductDeleteCommand.of(productToDelete));
        });
    }

    public static void withJoyrideChannels(final BlockingSphereClient sphereClient, final ExceptionalConsumer<List<Channel>> consumer) throws Exception {
        final List<Channel> joyrideChannels = CHANNEL_KEYS.stream()
                .map(channelKey -> sphereClient.executeBlocking(ChannelCreateCommand.of(ChannelDraft.of(channelKey).
                        withAddress(Address.of(CountryCode.DE)))))
                .collect(toList());
        consumer.accept(joyrideChannels);
        joyrideChannels.forEach(joyrideChannel -> sphereClient.executeBlocking(ChannelDeleteCommand.of(joyrideChannel)));
    }

    public static Product withPrice(final Function<PriceDraft, Product> function) {
        final PriceDraft priceDraft = PriceDraft.of(MoneyImpl.of(new BigDecimal("123456"), "EUR")).withCountry(CountryCode.DE);
        return function.apply(priceDraft);
    }

    public static void withPriceWithJoyrideChannel(final BlockingSphereClient sphereClient, final ExceptionalConsumer<PriceDraft> consumer) throws Exception {
        final String joyrideChannelKey = CHANNEL_KEYS.get(0);
        final Channel joyrideChannel = sphereClient.executeBlocking(ChannelCreateCommand.of(ChannelDraft.of(joyrideChannelKey)));
        final PriceDraft priceDraft = PriceDraft.of(MoneyImpl.of(new BigDecimal("123456"), "EUR")).withChannel(joyrideChannel);
        consumer.accept(priceDraft);
        sphereClient.executeBlocking(ChannelDeleteCommand.of(joyrideChannel));
    }

    public static void withInventoryEntry(final BlockingSphereClient sphereClient, final String sku, Consumer<InventoryEntry> consumer) {
        final Long quantityOnStock = 50L;
        final InventoryEntryDraft inventoryEntryDraft = InventoryEntryDraft.of(sku, quantityOnStock);
        final InventoryEntry inventoryEntry = sphereClient.executeBlocking(InventoryEntryCreateCommand.of(inventoryEntryDraft));
        consumer.accept(inventoryEntry);
        sphereClient.executeBlocking(InventoryEntryDeleteCommand.of(inventoryEntry));
    }

    public static ProductType createProductType(final BlockingSphereClient sphereClient) {
        final ProductTypeDraft productTypeDraft =
                ProductTypeDraft.of(RandomStringUtils.randomAlphabetic(10), "name", "a 'T' shaped cloth", Collections.emptyList());
        final ProductType productType = sphereClient.executeBlocking(ProductTypeCreateCommand.of(productTypeDraft));
        return productType;
    }

    public static Product createProduct(final BlockingSphereClient sphereClient) {
        return withPrice(priceDraft -> {
            final String sku = RandomStringUtils.randomAlphabetic(10);
            final ProductType productType = createProductType(sphereClient);
            final ProductDraftBuilder productDraftBuilder = ProductDraftBuilder.of(productType, LocalizedString.of(Locale.ENGLISH, "product-name"),
                    LocalizedString.of(Locale.ENGLISH, RandomStringUtils.randomAlphabetic(10)), ProductVariantDraftBuilder.of().sku(sku).build());
            final Product product = sphereClient.executeBlocking(ProductCreateCommand.of(productDraftBuilder.publish(true).build()));
            final AddPrice addPrice = AddPrice.of(product.getMasterData().getCurrent().getMasterVariant().getId(), priceDraft);
            Product productWithPrice = sphereClient.executeBlocking(ProductUpdateCommand.of(product, asList(addPrice, Publish.of())));
            return productWithPrice;
        });
    }

    public static void deleteInventoryEntries(final BlockingSphereClient sphereClient) {
        updateOrDeleteResources(sphereClient, InventoryEntryQuery.of(), (inventoryEntry) -> sphereClient.executeBlocking(InventoryEntryDeleteCommand.of(inventoryEntry)));
    }

    public static void deleteChannels(final BlockingSphereClient sphereClient) {
        updateOrDeleteResources(sphereClient, ChannelQuery.of(), (channel) -> sphereClient.executeBlocking(ChannelDeleteCommand.of(channel)));
    }

    public static void unpublishProducts(final BlockingSphereClient sphereClient) {
        updateOrDeleteResources(sphereClient, ProductProjectionQuery.ofCurrent(), (item) -> {
            final ProductUpdateCommand updateCommand = ProductUpdateCommand.of(item, Unpublish.of());
            return sphereClient.executeBlocking(updateCommand);
        });
    }

    public static void deleteProducts(final BlockingSphereClient sphereClient) {
        updateOrDeleteResources(sphereClient, ProductProjectionQuery.ofStaged(), (item) -> sphereClient.executeBlocking(ProductDeleteCommand.of(item)));
    }

    public static <T, S> void updateOrDeleteResources(final BlockingSphereClient sphereClient, final Query<T> query, final Function<T, S> function) {
        int modifiedResourcesAmount;
        do {
            final PagedQueryResult<T> pagedQueryResult = sphereClient.executeBlocking(query);
            final List<S> modifiedResources = pagedQueryResult.getResults()
                    .stream()
                    .map(item -> function.apply(item))
                    .collect(toList());
            modifiedResourcesAmount = modifiedResources.size();
        } while (modifiedResourcesAmount > 0);
    }

    public static void addCommercetoolsCredentialValues(final Environment env, final Map<String, JobParameter> jobParametersMap) {
        final List<String> keys = asList("commercetools.projectKey", "commercetools.clientId", "commercetools.clientSecret", "commercetools.authUrl", "commercetools.apiUrl");
        for (final String key : keys) {
            final String value = env.getProperty(key);
            jobParametersMap.put(key, new JobParameter(value));
        }
    }
}
