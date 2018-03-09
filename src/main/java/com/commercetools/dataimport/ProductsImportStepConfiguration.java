package com.commercetools.dataimport;

import com.commercetools.dataimport.products.ProductImportItemProcessor;
import com.commercetools.dataimport.products.ProductImportItemReader;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.categories.CategoryTree;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import io.sphere.sdk.taxcategories.commands.TaxCategoryDeleteCommand;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

@Configuration
@Slf4j
public class ProductsImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private CtpResourceRepository ctpResourceRepository;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Value("${resource.productType}")
    private Resource productTypeResource;

    @Value("${resource.taxCategory}")
    private Resource taxCategoryResource;

    @Value("${resource.products}")
    private Resource productsResource;

    @Bean
    public Flow productsDeleteFlow() {
        return new FlowBuilder<Flow>("productsDeleteFlow")
                .start(productsUnpublishStep())
                .next(productsDeleteStep())
                .build();
    }

    private Step productsUnpublishStep() {
        return stepBuilderFactory.get("unpublishProductsStep")
                .<Product, Product>chunk(50)
                .reader(productsUnpublishStepReader())
                .writer(productsUnpublishStepWriter())
                .build();
    }

    private Step productsDeleteStep() {
        return stepBuilderFactory.get("productsDeleteStep")
                .<Product, Product>chunk(50)
                .reader(productsDeleteStepReader())
                .writer(productsDeleteStepWriter())
                .build();
    }

    @Bean
    public Step productTypeDeleteStep() {
        return stepBuilderFactory.get("productTypeDeleteStep")
                .<ProductType, ProductType>chunk(1)
                .reader(productTypeDeleteStepReader())
                .writer(productTypeDeleteStepWriter())
                .build();
    }

    @Bean
    public Step productTypeImportStep() throws IOException {
        return stepBuilderFactory.get("productTypeImportStep")
                .<ProductTypeDraft, ProductTypeDraft>chunk(1)
                .reader(productTypeImportStepReader())
                .writer(productTypeImportStepWriter())
                .build();
    }

    @Bean
    public Step taxCategoryImportStep() throws IOException {
        return stepBuilderFactory.get("taxCategoryImportStep")
                .<TaxCategoryDraft, TaxCategoryDraft>chunk(1)
                .reader(taxCategoryImportStepReader())
                .writer(taxCategoryImportStepWriter())
                .build();
    }

    @Bean
    public Step taxCategoryDeleteStep() {
        return stepBuilderFactory.get("taxCategoryDeleteStep")
                .<TaxCategory, TaxCategory>chunk(1)
                .reader(taxCategoryDeleteStepReader())
                .writer(taxCategoryDeleteStepWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step productsImportStep() {
        final CategoryTree categoryTree = ctpResourceRepository.fetchCategoryTree();
        return stepBuilderFactory.get("productsImportStep")
                .<List<FieldSet>, ProductDraft>chunk(1)
                .reader(new ProductImportItemReader(productsResource))
                .processor(new ProductImportItemProcessor(ctpResourceRepository, categoryTree))
                .writer(productsImportStepWriter())
                .build();
    }

    private ItemReader<ProductTypeDraft> productTypeImportStepReader() throws IOException {
        return JsonUtils.createJsonListReader(productTypeResource, ProductTypeDraft.class);
    }

    private ItemReader<TaxCategoryDraft> taxCategoryImportStepReader() throws IOException {
        return JsonUtils.createJsonListReader(taxCategoryResource, TaxCategoryDraft.class);
    }

    private ItemWriter<ProductTypeDraft> productTypeImportStepWriter() {
        return items -> items.forEach(draft -> {
            final ProductType productType = sphereClient.executeBlocking(ProductTypeCreateCommand.of(draft));
            log.debug("Created product type \"{}\"", productType.getName());
        });
    }

    private ItemWriter<TaxCategoryDraft> taxCategoryImportStepWriter() {
        return items -> items.forEach(draft -> {
            final TaxCategory taxCategory = sphereClient.executeBlocking(TaxCategoryCreateCommand.of(draft));
            log.debug("Created tax category \"{}\"", taxCategory.getName());
        });
    }

    private ItemReader<Product> productsUnpublishStepReader() {
        final ProductQuery query = ProductQuery.of().withPredicates(m -> m.masterData().isPublished().is(true));
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, query);
    }

    private ItemWriter<Product> productsUnpublishStepWriter() {
        return items -> items.forEach(item -> {
            final Product product = sphereClient.executeBlocking(ProductUpdateCommand.of(item, Unpublish.of()));
            log.debug("Unpublished product \"{}\"", product.getId());
        });
    }

    private ItemReader<Product> productsDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductQuery.of());
    }

    private ItemWriter<Product> productsDeleteStepWriter() {
        return items -> items.forEach(draft -> {
            final Product product = sphereClient.executeBlocking(ProductDeleteCommand.of(draft));
            log.debug("Removed product \"{}\"", product.getId());
        });
    }

    private ItemReader<ProductType> productTypeDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, ProductTypeQuery.of());
    }

    private ItemWriter<ProductType> productTypeDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final ProductType productType = sphereClient.executeBlocking(ProductTypeDeleteCommand.of(item));
            log.debug("Removed product type \"{}\"", productType.getName());
        });
    }

    private ItemReader<TaxCategory> taxCategoryDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, TaxCategoryQuery.of());
    }

    private ItemWriter<TaxCategory> taxCategoryDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final TaxCategory taxCategory = sphereClient.executeBlocking(TaxCategoryDeleteCommand.of(item));
            log.debug("Removed tax category \"{}\"", taxCategory.getName());
        });
    }

    private ItemWriter<ProductDraft> productsImportStepWriter() {
        return items -> items.forEach(draft -> {
            final Product product = sphereClient.executeBlocking(ProductCreateCommand.of(draft));
            log.debug("Created product \"{}\"", product.getId());
        });
    }
}
