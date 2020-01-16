import { SingleBar, Presets } from 'cli-progress'
import {
  client,
  productsService,
  productTypesService,
  customerGroupService,
  channelsService
} from './services'
import { execute, NONE, getAll, setByKey, setBy, readJson } from './helpers'
require('dotenv').config()
const nconf = require('nconf')

const deleteAllProducts = cb => {
  const notify = {
    get: new SingleBar(
      {
        format:
          'Get products       {bar} |' +
          '| {percentage}% || {value}/{total} pages',
        barCompleteChar: '\u2588',
        barIncompleteChar: '\u2591'
      },
      Presets.rect
    ),
    unpublish: { stop: x => x },
    remove: { stop: x => x }
  }
  let started = false
  const updateStatus = (done, total) => {
    if (!started) {
      notify.get.start(total)
      started = true
    }
    notify.get.update(done)
  }

  getAll(execute, productsService)(
    {
      method: 'GET'
    },
    updateStatus
  )
    .then(products => {
      notify.get.stop()
      // for all products unplublish if it is published and return id and version of the product
      const published = products.filter(p => p.masterData.published)
      notify.unpublish = new SingleBar(
        {
          format:
            'Unpublish products {bar} |' +
            '| {percentage}% || {value}/{total} products',
          barCompleteChar: '\u2588',
          barIncompleteChar: '\u2591'
        },
        Presets.rect
      )
      notify.unpublish.start(published.length)
      let processed = 0
      return Promise.all(
        products.map(product =>
          product.masterData.published
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
            }).then(result => {
              const { id, version } = result.body
              notify.unpublish.update(++processed)
              return [id, version]
            })
            : [product.id, product.version]
        )
      )
    })
    .then(idVersions => {
      notify.unpublish.stop()
      notify.remove = new SingleBar(
        {
          format:
            'Delete products    {bar} |' +
            '| {percentage}% || {value}/{total} products',
          barCompleteChar: '\u2588',
          barIncompleteChar: '\u2591'
        },
        Presets.rect
      )
      notify.remove.start(idVersions.length)
      let processed = 0
      // products are unpublished, delete them
      return Promise.all(
        idVersions.map(([id, version]) =>
          execute({
            uri: productsService
              .byId(id)
              .withVersion(version)
              .build(),
            method: 'DELETE'
          }).then(resolve => {
            notify.remove.update(++processed)
            return resolve
          })
        )
      )
    })
    .then(() => {
      notify.remove.stop()
      console.log('\x1b[32m%s\x1b[0m', 'All products are succesfully deleted')
      // get all product types
      return getAll(
        execute,
        productTypesService
      )({
        method: 'GET'
      })
    })
    .then(productTypes =>
      Promise.all(
        productTypes.map(productType =>
          execute({
            uri: productTypesService
              .byId(productType.id)
              .withVersion(productType.version)
              .build(),
            method: 'DELETE'
          })
        )
      )
    )
    .then(() => {
      console.log(
        '\x1b[32m%s\x1b[0m',
        'All product types are succesfully deleted'
      )
      if (cb) {
        return cb()
      }
    })
    .catch(err => {
      // debugger
      notify.get.stop()
      notify.remove.stop()
      notify.unpublish.stop()
      console.log('Failed with error:', err)
    })
}

const importProductTypes = typesPath =>
  readJson(typesPath).then(productTypes =>
    Promise.all(
      productTypes.map(element => {
        const updateRequest = {
          uri: productTypesService.build(),
          method: 'POST',
          body: element
        }
        return client
          .execute(updateRequest)
          .catch(error => console.log(error.body.errors))
      })
    )
  )
const asSlugsEn = categoryString =>
  categoryString
    .toLowerCase()
    .split(';')
    .filter(c => c)
    .map(c => c.replace(/>|\s/g, '-'))
const withCategories = (allCategories, categories) =>
  asSlugsEn(categories).map(slug => {
    const category = allCategories.get(slug)
    if (!category) {
      throw new Error(`Cannot find category for slug:${slug}`)
    }
    return { key: category.key }
  })
