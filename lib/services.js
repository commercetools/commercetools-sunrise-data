import { config } from './config';
import { createClient } from '@commercetools/sdk-client';
import { createAuthMiddlewareForClientCredentialsFlow } from '@commercetools/sdk-middleware-auth';
import { createHttpMiddleware } from '@commercetools/sdk-middleware-http'

import { createRequestBuilder } from '@commercetools/api-request-builder'

export const client = createClient({
    // The order of the middlewares is important !!!
    middlewares: [
      createAuthMiddlewareForClientCredentialsFlow({
        host: config.authUrl,
        projectKey: config.project,
        credentials: {
          clientId: config.client_id,
          clientSecret: config.client_secret,
        },
      }),
      createHttpMiddleware({ host: config.apiUrl })
    ]
  });

const createProjectService = () => createRequestBuilder({ projectKey: config.project }).project;
const createZonesService = () => createRequestBuilder({ projectKey: config.project }).zones;
const createTaxService = () => createRequestBuilder({ projectKey: config.project }).taxCategories;
const createShippingMethodsService = () => createRequestBuilder({ projectKey: config.project }).shippingMethods;
const createCustomerGroupService = () => createRequestBuilder({ projectKey: config.project}).customerGroups;

export const projectService = createProjectService();
export const zonesService = createZonesService();
export const taxService = createTaxService();
export const shippingMethodService = createShippingMethodsService();
export const customerGroupService = createCustomerGroupService();