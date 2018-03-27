import { config } from './config';
import { client, shippingMethodService, taxService, zonesService } from './services';
import { createClient } from '@commercetools/sdk-client';
import { createAuthMiddlewareForClientCredentialsFlow } from '@commercetools/sdk-middleware-auth';
import { createHttpMiddleware } from '@commercetools/sdk-middleware-http'

import { createRequestBuilder } from '@commercetools/api-request-builder';

var data = require(process.cwd() +'/data/shipping-methods.json');

//find tax category-id
client.execute({
    uri: taxService.build(),
    method: 'GET'
}).then(result => getZones(result.body.results[0].id))
    .catch(error => console.log(error));


const getZones = (taxId) => {
    client.execute({
        uri: zonesService.build(),
        method: 'GET'
    }).then(result => addShippingMethods(taxId, result.body.results[0].id))
        .catch(error => console.log(error));
}

const addShippingMethods = (taxId, zoneId) => {

    // set taxId 
    data.forEach(element => {
        element.taxCategory.id =  taxId;
        // set zoneId
        element.zoneRates.forEach(zones => {
            zones.zone.id = zoneId;
        });
    });

    const uri = shippingMethodService.build();
    console.log(uri);
    
    data.forEach(element => {
          client.execute({
            uri,
            method: 'POST',
            body: element,
        }).then(result => console.log(result))
          .catch(error => console.log(error.body.errors));
    })
}