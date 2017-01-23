package com.commercetools.dataimport.joyrideavailability;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.queries.TypeQuery;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

import static com.commercetools.dataimport.joyrideavailability.JoyrideAvailabilityUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@NotThreadSafe
public abstract class JoyrideAvailabilityIntegrationTest {

    @Autowired
    @Qualifier("test")
    protected BlockingSphereClient sphereClient;

    private static final Logger logger = LoggerFactory.getLogger(JoyrideAvailabilityIntegrationTest.class);

    @Before
    public void setUp() throws Exception {

        unpublishProducts(sphereClient);
        final List<ProductProjection> publishedProducts = sphereClient.executeBlocking(ProductProjectionQuery.ofCurrent()).getResults();
        assertThat(publishedProducts).hasSize(0);
        logger.info("All products have been unpublised.");

        deleteProducts(sphereClient);
        final List<ProductProjection> stagedProducts = sphereClient.executeBlocking(ProductProjectionQuery.ofStaged()).getResults();
        assertThat(stagedProducts).hasSize(0);
        logger.info("All products have been deleted.");

        deleteInventoryEntries(sphereClient);
        final List<InventoryEntry> inventoryEntries = sphereClient.executeBlocking(InventoryEntryQuery.of()).getResults();
        assertThat(inventoryEntries).hasSize(0);
        logger.info("All inventory entries have been deleted.");

        deleteChannels(sphereClient);
        final List<Channel> channels = sphereClient.executeBlocking(ChannelQuery.of()).getResults();
        assertThat(channels).hasSize(0);
        logger.info("All channels have been deleted.");

        deleteTypes(sphereClient);
        final List<Type> types = sphereClient.executeBlocking(TypeQuery.of()).getResults();
        assertThat(types).hasSize(0);
        logger.info("All types have been deleted.");
    }
}
