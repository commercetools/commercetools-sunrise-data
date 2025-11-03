import {
  customerGroupService,
  channelsService
} from './services'
import { createRequestBuilder, features } from '@commercetools/api-request-builder'
import { config } from './config'
import { logAndExit, execute, getAll, setByKey } from './helpers'
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

const toPrice =
  (customerGroups, channels) => (stringPrice, sku) => {
    const [currencyCode, amount, custGroup] =
      stringPrice.split(/\s/)
    const [newAmount, channel] = amount.split('#')
    const [cntry, newCurrencyCode] = currencyCode.split(/-/)
    const channelInfo = channel && channels.get(channel)
      ? {
        channel: {
          typeId: 'channel',
          id: channels.get(channel).id
        }
      }
      : {}
    let centAmount = Number(channel ? newAmount : amount)
    // eslint-disable-next-line no-restricted-globals
    if (isNaN(centAmount)) {
      // prices like 1|2; 2 will be ignored
      centAmount = Number(
        (channel ? newAmount : amount).split('|')[0]
      )
    }
    const customerGroup = custGroup && customerGroups.get(custGroup)
      ? {
        customerGroup: {
          typeId: 'customer-group',
          id: customerGroups.get(custGroup).id
        }
      }
      : {}
    // Include country code if present (project is configured with required countries)
    // Only set country if newCurrencyCode exists (meaning currencyCode had a country prefix like DE-EUR)
    const country = newCurrencyCode ? { country: cntry } : {}
    const finalCurrencyCode = newCurrencyCode || currencyCode
    return {
      key: `${sku}-${finalCurrencyCode}${channel ? `-${channel}` : ''}${custGroup ? `-${custGroup}` : ''}${newCurrencyCode ? `-${cntry}` : ''}`,
      sku,
      value: {
        currencyCode: finalCurrencyCode,
        centAmount,
        type: 'centPrecision',
        fractionDigits: 2
      },
      ...customerGroup,
      ...country,
      ...channelInfo
    }
  }

const toPrices = (stringPrices, customerGroups, channels, sku) =>
  stringPrices
    .split(';')
    .map((priceStr) => {
      try {
        return toPrice(customerGroups, channels)(priceStr, sku)
      } catch (err) {
        // Skip prices with missing customer groups or channels
        return null
      }
    })
    .filter((x) => x)

export const importStandalonePriceFromProducts = (
  productPath = './data/products.csv',
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
  return Promise.all([
    csv().fromFile(productPath),
    getAll(execute, customerGroupService)({
      method: 'GET'
    }),
    getAll(execute, channelsService)({
      method: 'GET'
    })
  ]).then(([rawProducts, customerGroups, channels]) => {
    const customerGroupsByKey = setByKey(customerGroups)
    const channelsByKey = setByKey(channels)

    // Group products by variantId === 1 (same as standaloneProducts.js)
    // This ensures we process prices for all variants of the same grouped products
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

    // Group products and apply limit if provided (matches standaloneProducts.js logic)
    const groupedProducts = limit
      ? groupProducts(rawProducts).slice(0, parseInt(limit, 10))
      : groupProducts(rawProducts)

    // Flatten to get all variant rows from the grouped products
    const productsToProcess = groupedProducts.reduce((acc, group) => acc.concat(group), [])

    // Extract all prices from products
    const allPrices = []
    productsToProcess.forEach((product) => {
      if (product.sku && product.prices) {
        const prices = toPrices(
          product.prices,
          customerGroupsByKey,
          channelsByKey,
          product.sku
        )
        allPrices.push(...prices)
      }
    })

    // Deduplicate prices by actual price scope
    // Price scope = SKU + currency + country + customerGroup + channel
    const uniquePrices = new Map()
    const usedKeys = new Set()
    allPrices.forEach((price) => {
      const customerGroupId = price.customerGroup?.customerGroup?.id || ''
      const channelId = price.channel?.channel?.id || ''
      const countryCode = price.country || ''
      const scopeKey = `${price.sku}-${price.value.currencyCode}-${countryCode}-${customerGroupId}-${channelId}`

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
