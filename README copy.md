# Run the importer

## prepare a payload json file

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

## run it with Maven

```
export PAYLOAD_FILE=path/to/payload/file
mvn spring-boot:run -Dstart-class=com.commercetools.dataimport.all.PayloadJobMain
```

## joyride


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
