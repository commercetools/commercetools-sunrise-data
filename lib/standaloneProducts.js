import { SingleBar, Presets } from 'cli-progress'
import {
  productsService,
  productTypesService,
  standaloneVariantsService,
  storeService,
  productSelectionService
} from './services'
import {
  execute,
  NONE,
  getAll,
  setBy,
  setByKey,
  readJson,
  logAndExit,
  createStandardDelete
} from './helpers'
import { importStores } from './stores'

require('dotenv').config()
const nconf = require('nconf')

export const deleteAllStandaloneVariants = createStandardDelete({
  itemName: 'standalone variants',
  service: standaloneVariantsService,
  deleteFunction: (variant) =>
    Promise.resolve(
      variant.published
        ? execute({
          uri: standaloneVariantsService.byId(variant.id).build(),
          method: 'POST',
          body: {
            version: variant.version,
            actions: [
              {
                action: 'unpublish'
              }
            ]
          }
        }).then(({ body }) => body)
        : variant
    ).then((variant) =>
      execute({
        uri: standaloneVariantsService
          .byId(variant.id)
          .withVersion(variant.version)
          .build(),
        method: 'DELETE'
      }).catch((err) => {
        // Ignore 404 errors (already deleted)
        if (err.statusCode === 404) {
          return Promise.resolve()
        }
        throw err
      })
    )
})

export const deleteAllProducts = createStandardDelete({
  itemName: 'products',
  service: productsService,
  deleteFunction: (product) =>
    Promise.resolve(
      product.masterData && product.masterData.published
        ? execute({
          uri: productsService.byId(product.id).build(),
          method: 'POST',
          body: {
            version: product.version,
            actions: [
              {
                action: 'unpublish'
              }
            ]
          }
        }).then(({ body }) => body)
        : product
    ).then((product) =>
      execute({
        uri: productsService
          .byId(product.id)
          .withVersion(product.version)
          .build(),
        method: 'DELETE'
      }).catch((err) => {
        // Ignore 404 errors (already deleted)
        if (err.statusCode === 404) {
          return Promise.resolve()
        }
        throw err
      })
    )
})

export const deleteAllProductTypes = createStandardDelete({
  itemName: 'product types',
  service: productTypesService
})

export const removeProductSelectionsFromStores = async () => {
  console.log('Removing product selections from stores')
  const storeKeys = ['default', 'dach', 'europe', 'uk']

  for (const storeKey of storeKeys) {
    try {
      // Get the store
      const storeResponse = await execute({
        uri: storeService.byKey(storeKey).build(),
        method: 'GET'
      })

      const store = storeResponse.body
      const productSelections = store.productSelections || []

      // Remove all product selections from the store
      if (productSelections.length > 0) {
        let currentVersion = store.version
        for (const ps of productSelections) {
          const updateResponse = await execute({
            uri: storeService.byKey(storeKey).build(),
            method: 'POST',
            body: {
              version: currentVersion,
              actions: [{
                action: 'removeProductSelection',
                productSelection: {
                  typeId: 'product-selection',
                  id: ps.productSelection.id
                }
              }]
            }
          })
          currentVersion = updateResponse.body.version
        }
      }
    } catch (error) {
      // Ignore 404 errors (store doesn't exist)
      if (error.statusCode !== 404) {
        console.error(`Error removing product selections from store ${storeKey}:`, error.message)
      }
    }
  }
}

export const deleteAllProductSelections = createStandardDelete({
  itemName: 'product selections',
  service: productSelectionService
})

// Attributes that are shared across all variants (product-level)
const PRODUCT_LEVEL_ATTRIBUTES = [
  'baseId',
  'matrixId',
  'articleNumberMax',
  'designer',
  'creationDate'
]

export const importProductTypes = (
  typesPath = './data/product-type.json'
) =>
  readJson(typesPath)
    .then((productTypes) =>
      Promise.all(
        productTypes.map((element) => {
          // Add level to attributes based on whether they're product or variant level
          if (element.attributes) {
            element.attributes = element.attributes.map((attr) => {
              if (!attr.level) {
                const isProductLevel = PRODUCT_LEVEL_ATTRIBUTES.includes(attr.name)
                if (isProductLevel) {
                  attr.level = 'Product'
                  // Product-level attributes can only have constraint 'None'
                  if (attr.attributeConstraint && attr.attributeConstraint !== 'None') {
                    attr.attributeConstraint = 'None'
                  }
                } else {
                  attr.level = 'Variant'
                }
              }
              return attr
            })
          }
          const updateRequest = {
            uri: productTypesService.build(),
            method: 'POST',
            body: element
          }
          return execute(updateRequest)
        })
      )
    )
    .then(() =>
      // eslint-disable-next-line no-console
      console.log(
        '\x1b[32m%s\x1b[0m',
        'Product types imported'
      )
    )
    .catch((error) =>
      logAndExit(error, 'Failed to import product types')
    )

