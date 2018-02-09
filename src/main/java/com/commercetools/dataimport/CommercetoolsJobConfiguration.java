package com.commercetools.dataimport;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClientFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@EnableBatchProcessing
public abstract class CommercetoolsJobConfiguration {

    @Autowired
    protected JobBuilderFactory jobBuilderFactory;

    @Autowired
    protected StepBuilderFactory stepBuilderFactory;

    @Bean(destroyMethod = "close")
    @JobScope
    protected BlockingSphereClient sphereClient(
            @Value("#{jobParameters['commercetools.projectKey']}") final String projectKey,
            @Value("#{jobParameters['commercetools.clientId']}") final String clientId,
            @Value("#{jobParameters['commercetools.clientSecret']}") final String clientSecret,
            @Value("#{jobParameters['commercetools.authUrl']}") final String authUrl,
            @Value("#{jobParameters['commercetools.apiUrl']}") final String apiUrl
    ) {
        final SphereClientConfig config = SphereClientConfig.of(projectKey, clientId, clientSecret, authUrl, apiUrl);
        final SphereClient asyncClient = SphereClientFactory.of().createClient(config);
        return BlockingSphereClient.of(asyncClient, 30, TimeUnit.SECONDS);
    }
}
