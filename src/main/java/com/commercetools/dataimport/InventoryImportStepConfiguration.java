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

    private static final String[] INVENTORY_CSV_HEADER_NAMES = new String[]{"sku", "quantityOnStock", "supplyChannel"};

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private CtpResourceRepository ctpResourceRepository;

    @Autowired
    private CtpBatch ctpBatch;

    @Value("${resource.inventory}")
    private Resource inventoryResource;

    @Value("${resource.inventoryStores}")
    private Resource inventoryStoresResource;

    @Bean
    public Step inventoryDeleteStep() throws Exception {
        return stepBuilderFactory.get("inventoryDeleteStep")
                .<InventoryEntry, Future<InventoryEntryDeleteCommand>>chunk(1)
                .reader(ctpBatch.queryReader(InventoryEntryQuery.of()))
                .processor(ctpBatch.asyncProcessor(InventoryEntryDeleteCommand::of))
                .writer(ctpBatch.asyncWriter())
                .build();
    }

    @Bean
    public Step inventoryImportStep() throws Exception {
        return stepBuilderFactory.get("inventoryImportStep")
                .<InventoryCsvEntry, Future<InventoryEntryCreateCommand>>chunk(1)
                .reader(ctpBatch.csvReader(inventoryResource, INVENTORY_CSV_HEADER_NAMES, InventoryCsvEntry.class))
                .processor(ctpBatch.asyncProcessor(new InventoryItemProcessor(ctpResourceRepository)))
                .writer(ctpBatch.asyncWriter())
                .build();
    }

    @Bean
    public Step inventoryStoresImportStep() throws Exception {
        return stepBuilderFactory.get("inventoryStoresImportStep")
                .<InventoryCsvEntry, Future<InventoryEntryCreateCommand>>chunk(1)
                .reader(ctpBatch.csvReader(inventoryStoresResource, INVENTORY_CSV_HEADER_NAMES, InventoryCsvEntry.class))
                .processor(ctpBatch.asyncProcessor(new InventoryItemProcessor(ctpResourceRepository)))
                .writer(ctpBatch.asyncWriter())
                .build();
    }
}
