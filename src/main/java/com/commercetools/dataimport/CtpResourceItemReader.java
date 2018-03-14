package com.commercetools.dataimport;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.models.Identifiable;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.queries.QuerySort;
import org.springframework.batch.item.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.lang.String.format;

public final class CtpResourceItemReader<T extends Identifiable<?>, C extends QueryDsl<T, C>> implements ItemStreamReader<T> {

    private static final String LAST_ID_RETURNED_CONTEXT_KEY = "lastIdReturned";

    private final BlockingSphereClient client;
    private Queue<T> buffer = new LinkedList<>();
    private String lastIdReturned;
    private String lastIdBuffer;
    private QueryDsl<T, C> query;

    public CtpResourceItemReader(final BlockingSphereClient client, final QueryDsl<T, C> query) {
        this.client = client;
        this.query = query.withSort(QuerySort.of("id asc")).withFetchTotal(false).withLimit(200);
    }

    @Override
    public void open(final ExecutionContext executionContext) throws ItemStreamException {
        lastIdBuffer = executionContext.getString(LAST_ID_RETURNED_CONTEXT_KEY, null);
        lastIdReturned = lastIdBuffer;
    }

    @Override
    public T read() throws ParseException, NonTransientResourceException {
        if (buffer.isEmpty()) {
            refillBuffer();
        }
        final T item = buffer.poll();
        saveLastIdReturned(item);
        return item;
    }

    @Override
    public void update(final ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putString(LAST_ID_RETURNED_CONTEXT_KEY, lastIdReturned);
    }

    @Override
    public void close() throws ItemStreamException {

    }

    private void saveLastIdReturned(final Identifiable<?> value) {
        lastIdReturned = value != null ? value.getId() : null;
    }

    private void refillBuffer() {
        final List<T> nextItems = client.executeBlocking(nextItemsQuery()).getResults();
        if (!nextItems.isEmpty()) {
            lastIdBuffer = nextItems.get(nextItems.size() - 1).getId();
            buffer.addAll(nextItems);
        }
    }

    private QueryDsl<T, C> nextItemsQuery() {
        return lastIdBuffer == null ? query : query.plusPredicates(QueryPredicate.of(format("id > \"%s\"", lastIdBuffer)));
    }
}
