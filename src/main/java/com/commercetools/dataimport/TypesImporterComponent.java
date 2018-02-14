package com.commercetools.dataimport;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TypesImporterComponent {

    @Bean
    @StepScope
    public ItemReader<TypeDraft> typesImportReader(@Value("#{jobParameters['resource']}") final Resource resource) throws IOException {
        return JsonUtils.createJsonListReader(resource, TypeDraft.class);
    }

    @Bean
    public ItemWriter<TypeDraft> typesImportWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(customType -> sphereClient.executeBlocking(TypeCreateCommand.of(customType)));
    }
}
