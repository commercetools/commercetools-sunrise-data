package com.commercetools.dataimport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;

@Slf4j
public class ProcessedItemsChunkListener implements ChunkListener {

    private Long batchStartTime;

    @Override
    public void beforeChunk(final ChunkContext context) {
        if (batchStartTime == null) {
            batchStartTime = System.currentTimeMillis();
        }
    }

    @Override
    public void afterChunk(final ChunkContext context) {
        final StepExecution stepExecution = context.getStepContext().getStepExecution();
        final int writeCount = stepExecution.getWriteCount();
        if (writeCount % 100 == 0 && writeCount != 0) {
            final long duration = System.currentTimeMillis() - batchStartTime;
            batchStartTime = null;
            log.info("Processed 100 items in {} ms ({} items in total)", duration, writeCount);
        }
    }

    @Override
    public void afterChunkError(final ChunkContext context) {

    }
}
