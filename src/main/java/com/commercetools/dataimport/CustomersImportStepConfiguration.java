package com.commercetools.dataimport;

import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.CustomerGroupDraft;
import io.sphere.sdk.customergroups.commands.CustomerGroupCreateCommand;
import io.sphere.sdk.customergroups.commands.CustomerGroupDeleteCommand;
import io.sphere.sdk.customergroups.queries.CustomerGroupQuery;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.commands.CustomerDeleteCommand;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.concurrent.Future;

@Configuration
@Slf4j
public class CustomersImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private CtpBatch ctpBatch;

    @Value("${chunkSize}")
    private int chunkSize;

    @Value("${maxThreads}")
    private int maxThreads;

    @Value("${resource.customerType}")
    private Resource customerTypeResource;

    @Value("${resource.customerGroup}")
    private Resource customerGroupResource;

    @Bean
    public Step customerTypeImportStep() throws Exception {
        return stepBuilderFactory.get("customerTypeImportStep")
                .<TypeDraft, Future<TypeCreateCommand>>chunk(chunkSize)
                .reader(ctpBatch.jsonReader(customerTypeResource, TypeDraft.class))
                .processor(ctpBatch.asyncProcessor(TypeCreateCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step customerTypeDeleteStep() throws Exception {
        return stepBuilderFactory.get("customerTypeDeleteStep")
                .<Type, Future<TypeDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.typeQueryReader("customer"))
                .processor(ctpBatch.asyncProcessor(TypeDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step customerGroupImportStep() throws Exception {
        return stepBuilderFactory.get("customerGroupImportStep")
                .<CustomerGroupDraft, Future<CustomerGroupCreateCommand>>chunk(chunkSize)
                .reader(ctpBatch.jsonReader(customerGroupResource, CustomerGroupDraft.class))
                .processor(ctpBatch.asyncProcessor(CustomerGroupCreateCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step customerGroupDeleteStep() throws Exception {
        return stepBuilderFactory.get("customerGroupDeleteStep")
                .<CustomerGroup, Future<CustomerGroupDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.queryReader(CustomerGroupQuery.of()))
                .processor(ctpBatch.asyncProcessor(CustomerGroupDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step customerDeleteStep() throws Exception {
        return stepBuilderFactory.get("customerDeleteStep")
                .<Customer, Future<CustomerDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.queryReader(CustomerQuery.of()))
                .processor(ctpBatch.asyncProcessor(CustomerDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }
}
