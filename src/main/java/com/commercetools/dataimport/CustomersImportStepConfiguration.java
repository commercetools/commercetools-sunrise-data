package com.commercetools.dataimport;

import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.CustomerGroupDraft;
import io.sphere.sdk.customergroups.commands.CustomerGroupCreateCommand;
import io.sphere.sdk.customergroups.commands.CustomerGroupDeleteCommand;
import io.sphere.sdk.customergroups.queries.CustomerGroupQuery;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.queries.TypeQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static java.util.Collections.singletonList;

@Configuration
@Slf4j
public class CustomersImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Value("${resource.customerType}")
    private Resource customerTypeResource;

    @Value("${resource.customerGroup}")
    private Resource customerGroupResource;

    @Bean
    public Step customerTypeImportStep(ItemWriter<TypeDraft> typeImportWriter) throws IOException {
        return stepBuilderFactory.get("customerTypeImportStep")
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(customerTypeImportStepReader())
                .writer(typeImportWriter)
                .build();
    }

    @Bean
    public Step customerTypeDeleteStep(final ItemWriter<Type> typeDeleteWriter) {
        return stepBuilderFactory.get("customerTypeDeleteStep")
                .<Type, Type>chunk(1)
                .reader(customerTypeDeleteStepReader())
                .writer(typeDeleteWriter)
                .build();
    }

    @Bean
    public Step customerGroupImportStep() throws IOException {
        return stepBuilderFactory.get("customerGroupImportStep")
                .<CustomerGroupDraft, CustomerGroupDraft>chunk(1)
                .reader(customerGroupImportStepReader())
                .writer(customerGroupImportStepWriter())
                .build();
    }

    @Bean
    public Step customerGroupDeleteStep() {
        return stepBuilderFactory.get("customerGroupDeleteStep")
                .<CustomerGroup, CustomerGroup>chunk(1)
                .reader(customerGroupDeleteStepReader())
                .writer(customerGroupDeleteStepWriter())
                .build();
    }

    private ItemReader<TypeDraft> customerTypeImportStepReader() throws IOException {
        return JsonUtils.createJsonListReader(customerTypeResource, TypeDraft.class);
    }

    private ItemReader<Type> customerTypeDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList("customer"))));
    }

    private ItemReader<CustomerGroupDraft> customerGroupImportStepReader() throws IOException {
        return JsonUtils.createJsonListReader(customerGroupResource, CustomerGroupDraft.class);
    }

    private ItemWriter<CustomerGroupDraft> customerGroupImportStepWriter() {
        return items -> items.forEach(draft -> {
            final CustomerGroup customerGroup = sphereClient.executeBlocking(CustomerGroupCreateCommand.of(draft));
            log.debug("Created customer group \"{}\"", customerGroup.getName());
        });
    }

    private ItemReader<CustomerGroup> customerGroupDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CustomerGroupQuery.of());
    }

    private ItemWriter<CustomerGroup> customerGroupDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final CustomerGroup customerGroup = sphereClient.executeBlocking(CustomerGroupDeleteCommand.of(item));
            log.debug("Removed tax category \"{}\"", customerGroup.getName());
        });
    }
}
