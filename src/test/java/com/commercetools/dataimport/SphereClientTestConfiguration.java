package com.commercetools.dataimport;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClientFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

@Configuration
@PropertySource(value = "classpath:test.properties")
@TestPropertySource(value = "classpath:test.properties")
public class SphereClientTestConfiguration {

    @Value("${ctp.projectKey}")
    private String projectKey;

    @Value("${ctp.clientId}")
    private String clientId;

    @Value("${ctp.clientSecret}")
    private String clientSecret;

    @Value("${ctp.authUrl}")
    private String authUrl;

    @Value("${ctp.apiUrl}")
    private String apiUrl;

    @Bean(destroyMethod = "close")
    @Qualifier("test")
    public BlockingSphereClient sphereClient() {
        final SphereClientConfig config = SphereClientConfig.of(projectKey, clientId, clientSecret, authUrl, apiUrl);
        final SphereClient asyncClient = SphereClientFactory.of().createClient(config);
        return BlockingSphereClient.of(asyncClient, 30, TimeUnit.SECONDS);
    }
}