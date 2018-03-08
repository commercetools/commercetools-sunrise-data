package com.commercetools.dataimport;

import com.commercetools.dataimport.inventory.InventoryCsvEntry;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.Reference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
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
public class InventoryImportStepConfiguration {

    private static final String[] INVENTORY_CSV_HEADER_NAMES = new String[]{"sku", "quantityOnStock", "supplyChannel"};

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private CachedResources cachedResources;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Value("${resource.inventory}")
    private Resource inventoryResource;

    @Bean
    @JobScope
    public Step inventoryDeleteStep() {
        return stepBuilderFactory.get("inventoryDeleteStep")
                .<InventoryEntry, InventoryEntry>chunk(50)
                .reader(inventoryDeleteStepReader())
                .writer(inventoryDeleteStepWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step inventoryImportStep() {
        return stepBuilderFactory.get("productTypeImportStep")
                .<InventoryCsvEntry, InventoryEntryDraft>chunk(1)
                .reader(inventoryImportStepReader())
                .processor(inventoryImportStepProcessor())
                .writer(inventoryImportStepWriter())
                .build();
    }

    private ItemReader<InventoryEntry> inventoryDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, InventoryEntryQuery.of());
    }

    private ItemWriter<InventoryEntry> inventoryDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final InventoryEntry inventoryEntry = sphereClient.executeBlocking(InventoryEntryDeleteCommand.of(item));
            log.debug("Removed inventory entry \"{}\"", inventoryEntry.getSku());
        });
    }

    private ItemReader<InventoryCsvEntry> inventoryImportStepReader() {
        final FlatFileItemReader<InventoryCsvEntry> reader = new FlatFileItemReader<>();
        reader.setResource(inventoryResource);
        reader.setLineMapper(new DefaultLineMapper<InventoryCsvEntry>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(INVENTORY_CSV_HEADER_NAMES);
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<InventoryCsvEntry>() {{
                setTargetType(InventoryCsvEntry.class);
            }});
        }});
        reader.setLinesToSkip(1);
        return reader;
    }

    private ItemProcessor<InventoryCsvEntry, InventoryEntryDraft> inventoryImportStepProcessor() {
        return item -> {
            final String channelKey = item.getSupplyChannel();
            final Reference<Channel> channelRef = channelKey != null ? cachedResources.fetchChannelRef(channelKey) : null;
            return InventoryEntryDraftBuilder.of(item.getSku(), item.getQuantityOnStock())
                    .supplyChannel(channelRef)
                    .build();
        };
    }

    private ItemWriter<InventoryEntryDraft> inventoryImportStepWriter() {
        return items -> items.forEach(draft -> {
            final InventoryEntry inventoryEntry = sphereClient.executeBlocking(InventoryEntryCreateCommand.of(draft));
            log.debug("Created inventory entry \"{}\"", inventoryEntry.getSku());
        });
    }


}
