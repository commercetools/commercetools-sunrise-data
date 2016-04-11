package com.commercetools.dataimport.commercetools;

import com.commercetools.dataimport.all.PayloadJobMain;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
public class CommercetoolsPayloadFileConfig {
    @Value("${payloadFile}")
    private String payloadFile;

    @Bean(destroyMethod = "close")
    public BlockingSphereClient client() throws IOException {
        final JsonNode payloadJson = PayloadJobMain.parsePayloadFile(payloadFile);
        final ObjectNode commercetools = (ObjectNode) payloadJson.get("commercetools");
        final String projectKey = commercetools.get("projectKey").asText();
        final String clientId = commercetools.get("clientId").asText();
        final String clientSecret = commercetools.get("clientSecret").asText();
        final String authUrl = commercetools.get("authUrl").asText();
        final String apiUrl = commercetools.get("apiUrl").asText();
        final SphereClientConfig config = SphereClientConfig.of(projectKey, clientId, clientSecret, authUrl, apiUrl);
        final SphereClient asyncClient = SphereClientFactory.of()
                .createClient(config);
        return BlockingSphereClient.of(asyncClient, 5, TimeUnit.SECONDS);
    }
}
