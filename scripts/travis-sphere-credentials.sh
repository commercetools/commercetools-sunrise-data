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