import { client, shippingMethodService, taxService, zonesService } from './services'

var shippingMethods = require(process.cwd() + '/data/shipping-methods.json')

const getTaxCategories = () => {
  return client.execute({
      uri: taxService.build(),
      method: 'GET'
  }).then(result => result.body)
      .catch(error => console.log(error));
}

const getZones = () => {
     return client.execute({
        uri: zonesService.build(),
        method: 'GET'
    }).then(result => result.body)
        .catch(error => console.log(error));
}

const addShippingMethods = (taxCategoriesByName, zonesForName) => {
  shippingMethods.forEach(shippingMethod => {

    shippingMethod.taxCategory.id =  taxCategoriesByName["standard"].id;

    shippingMethod.zoneRates.forEach(zoneRate => {
      zoneRate.zone.id = zonesForName[zoneRate.zone.id].id
    })

    const uri = shippingMethodService.build();

    client.execute({
        uri,
        method: 'POST',
        body: shippingMethod,
      }).then(result => console.log(result))
        .catch(error => console.log(error.body.errors));
  })
}

async function run() {
    try {
      let taxCategories = await getTaxCategories();
      const taxCategoriesByName = taxCategories.results.reduce((obj, taxCategory) => {
        obj[taxCategory.name] = taxCategory;
        return obj;
      }, {});

      let zones = await getZones();
      const zonesForName = zones.results.reduce((obj, zone) => {
        obj[zone.name] = zone;
        return obj;
      }, {});


      addShippingMethods(taxCategoriesByName,zonesForName);
    } catch(e) {
        console.log(e);
        throw e;      // let caller know the promise rejected with this reason
    }
}

run();
