package com.commercetools.demo.dataimport.categories;

import com.commercetools.demo.dataimport.commercetools.CommercetoolsConfig;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Referenceable;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Locale;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class CategoriesImportJobConfiguration {

    public static class LocalizedField {
        private String de;
        private String en;
        private String it;

        public LocalizedField() {
        }

        public String getDe() {
            return de;
        }

        public void setDe(final String de) {
            this.de = de;
        }

        public String getEn() {
            return en;
        }

        public void setEn(final String en) {
            this.en = en;
        }

        public String getIt() {
            return it;
        }

        public void setIt(final String it) {
            this.it = it;
        }
    }

    public static class CategoryCsvLineValue {//should be serializable?
        private LocalizedField name = new LocalizedField();
        private LocalizedField slug = new LocalizedField();
        private String parentId;
        private String externalId;

        public CategoryCsvLineValue() {
        }

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(final String externalId) {
            this.externalId = externalId;
        }

        public LocalizedField getName() {
            return name;
        }

        public void setName(final LocalizedField name) {
            this.name = name;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(final String parentId) {
            this.parentId = parentId;
        }

        public LocalizedField getSlug() {
            return slug;
        }

        public void setSlug(final LocalizedField slug) {
            this.slug = slug;
        }
    }

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Autowired
    private Resource categoryCsvResource;

    @Bean
    public Job importCategories(final Step importStep) {
        return jobBuilderFactory.get("categoriesImportJob")
                .start(importStep)
                .build();
    }

    @Bean
    public Step importStep(final ItemReader<CategoryCsvLineValue> categoryReader,
                           final ItemProcessor<CategoryCsvLineValue, CategoryDraft> categoryProcessor,
                           final ItemWriter<CategoryDraft> categoryWriter) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("importStep");
        final SimpleStepBuilder<CategoryCsvLineValue, CategoryDraft> chunk = stepBuilder
                .chunk(1);
        return chunk
                .reader(categoryReader)
                .processor(categoryProcessor)
                .writer(categoryWriter)
                .build();
    }

    @Bean
    public ItemWriter<CategoryDraft> categoryWriter() {
        return new AbstractItemStreamItemWriter<CategoryDraft>() {
            @Override
            public void write(final List<? extends CategoryDraft> items) throws Exception {
                items.forEach(draft -> sphereClient.executeBlocking(CategoryCreateCommand.of(draft)));
            }
        };
    }

    @Bean
    public ItemProcessor<CategoryCsvLineValue, CategoryDraft> categoryProcessor() {
        return item -> {
            final LocalizedString name = importLocalizedField(item.getName());
            final LocalizedString slug = importLocalizedField(item.getSlug());
            final String externalId = item.getExternalId();
            final String externalParentId = item.getParentId();
            final Referenceable<Category> parent = externalParentId == null
                    ? null
                    : sphereClient.executeBlocking(CategoryQuery.of().byExternalId(externalParentId)).head().orElse(null);
            return CategoryDraftBuilder.of(name, slug).externalId(externalId).parent(parent).build();
        };
    }

    private LocalizedString importLocalizedField(final LocalizedField localizedField) {
        return LocalizedString.of(Locale.GERMAN, localizedField.getDe())
                .plus(Locale.ENGLISH, localizedField.getEn())
                .plus(Locale.ITALIAN, localizedField.getIt());
    }

    @Bean
    public ItemReader<CategoryCsvLineValue> categoryReader() {
        FlatFileItemReader<CategoryCsvLineValue> reader = new FlatFileItemReader<>();
        reader.setResource(categoryCsvResource);
        reader.setLineMapper(new DefaultLineMapper<CategoryCsvLineValue>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(new String[] {"externalId", "name.de", "slug.de", "name.en", "slug.en", "name.it", "slug.it", "parentId"});
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<CategoryCsvLineValue>() {{
                setTargetType(CategoryCsvLineValue.class);

            }});

        }});
        reader.setLinesToSkip(1);
        return reader;
    }

    @Configuration
    public static class MainConfiguration {//TODO improve
        public MainConfiguration() {
        }

        @Bean Resource categoryCsvResource() {
            return new FileSystemResource("../categories/sunrise-categories.csv");
        }

    }

    public static void main(String [] args) {
        final Object[] sources = {CommercetoolsConfig.class, CategoriesImportJobConfiguration.class, MainConfiguration.class};
        System.exit(SpringApplication.exit(SpringApplication.run(sources, args)));
    }
}
