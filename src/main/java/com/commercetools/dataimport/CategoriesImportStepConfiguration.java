package com.commercetools.dataimport;

import com.commercetools.dataimport.categories.CategoryCsvEntry;
import com.commercetools.dataimport.categories.CategoryImportItemProcessor;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.concurrent.Future;

@Configuration
@Slf4j
public class CategoriesImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private CtpBatch ctpBatch;

    @Value("${chunkSize}")
    private int chunkSize;

    @Value("${maxThreads}")
    private int maxThreads;

    @Value("${resource.categories}")
    private Resource categoriesResource;

    @Value("${headers.categories}")
    private String[] categoriesHeaders;

    @Bean
    public Flow categoriesDeleteFlow() throws Exception {
        return new FlowBuilder<Flow>("categoriesDeleteFlow")
                .start(rootCategoriesDeleteStep())
                .next(remainingCategoriesDeleteStep())
                .build();
    }

    private Step rootCategoriesDeleteStep() throws Exception {
        return stepBuilderFactory.get("rootCategoriesDeleteStep")
                .<Category, Future<CategoryDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.queryReader(CategoryQuery.of().byIsRoot()))
                .processor(ctpBatch.asyncProcessor(CategoryDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    private Step remainingCategoriesDeleteStep() throws Exception {
        return stepBuilderFactory.get("remainingCategoriesDeleteStep")
                .<Category, Future<CategoryDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.queryReader(CategoryQuery.of()))
                .processor(ctpBatch.asyncProcessor(CategoryDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step categoriesImportStep() throws Exception {
        return stepBuilderFactory.get("categoriesImportStep")
                .<CategoryCsvEntry, Future<CategoryCreateCommand>>chunk(chunkSize)
                .reader(ctpBatch.csvReader(categoriesResource, categoriesHeaders, CategoryCsvEntry.class))
                .processor(ctpBatch.asyncProcessor(new CategoryImportItemProcessor()))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }
}
