package com.commercetools.dataimport.all;

import com.commercetools.dataimport.commercetools.CommercetoolsJobConfiguration;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy
@Configuration
public class CombinedJobsConfiguration extends CommercetoolsJobConfiguration {
    @Autowired
    BeanFactory ctx;//necessary for lazy initialization and fighting circular dependencies

    @Bean
    Job deleteAllJob(Job productsDeleteJob, Job productTypesDeleteJob, Job categoriesDeleteJob) {
        return jobBuilderFactory.get("allDeleteJob")
                .start(stepBuilderFactory.get("productsDeleteJob").job(productsDeleteJob).build())
                .next(stepBuilderFactory.get("categoriesDeleteJob").job(categoriesDeleteJob).build())
                .next(stepBuilderFactory.get("productTypesDeleteJob").job(productTypesDeleteJob).build())
                .build();
    }

    @Bean
    Job allImportsJob() {
        final Job importCategories = ctx.getBean("importProducts", Job.class);
        final Job productTypeCreateJob = ctx.getBean("productTypeCreateJob", Job.class);
        final Job importProducts = ctx.getBean("importProducts", Job.class);
        return jobBuilderFactory.get("allImportsJob")
                .start(stepBuilderFactory.get("importCategoriesJob").job(importCategories).build())
                .next(stepBuilderFactory.get("productTypeCreateJob").job(productTypeCreateJob).build())
                .next(stepBuilderFactory.get("importProductsJob").job(importProducts).build())
                .build();
    }
}
