package com.commercetools.dataimport;

import com.commercetools.dataimport.categories.CategoryCsvEntry;
import com.commercetools.dataimport.categories.CategoryImportItemProcessor;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@Slf4j
public class CategoriesImportStepConfiguration {

    private static final String[] CATEGORY_CSV_HEADER_NAMES = new String[]{"key", "externalId", "name.de", "slug.de", "name.en", "slug.en", "parentId", "webImageUrl", "iosImageUrl"};
    private static final int CHUNK_SIZE = 100;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Value("${resource.categories}")
    private Resource categoriesResource;

    @Bean
    @JobScope
    public Step rootCategoriesDeleteStep() {
        return stepBuilderFactory.get("rootCategoriesDeleteStep")
                .<Category, Category>chunk(1)
                .reader(rootCategoriesDeleteStepReader())
                .chunk(CHUNK_SIZE)
                .writer(categoriesDeleteStepWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step remainingCategoriesDeleteStep() {
        return stepBuilderFactory.get("remainingCategoriesDeleteStep")
                .<Category, Category>chunk(1)
                .reader(remainingCategoriesDeleteStepReader())
                .chunk(CHUNK_SIZE)
                .writer(categoriesDeleteStepWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step categoriesImportStep() {
        return stepBuilderFactory.get("categoriesImportStep")
                .<CategoryCsvEntry, CategoryDraft>chunk(1)
                .reader(categoriesImportStepReader())
                .processor(new CategoryImportItemProcessor())
                .writer(categoriesImportStepWriter())
                .build();
    }

    private ItemReader<Category> rootCategoriesDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of().byIsRoot().withLimit(CHUNK_SIZE));
    }

    private ItemReader<Category> remainingCategoriesDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, CategoryQuery.of().withLimit(CHUNK_SIZE));
    }

    private ItemWriter<Category> categoriesDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final Category category = sphereClient.executeBlocking(CategoryDeleteCommand.of(item));
            log.debug("Removed category \"{}\"", category.getKey());
        });
    }

    private ItemReader<CategoryCsvEntry> categoriesImportStepReader() {
        final FlatFileItemReader<CategoryCsvEntry> reader = new FlatFileItemReader<>();
        reader.setResource(categoriesResource);
        reader.setLineMapper(new DefaultLineMapper<CategoryCsvEntry>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(CATEGORY_CSV_HEADER_NAMES);
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<CategoryCsvEntry>() {{
                setTargetType(CategoryCsvEntry.class);
            }});
        }});
        reader.setLinesToSkip(1);
        return reader;
    }

    private ItemWriter<CategoryDraft> categoriesImportStepWriter() {
        return items -> items.forEach(draft -> {
            final Category category = sphereClient.executeBlocking(CategoryCreateCommand.of(draft));
            log.debug("Created category \"{}\"", category.getExternalId());
        });
    }
}
