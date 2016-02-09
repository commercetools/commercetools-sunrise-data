package com.commercetools.demo.dataimport.commercetools;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CommercetoolsConfig {
    @Bean(destroyMethod = "close")
    public BlockingSphereClient client() {
        final SphereClient asyncClient = SphereClientFactory.of()
                .createClient("", "", "");//TODO
        return BlockingSphereClient.of(asyncClient, 5, TimeUnit.SECONDS);
    }
}
