package com.commercetools.dataimport;

import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.orders.queries.OrderQuery;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.types.queries.TypeQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
@ActiveProfiles({"ci", "it"})
public class ImportJobIntegrationTest {

    @Autowired
    private BlockingSphereClient sphereClient;

    @Test
    public void runs() {
        assertThat(fetchTotalCategories()).as("Categories are imported").isEqualTo(131);
        assertThat(fetchTotalProducts()).as("Products are imported").isEqualTo(2);
        assertThat(fetchTotalChannels()).as("Channels are imported").isEqualTo(18);
        assertThat(fetchTotalTypes("order")).as("Order types are imported").isEqualTo(1);
        assertThat(fetchTotalTypes("customer")).as("Customer types are imported").isEqualTo(1);
        assertThat(fetchTotalTypes("channel")).as("Channel types are imported").isEqualTo(1);
        assertThat(fetchTotalOrders()).as("Orders are imported").isEqualTo(2);
        assertThat(fetchTotalInventory()).as("Inventory is imported").isEqualTo(4);
        assertThat(fetchTotalProductTypes()).as("Product types are imported").isEqualTo(1);
    }

    private Long fetchTotalProductTypes() {
        return sphereClient.executeBlocking(ProductTypeQuery.of().withLimit(0)).getTotal();
    }

    private Long fetchTotalInventory() {
        return sphereClient.executeBlocking(InventoryEntryQuery.of().withLimit(0)).getTotal();
    }

    private Long fetchTotalCategories() {
        return sphereClient.executeBlocking(CategoryQuery.of().withLimit(0)).getTotal();
    }

    private Long fetchTotalProducts() {
        return sphereClient.executeBlocking(ProductQuery.of().withLimit(0)).getTotal();
    }

    private Long fetchTotalChannels() {
        return sphereClient.executeBlocking(ChannelQuery.of().withLimit(0)).getTotal();
    }

    private Long fetchTotalOrders() {
        return sphereClient.executeBlocking(OrderQuery.of().withLimit(0)).getTotal();
    }

    private Long fetchTotalTypes(final String resourceTypeId) {
        final TypeQuery query = TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList(resourceTypeId)))
                .withLimit(0);
        return sphereClient.executeBlocking(query).getTotal();
    }
}
