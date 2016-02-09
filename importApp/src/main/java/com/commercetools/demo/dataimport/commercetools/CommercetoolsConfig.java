package com.commercetools.demo.dataimport.commercetools;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CommercetoolsConfig {
    @Value("${commercetools.projectKey}")
    private String projectKey;
    @Value("${commercetools.clientId}")
    private String clientId;
    @Value("${commercetools.clientSecret}")
    private String clientSecret;
    @Value("${commercetools.authUrl}")
    private String authUrl;
    @Value("${commercetools.apiUrl}")
    private String apiUrl;

    @Bean(destroyMethod = "close")
    public BlockingSphereClient client() {
        final SphereClientConfig config = SphereClientConfig.of(projectKey, clientId, clientSecret, authUrl, apiUrl);
        final SphereClient asyncClient = SphereClientFactory.of()
                .createClient(config);
        return BlockingSphereClient.of(asyncClient, 5, TimeUnit.SECONDS);
    }
}
