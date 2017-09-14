package com.commercetools.dataimport;

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
import io.sphere.sdk.orders.commands.OrderDeleteCommand;
import io.sphere.sdk.orders.queries.OrderQuery;
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
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class TestUtils {

    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

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
        logger.info("Deleting inventory entries ...");
        updateOrDeleteResources(sphereClient, InventoryEntryQuery.of(), (inventoryEntry) -> sphereClient.executeBlocking(InventoryEntryDeleteCommand.of(inventoryEntry)));
    }

    public static void deleteChannels(final BlockingSphereClient sphereClient) {
        logger.info("Deleting channels ...");
        updateOrDeleteResources(sphereClient, ChannelQuery.of(), (channel) -> sphereClient.executeBlocking(ChannelDeleteCommand.of(channel)));
    }

    public static void deleteTypes(final BlockingSphereClient sphereClient) {
        logger.info("Deleting types ...");
        updateOrDeleteResources(sphereClient, TypeQuery.of(), (type) -> sphereClient.executeBlocking(TypeDeleteCommand.of(type)));
    }

    public static void unpublishProducts(final BlockingSphereClient sphereClient) {
        logger.info("Unpublishing products ...");
        updateOrDeleteResources(sphereClient, ProductProjectionQuery.ofCurrent(), (item) -> {
            final ProductUpdateCommand updateCommand = ProductUpdateCommand.of(item, Unpublish.of());
            return sphereClient.executeBlocking(updateCommand);
        });
    }

    public static void deleteProducts(final BlockingSphereClient sphereClient) {
        logger.info("Deleting products ...");
        updateOrDeleteResources(sphereClient, ProductProjectionQuery.ofStaged(), (item) -> sphereClient.executeBlocking(ProductDeleteCommand.of(item)));
    }

    public static void deleteOrders(final BlockingSphereClient sphereClient) {
        logger.info("Deleting orders ...");
        updateOrDeleteResources(sphereClient, OrderQuery.of(), (item) -> sphereClient.executeBlocking(OrderDeleteCommand.of(item)));
    }

    public static <T, S> void updateOrDeleteResources(final BlockingSphereClient sphereClient, final Query<T> query, final Function<T, S> function) {
        long resourceCount;
        long processedItems = 0L;
        long totalItems = sphereClient.executeBlocking(query).getTotal();
        logger.info("Items to process: " + totalItems);
        do {
            final PagedQueryResult<T> pagedQueryResult = sphereClient.executeBlocking(query);
            resourceCount = pagedQueryResult.getCount();
            pagedQueryResult.getResults().forEach(item -> function.apply(item));
            if (resourceCount > 0) {
                processedItems += resourceCount;
                logger.info("Processed " + processedItems + " of " + totalItems);
            }
        } while (resourceCount > 0);
    }

    public static void addCommercetoolsCredentialValues(final Environment env, final Map<String, JobParameter> jobParametersMap) {
        final List<String> keys = asList("commercetools.projectKey", "commercetools.clientId", "commercetools.clientSecret", "commercetools.authUrl", "commercetools.apiUrl");
        for (final String key : keys) {
            final String value = env.getProperty(key);
            jobParametersMap.put(key, new JobParameter(value));
        }
    }
}
