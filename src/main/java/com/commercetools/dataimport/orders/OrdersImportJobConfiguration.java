package com.commercetools.dataimport.orders;

import com.commercetools.dataimport.CommercetoolsJobConfiguration;
import com.commercetools.dataimport.orders.csvline.OrderCsvLineValue;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.orders.OrderImportDraft;
import io.sphere.sdk.orders.commands.OrderImportCommand;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class OrdersImportJobConfiguration extends CommercetoolsJobConfiguration {

    private static final String[] ORDER_CSV_HEADER_NAMES = new String[]{"customerEmail", "orderNumber", "lineitems.variant.sku", "lineitems.price", "lineitems.quantity", "totalPrice"};

    @Bean
    public Job ordersCreateJob(final Step ordersImportStep) {
        return jobBuilderFactory.get("ordersImportJob")
                .start(ordersImportStep)
                .build();
    }

    @Bean
    public Step ordersImportStep(final ItemReader<OrderImportDraft> ordersImportReader,
                                 final ItemWriter<OrderImportDraft> ordersImportWriter) {
        return stepBuilderFactory.get("ordersImportStep")
                .<OrderImportDraft, OrderImportDraft>chunk(1)
                .reader(ordersImportReader)
                .writer(ordersImportWriter)
                .build();
    }

    @Bean
    public ItemWriter<OrderImportDraft> ordersImportWriter(final BlockingSphereClient sphereClient) {
        return items -> items.stream()
                .map(OrderImportCommand::of)
                .forEach(sphereClient::executeBlocking);
    }

    @Bean
    @StepScope
    public OrderImportItemReader ordersImportReader(@Value("#{jobParameters['resource']}") final Resource resource) {
        final OrderImportItemReader reader = new OrderImportItemReader();
        final SingleItemPeekableItemReader<OrderCsvLineValue> itemReader = new SingleItemPeekableItemReader<>();
        itemReader.setDelegate(flatFileItemReader(resource));
        reader.setDelegate(itemReader);
        return reader;
    }

    private static FlatFileItemReader<OrderCsvLineValue> flatFileItemReader(final Resource resource) {
        FlatFileItemReader<OrderCsvLineValue> flatFileItemReader = new FlatFileItemReader<>();
        flatFileItemReader.setLineMapper(lineMapper());
        flatFileItemReader.setLinesToSkip(1);
        flatFileItemReader.setResource(resource);
        return flatFileItemReader;
    }

    private static DefaultLineMapper<OrderCsvLineValue> lineMapper() {
        return new DefaultLineMapper<OrderCsvLineValue>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(ORDER_CSV_HEADER_NAMES);
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<OrderCsvLineValue>() {{
                setTargetType(OrderCsvLineValue.class);
            }});
        }};
    }
}
