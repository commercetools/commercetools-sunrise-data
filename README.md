# Sunrise Data

[![Build Status](https://travis-ci.org/commercetools/commercetools-sunrise-data.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sunrise-data)


## How to create a project with Sunrise data

Before starting the import, make sure you have access to the [Admin Center](https://admin.commercetools.com) and the [IMPEX tool](https://impex.commercetools.com/). To check that you can run the application in this repository read the _[Requirements](#requirements)_ section.

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
1. Go to [Category import](https://impex.commercetools.com/commands/category-import) and drop the file [categories.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/categories/categories.csv), enable "Sort categories by parentId before importing" and run.
2. Go to [Product Type import](https://impex.commercetools.com/commands/product-type-import) and drop the file [product-type-template.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/producttypes/product-type-template.csv) on "Product Type template as CSV" and [product-type-attributes.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/producttypes/product-type-attributes.csv) on "Product Type attributes as CSV", then run. 
3. Go to [Product import](https://impex.commercetools.com/commands/product-import) and drop the file [products.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/products/products.csv), then run.
4. Go to [Stock import](https://impex.commercetools.com/commands/stock-import) and drop the file [inventory.csv](https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/inventory/inventory.csv), then run.

### 4. Import orders

1. Go back to the root folder of the cloned project and create another file named `payload-orders.json` with the following content:
    ```json
    {
      "commercetools": {
        "projectKey": "demo-playground",
        "clientId": "7_QU3-WVwJnIyYQlIsHDhp2R",
        "clientSecret": "UbJN7TijNwICwTKUjteVvYcmYr7RjQiC",
        "authUrl": "https://auth.sphere.io",
        "apiUrl": "https://api.sphere.io"
      },
      "jobs": [
        {
          "name": "ordersCreateJob",
          "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/orders/orders.json"
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
- Install [Maven](https://maven.apache.org/)

### Payload JSON file

In order to run the application, you need to define a payload file that specifies what kind of tasks it is going to execute. This file should have the following content:

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
     # Add desired jobs here
  ]
}
```
Edit the project credentials and add the jobs you need to execute.

#### Jobs

You can combine as many jobs as you need in your payload file. They will be executed sequentially in the given order.

##### Delete products
```json
{
  "name": "productsDeleteJob"
}
```

##### Import products
```json
{
  "name": "productsCreateJob",
  "maxProducts": 4000,
  "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/products/products.csv"
}
```

##### Delete categories
```json
{
  "name": "categoriesDeleteJob"
}
```

##### Import categories
```json
{
  "name": "categoriesCreateJob",
  "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/categories/categories.csv"
}
```

##### Delete product types
```json
{
  "name": "productTypesDeleteJob"
}
```

##### Import product types
```json
{
  "name": "productTypeCreateJob",
  "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/product-types/product-types.json"
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
  "typesResource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/channels/types.json"
}
```

##### Generate inventory
```json
{
  "name": "inventoryEntryCreationJob"
}
```

##### Generate prices for channels
```json
{
  "name": "availabilityPricesImportJob"
}
```

### Run the application
Run the application with maven:
```bash
export PAYLOAD_FILE=path/to/payload/file
mvn spring-boot:run -Dstart-class=com.commercetools.dataimport.all.PayloadJobMain
``` 