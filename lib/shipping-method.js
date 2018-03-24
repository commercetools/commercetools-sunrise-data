import { config } from './config';
import { client, shippingMethodService, taxService, zonesService } from './services';
import { createClient } from '@commercetools/sdk-client';
import { createAuthMiddlewareForClientCredentialsFlow } from '@commercetools/sdk-middleware-auth';
import { createHttpMiddleware } from '@commercetools/sdk-middleware-http'

import { createRequestBuilder } from '@commercetools/api-request-builder';

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
    const standardData = {
        "name": "Standard",
        "description": "Delivery in 5-6 working days",
        "taxCategory": {
            "typeId": "tax-category",
            "id": taxId
        },
        "zoneRates": [
            {
                "zone": {
                    "typeId": "zone",
                    "id": zoneId
                },
                "shippingRates": [
                    {
                        "price": {
                            "currencyCode": "EUR",
                            "centAmount": 300
                        },
                        "freeAbove": {
                            "currencyCode": "EUR",
                            "centAmount": 20000
                        },
                        "tiers": []
                    }
                ]
            }
        ],
        "isDefault": true
    };
    const expressData = {
        "name": "Express",
        "description": "Delivery the same day",
        "taxCategory": {
            "typeId": "tax-category",
            "id": taxId
        },
        "zoneRates": [
            {
                "zone": {
                    "typeId": "zone",
                    "id": zoneId
                },
                "shippingRates": [
                    {
                        "price": {
                            "currencyCode": "EUR",
                            "centAmount": 1000
                        },
                        "tiers": []
                    }
                ]
            }
        ],
        "isDefault": false
    };


    const uri = shippingMethodService.build();
    console.log(uri);



    client.execute({
        uri,
        method: 'POST',
        body: standardData,
    })
        .then(result => console.log(result))
        .catch(error => console.log(error.body.errors));

    client.execute({
        uri,
        method: 'POST',
        body: expressData,
    })
        .then(result => console.log(result))
        .catch(error => console.log(error.body.errors));
}