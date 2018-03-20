package com.commercetools.dataimport;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

public class FlagFlowDecider implements JobExecutionDecider {

    public static final String SKIP = "SKIP";
    public static final String RUN = "RUN";

    private final boolean jobFlag;

    public FlagFlowDecider(final boolean jobFlag) {
        this.jobFlag = jobFlag;
    }

    @Override
    public FlowExecutionStatus decide(final JobExecution jobExecution, final StepExecution stepExecution) {
        return jobFlag ? new FlowExecutionStatus(RUN) : new FlowExecutionStatus(SKIP);
    }
}
