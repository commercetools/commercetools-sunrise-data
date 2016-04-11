package com.commercetools.dataimport.all;

import com.commercetools.dataimport.categories.CategoriesDeleteJobConfiguration;
import com.commercetools.dataimport.commercetools.CommercetoolsConfig;
import com.commercetools.dataimport.commercetools.CommercetoolsJobConfiguration;
import com.commercetools.dataimport.products.ProductDeleteJobConfiguration;
import com.commercetools.dataimport.producttypes.ProductTypeDeleteJobConfiguration;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
@ConfigurationProperties("spring.batch.job.enabled=false")
public class DeleteAllJobConfiguration extends CommercetoolsJobConfiguration {
    @Autowired
    Job categoriesDeleteJob;
    @Autowired
    Job productsDeleteJob;
    @Autowired
    Job productTypesDeleteJob;

    @Bean
    Job allDelete(Job productsDeleteJob, Job productTypesDeleteJob, Job categoriesDeleteJob) {
        return jobBuilderFactory.get("allDelete")
                .start(stepBuilderFactory.get("productsDeleteJob").job(productsDeleteJob).build())
                .next(stepBuilderFactory.get("categoriesDeleteJob").job(categoriesDeleteJob).build())
                .next(stepBuilderFactory.get("productTypesDeleteJob").job(productTypesDeleteJob).build())
                .build();
    }

    public static void main(String [] args) throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        final Object[] sources = {CommercetoolsConfig.class,
                DeleteAllJobConfiguration.class,
                ProductDeleteJobConfiguration.class,
                CategoriesDeleteJobConfiguration.class,
                ProductTypeDeleteJobConfiguration.class};
        final ConfigurableApplicationContext context = SpringApplication.run(sources, args);
        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
        final Job job = context.getBean("allDelete", Job.class);
        jobLauncher.run(job, new JobParameters());
        System.exit(SpringApplication.exit(context));
    }
}
