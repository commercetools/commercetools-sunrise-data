package com.commercetools.dataimport;

import io.sphere.sdk.projects.Project;
import io.sphere.sdk.projects.queries.ProjectGet;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class ImportJobIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void runs() {
        assertThat(fetchTotalCategories()).as("Categories are imported").isEqualTo(131);
        assertThat(fetchTotalTypes("order")).as("Order types are imported").isEqualTo(1);
        assertThat(fetchTotalTypes("customer")).as("Customer types are imported").isEqualTo(1);
        assertThat(fetchTotalTypes("channel")).as("Channel types are imported").isEqualTo(1);
        assertThat(fetchTotalOrders()).as("Orders are imported").isEqualTo(2);
        assertThat(fetchTotalProductTypes()).as("Product types are imported").isEqualTo(1);
        assertThat(fetchTotalProducts()).as("Products are imported").isEqualTo(2);
        assertThat(fetchTotalChannels()).as("Channels are imported").isEqualTo(18);
        assertThat(fetchTotalInventory()).as("Inventory is imported").isEqualTo(4);

        final Project project = sphereClient.executeBlocking(ProjectGet.of());
        assertThat(project.getCountries()).hasSize(2);
        assertThat(project.getCurrencies()).hasSize(1);
        assertThat(project.getLanguages()).hasSize(2);
    }
}
