package com.commercetools.dataimport;


import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@Slf4j
public class InventoryImportStepConfiguration {

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private BlockingSphereClient sphereClient;

    @Value("${resource.inventory}")
    private Resource inventoryResource;

    @Bean
    @JobScope
    public Step inventoryDeleteStep() {
        return stepBuilderFactory.get("inventoryDeleteStep")
                .<InventoryEntry, InventoryEntry>chunk(1000)
                .reader(inventoryDeleteStepReader())
                .writer(inventoryDeleteStepWriter())
                .build();
    }

    private ItemReader<InventoryEntry> inventoryDeleteStepReader() {
        return ItemReaderFactory.sortedByIdQueryReader(sphereClient, InventoryEntryQuery.of());
    }

    private ItemWriter<InventoryEntry> inventoryDeleteStepWriter() {
        return items -> items.forEach(item -> {
            final InventoryEntry inventory = sphereClient.executeBlocking(InventoryEntryDeleteCommand.of(item));
            log.debug("Removed inventory \"{}\"", inventory.getId());
        });
    }
}
