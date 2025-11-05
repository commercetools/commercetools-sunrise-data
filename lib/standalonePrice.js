import { createRequestBuilder, features } from '@commercetools/api-request-builder'
import { config } from './config'
import { logAndExit, execute, getAll } from './helpers'
import { SingleBar, Presets } from 'cli-progress'
import { createStandardDelete } from './helpers'

require('dotenv').config()
const nconf = require('nconf')

const getStandalonePriceService = () =>
  createRequestBuilder({
    projectKey: config.projectKey,
    customServices: {
      'standalone-price': {
        type: 'standalone-price',
        endpoint: '/standalone-prices',
        features: [features.query, features.queryOne]
      }
    }
  })['standalone-price']

export const deleteStandalonePrice = createStandardDelete({
  itemName: 'standalone price',
  service: getStandalonePriceService
})

// Simple price parser for EUR and USD prices only
// Format: "EUR 15000" or "USD 15000" or "US-USD 15000"
const parsePrice = (priceStr, sku) => {
  const trimmed = priceStr.trim()

  // Match EUR or USD/US-USD followed by amount
  const match = trimmed.match(/^(EUR|USD|US-USD)\s+(\d+)/)
  if (!match) {
    return null
  }

  const currencyCode = match[1] === 'US-USD' ? 'USD' : match[1]
  const centAmount = parseInt(match[2], 10)

  if (isNaN(centAmount)) {
    return null
  }

  return {
    key: `${sku}-${currencyCode}`,
    sku,
    value: {
      currencyCode,
      centAmount,
      type: 'centPrecision',
      fractionDigits: 2
    }
  }
}

// Parse prices from semicolon-separated string
const parsePrices = (pricesStr, sku) => {
  if (!pricesStr || !pricesStr.trim()) {
    return []
  }

  return pricesStr
    .split(';')
    .map(priceStr => parsePrice(priceStr, sku))
    .filter(price => price !== null)
}

export const importStandalonePriceFromProducts = (
  productPath = './data/products-standalone.csv',
  limit
) => {
  const standalonePriceService = getStandalonePriceService()
  const csv = require('csvtojson')
  const notifySave = new SingleBar(
    {
      format:
        'Save prices         {bar} |' +
        '| {percentage}% || {value}/{total} prices',
      barCompleteChar: '\u2588',
      barIncompleteChar: '\u2591'
    },
    Presets.rect
  )

  return csv()
    .fromFile(productPath)
    .then((rawProducts) => {
      // Group products by variantId === 1 (same as standaloneProducts.js)
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

      // Group products and apply limit if provided
      const groupedProducts = limit
        ? groupProducts(rawProducts).slice(0, parseInt(limit, 10))
        : groupProducts(rawProducts)

      // Flatten to get all variant rows from the grouped products
      const productsToProcess = groupedProducts.reduce((acc, group) => acc.concat(group), [])

      // Extract all prices from products
      const allPrices = []
      productsToProcess.forEach((product) => {
        if (product.sku && product.prices) {
          const prices = parsePrices(product.prices, product.sku)
          allPrices.push(...prices)
        }
      })

      // Deduplicate prices by SKU + currency
      const uniquePrices = new Map()
      const usedKeys = new Set()
      allPrices.forEach((price) => {
        const scopeKey = `${price.sku}-${price.value.currencyCode}`

        if (!uniquePrices.has(scopeKey)) {
          // Ensure price key is unique
          let uniqueKey = price.key
          let counter = 1
          while (usedKeys.has(uniqueKey)) {
            uniqueKey = `${price.key}-${counter}`
            counter++
          }
          usedKeys.add(uniqueKey)
          uniquePrices.set(scopeKey, { ...price, key: uniqueKey })
        }
      })
      const deduplicatedPrices = Array.from(uniquePrices.values())

      console.log(`\nTotal price entries: ${allPrices.length}`)
      console.log(`Unique prices (after deduplication): ${deduplicatedPrices.length}`)
      if (allPrices.length !== deduplicatedPrices.length) {
        console.log(`Skipped ${allPrices.length - deduplicatedPrices.length} duplicate prices`)
      }

      notifySave.start(deduplicatedPrices.length, 0, {})
      let processed = 0
      let skipped = 0

      return Promise.all(
        deduplicatedPrices.map((priceDraft) => {
          return execute({
            uri: standalonePriceService.build(),
            method: 'POST',
            body: priceDraft
          })
            .then(() => notifySave.update(++processed))
            .catch((err) => {
              // Skip duplicate scope errors - these are expected
              if (err.statusCode === 400 && err.body?.errors?.some(e => e.code === 'DuplicateStandalonePriceScope')) {
                skipped++
                return Promise.resolve()
              }
              // Re-throw other errors
              throw err
            })
        })
      ).then(() => {
        notifySave.stop()
        if (skipped > 0) {
          console.log(`Skipped ${skipped} duplicate price scopes during import`)
        }
        // eslint-disable-next-line no-console
        console.log(
          '\x1b[32m%s\x1b[0m',
          'Standalone Prices imported from products'
        )
      })
        .catch((err) => {
          notifySave.stop()
          return logAndExit(
            err,
            'Failed to import standalonePrices from products'
          )
        })
    })
}

if (nconf.get('clean')) {
  deleteStandalonePrice()
} else if (nconf.get('import')) {
  if (!nconf.get('fromProducts')) {
    logAndExit(
      new Error('Standalone price import requires --fromProducts flag. Use: node standalonePrice.js --import --fromProducts --csv ./data/products.csv')
    )
  }
  // eslint-disable-next-line no-console
  console.log(
    '\x1b[32m%s\x1b[0m',
    'Importing StandalonePrices from products...'
  )
  importStandalonePriceFromProducts(nconf.get('csv'), nconf.get('limit'))
}
