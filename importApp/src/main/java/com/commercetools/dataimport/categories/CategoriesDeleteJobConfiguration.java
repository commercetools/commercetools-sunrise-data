package com.commercetools.dataimport.categories;

import com.commercetools.dataimport.commercetools.CommercetoolsConfig;
import com.commercetools.dataimport.commercetools.CommercetoolsJobConfiguration;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.models.Versioned;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class CategoriesDeleteJobConfiguration extends CommercetoolsJobConfiguration {

    @Bean
    public Job categoriesDeleteJob() {
        return jobBuilderFactory.get("categoriesDeleteJob")
                .start(deleteRootCategories())
                .next(deleteRemainingCategories())
                .build();
    }

    protected Step deleteRemainingCategories() {
        return stepBuilderFactory.get("deleteRemainingCategoriesStep")
                .<Category, Category>chunk(1)
                .reader(ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of()))
                .writer(categoryDeleteWriter())
                .build();
    }

    protected Step deleteRootCategories() {
        return stepBuilderFactory.get("deleteRootCategoriesStep")
                .<Category, Category>chunk(1)
                .reader(ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of().byIsRoot()))
                .writer(categoryDeleteWriter())
                .build();
    }

    private ItemWriter<Versioned<Category>> categoryDeleteWriter() {
        return items -> items.forEach(item ->
                sphereClient.executeBlocking(CategoryDeleteCommand.of(item)));
    }

    public static void main(String [] args) {
        final Object[] sources = {CommercetoolsConfig.class, CategoriesDeleteJobConfiguration.class};
        System.exit(SpringApplication.exit(SpringApplication.run(sources, args)));
    }
}
