package com.commercetools.dataimport.orders;

import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.orders.OrderImportDraft;
import io.sphere.sdk.orders.commands.OrderImportCommand;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class OrdersImportJobConfiguration extends DefaultCommercetoolsJobConfiguration {

    private volatile int counter = 0;

    @Bean
    public Job ordersCreateJob(final Step ordersImportStep) {
        return jobBuilderFactory.get("ordersImportJob")
                .start(ordersImportStep)
                .build();
    }

    @Bean
    public Step ordersImportStep(final ItemReader<OrderImportDraft> orderReader,
                                 final ItemWriter<OrderImportDraft> orderWriter) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("ordersImportStep");
        return stepBuilder
                .<OrderImportDraft, OrderImportDraft>chunk(1)
                .reader(orderReader)
                .writer(orderWriter)
                .build();
    }

    @Bean
    protected ItemWriter<OrderImportDraft> orderWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(item -> {

             sphereClient.executeBlocking(OrderImportCommand.of(item));

        });
    }


    @Bean
    @StepScope
    protected OrderImportItemReader orderReader(@Value("#{jobParameters['resource']}") final Resource orderCsvResource) {
        OrderImportItemReader reader = new OrderImportItemReader();
        reader.setResource(orderCsvResource);
        return reader;
    }
}
