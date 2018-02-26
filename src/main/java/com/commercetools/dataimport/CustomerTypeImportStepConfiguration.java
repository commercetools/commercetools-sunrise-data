package com.commercetools.dataimport;

import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.queries.TypeQuery;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static java.util.Collections.singletonList;

@Configuration
public class CustomerTypeImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Bean
    @JobScope
    public Step customerTypeImportStep(ItemReader<TypeDraft> customerTypeImportStepReader, ItemWriter<TypeDraft> typeImportWriter) {
        return stepBuilderFactory.get("customerTypeImportStep")
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(customerTypeImportStepReader)
                .writer(typeImportWriter)
                .build();
    }

    @Bean
    @JobScope
    public Step customerTypeDeleteStep(final ItemWriter<Type> typeDeleteWriter) {
        return stepBuilderFactory.get("customerTypeDeleteStep")
                .<Type, Type>chunk(1)
                .reader(customerTypeDeleteStepReader())
                .writer(typeDeleteWriter)
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<TypeDraft> customerTypeImportStepReader(@Value("${resource.customerType}") Resource resource) throws IOException {
        return JsonUtils.createJsonListReader(resource, TypeDraft.class);
    }

    private ItemReader<Type> customerTypeDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList("customer"))));
    }
}