const asSlugsEn = (categoryString) =>
  categoryString
    .toLowerCase()
    .split(';')
    .filter((c) => c)
    .map((c) => c.replace(/>|\s/g, '-'))
const withCategories = (allCategories, categories) =>
  asSlugsEn(categories).map((slug) => {
    const category = allCategories.get(slug)
    if (!category) {
      throw new Error(
        `Cannot find category for slug:${slug}`
      )
    }
    return { key: category.key }
  })

const groupProducts = (products) =>
  products
    .map((p) => ({
      ...p,
      variantIdNum: Number(p.variantId)
    }))
    .reduce((grouped, product) => {
      if (product.variantIdNum === 1) {
        grouped.push([product])
        return grouped
      }
      grouped[grouped.length - 1].push(product)
      return grouped
    }, [])

const removeEmpty = (o) => {
  const ret = Object.entries(o).reduce(
    (result, [key, value]) =>
      value !== ''
        ? ((result[key] = value), result)
        : result,
    {}
  )
  return Object.keys(ret).length === 0 ? NONE : ret
}
const noAllEmpty = (o) =>
  Object.keys(o).length === 0 ? undefined : o
const toImages = (images) => images.split(';').map(url => ({
  url,
  dimensions: {
    w: 0,
    h: 0
  }
}))

const toAttribute = (
  attributeName,
  value,
  attributeType
) => {
  if (value === undefined || value === '') {
    return NONE
  }
  if (attributeType.name === 'boolean') {
    value = value === 'TRUE'
  }
  // "dateTime","lenum", "enum" and "text" don't need anything
  if (attributeType.name === 'set') {
    if (attributeType.elementType.name === 'text') {
      return {
        name: attributeName,
        value: value.split(';').filter((x) => x)
      }
    }
    // details is set of ltext but does not have a value for any product
    //  therefor it is ignored for now
    return NONE
  }
  if (attributeType.name === 'ltext') {
    value = removeEmpty(value)
    if (Object.keys(value).length === 0) {
      return NONE
    }
  }

  return {
    name: attributeName,
    value
  }
}

const toProduct = (
  categoriesBySlug,
  attributesByType
) =>
  (products) => {
    const {
      productType,
      tax,
      categories,
      name,
      baseId,
      slug,
      publish
    } = products[0]
    if (Object.keys(removeEmpty(name)).length === 0) {
      return {
        type: NONE,
        products,
        rejected: 'empty name'
      }
    }
    if (Object.keys(removeEmpty(slug)).length === 0) {
      return {
        type: NONE,
        products,
        rejected: 'empty slug'
      }
    }
    const metaDescription = noAllEmpty(
      removeEmpty(products[0].description)
    )
    const metaTitle = noAllEmpty(
      removeEmpty(products[0].metaTitle)
    )
    const metaKeywords = noAllEmpty(
      removeEmpty(products[0].metaKeywords)
    )
    // Separate product-level and variant-level attributes
    const productAttributes = attributesByType
      .get(productType)
      .filter(([attributeName]) => PRODUCT_LEVEL_ATTRIBUTES.includes(attributeName))
      .map(([attributeName, attributeType]) =>
        toAttribute(
          attributeName,
          products[0][attributeName],
          attributeType
        )
      )
      .filter((attribute) => attribute !== NONE)

    const mainProduct = {
      key: baseId,
      productType: { key: productType },
      taxCategory: {
        key: tax
      },
      categories: withCategories(
        categoriesBySlug,
        categories
      ),
      name: removeEmpty(name),
      slug: removeEmpty(slug),
      publish: publish === 'TRUE',
      metaDescription,
      metaTitle,
      metaKeywords,
      attributes: productAttributes
    }

    return {
      product: mainProduct,
      variants: products
    }
  }

