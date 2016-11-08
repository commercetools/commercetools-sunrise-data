package com.commercetools.dataimport.joyrideavailability;

import io.sphere.sdk.client.BlockingSphereClient;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static com.commercetools.dataimport.joyrideavailability.JoyrideAvailabilityUtils.*;

public abstract class JoyrideAvailabilityIntegrationTest {

    @Autowired
    @Qualifier("test")
    protected BlockingSphereClient sphereClient;

    @Before
    public void setUp() throws Exception {
        unpublishProducts(sphereClient);
        deleteProducts(sphereClient);
        deleteInventoryEntries(sphereClient);
        deleteChannels(sphereClient);
    }
}
