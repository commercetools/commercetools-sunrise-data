# Sunrise Data

[![Build Status](https://travis-ci.org/commercetools/commercetools-sunrise-data.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sunrise-data)

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
      "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/product-types/product-types.json"
    },
    {
      "name": "categoriesCreateJob",
      "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/categories/categories.csv"
    },
    {
      "name": "productsCreateJob",
      "maxProducts": 4000,
      "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/products/products.csv"
    },
    {
      "name": "ordersCreateJob",
      "resource": "https://raw.githubusercontent.com/commercetools/commercetools-sunrise-data/master/orders/orders.json"
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
            "channelsResource": "file:///Users/yourusername/dev/commercetools-sunrise-data/joyride/channels.json",
            "typesResource": "file:///Users/yourusername/dev/commercetools-sunrise-data/joyride/types.json"
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

