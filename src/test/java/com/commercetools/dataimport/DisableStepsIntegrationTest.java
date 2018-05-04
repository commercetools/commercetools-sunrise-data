package com.commercetools.dataimport;

import io.sphere.sdk.projects.Project;
import io.sphere.sdk.projects.queries.ProjectGet;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Java6Assertions.assertThat;

@TestPropertySource(properties = {
        "catalogImport=false",
        "ordersImport=false",
        "reserveInStore=false",
        "channelsImport=false"
})
public class DisableStepsIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void disables() {
        assertThat(fetchTotalProducts()).as("Products are not imported").isZero();
        assertThat(fetchTotalChannels()).as("Channels are not imported").isZero();
        assertThat(fetchTotalOrders()).as("Orders are imported").isZero();
        assertThat(fetchTotalInventory()).as("Inventory is imported").isZero();

        final Project project = sphereClient.executeBlocking(ProjectGet.of());
        assertThat(project.getCountries()).hasSize(2);
        assertThat(project.getCurrencies()).hasSize(1);
        assertThat(project.getLanguages()).hasSize(2);
    }
}
