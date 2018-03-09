package com.commercetools.dataimport;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableBatchProcessing
@EnableCaching
public class App {

    public static void main(String[] args) throws Exception {
        try (final ConfigurableApplicationContext ctx = SpringApplication.run(App.class, args)) {
            final JobLauncher launcher = ctx.getBean("jobLauncher", JobLauncher.class);
            final Job job = ctx.getBean(jobName(args), Job.class);
            launcher.run(job, new JobParameters());
        }
        System.exit(0);
    }

    private static String jobName(final String[] args) {
        return args.length > 0 ? args[0] : "dataImport";
    }
}
