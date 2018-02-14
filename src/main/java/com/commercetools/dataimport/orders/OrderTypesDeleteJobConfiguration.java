package com.commercetools.dataimport.orders;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.singletonList;

@Configuration
public class OrderTypesDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job orderTypesDeleteJob(final Step orderTypesDeleteStep) {
        return jobBuilderFactory.get("orderTypesDeleteJob")
                .start(orderTypesDeleteStep)
                .build();
    }

    @Bean
    public Step orderTypesDeleteStep(final ItemReader<Type> orderTypesDeleteReader,
                                       final ItemWriter<Type> orderTypesDeleteWriter) {
        return stepBuilderFactory.get("orderTypesDeleteStep")
                .<Type, Type>chunk(1)
                .reader(orderTypesDeleteReader)
                .writer(orderTypesDeleteWriter)
                .build();
    }

    @Bean
    public ItemReader<Type> orderTypesDeleteReader(final BlockingSphereClient sphereClient) {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList("order"))));
    }

    @Bean
    public ItemWriter<Type> orderTypesDeleteWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> sphereClient.executeBlocking(TypeDeleteCommand.of(item)));
    }
}
