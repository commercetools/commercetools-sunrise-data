# Sunrise Data

[![Build Status](https://travis-ci.org/commercetools/commercetools-sunrise-data.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sunrise-data)


## How to create a project with Sunrise data

Before starting the import, make sure you have access to the [Admin Center](https://admin.commercetools.com), the [IMPEX tool](https://impex.commercetools.com/) and the application in this repository.

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
          "channelsResource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/joyride/channels.json",
          "typesResource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/joyride/types.json"
        }
      ]
    }
    ```
3. Adapt the file with your commercetools project credentials.
4. Run the following commands:
    ```bash
    export PAYLOAD_FILE=payload-channels.json
    mvn spring-boot:run -Dstart-class=com.commercetools.dataimport.all.PayloadJobMain
    ```

### 3. Import catalog




    
        
  



## How to create the data with the commercetools impex tool

### csv files encoding:
 - delimited by ","
 - encoding / file origin: UTF-8
 - use text data format for ALL columns
 
### import product types
1. Import it with https://impex.sphere.io/playground
 - Endpoint: Product Types
 - Method: Create
 - use product-types/product-types.json as body, **but remove the surrounding square brackets before**

### import categories
1. Use the category structure as in categories.csv

2. Import it with https://impex.sphere.io/commands/category-import

### import products
1. Use the product structure as in products.csv
2. Import it with https://impex.sphere.io/commands/product-import



## Run the importer

### prepare a payload json file

The payload file does not contain commerce data but configuration for the import.

Add your commercetools credentials and may remove jobs you don't need.

```
{
  "commercetools": {
    "projectKey": "",
    "clientId": "",
    "clientSecret": "",
    "authUrl": "",
    "apiUrl": ""
  },
  "jobs": [
    {
      "name": "productsDeleteJob"
    },
    {
      "name": "categoriesDeleteJob"
    },
    {
      "name": "productTypesDeleteJob"
    },
    {
      "name": "productTypeCreateJob",
      "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/product-types/product-types.json"
    },
    {
      "name": "categoriesCreateJob",
      "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/categories/categories.csv"
    },
    {
      "name": "productsCreateJob",
      "maxProducts": 4000,
      "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/products/products.csv"
    },
    {
      "name": "ordersCreateJob",
      "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/orders/orders.json"
    }
  ]
}
```

### run it with Maven

```
export PAYLOAD_FILE=path/to/payload/file
mvn spring-boot:run -Dstart-class=com.commercetools.dataimport.all.PayloadJobMain
```

### joyride


```
{
    "commercetools": {
    "projectKey": "",
    "clientId": "",
    "clientSecret": "",
    "authUrl": "",
    "apiUrl": ""
    },
    "jobs": [
        {
            "name": "importJoyrideChannelsJob",
            "channelsResource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/joyride/channels.json",
            "typesResource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/data/joyride/types.json"
        },
        {
            "name": "inventoryEntryCreationJob"
        },
        {
            "name": "availabilityPricesImportJob"
        }
    ]
}
```

