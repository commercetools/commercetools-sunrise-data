package com.commercetools.dataimport.all;

import com.commercetools.dataimport.categories.CategoriesImportJobConfiguration;
import com.commercetools.dataimport.commercetools.CommercetoolsConfig;
import com.commercetools.dataimport.commercetools.CommercetoolsJobConfiguration;
import com.commercetools.dataimport.products.ProductsImportJobConfiguration;
import com.commercetools.dataimport.producttypes.ProductTypesImportJobConfiguration;
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
public class ImportJobConfiguration extends CommercetoolsJobConfiguration {
    @Autowired
    Job importCategories;
    @Autowired
    Job importProducts;
    @Autowired
    Job productTypeCreateJob;

    @Bean
    Job allImports(Job importCategories, Job importProducts, Job productTypeCreateJob) {
        return jobBuilderFactory.get("allImports")
                .start(stepBuilderFactory.get("importCategories").job(importCategories).build())
                .next(stepBuilderFactory.get("productTypeCreateJob").job(productTypeCreateJob).build())
                .next(stepBuilderFactory.get("importProducts").job(importProducts).build())
                .build();
    }

    public static void main(String [] args) throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        final Object[] sources = {CommercetoolsConfig.class,
                ImportJobConfiguration.class,
                ProductTypesImportJobConfiguration.class,
                CategoriesImportJobConfiguration.class,
                ProductsImportJobConfiguration.class};
        final ConfigurableApplicationContext context = SpringApplication.run(sources, args);
        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
        final Job job = context.getBean("allImports", Job.class);
        jobLauncher.run(job, new JobParameters());
        System.exit(SpringApplication.exit(context));
    }
}
