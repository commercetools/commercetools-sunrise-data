cat > "travis-payload.json" << EOF
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
      "name": "productsCreateJob",
      "maxProducts": 4000,
      "resource": "file://$ROOT_PROJ/data/products/products.csv"
    }
  ]
}
EOF

cat > "src/test/resources/test.properties" << EOF
commercetools.projectKey=$PROJECT_KEY
commercetools.clientId=$CLIENT_ID
commercetools.clientSecret=$CLIENT_SECRET
commercetools.authUrl=https://auth.sphere.io
commercetools.apiUrl=https://api.sphere.io
EOF