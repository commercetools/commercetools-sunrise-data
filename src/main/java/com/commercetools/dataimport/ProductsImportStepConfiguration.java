package com.commercetools.dataimport;

import com.commercetools.dataimport.products.ProductDeleteItemProcessor;
import com.commercetools.dataimport.products.ProductImportItemProcessor;
import com.commercetools.dataimport.products.ProductImportItemReader;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
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
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.concurrent.Future;

@Configuration
@Slf4j
public class ProductsImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private CtpResourceRepository ctpResourceRepository;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Autowired
    private CtpBatch ctpBatch;

    @Value("${chunkSize}")
    private int chunkSize;

    @Value("${maxThreads}")
    private int maxThreads;

    @Value("${resource.productType}")
    private Resource productTypeResource;

    @Value("${resource.taxCategory}")
    private Resource taxCategoryResource;

    @Value("${resource.products}")
    private Resource productsResource;

    @Bean
    public Step productsDeleteStep() throws Exception {
        return stepBuilderFactory.get("productsDeleteStep")
                .<ProductProjection, Future<ProductDeleteCommand>>chunk(1)
                .reader(ctpBatch.queryReader(ProductProjectionQuery.ofStaged()))
                .processor(ctpBatch.asyncProcessor(new ProductDeleteItemProcessor(sphereClient)))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step productTypeDeleteStep() throws Exception {
        return stepBuilderFactory.get("productTypeDeleteStep")
                .<ProductType, Future<ProductTypeDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.queryReader(ProductTypeQuery.of()))
                .processor(ctpBatch.asyncProcessor(ProductTypeDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step productTypeImportStep() throws Exception {
        return stepBuilderFactory.get("productTypeImportStep")
                .<ProductTypeDraft, Future<ProductTypeCreateCommand>>chunk(chunkSize)
                .reader(ctpBatch.jsonReader(productTypeResource, ProductTypeDraft.class))
                .processor(ctpBatch.asyncProcessor(ProductTypeCreateCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step taxCategoryImportStep() throws Exception {
        return stepBuilderFactory.get("taxCategoryImportStep")
                .<TaxCategoryDraft, Future<TaxCategoryCreateCommand>>chunk(chunkSize)
                .reader(ctpBatch.jsonReader(taxCategoryResource, TaxCategoryDraft.class))
                .processor(ctpBatch.asyncProcessor(TaxCategoryCreateCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step taxCategoryDeleteStep() throws Exception {
        return stepBuilderFactory.get("taxCategoryDeleteStep")
                .<TaxCategory, Future<TaxCategoryDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.queryReader(TaxCategoryQuery.of()))
                .processor(ctpBatch.asyncProcessor(TaxCategoryDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step productsImportStep() throws Exception {
        return stepBuilderFactory.get("productsImportStep")
                .<List<FieldSet>, Future<ProductCreateCommand>>chunk(chunkSize)
                .reader(new ProductImportItemReader(productsResource))
                .processor(ctpBatch.asyncProcessor(new ProductImportItemProcessor(ctpResourceRepository)))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }
}
