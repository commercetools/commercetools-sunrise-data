package com.commercetools.dataimport;

import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.types.queries.TypeQuery;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Collections.singletonList;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
@ActiveProfiles({"ci", "it"})
public class AbstractIntegrationTest {

    @Autowired
    protected BlockingSphereClient sphereClient;

    protected Long fetchTotalProductTypes() {
        return sphereClient.executeBlocking(ProductTypeQuery.of().withLimit(0)).getTotal();
    }

    protected Long fetchTotalInventory() {
        return sphereClient.executeBlocking(InventoryEntryQuery.of().withLimit(0)).getTotal();
    }

    protected Long fetchTotalCategories() {
        return sphereClient.executeBlocking(CategoryQuery.of().withLimit(0)).getTotal();
    }

    protected Long fetchTotalProducts() {
        return sphereClient.executeBlocking(ProductQuery.of().withLimit(0)).getTotal();
    }

    protected Long fetchTotalChannels() {
        return sphereClient.executeBlocking(ChannelQuery.of().withLimit(0)).getTotal();
    }

    protected Long fetchTotalOrders() {
        return sphereClient.executeBlocking(OrderQuery.of().withLimit(0)).getTotal();
    }

    protected Long fetchTotalTypes(final String resourceTypeId) {
        final TypeQuery query = TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList(resourceTypeId)))
                .withLimit(0);
        return sphereClient.executeBlocking(query).getTotal();
    }
}
