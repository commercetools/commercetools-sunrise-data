import { client, shippingMethodService, taxService, zonesService } from './services'
var nconf = require('nconf')

var shippingMethods = require(process.cwd() + '/data/shipping-methods.json')

const getTaxCategories = () => {
  return client.execute({
    uri: taxService.build(),
    method: 'GET'
  }).catch(error => console.log(error))
}

const getZones = () => {
  return client.execute({
    uri: zonesService.build(),
    method: 'GET'
  }).catch(error => console.log(error))
}

const addShippingMethods = (taxCategoriesByName, zonesForName) => {
  shippingMethods.forEach(shippingMethod => {
    shippingMethod.taxCategory.id = taxCategoriesByName['standard'].id

    shippingMethod.zoneRates.forEach(zoneRate => {
      zoneRate.zone.id = zonesForName[zoneRate.zone.id].id
    })

    const uri = shippingMethodService.build()

    client.execute({
      uri,
      method: 'POST',
      body: shippingMethod
    }).catch(error => console.log(error.body.errors))
  })
}

const deleteAllShippingMethods = () => {
  const request = {
    uri: shippingMethodService
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
            uri: shippingMethodService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('\x1b[32m%s\x1b[0m', 'All shipping methods are succesfully deleted')
  })
    .catch(err => console.log(err))
}

async function run () {
  try {
    let taxCategories = await getTaxCategories()
    const taxCategoriesByName = taxCategories.body.results.reduce((obj, taxCategory) => {
      obj[taxCategory.name] = taxCategory
      return obj
    }, {})

    let zones = await getZones()
    const zonesForName = zones.body.results.reduce((obj, zone) => {
      obj[zone.name] = zone
      return obj
    }, {})

    addShippingMethods(taxCategoriesByName, zonesForName)
  } catch (e) {
    console.log(e)
    throw e // let caller know the promise rejected with this reason
  }
}

if (nconf.get('clean')) {
  deleteAllShippingMethods()
} else if (nconf.get('import')) {
  run()
}
