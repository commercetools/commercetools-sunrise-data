import { client, ordersService, ordersImportService } from './services'
const nconf = require('nconf')

const deleteAllOrders = (cb) => {
  const request = {
    uri: ordersService
      .build(),
    method: 'GET'
  }
  client.process(
    request,
    (payload) => {
      const results = payload.body.results
      return Promise.all(
        results.map((element) => {
          return client.execute({
            uri: ordersService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('\x1b[32m%s\x1b[0m', 'All orders are succesfully deleted')
    if (cb) {
      return cb()
    }
  })
    .catch(err => console.log(err))
}

const lineItemDraft = (item) => {
  return {
    name: {
      en: 'Product Name'
    },
    variant: {
      sku: item.lineitems.variant.sku
    },
    price: {
      value: {
        currencyCode: 'EUR',
        centAmount: item.lineitems.price * 100,
        fractionDigits: 2
      }
    },
    quantity: parseInt(item.lineitems.quantity)
  }
}

const orderDraft = (item) => {
  return {
    /* eslint quote-props: ["error", "consistent"] */
    'customerEmail': item.customerEmail,
    'orderNumber': item.orderNumber,
    'lineItems': [{
      'name': {
        'en': 'Product Name'
      },
      'variant': {
        'sku': item.lineitems.variant.sku
      },
      'price': {
        'value': {
          'currencyCode': 'EUR',
          'centAmount': item.lineitems.price * 100,
          'fractionDigits': 2
        }
      },
      'quantity': parseInt(item.lineitems.quantity)
    }],
    'totalPrice': {
      'currencyCode': 'EUR',
      'centAmount': item.totalPrice * 100,
      'fractionDigits': 2
    }
  }
}

const importOrders = (csvFilePath) => {
  const csv = require('csvtojson')
  csv()
    .fromFile(csvFilePath)
    .then((rawJson) => {
      var mergedJson = []
      rawJson.forEach((item) => {
        var existing = mergedJson.filter(function (v, i) {
          return v.orderNumber === item.orderNumber
        })
        if (existing.length) {
          var existingIndex = mergedJson.indexOf(existing[0])
          mergedJson[existingIndex].lineItems.push(lineItemDraft(item))
        } else {
          mergedJson.push(orderDraft(item))
        }
      })
      mergedJson.forEach((item) => {
        const request = {
          uri: ordersImportService
            .build(),
          method: 'POST',
          body: item
        }
        client.execute(request)
          .catch(err => console.log(err))
      })
    })
}

if (nconf.get('clean')) {
  deleteAllOrders()
} else if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing orders...')
  importOrders(nconf.get('csv'))
}

module.exports = {
  deleteAllOrders
}
