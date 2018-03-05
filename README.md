# Sunrise Data

[![Build Status](https://travis-ci.org/commercetools/commercetools-sunrise-data.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sunrise-data)


## How to create a project with Sunrise data

Before starting the import, make sure you have access to the [Admin Center](https://admin.commercetools.com) and the [IMPEX tool](https://impex.commercetools.com/). You will also need to run the application in this repository, check that you meet the [requirements](#requirements) and [clone](https://help.github.com/articles/cloning-a-repository/) this repository to your computer.


### 1. Set up your project
1. Open the [Admin Center](https://admin.commercetools.com) and create an empty project (without sample data).
2. Select your new project and go to **`Settings`**:
    - In the tab **`International`**:
        - **Currencies**: Euro (EUR)
        - **Countries**: Germany (DE), Austria (AT)
        - **Languages**: German (DE), English (EN)
        - **Zone**: Europe with DE, AT
    - In the tab **`Shipping Methods`** (optional):
        - **Standard shipping**:
            - Name: "Standard"
            - Description: "Delivery in 5-6 working days"
            - Price: 3 EUR
            - Free above: 200 EUR
            - Set as default: true
        - **Express shipping**:
            - Name: "Express"
            - Description: "Delivery the same day"
            - Price: 10 EUR
        
### 2. Import channels
1. Adapt the [`src/main/resources/application.properties`](src/main/resources/application.properties#L12-L14) file with your commercetools project credentials.
2. Go to the root folder and run the following command:
    ```bash
    ./mvnw spring-boot:run
    ```

### 3. Import catalog
1. Go to [Category import](https://impex.commercetools.com/commands/category-import) and drop the file [categories.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/categories/categories.csv) and run.
2. Go to [Product import](https://impex.commercetools.com/commands/product-import) and drop the file [products.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/products/products.csv), enable "Publish all changes immediately" and then run.
3. Go to [Stock import](https://impex.commercetools.com/commands/stock-import) and import the file [inventory-no-stores.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/inventory/inventory-no-stores.csv). Alternatively use the file [inventory.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/inventory/inventory.csv), which is large but includes per-local-store inventories.

### 4. Import orders

1. Run the following command:
    ```bash
    ./mvnw spring-boot:run -Drun.arguments=ordersImport
    ```
    
## How to use the application

### Requirements

- Install [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

### Configuration

Adapt [`src/main/resources/application.properties`](src/main/resources/application.properties) with the credentials of the commercetools project where to run the import. 

In the same file you can also find the localization of all resources and modify them accordingly.

### Run the application

Run the application with maven to run the job [`DataImportJobConfiguration`](/src/main/java/com/commercetools/dataimport/DataImportJobConfiguration.java), which imports the basic data:
```bash
./mvnw spring-boot:run
``` 

Optionally you can run other jobs by specifying its name via run arguments:
```bash
./mvnw spring-boot:run -Drun.arguments=jobName
```

##### Available jobs

Check the files to see in detail which steps does each job execute.

- [`DataImportJobConfiguration`](/src/main/java/com/commercetools/dataimport/DataImportJobConfiguration.java)
- [`OrdersImportJobConfiguration`](/src/main/java/com/commercetools/dataimport/OrdersImportJobConfiguration.java)