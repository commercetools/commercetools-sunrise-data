import { config } from './config';
import { client, zonesService } from './services';
import { createClient } from '@commercetools/sdk-client';
import { createAuthMiddlewareForClientCredentialsFlow } from '@commercetools/sdk-middleware-auth';
import { createHttpMiddleware } from '@commercetools/sdk-middleware-http'

import { createRequestBuilder } from '@commercetools/api-request-builder';

export const zones = {
    "name":"Europe",
    "description": "",
    "locations":[{ "country": "AT", "state": ""},
                 { "country": "DE", "state": ""}
                ]
  };


const zonesUri = zonesService.build();
const zonesGetRequest = {
    uri: zonesUri,
    method: 'GET'
}
console.log(zonesUri);
client.execute(zonesGetRequest)
        .then(result => addZones(resolveVersion(result.body.version)))
        .catch(error => console.log(error));

const resolveVersion = (version) => {
    if(version === 'undefined') {
        return 0;
    } else {
        return version;
    }
}

const addZones = (version) => {
    console.log(version);
    const updateRequest = {
        uri: zonesUri,
        method: 'POST',
        body: zones
    }

    client.execute(updateRequest)
        .then(result => console.log(result))
        .catch(error => console.log(error.body.errors));
}