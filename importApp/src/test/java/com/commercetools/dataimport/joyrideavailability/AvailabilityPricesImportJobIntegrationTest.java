package com.commercetools.dataimport.joyrideavailability;

import com.commercetools.CommercetoolsTestConfiguration;
import com.commercetools.dataimport.categories.TestConfiguration;
import io.sphere.sdk.products.*;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.AddPrice;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Optional;

import static com.commercetools.dataimport.joyrideavailability.AvailabilityPricesImportJobConfiguration.*;
import static com.commercetools.dataimport.joyrideavailability.JoyrideAvailabilityUtils.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest
@ContextConfiguration(classes = {TestConfiguration.class, AvailabilityPricesImportJobConfiguration.class, CommercetoolsTestConfiguration.class})
@TestPropertySource("/test.properties")
@EnableAutoConfiguration
@Configuration
public class AvailabilityPricesImportJobIntegrationTest extends JoyrideAvailabilityIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    public void findLastProductWithJoyridePrice() {
        final Product product = createProduct(sphereClient);
        final Long productsCount = 1L;
        assertThat(findLastProductWithJoyrideChannel(sphereClient, 0L, productsCount - 1)).isEmpty();
        withPriceWithJoyrideChannel(sphereClient, priceDraft -> {
            final AddPrice addPrice = AddPrice.of(product.getMasterData().getCurrent().getMasterVariant().getId(), priceDraft);
            Product updatedProduct = sphereClient.executeBlocking(ProductUpdateCommand.of(product, asList(addPrice, Publish.of())));
            final Optional<ProductProjection> lastProductWithJoyrideChannel = findLastProductWithJoyrideChannel(sphereClient, 0L, productsCount - 1);
            assertThat(lastProductWithJoyrideChannel).isPresent();
            assertThat(lastProductWithJoyrideChannel.get().getMasterVariant().getSku()).isEqualTo(product.getMasterData().getCurrent().getMasterVariant().getSku());
            final Product productToDelete = sphereClient.executeBlocking(ProductUpdateCommand.of(updatedProduct, Unpublish.of()));
            sphereClient.executeBlocking(ProductDeleteCommand.of(productToDelete));
        });
    }

    @Test
    public void jobAvailabilityPricesImport() throws Exception {
        final int amountOfProducts = 10;
        withJoyrideChannels(sphereClient, joyrideChannels -> {
            withListOfProductProjections(sphereClient, amountOfProducts, productsWithoutJoyride -> {
                assertThat(productsWithoutJoyride.size()).isEqualTo(amountOfProducts);
                executeAvailabilityPricesImportJob();
                final List<ProductProjection> updatedProducts = sphereClient.executeBlocking(ProductProjectionQuery.ofCurrent().withExpansionPaths(m -> m.allVariants().prices().channel())).getResults();
                updatedProducts.forEach(m -> {
                    assertThat(productContainJoyridePrice(m)).isTrue();
                    final ProductProjection productProjectionWithoutJoyride = productsWithoutJoyride.stream().filter(p -> p.getId().equals(m.getId())).limit(1L).collect(toList()).get(0);
                    final Price originalPrice = productProjectionWithoutJoyride.getMasterVariant().getPrices().get(0);
                    final ProductVariant masterVariant = m.getMasterVariant();
                    masterVariant.getPrices().forEach(variantPrice -> {
                        if (variantPrice.getChannel() != null) {
                            final PriceDraftDsl randomPriceDraft = randomPriceDraft(masterVariant, variantPrice.getChannel().getObj(), originalPrice);
                            assertThat(variantPrice.getValue()).isEqualTo(randomPriceDraft.getValue());
                        }
                    });
                });
                return updatedProducts;
            });
        });
    }

    private void executeAvailabilityPricesImportJob() {
        try {
            final JobExecution jobExecution = jobLauncherTestUtils.launchJob();
            assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void readerRestartAfterPartialExecution() throws Exception {
        final int amountOfProducts = 10;
        final int stopIndex = 4;
        withPriceWithJoyrideChannel(sphereClient, priceDraft -> {
            withListOfProductProjections(sphereClient, amountOfProducts, productsWithoutJoyride -> {
                final ItemReader<ProductProjection> initialReader = createReader(sphereClient);
                addJoyridePriceToProductsUntilStopIndex(stopIndex, priceDraft, initialReader);

                final ItemReader<ProductProjection> restartReader = createReader(sphereClient);
                addJoyrideToMissingProducts(stopIndex, priceDraft, productsWithoutJoyride, restartReader);

                final List<ProductProjection> products = sphereClient.executeBlocking(ProductProjectionQuery.ofCurrent().withExpansionPaths(m -> m.allVariants().prices().channel())).getResults();
                products.forEach(m -> assertThat(productContainJoyridePrice(m)).isTrue());
                return products;
            });
        });
    }

    private void addJoyrideToMissingProducts(final int stopIndex, final PriceDraft priceDraft, final List<ProductProjection> productsWithoutJoyride, final ItemReader<ProductProjection> restartReader) {
        try {
            final ProductProjection restartProduct = restartReader.read();
            assertThat(restartProduct.getId()).isEqualTo(productsWithoutJoyride.get(stopIndex + 1).getId());
            final AddPrice addPrice = AddPrice.of(restartProduct.getMasterVariant().getId(), priceDraft);
            sphereClient.executeBlocking(ProductUpdateCommand.of(restartProduct, asList(addPrice, Publish.of())));
            ProductProjection productProjection;
            while ((productProjection = restartReader.read()) != null) {
                final AddPrice addPrice2 = AddPrice.of(productProjection.getMasterVariant().getId(), priceDraft);
                sphereClient.executeBlocking(ProductUpdateCommand.of(productProjection, asList(addPrice2, Publish.of())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addJoyridePriceToProductsUntilStopIndex(final int stopIndex, final PriceDraft priceDraft, final ItemReader<ProductProjection> initialReader) {
        int index = 0;
        while (index <= stopIndex) {
            try {
                final ProductProjection productProjection = initialReader.read();
                final AddPrice addPrice = AddPrice.of(productProjection.getMasterVariant().getId(), priceDraft);
                sphereClient.executeBlocking(ProductUpdateCommand.of(productProjection, asList(addPrice, Publish.of())));
                index++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}