# Standalone Variants Import Guide

This guide explains how to create a project and import standalone products/variants with standalone prices.

## Step 1: Create Project

Set up environment variables and create the project (make sure you are logged in for staging vault):

```bash
export API_URL=https://prototype-64-api.prototype.europe-west1.gcp.commercetools.com
export AUTH_URL=https://prototype-64-auth.prototype.europe-west1.gcp.commercetools.com
export CORE_URL=https://prototype-64-core.prototype.europe-west1.gcp.commercetools.com
export CORE_CLIENT_ID=sphere-prototype-internal.1.0
export VAULT_ADDR="https://vault.sre.europe-west1.gcp.commercetools.com"
export CORE_CLIENT_SECRET=$(vault kv get --field=SPHERE_PROTOTYPE_INTERNAL_1_0_TRUSTED_CLIENT_SECRET kv2/repositories/ctp-prototype/ctp-prototype-v1/static-secrets)

bash create_project.sh $PROJECT_NAME
```

This creates a project with a name like
`sv-poc-$PROJECT_NAME-868148896451206` and saves the credentials in
Postman format in file `$PROJECT_NAME.postman_environment.json`. (The
project name param is optional, if you leave it out the credentials
will just be printed.)

Save the output: **Project Key**, **Client ID**, and **Client Secret**.

The project is created with **EUR** and **USD** currencies and required countries.

## Step 2: Set Import Environment Variables

```bash
export CTP_PROJECT_KEY=<project-key-from-step-1>
export CTP_CLIENT_ID=<client-id-from-step-1>
export CTP_CLIENT_SECRET=<client-secret-from-step-1>
export CTP_AUTH_URL=https://prototype-64-auth.prototype.europe-west1.gcp.commercetools.com
export CTP_API_URL=https://prototype-64-api.prototype.europe-west1.gcp.commercetools.com
```

## Step 3: Import Data

```bash
npm run import:types
npm run import:categories
npm run import:taxCategories
npm run import:channels
npm run import:standaloneProducts
```

## Verify Import

```bash
TOKEN=$(curl -s -X POST -u "$CTP_CLIENT_ID:$CTP_CLIENT_SECRET" "$CTP_AUTH_URL/oauth/token" -d "grant_type=client_credentials" | jq -r .access_token)

curl -s -H "Authorization: Bearer $TOKEN" "$CTP_API_URL/$CTP_PROJECT_KEY/products?limit=0" | jq '{total: .total}'
curl -s -H "Authorization: Bearer $TOKEN" "$CTP_API_URL/$CTP_PROJECT_KEY/standalone-variants?limit=0" | jq '{total: .total}'
curl -s -H "Authorization: Bearer $TOKEN" "$CTP_API_URL/$CTP_PROJECT_KEY/standalone-prices?limit=0" | jq '{total: .total}'
```

## Example Data Structures

### Sample Product

Products contain only metadata and product-level attributes now.

```json
{
  "id": "ce1b7655-b175-4114-822f-bc868af0a8ae",
  "key": "82054",
  "version": 1,
  "priceMode": "Standalone",
  "masterData": {
    "current": {
      "name": { "en": "Shoulder bag DKNY black", "de": "Umhängetasche DKNY schwarz" },
      "slug": { "en": "dkny-shoulder-bag-r1513010-black", "de": "dkny-umhaengetasche-r1513010-schwarz" },
      "attributes": [
        {
          "name": "designer",
          "value": { "key": "dkny", "label": "DKNY" }
        }
      ],
      "masterVariant": { "id": 1, "prices": [], "images": [], "attributes": [] },
      "variants": []
    },
    "staged": { ...  },
    "published": true,
    "hasStagedChanges": false
  }
}
```

**Note**: In Standalone Variants mode, products don't contain actual variant data in `masterVariant` or `variants` arrays - those are empty. Variants are managed separately as standalone variants.

### Sample Standalone Variant

Standalone variants contain SKU, images, variant-level attributes, and are linked to their product. Variant data is stored in `current` (published) and optionally `staged` (unpublished changes):

```json
{
  "id": "7c3a5b5e-fc68-4b6f-a21a-bcf95f46e538",
  "key": "M0E20000000E1B8",
  "version": 1,
  "product": { "typeId": "product", "id": "bbe329cd-7f64-4dc4-8bbd-c98df353ba84" },
  "published": true,
  "current": {
    "sku": "M0E20000000E1B8",
    "images": [
      {
        "url": "https://s3-eu-west-1.amazonaws.com/commercetools-maximilian/products/079560_1_large.jpg",
        "dimensions": { "w": 0, "h": 0 }
      }
    ],
    "attributes": [ { "name": "size", "value": "9.5" } ]
  },
  "staged": null
}
```

**Note**: When `published=true`, the `current` section contains the published variant data. The `staged` section contains unpublished changes (if any), otherwise it's `null`.

## Storefront API Examples

### Standalone Variant Projections API

For storefronts, use the **Standalone Variant Projections** endpoint which combines variant data with embedded product information:

```bash
TOKEN=$(curl -s -X POST -u "$CTP_CLIENT_ID:$CTP_CLIENT_SECRET" "$CTP_AUTH_URL/oauth/token" -d "grant_type=client_credentials" | jq -r .access_token)

# Query with price selection (e.g., for a specific currency)
curl -s -H "Authorization: Bearer $TOKEN" \
  "$CTP_API_URL/$CTP_PROJECT_KEY/standalone-variant-projections?staged=false&priceCurrency=USD"
```

**Example Projection Response (with price selection):**

```json
{
  "id": "93783d6b-4bf9-4a3e-9d2a-4fab7254ca20",
  "staged": false,
  "version": 1,
  "product": { "typeId": "product", "id": "bbe329cd-7f64-4dc4-8bbd-c98df353ba84" },
  "name": { "en": "Sneakers New Balance multi" },
  "slug": { "en": "newbalance-sneakers-MT980BB-multi" },
  "sku": "M0E20000000E1B4",
  "attributes": [ { "name": "size", "value": "7.5" } ],
  "images": [ { "url": "https://s3-eu-west-1.amazonaws.com/commercetools-maximilian/products/079560_1_large.jpg" } ],
  "price": {
    "id": "2c8d0e43-7b66-4981-ad83-367c0719d0e6",
    "value": {
      "type": "centPrecision",
      "currencyCode": "USD",
      "centAmount": 15000,
      "fractionDigits": 2
    },
    "key": "M0E20000000E1B4-US-USD-US"
  }
}
```

**Note**: Without `priceCurrency` parameter, the `price` field will be `null`. Prices are selected and included only when you specify `priceCurrency` (and optionally `priceCountry`, `priceCustomerGroup`, `priceChannel`).

## Data Model Overview

The **Sunrise Data Model** uses standalone variants where:

1. **Products** (`/products`) contain:
   - Product metadata (name, description, slug)
   - Product-level attributes (e.g., `baseId`, `matrixId`, `designer`)
   - **No embedded variants** - variants are managed separately

2. **Standalone Variants** (`/standalone-variants`) contain:
   - Variant-specific data (SKU, images, assets)
   - Variant-level attributes (e.g., `size`, `color`, `gender`)
   - Reference to parent product

3. **Standalone Prices** (`/standalone-prices`) contain:
   - Price values per currency
   - Channel and customer group associations
   - Linked to variants via SKU
