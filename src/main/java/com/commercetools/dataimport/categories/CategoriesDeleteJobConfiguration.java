package com.commercetools.dataimport.categories;

import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class CategoriesDeleteJobConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Bean
    public Job categoriesDeleteJob() {
        return jobBuilderFactory.get("categoriesDeleteJob")
                .start(rootCategoriesDeleteStep())
                .next(remainingCategoriesDeleteStep())
                .build();
    }

    @Bean
    public Step rootCategoriesDeleteStep() {
        return stepBuilderFactory.get("rootCategoriesDeleteStep")
                .<Category, Category>chunk(1)
                .reader(rootReader())
                .writer(writer())
                .build();
    }

    @Bean
    public Step remainingCategoriesDeleteStep() {
        return stepBuilderFactory.get("remainingCategoriesDeleteStep")
                .<Category, Category>chunk(1)
                .reader(remainingReader())
                .writer(writer())
                .build();
    }

    private ItemStreamReader<Category> rootReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of().byIsRoot());
    }

    private ItemStreamReader<Category> remainingReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of());
    }

    private ItemWriter<Category> writer() {
        return items -> items.forEach(item -> sphereClient.executeBlocking(CategoryDeleteCommand.of(item)));
    }
}
