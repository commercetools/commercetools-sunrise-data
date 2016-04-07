# How to create the data with the commercetools impex tool

## csv files encoding:
 - delimited by ","
 - encoding / file origin: UTF-8
 - use text data format for ALL columns
 
## import product types
1. Go to https://impex.sphere.io/commands/product-type-generator
Use product-types.csv and product-type-attribute-definitions.csv for generate a JSON

2. Import it with https://impex.sphere.io/playground
 - Endpoint: Product Types
 - Method: Create

## import categories
1. Use the category structure as in categories.csv

2. Import it with https://impex.sphere.io/commands/category-import

## import products
1. Use the product structure as in products.csv
2. Import it with https://impex.sphere.io/commands/product-import

# Import app

[![Build Status](https://travis-ci.org/sphereio/commercetools-sunrise-data.svg?branch=master)](https://travis-ci.org/sphereio/commercetools-sunrise-data)
