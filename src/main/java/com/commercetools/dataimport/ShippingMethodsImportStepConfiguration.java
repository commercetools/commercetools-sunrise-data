package com.commercetools.dataimport;

import io.sphere.sdk.shippingmethods.ShippingMethod;
import io.sphere.sdk.shippingmethods.commands.ShippingMethodDeleteCommand;
import io.sphere.sdk.shippingmethods.queries.ShippingMethodQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Future;

@Configuration
@Slf4j
public class ShippingMethodsImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private CtpBatch ctpBatch;

    @Value("${chunkSize}")
    private int chunkSize;

    @Value("${maxThreads}")
    private int maxThreads;

    @Bean
    public Step shippingMethodsDeleteStep() throws Exception {
        return stepBuilderFactory.get("shippingMethodsDeleteStep")
                .<ShippingMethod, Future<ShippingMethodDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.queryReader(ShippingMethodQuery.of()))
                .processor(ctpBatch.asyncProcessor(ShippingMethodDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }
}
