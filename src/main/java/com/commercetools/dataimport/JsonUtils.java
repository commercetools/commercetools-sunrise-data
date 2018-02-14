package com.commercetools.dataimport;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.sphere.sdk.json.SphereJsonUtils;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = SphereJsonUtils.newObjectMapper();

    private JsonUtils() {
    }

    /**
     * Reads a file containing direcly an array of objects which should be converted to a list of {@code clazz} elements.
     *
     * @param resource the resource wich a JSON array of objects where each object should be converted to an instance of {@code clazz}
     * @param clazz the type element the ItemReader should return
     * @param <T> the type element the ItemReader should return
     * @return item reader
     * @throws IOException in case the resource reading did not work or the JSON mapping
     */
    public static <T> ItemReader<T> createJsonListReader(final Resource resource, final Class<T> clazz) throws IOException {
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
    public static <T> List<T> createJsonList(final Resource resource, final Class<T> clazz) throws IOException {
        final TypeFactory typeFactory = TypeFactory.defaultInstance();
        final JavaType javaType = typeFactory.constructCollectionType(List.class, clazz);
        final ObjectReader reader = MAPPER.readerFor(javaType);
        final InputStream inputStream = resource.getInputStream();
        return reader.readValue(inputStream);
    }
}
