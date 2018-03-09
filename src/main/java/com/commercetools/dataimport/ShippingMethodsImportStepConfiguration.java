package com.commercetools.dataimport;

import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.shippingmethods.ShippingMethod;
import io.sphere.sdk.shippingmethods.commands.ShippingMethodDeleteCommand;
import io.sphere.sdk.shippingmethods.queries.ShippingMethodQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ShippingMethodsImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Bean
    public Step shippingMethodsDeleteStep() {
        return stepBuilderFactory.get("shippingMethodsDeleteStep")
                .<ShippingMethod, ShippingMethod>chunk(1)
                .reader(shippingMethodsDeleteStepReader())
                .writer(shippingMethodsDeleteStepWriter())
                .build();
    }

    private ItemReader<ShippingMethod> shippingMethodsDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ShippingMethodQuery.of());
    }

    private ItemWriter<ShippingMethod> shippingMethodsDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final ShippingMethod shippingMethod = sphereClient.executeBlocking(ShippingMethodDeleteCommand.of(item));
            log.debug("Removed shipping method \"{}\"", shippingMethod.getKey());
        });
    }
}
