package com.commercetools.dataimport;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class SphereClientConfiguration {

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

    @Value("${ctp.protocol}")
    private String protocol;

    @Bean(destroyMethod = "close")
    public BlockingSphereClient sphereClient() {
        final SphereClientConfig config = SphereClientConfig.of(projectKey, clientId, clientSecret, protocol + "://" + authUrl, protocol + "://" + apiUrl);
        final SphereClient asyncClient = SphereClientFactory.of().createClient(config);
        final BlockingSphereClient sphereClient = BlockingSphereClient.of(asyncClient, 30, TimeUnit.SECONDS);
        log.debug("Created CTP client for project \"{}\"", projectKey);
        return sphereClient;
    }
}