const createStandaloneVariant = (
  attributesByType,
  productType
) =>
  (product, productId) => {
    const { sku, images, variantKey, publish } = product
    const variant = {
      productId,
      key: variantKey || sku,
      attributes: attributesByType
        .get(productType)
        .filter(([attributeName]) => !PRODUCT_LEVEL_ATTRIBUTES.includes(attributeName))
        .map(([attributeName, attributeType]) =>
          toAttribute(
            attributeName,
            product[attributeName],
            attributeType
          )
        )
        .filter((attribute) => attribute !== NONE),
      sku,
      images: toImages(images),
      publish: publish === 'TRUE'
    }
    return variant
  }

// Create product selections for each store and add variant exclusions
const createProductSelections = async (exclusionsByStore, productsByKey) => {
  // Store keys: default, dach, europe, uk
  const storeKeys = ['default', 'dach', 'europe', 'uk']

  const notifyProductSelections = new SingleBar(
    {
      format:
        'Create selections   {bar} |' +
        '| {percentage}% || {value}/{total} selections',
      barCompleteChar: '\u2588',
      barIncompleteChar: '\u2591'
    },
    Presets.rect
  )

  notifyProductSelections.start(storeKeys.length, 0, {})

  // Create product selection for each store
  for (const storeKey of storeKeys) {
    const productSelection = {
      key: `store-selection-${storeKey}`,
      name: {
        en: `Store Selection - ${storeKey}`,
        de: `Store Selection - ${storeKey}`,
        it: `Store Selection - ${storeKey}`
      },
      mode: 'Individual'
    }

    try {
      const selectionResponse = await execute({
        uri: productSelectionService.build(),
        method: 'POST',
        body: productSelection
      })

      const selectionId = selectionResponse.body.id
      const selectionVersion = selectionResponse.body.version

      // Add all products to selection with variant exclusions
      const exclusionsForStore = exclusionsByStore[storeKey] || {}
      let currentVersion = selectionVersion

      for (const productKey of Object.keys(productsByKey)) {
        const productExclusions = exclusionsForStore[productKey] || []

        try {
          const action = {
            action: 'addProduct',
            product: {
              typeId: 'product',
              key: productKey
            }
          }

          // Add variant selection to exclude specific SKUs if exclusions exist
          if (productExclusions.length > 0) {
            action.variantSelection = {
              type: 'includeAllExcept',
              skus: productExclusions
            }
          }

          const updateResponse = await execute({
            uri: productSelectionService
              .byId(selectionId)
              .build(),
            method: 'POST',
            body: {
              version: currentVersion,
              actions: [action]
            }
          })
          currentVersion = updateResponse.body.version
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(`Error adding product ${productKey} to selection:`, error.message)
        }
      }

      // Link product selection to store
      try {
        const storeResponse = await execute({
          uri: storeService.byKey(storeKey).build(),
          method: 'GET'
        })

        const storeVersion = storeResponse.body.version
        await execute({
          uri: storeService.byKey(storeKey).build(),
          method: 'POST',
          body: {
            version: storeVersion,
            actions: [{
              action: 'addProductSelection',
              productSelection: {
                typeId: 'product-selection',
                id: selectionId
              },
              active: true
            }]
          }
        })
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(`Error linking product selection to store ${storeKey}:`, error.message)
      }

      notifyProductSelections.increment()
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(`Error creating product selection for store ${storeKey}:`, error.message)
      notifyProductSelections.increment()
    }
  }

  notifyProductSelections.stop()
}

