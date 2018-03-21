package com.commercetools.dataimport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

@Slf4j
public class DurationStepListener implements StepExecutionListener {

    private long startTime;

    @Override
    public void beforeStep(final StepExecution stepExecution) {
        startTime = System.currentTimeMillis();
    }

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        final long duration = System.currentTimeMillis() - startTime;
        log.info("Step [{}] finished: processed {} items in {} ms", stepExecution.getStepName(), stepExecution.getWriteCount(), duration);
        return stepExecution.getExitStatus();
    }
}
