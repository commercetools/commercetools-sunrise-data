package com.commercetools.dataimport;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableBatchProcessing
public class App {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

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

    @Bean
    public Job step1(Step productsUnpublishStep, Step productsDeleteStep, Step productTypeDeleteStep,
                     Step orderTypeDeleteStep, Step customerTypeDeleteStep,
                     Step channelTypeImportStep, Step channelsImportStep,
                     Step channelTypeDeleteStep, Step channelsDeleteStep,
                     Step customerTypeImportStep, Step orderTypeImportStep) {
        return jobBuilderFactory.get("step1")
                // products (delete)
                .start(productsUnpublishStep)
                .next(productsDeleteStep)
                .next(productTypeDeleteStep)
                // order type
                .next(orderTypeDeleteStep)
                .next(orderTypeImportStep)
                // customer type
                .next(customerTypeDeleteStep)
                .next(customerTypeImportStep)
                // channels
                .next(channelsDeleteStep)
                .next(channelTypeDeleteStep)
                .next(channelTypeImportStep)
                .next(channelsImportStep)
                .build();
    }

    @Bean
    public Job step2(Step ordersDeleteStep, Step ordersImportStep) {
        return jobBuilderFactory.get("step2")
                .start(ordersDeleteStep)
                .next(ordersImportStep)
                .build();
    }
}
