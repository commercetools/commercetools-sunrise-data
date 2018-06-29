import { config } from './config';
import { client, shippingMethodService, taxService, zonesService } from './services';
import { createClient } from '@commercetools/sdk-client';
import { createAuthMiddlewareForClientCredentialsFlow } from '@commercetools/sdk-middleware-auth';
import { createHttpMiddleware } from '@commercetools/sdk-middleware-http'

import { createRequestBuilder } from '@commercetools/api-request-builder';

var data = require(process.cwd() +'/data/shipping-methods.json');

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

const addShippingMethods = (taxCategories, zones) => {
  data.forEach(element => {
    // set tax category ID
    element.taxCategory.id =  taxCategories.results[0].id;

    // for this dataset, the first zone returned is Europe and the second is the US
    switch (element.zoneRates[0].zone.id) {
      case "EU":
        element.zoneRates[0].zone.id = zones.results[0].id;
        break;
      case "US":
        element.zoneRates[0].zone.id = zones.results[1].id;
        break;
    }

    const uri = shippingMethodService.build();

    client.execute({
        uri,
        method: 'POST',
        body: element,
      }).then(result => console.log(result))
        .catch(error => console.log(error.body.errors));
  })
}

async function run() {
    try {
      let taxCategories = await getTaxCategories();
      let zones = await getZones();

      addShippingMethods(taxCategories,zones);
    } catch(e) {
        console.log(e);
        throw e;      // let caller know the promise rejected with this reason
    }
}

run();
