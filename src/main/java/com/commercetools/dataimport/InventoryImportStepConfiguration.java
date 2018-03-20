package com.commercetools.dataimport;

import com.commercetools.dataimport.inventory.InventoryCsvEntry;
import com.commercetools.dataimport.inventory.InventoryItemProcessor;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.concurrent.Future;

@Configuration
@Slf4j
public class InventoryImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private CtpResourceRepository ctpResourceRepository;

    @Autowired
    private CtpBatch ctpBatch;

    @Value("${chunkSize}")
    private int chunkSize;

    @Value("${maxThreads}")
    private int maxThreads;

    @Value("${resource.inventory}")
    private Resource inventoryResource;

    @Value("${resource.inventoryStores}")
    private Resource inventoryStoresResource;

    @Value("${headers.inventory}")
    private String[] inventoryHeaders;

    @Bean
    public Step inventoryDeleteStep() throws Exception {
        return stepBuilderFactory.get("inventoryDeleteStep")
                .<InventoryEntry, Future<InventoryEntryDeleteCommand>>chunk(chunkSize)
                .reader(ctpBatch.queryReader(InventoryEntryQuery.of()))
                .processor(ctpBatch.asyncProcessor(InventoryEntryDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step inventoryImportStep() throws Exception {
        return stepBuilderFactory.get("inventoryImportStep")
                .<InventoryCsvEntry, Future<InventoryEntryCreateCommand>>chunk(chunkSize)
                .reader(ctpBatch.csvReader(inventoryResource, inventoryHeaders, InventoryCsvEntry.class))
                .processor(ctpBatch.asyncProcessor(new InventoryItemProcessor(ctpResourceRepository)))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }

    @Bean
    public Step inventoryStoresImportStep() throws Exception {
        return stepBuilderFactory.get("inventoryStoresImportStep")
                .<InventoryCsvEntry, Future<InventoryEntryCreateCommand>>chunk(chunkSize)
                .reader(ctpBatch.csvReader(inventoryStoresResource, inventoryHeaders, InventoryCsvEntry.class))
                .processor(ctpBatch.asyncProcessor(new InventoryItemProcessor(ctpResourceRepository)))
                .writer(ctpBatch.asyncWriter())
                .listener(new ProcessedItemsChunkListener())
                .listener(new DurationStepListener())
                .throttleLimit(maxThreads)
                .build();
    }
}
