package com.commercetools.dataimport;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.Command;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.Identifiable;
import io.sphere.sdk.models.Versioned;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.queries.TypeQuery;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.util.Collections.singletonList;

@Service
public class CtpBatch {

    private static final ObjectMapper MAPPER = SphereJsonUtils.newObjectMapper();

    @Autowired
    private BlockingSphereClient sphereClient;

    @Autowired
    private TaskExecutor taskExecutor;

    /**
     * Reads a file containing direcly an array of objects which should be converted to a list of {@code clazz} elements.
     *
     * @param resource the resource wich a JSON array of objects where each object should be converted to an instance of {@code clazz}
     * @param clazz the type element the ItemReader should return
     * @param <T> the type element the ItemReader should return
     * @return item reader
     * @throws IOException in case the resource reading did not work or the JSON mapping
     */
    public <T> ListItemReader<T> jsonReader(final Resource resource, final Class<T> clazz) throws IOException {
        final List<T> listFromJsonResource = createJsonList(resource, clazz);
        return new ListItemReader<>(listFromJsonResource);
    }

    /**
     * Reads a file containing direcly an array of objects which should be converted to a list of {@code clazz} elements.
     *
     * @param resource the resource wich a JSON array of objects where each object should be converted to an instance of {@code clazz}
     * @param clazz the type element the List should return
     * @param <T> the type element the List should return
     * @return List
     * @throws IOException in case the resource reading did not work or the JSON mapping
     */
    private static <T> List<T> createJsonList(final Resource resource, final Class<T> clazz) throws IOException {
        final TypeFactory typeFactory = TypeFactory.defaultInstance();
        final JavaType javaType = typeFactory.constructCollectionType(List.class, clazz);
        final ObjectReader reader = MAPPER.readerFor(javaType);
        final InputStream inputStream = resource.getInputStream();
        return reader.readValue(inputStream);
    }

    public <T> FlatFileItemReader<T> csvReader(final Resource resource, final String[] headerNames, final Class<T> csvEntryClass) {
        final FlatFileItemReader<T> reader = new FlatFileItemReader<>();
        reader.setResource(resource);
        reader.setLineMapper(new DefaultLineMapper<T>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(headerNames);
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<T>() {{
                setTargetType(csvEntryClass);
            }});
        }});
        reader.setLinesToSkip(1);
        return reader;
    }

    public <T extends Identifiable<?>, C extends QueryDsl<T, C>> ItemReader<T> queryReader(final QueryDsl<T, C> query) {
        return new CtpResourceItemReader<>(sphereClient, query);
    }

    public ItemReader<Type> typeQueryReader(final String resourceTypeId) {
        return queryReader(TypeQuery.of()
                .withPredicates(type -> type.resourceTypeIds().containsAny(singletonList(resourceTypeId))));
    }

    public <I, O> AsyncItemProcessor<I, O> asyncProcessor(final ItemProcessor<I, O> delegate) throws Exception {
        final AsyncItemProcessor<I, O> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(delegate);
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.afterPropertiesSet();
        return asyncItemProcessor;
    }

    public <C extends Command<T>, T extends Versioned<T>> AsyncItemWriter<C> asyncWriter() {
        final AsyncItemWriter<C> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(new CtpResourceItemWriter<>(sphereClient));
        return asyncItemWriter;
    }
}
