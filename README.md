# Sunrise Data

[![Build Status](https://travis-ci.org/commercetools/commercetools-sunrise-data.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sunrise-data)


## How to create a project with Sunrise data

Before starting the import, make sure you have access to the [Admin Center](https://admin.commercetools.com) and the [IMPEX tool](https://impex.commercetools.com/). You will also need to run the application in this repository, check that you meet the [requirements](#requirements).

### 1. Set up your project
1. Open the [Admin Center](https://admin.commercetools.com) and create an empty project (without sample data).
2. Select your new project and go to **`Settings`**:
    - In the tab **`International`**:
        - **Currencies**: Euro (EUR)
        - **Countries**: Germany (DE), Austria (AT)
        - **Languages**: German (DE), English (EN)
        - **Zone**: Europe with DE, AT
    - In the tab **`Taxes`**:
        - **Tax Category name**: "standard"
        - **Tax rates**:
            - Germany: 19% incl. in price
            - Austria: 20% incl. in price
    - In the tab **`Shipping Methods`**:
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
    - In the tab **`Customer Groups`**:
        - **Name**: "b2b"
        
### 2. Import channels
1. [Clone](https://help.github.com/articles/cloning-a-repository/) this repository to your computer.
2. Go to the root folder of the cloned project and create a file named `payload-channels.json` with the following content:
    ```json
    {
      "commercetools": {
        "projectKey": "your-project-key",
        "clientId": "your-client-id",
        "clientSecret": "your-client-secret",
        "authUrl": "https://auth.sphere.io",
        "apiUrl": "https://api.sphere.io"
      },
      "jobs": [
        {
          "name": "importJoyrideChannelsJob",
          "channelsResource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/channels/channels.json",
          "typesResource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/channels/types.json"
        }
      ]
    }
    ```
3. Adapt the file with your commercetools project credentials.
4. Run the following command:
    ```bash
    export PAYLOAD_FILE=payload-channels.json && mvn spring-boot:run -Dstart-class=com.commercetools.dataimport.all.PayloadJobMain
    ```

### 3. Import catalog
1. Go to [Category import](https://impex.commercetools.com/commands/category-import) and drop the file [categories.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/categories/categories.csv) and run.
2. Go to [Product Type import](https://impex.commercetools.com/commands/product-type-import) and drop the file [product-type-template.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/producttypes/product-type-template.csv) on "Product Type template as CSV" and [product-type-attributes.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/producttypes/product-type-attributes.csv) on "Product Type attributes as CSV", then run. 
3. Go to [Product import](https://impex.commercetools.com/commands/product-import) and drop the file [products.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/products/products.csv), enable "Publish all changes immediately" and then run.
4. Go to [Stock import](https://impex.commercetools.com/commands/stock-import) and import the file [inventory-no-stores.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/inventory/inventory-no-stores.csv). Alternatively use the file [inventory.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/inventory/inventory.csv), which is large but includes per-local-store inventories.

### 4. Import orders

1. Go back to the root folder of the cloned project and create another file named `payload-orders.json` with the following content:
    ```json
    {
      "commercetools": {
        "projectKey": "your-project-key",
        "clientId": "your-client-id",
        "clientSecret": "your-client-secret",
        "authUrl": "https://auth.sphere.io",
        "apiUrl": "https://api.sphere.io"
      },
      "jobs": [
        {
          "name": "ordersCreateJob",
          "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/orders/orders.csv"
        }
      ]
    }
    ```
2. Adapt the file with your commercetools project credentials.
3. Run the following command:
    ```bash
    export PAYLOAD_FILE=payload-orders.json && mvn spring-boot:run -Dstart-class=com.commercetools.dataimport.all.PayloadJobMain
    ```
    
## How to use the application

### Requirements

- Install [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

### Payload JSON file

In order to run the application, you need to define a payload file that specifies what kind of tasks it is going to execute. This file should have the following content:

```
{
  "commercetools": {
    "projectKey": "your-project-key",
    "clientId": "your-client-id",
    "clientSecret": "your-client-secret",
    "authUrl": "https://auth.sphere.io",
    "apiUrl": "https://api.sphere.io"
  },
  "jobs": [
     /* Add desired jobs here */
  ]
}
```
Edit the project credentials and add the jobs you need to execute.

#### Jobs

You can combine as many jobs as you need in your payload file. They will be executed sequentially in the given order.

##### Delete categories
```json
{
  "name": "categoriesDeleteJob"
}
```

##### Delete product types
```json
{
  "name": "productTypesDeleteJob"
}
```

##### Delete products
```json
{
  "name": "productsDeleteJob"
}
```

##### Import orders
```json
{
  "name": "ordersCreateJob",
  "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/orders/orders.json"
}
```

##### Import channels
```json
{
  "name": "importJoyrideChannelsJob",
  "channelsResource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/channels/channels.json",
  "typesResource": channel-types.json
}
```

##### Generate inventory
```json
{
  "name": "inventoryEntryGenerationJob"
}
```

##### Generate prices for channels
```json
{
  "name": "pricesPerChannelImportJob"
}
```

### Run the application
Run the application with maven:
```bash
export PAYLOAD_FILE=path/to/payload/file
./mvnw spring-boot:run -Dstart-class=com.commercetools.dataimport.PayloadJobMain
``` 
