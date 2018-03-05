package com.commercetools.dataimport;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableBatchProcessing
public class App {

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            final ConfigurableApplicationContext ctx = SpringApplication.run(App.class, args);
            final JobLauncher launcher = ctx.getBean("jobLauncher", JobLauncher.class);
            final Job job = ctx.getBean(args[0], Job.class);
            launcher.run(job, new JobParameters());
            ctx.close();
            System.exit(0);
        } else {
            System.err.println("Missing the name of the job to be executed");
            System.exit(1);
        }
    }
}
