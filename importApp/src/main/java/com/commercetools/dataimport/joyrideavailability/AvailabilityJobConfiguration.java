package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.dataimport.categories.CategoryCsvLineValue;
import com.commercetools.dataimport.commercetools.DefaultCommercetoolsJobConfiguration;
import com.commercetools.dataimport.products.ProductDraftReader;
import com.commercetools.sdk.jvm.spring.batch.item.ItemReaderFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.channels.ChannelDraftDsl;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.search.SearchKeywords;
import io.sphere.sdk.types.Custom;
import io.sphere.sdk.types.CustomDraft;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.sphere.sdk.client.SphereClientUtils.blockingWait;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@Component
@Lazy
public class AvailabilityJobConfiguration extends DefaultCommercetoolsJobConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityJobConfiguration.class);

    @Bean
    public Job productsSuggestionsCopyJob(final Step channelImportStep, final Step customTypeImportStep) {
        return jobBuilderFactory.get("importJoyrideAvailabilityJob")
                  .start(customTypeImportStep)
                  .next(channelImportStep)
                .build();
    }

    @Bean
    public Step channelImportStep(final BlockingSphereClient sphereClient,
                                  final ItemReader<ChannelDraftDsl> channelsDraftReader,
                                  final ItemWriter<ChannelDraftDsl> channelsDraftItemWriter) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("channelImportStep");
        return stepBuilder
                .<ChannelDraftDsl, ChannelDraftDsl>chunk(1)
                .reader(channelsDraftReader)
                .writer(channelsDraftWriter(sphereClient))
                .build();
    }

    @Bean
    @StepScope
    private ItemReader<ChannelDraftDsl> channelsDraftReader(@Value("#{jobParameters['resource']}") final Resource channelsJsonResource) throws IOException {
        logger.info("URL_Channels: " + channelsJsonResource);
        final ObjectReader reader = SphereJsonUtils.newObjectMapper().readerFor(new TypeReference<List<ChannelDraftDsl>>() {
        });
        final InputStream inputStream = channelsJsonResource.getInputStream();
        final List<ChannelDraftDsl> list = reader.readValue(inputStream);
        return new ListItemReader<>(list);
    }

    @Bean
    protected ItemWriter<ChannelDraftDsl> channelsDraftWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(channelDraft -> sphereClient.executeBlocking(ChannelCreateCommand.of(channelDraft)));
    }

    @Bean
    public Step customTypeImportStep(final BlockingSphereClient sphereClient,
                                  final ItemReader<TypeDraft> customTypeReader,
                                  final ItemWriter<TypeDraft> customTypeItemWriter) {
        final StepBuilder stepBuilder = stepBuilderFactory.get("customTypesImportStep");
        return stepBuilder
                .<TypeDraft, TypeDraft>chunk(1)
                .reader(customTypeReader)
                .writer(customTypeWriter(sphereClient))
                .build();
    }

    @Bean
    @StepScope
    private ItemReader<TypeDraft> customTypeReader(@Value("#{jobParameters['resource_types']}") final Resource typesJsonResource) throws IOException {
        logger.info("URL_Channels: " + typesJsonResource);
        final ObjectReader reader = SphereJsonUtils.newObjectMapper().readerFor(new TypeReference<List<TypeDraft>>() {
        });
        final InputStream inputStream = typesJsonResource.getInputStream();
        final List<TypeDraft> list = reader.readValue(inputStream);
        return new ListItemReader<>(list);
    }

    @Bean
    protected ItemWriter<TypeDraft> customTypeWriter(final BlockingSphereClient sphereClient) {
        return items -> items.forEach(customType -> sphereClient.executeBlocking(TypeCreateCommand.of(customType)));
    }
}
