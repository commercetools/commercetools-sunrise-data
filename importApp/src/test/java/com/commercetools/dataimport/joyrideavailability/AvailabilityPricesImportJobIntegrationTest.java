package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.CommercetoolsTestConfiguration;
import com.commercetools.dataimport.IntegrationTest;
import com.commercetools.dataimport.TestConfiguration;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraftDsl;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.commercetools.dataimport.joyrideavailability.AvailabilityPricesImportJobConfiguration.*;
import static com.commercetools.dataimport.TestUtils.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@org.springframework.boot.test.IntegrationTest
@ContextConfiguration(classes = {TestConfiguration.class, AvailabilityPricesImportJobConfiguration.class, CommercetoolsTestConfiguration.class})
@EnableAutoConfiguration
@Configuration
@TestPropertySource("classpath:/test.properties")
@NotThreadSafe
public class AvailabilityPricesImportJobIntegrationTest extends IntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private ItemProcessor<ProductProjection, ProductUpdateCommand> priceCreationProcessor;

    @Autowired
    private ItemWriter<ProductUpdateCommand> productPriceWriter;

    @Autowired
    private Environment env;

    @Test
    public void findLastProductWithJoyridePrice() throws Exception {
        final int amountOfProducts = 10;
        withJoyrideChannels(sphereClient, joyrideChannels -> {
            withListOfProductProjections(sphereClient, amountOfProducts, productsWithoutJoyride -> {
                assertThat(productsWithoutJoyride).hasSize(amountOfProducts);
                final Long lastProductIndex = (long) (amountOfProducts - 1);
                final Optional<ProductProjection> lastProductWithJoyrideChannel = findLastProductWithJoyrideChannel(sphereClient, 0L, lastProductIndex);
                assertThat(lastProductWithJoyrideChannel).isEmpty();

                final int stopIndex = 6;
                addJoyridePriceToProductsUntilStopIndex(stopIndex);
                final Optional<ProductProjection> lastProductWithJoyrideChannel1 = findLastProductWithJoyrideChannel(sphereClient, 0L, lastProductIndex);
                final ProductProjection stopProduct = productsWithoutJoyride.get(stopIndex);
                assertThat(stopProduct.getId()).isEqualTo(lastProductWithJoyrideChannel1.get().getId());

                addJoyrideToMissingProducts();
                final Optional<ProductProjection> lastProductWithJoyrideChannel2 = findLastProductWithJoyrideChannel(sphereClient, 0L, lastProductIndex);
                final ProductProjection lastProduct = productsWithoutJoyride.get(lastProductIndex.intValue());
                assertThat(lastProduct.getId()).isEqualTo(lastProductWithJoyrideChannel2.get().getId());

                return sphereClient.executeBlocking(ProductProjectionQuery.ofCurrent()).getResults();
            });
        });
    }

    @Test
    public void readerRestartAfterPartialExecution() throws Exception {
        final int amountOfProducts = 10;
        final int stopIndex = 4;
        withJoyrideChannels(sphereClient, joyrideChannels -> {
            withListOfProductProjections(sphereClient, amountOfProducts, productsWithoutJoyride -> {
                addJoyridePriceToProductsUntilStopIndex(stopIndex);
                final ItemReader<ProductProjection> restartReader = createProductProjectionReader(sphereClient);
                final ProductProjection restartProduct = restartReader.read();
                assertThat(restartProduct.getId()).isEqualTo(productsWithoutJoyride.get(stopIndex + 1).getId());
                return sphereClient.executeBlocking(ProductProjectionQuery.ofCurrent()).getResults();
            });
        });
    }

    @Test
    public void jobAvailabilityPricesImport() throws Exception {
        final int amountOfProducts = 10;
        withJoyrideChannels(sphereClient, joyrideChannels -> {
            withListOfProductProjections(sphereClient, amountOfProducts, productsWithoutJoyride -> {
                assertThat(productsWithoutJoyride).hasSize(amountOfProducts);
                executeAvailabilityPricesImportJob();
                final List<ProductProjection> updatedProducts = sphereClient.executeBlocking(ProductProjectionQuery.ofCurrent().withExpansionPaths(m -> m.allVariants().prices().channel())).getResults();
                updatedProducts.forEach(updatedProductProjection -> {
                    final ProductProjection productWithoutJoyridePrices = productsWithoutJoyride.stream().filter(p -> p.getId().equals(updatedProductProjection.getId())).limit(1L).findAny().get();
                    final Price originalPrice = productWithoutJoyridePrices.getMasterVariant().getPrices().get(0);
                    final ProductVariant masterVariantUpdatedProduct = updatedProductProjection.getMasterVariant();
                    masterVariantUpdatedProduct.getPrices().stream()
                            .filter(m -> m.getChannel() != null)
                            .forEach(masterVariantPrice -> {
                                final PriceDraftDsl expectedPrice = randomPriceDraft(masterVariantUpdatedProduct, masterVariantPrice.getChannel().getObj(), originalPrice);
                                assertThat(masterVariantPrice.getValue()).isEqualTo(expectedPrice.getValue());
                            });
                });
                return updatedProducts;
            });
        });
    }

    private void addJoyrideToMissingProducts() throws Exception {
        final ItemReader<ProductProjection> restartReader = createProductProjectionReader(sphereClient);
        ProductProjection productProjection;
        while ((productProjection = restartReader.read()) != null) {
            processProductAndWritePrice(productProjection);
        }
    }

    private void addJoyridePriceToProductsUntilStopIndex(final int stopIndex) throws Exception {
        final ItemReader<ProductProjection> initialReader = createProductProjectionReader(sphereClient);
        int index = 0;
        while (index <= stopIndex) {
            processProductAndWritePrice(initialReader.read());
            index++;
        }
    }

    private void executeAvailabilityPricesImportJob() throws Exception {
        final Map<String, JobParameter> jobParametersMap = new HashMap<>();
        addCommercetoolsCredentialValues(env, jobParametersMap);
        final JobParameters jobParameters = new JobParameters(jobParametersMap);
        final JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }

    private void processProductAndWritePrice(final ProductProjection productProjection) throws Exception {
        final ProductUpdateCommand productUpdateCommand = priceCreationProcessor.process(productProjection);
        productPriceWriter.write(asList(productUpdateCommand));
    }
}