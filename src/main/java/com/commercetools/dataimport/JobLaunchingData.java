package com.commercetools.dataimport;

import io.sphere.sdk.models.Base;
import org.springframework.batch.core.JobParameters;

public class JobLaunchingData extends Base {
    private final String jobName;
    private final JobParameters jobParameters;

    public JobLaunchingData(final String jobName, final JobParameters jobParameters) {
        this.jobName = jobName;
        this.jobParameters = jobParameters;
    }

    public String getJobName() {
        return jobName;
    }

    public JobParameters getJobParameters() {
        return jobParameters;
    }
}
