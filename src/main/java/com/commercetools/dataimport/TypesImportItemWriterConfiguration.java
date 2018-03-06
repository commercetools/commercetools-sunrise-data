package com.commercetools.dataimport;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TypesImportItemWriterConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypesImportItemWriterConfiguration.class);

    @Bean
    public ItemWriter<TypeDraft> typesImportWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(customType -> {
            final Type type = sphereClient.executeBlocking(TypeCreateCommand.of(customType));
            LOGGER.debug("Created type \"{}\" for {}", type.getKey(), type.getResourceTypeIds());
        });
    }

    @Bean
    public ItemWriter<Type> typesDeleteWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> {
            final Type type = sphereClient.executeBlocking(TypeDeleteCommand.of(item));
            LOGGER.debug("Removed type \"{}\" for {}", type.getKey(), type.getResourceTypeIds());
        });
    }
}
