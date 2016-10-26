package com.commercetools.dataimport.commercetools;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CommercetoolsConfiguration {

    @Bean(destroyMethod = "close")
    protected BlockingSphereClient sphereClient(
            @Value("${commercetools.projectKey}") final String projectKey,
            @Value("${commercetools.clientId}")  final String clientId,
            @Value("${commercetools.clientSecret}")  final String clientSecret,
            @Value("${commercetools.authUrl}")  final String authUrl,
            @Value("${commercetools.apiUrl}")  final String apiUrl
    ) throws IOException  {
        final SphereClientConfig config = SphereClientConfig.of(projectKey, clientId, clientSecret, authUrl, apiUrl);
        final SphereClient asyncClient = SphereClientFactory.of().createClient(config);
        return BlockingSphereClient.of(asyncClient, 20, TimeUnit.SECONDS);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
