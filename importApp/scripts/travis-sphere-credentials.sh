cat > "importApp/travis-payload.json" << EOF
{
  "commercetools": {
    "projectKey": "$PROJECT_KEY",
    "clientId": "$CLIENT_ID",
    "clientSecret": "$CLIENT_SECRET",
    "authUrl": "https://auth.sphere.io",
    "apiUrl": "https://api.sphere.io"
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
      "resource": "../product-types/product-types.json"
    },
    {
      "name": "categoriesCreateJob",
      "resource": "../categories/categories.csv"
    },
    {
      "name": "productsCreateJob",
      "maxProducts": 4000,
      "resource": "../products/products.csv"
    }
  ]
}
EOF