const groupProducts = products =>
  products
    .map(p => ({
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

const toPrice = (customerGroups, channels) => stringPrice => {
  const [currencyCode, amount, customerGroup] = stringPrice.split(/\s/)
  const [newAmount, channel] = amount.split('#')
  const channelInfo = channel && {
    channel: {
      typeId: 'channel',
      id: channels.get(channel).id
    }
  }
  let centAmount = Number(channel ? newAmount : amount)
  if (isNaN(centAmount)) {
    // prices like 1|2; 2 will be ignored
    centAmount = Number((channel ? newAmount : amount).split('|')[0])
  }
  if (customerGroup) {
    return {
      value: {
        currencyCode,
        centAmount
      },
      customerGroup: {
        typeId: 'customer-group',
        id: customerGroups.get(customerGroup).id
      },
      ...channelInfo
    }
  }
  const [country, newCurrencyCode] = currencyCode.split(/-/)
  if (newCurrencyCode) {
    return {
      value: {
        currencyCode: newCurrencyCode,
        centAmount
      },
      country,
      ...channelInfo
    }
  }
  return {
    value: {
      currencyCode,
      centAmount
    },
    ...channelInfo
  }
}
const toPrices = (stringPrices, customerGroups, channels) =>
  stringPrices
    .split(';')
    .map(toPrice(customerGroups, channels))
    .filter(x => x)

const toImage = url => ({
  url,
  dimensions: {
    w: 0,
    h: 0
  }
})
const removeEmpty = o => {
  const ret = Object.entries(o).reduce(
    (result, [key, value]) =>
      value !== '' ? ((result[key] = value), result) : result,
    {}
  )
  return Object.keys(ret).length === 0 ? NONE : ret
}
const noAllEmpty = o => (Object.keys(o).length === 0 ? undefined : o)
const toImages = images => images.split(';').map(toImage)
const toAttribute = (attributeName, value, attributeType) => {
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
        value: value.split(';').filter(x => x)
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
const createVariant = (
  customerGroups,
  channels,
  attributesByType,
  productType
) => product => {
  const { variantId, sku, prices, images } = product
  return {
    id: Number(variantId),
    attributes: attributesByType
      .get(productType)
      .map(([attributeName, attributeType]) =>
        toAttribute(attributeName, product[attributeName], attributeType)
      )
      .filter(attribute => attribute !== NONE),
    sku,
    prices: toPrices(prices, customerGroups, channels),
    images: toImages(images)
  }
}
const toProduct = (
  categoriesBySlug,
  customerGroups,
  channels,
  attributesByType
) => products => {
  const { productType, tax, categories, name, slug } = products[0]
  if (Object.keys(removeEmpty(name)).length === 0) {
    return { type: NONE, products, rejected: 'empty name' }
  }
  if (Object.keys(removeEmpty(slug)).length === 0) {
    return { type: NONE, products, rejected: 'empty slug' }
  }
  const metaDescription = noAllEmpty(removeEmpty(products[0].description))
  const metaTitle = noAllEmpty(removeEmpty(products[0].metaTitle))
  const metaKeywords = noAllEmpty(removeEmpty(products[0].metaKeywords))
  const mainProduct = {
    productType: { key: productType }, // is ok
    masterVariant: createVariant(
      customerGroups,
      channels,
      attributesByType,
      productType
    )(products[0]),
    taxCategory: {
      key: tax
    },
    categories: withCategories(categoriesBySlug, categories),
    variants: products
      .slice(1)
      .map(
        createVariant(customerGroups, channels, attributesByType, productType)
      ),
    name: removeEmpty(name),
    slug: removeEmpty(slug),
    publish: true,
    metaDescription,
    metaTitle,
    metaKeywords
  }

  return mainProduct
}

const importProducts = (productPath, categoriesPath, typesPath) => {
  const csv = require('csvtojson')
  const notifySave = new SingleBar(
    {
      format:
        'Save products      {bar} |' +
        '| {percentage}% || {value}/{total} master variants',
      barCompleteChar: '\u2588',
      barIncompleteChar: '\u2591'
    },
    Presets.rect
  )
  Promise.all(
    [productPath, categoriesPath]
      .map(path => csv().fromFile(path))
      .concat([
        getAll(
          execute,
          customerGroupService
        )({
          method: 'GET'
        }),
        getAll(
          execute,
          channelsService
        )({
          method: 'GET'
        }),
        readJson(typesPath)
      ])
  )
    .then(
      ([
        rawProducts,
        rawCategories,
        customerGroups,
        channels,
        productTypes
      ]) => {
        const categoriesBySlug = setBy(x => x.slug.en)(rawCategories)
        const customerGroupsByKey = setByKey(customerGroups)
        const channelsByKey = setByKey(channels)
        const attributesByType = [
          ...setBy(x => x.key)(productTypes).entries()
        ].reduce(
          (result, [key, value]) =>
            result.set(
              key,
              value.attributes.map(v => [v.name, v.type])
            ),
          new Map()
        )
        const groupedProducts = groupProducts(rawProducts)
        notifySave.start(groupedProducts.length, 0, {})
        let processed = 0
        const productsToSave = groupedProducts.map(
          toProduct(
            categoriesBySlug,
            customerGroupsByKey,
            channelsByKey,
            attributesByType
          )
        )
        return Promise.all(
          productsToSave.map(
            // product => product
            product =>
              execute({
                uri: productsService.build(),
                method: 'POST',
                body: product
              }).then(item => {
                notifySave.update(++processed)
                return item
              })
          )
        ).then(() => {
          notifySave.stop()
          // const r = resolve
          // const rejected = resolve.filter(r => !r.body)
          // debugger
        })
      }
    )
    .catch(reject => {
      notifySave.stop()
      console.log('rejeted with:', reject)
      // debugger
    })
}
if (nconf.get('clean')) {
  deleteAllProducts()
} else if (nconf.get('importtypes')) {
  importProductTypes(nconf.get('types'))
} else if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing products...')
  importProducts(nconf.get('csv'), nconf.get('categories'), nconf.get('types'))
}

module.exports = {
  deleteAllProducts
}
