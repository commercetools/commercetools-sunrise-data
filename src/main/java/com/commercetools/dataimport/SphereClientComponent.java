package com.commercetools.dataimport;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@PropertySource(value="classpath:credentials.properties")
public class SphereClientComponent {

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
    public BlockingSphereClient sphereClient() {
        final SphereClientConfig config = SphereClientConfig.of(projectKey, clientId, clientSecret, authUrl, apiUrl);
        final SphereClient asyncClient = SphereClientFactory.of().createClient(config);
        return BlockingSphereClient.of(asyncClient, 30, TimeUnit.SECONDS);
    }
}