export const importStandaloneProducts = (
  productPath = './data/products-standalone.csv',
  categoriesPath = './data/categories.csv',
  typesPath = './data/product-type.json',
  limit = Number.POSITIVE_INFINITY
) => {
  const csv = require('csvtojson')
  const notifySave = new SingleBar(
    {
      format:
        'Save products      {bar} |' +
        '| {percentage}% || {value}/{total} products',
      barCompleteChar: '\u2588',
      barIncompleteChar: '\u2591'
    },
    Presets.rect
  )
  const notifyVariants = new SingleBar(
    {
      format:
        'Save variants      {bar} |' +
        '| {percentage}% || {value}/{total} variants',
      barCompleteChar: '\u2588',
      barIncompleteChar: '\u2591'
    },
    Presets.rect
  )

  // First import stores
  return importStores()
    .then(() =>
      Promise.all(
        [productPath, categoriesPath]
          .map((path) => csv().fromFile(path))
          .concat([
            readJson(typesPath)
          ])
      )
    )
    .then(
      ([
        rawProducts,
        rawCategories,
        productTypes
      ]) => {
        const categoriesBySlug = setBy((x) => x.slug.en)(
          rawCategories
        )
        const attributesByType = [
          ...setBy((x) => x.key)(productTypes).entries()
        ].reduce(
          (result, [key, value]) =>
            result.set(
              key,
              value.attributes.map((v) => [v.name, v.type])
            ),
          new Map()
        )
        const groupedProducts = groupProducts(
          rawProducts
        ).slice(0, limit)
        notifySave.start(groupedProducts.length, 0, {})
        let processed = 0
        const productsToSave = groupedProducts
          .map(
            toProduct(
              categoriesBySlug,
              attributesByType
            )
          )
          .filter((p) => !p.rejected)

        // Track exclusions: { storeKey: { productKey: [skus...] } }
        const exclusionsByStore = {}
        const productsByKey = {}

        // First, create all products
        return Promise.all(
          productsToSave.map(
            (productData) =>
              execute({
                uri: productsService.build(),
                method: 'POST',
                body: productData.product
              }).then((response) => {
                notifySave.update(++processed)
                const productKey = productData.product.key
                productsByKey[productKey] = response.body.id

                return {
                  productId: response.body.id,
                  productKey: productKey,
                  productType: productData.product.productType.key,
                  variants: productData.variants
                }
              })
          )
        ).then((productsWithIds) => {
          notifySave.stop()

          // Then create all standalone variants and track exclusions
          let totalVariants = 0
          const variantPromises = []
          productsWithIds.forEach(({ productId, productKey, productType, variants }) => {
            variants.forEach((variantData) => {
              totalVariants++
              const variantDraft = createStandaloneVariant(
                attributesByType,
                productType
              )(variantData, productId)

              // Track variant exclusions from excludedInStore column
              const excludedInStore = (variantData.excludedInStore || '').trim()
              if (excludedInStore) {
                excludedInStore.split(/\s+/).forEach(storeKey => {
                  if (!exclusionsByStore[storeKey]) {
                    exclusionsByStore[storeKey] = {}
                  }
                  if (!exclusionsByStore[storeKey][productKey]) {
                    exclusionsByStore[storeKey][productKey] = []
                  }
                  exclusionsByStore[storeKey][productKey].push(variantData.sku)
                })
              }

              variantPromises.push(
                execute({
                  uri: standaloneVariantsService.build(),
                  method: 'POST',
                  body: variantDraft
                })
              )
            })
          })

          notifyVariants.start(totalVariants, 0, {})
          let variantProcessed = 0
          return Promise.all(
            variantPromises.map((promise) =>
              promise.then(() => {
                notifyVariants.update(++variantProcessed)
                return true
              })
            )
          ).then(() => {
            notifyVariants.stop()
            // Create product selections after all variants are created
            return createProductSelections(exclusionsByStore, productsByKey)
          })
        })
      }
    )
    .then(() =>
      // eslint-disable-next-line no-console
      console.log('\x1b[32m%s\x1b[0m', 'Standalone products imported')
    )
    .catch((reject) => {
      notifySave.stop()
      notifyVariants.stop()
      return logAndExit(reject, 'Failed to import standalone products')
    })
}

if (nconf.get('clean')) {
  // Important deletion order:
  // 1. Delete standalone variants (can't delete products while variants exist)
  // 2. Remove product selections from stores (can't delete product selections while referenced by stores)
  // 3. Delete product selections (can't delete products while referenced by selections)
  // 4. Delete products
  // 5. Delete product types (can't delete product types while products exist)
  deleteAllStandaloneVariants()
    .then(() => removeProductSelectionsFromStores())
    .then(() => deleteAllProductSelections())
    .then(() => deleteAllProducts())
    .then(() => deleteAllProductTypes())
    .then(() => {
      // eslint-disable-next-line no-console
      console.log('\x1b[32m%s\x1b[0m', 'Cleanup completed successfully')
    })
    .catch((err) => {
      logAndExit(err, 'Failed during cleanup')
    })
} else if (nconf.get('importtypes')) {
  importProductTypes(nconf.get('types'))
} else if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log('\x1b[32m%s\x1b[0m', 'Importing standalone products...')
  const limit = nconf.get('limit') ? Number(nconf.get('limit')) : Number.POSITIVE_INFINITY
  importStandaloneProducts(
    nconf.get('csv'),
    nconf.get('categories'),
    nconf.get('types'),
    limit
  )
}

