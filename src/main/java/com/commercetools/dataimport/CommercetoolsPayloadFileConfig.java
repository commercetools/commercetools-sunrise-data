package com.commercetools.dataimport;

import io.sphere.sdk.client.BlockingSphereClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class CommercetoolsPayloadFileConfig {
//    @Value("${payloadFile}")
//    private String payloadFile;

    @Bean(destroyMethod = "close")
    public BlockingSphereClient client() throws IOException {
//        //TODO another workaround to fix
//        final String payloadFileWithEnvFallback = Optional.ofNullable(payloadFile)
//                .filter(s -> !s.startsWith("$"))
//                .orElseGet(() -> System.getenv("PAYLOAD_FILE"));
//        System.err.println("payloadFileWithEnvFallback " + payloadFileWithEnvFallback);
//        final ObjectNode commercetools = (ObjectNode) payloadJson.get("commercetools");
//        final String projectKey = commercetools.get("projectKey").asText();
//        final String clientId = commercetools.get("clientId").asText();
//        final String clientSecret = commercetools.get("clientSecret").asText();
//        final String authUrl = commercetools.get("authUrl").asText();
//        final String apiUrl = commercetools.get("apiUrl").asText();
//        final SphereClientConfig config = SphereClientConfig.of(projectKey, clientId, clientSecret, authUrl, apiUrl);
//        final SphereClient asyncClient = SphereClientFactory.of()
//                .createClient(config);
//        return BlockingSphereClient.of(asyncClient, 5, TimeUnit.SECONDS);
        return null;
    }
}
