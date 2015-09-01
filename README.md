# How to create the data (product types, products and categories)

## csv files encoding:
 - delimited by ";"
 - encoding / file origin: UTF-8
 - use text data format for ALL columns
 
## creating product types
### Go to: https://impex.sphere.io/commands/product-type-generator
Use sunrise-producttypes.csv and sunrise-producttype-attributes.csv for generate a JSON

### Import it here: https://impex.sphere.io/playground
 - Project credentials: Project key / Client ID / Client secret
 - Endpoint: Product Types
 - Method: Create

## import categories
### Use the category structure as in sunrise-categories.csv

### Import it here:https://impex.sphere.io/commands/category-import
 - Project credentials: Project key / Client ID / Client secret
 
## import product data
### Use the product structure as in sunrise-product-data.csv

### Import it here:https://impex.sphere.io/commands/product-import
 - Project credentials: Project key / Client ID / Client secret
