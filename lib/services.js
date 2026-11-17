import { config } from './config'
import { createClient } from '@commercetools/sdk-client'
import { createAuthMiddlewareForClientCredentialsFlow } from '@commercetools/sdk-middleware-auth'
import { createHttpMiddleware } from '@commercetools/sdk-middleware-http'
import {
  createRequestBuilder,
  features
} from '@commercetools/api-request-builder'
require('es6-promise').polyfill()
const fetch = require('isomorphic-fetch')
const http = require('http')
const https = require('https')

// Connections are kept open and reused. Without an agent, node-fetch sends
// `Connection: close`, so every request paid for a new TCP and TLS handshake.
// maxSockets caps the connections each importer process opens to one API host.
const MAX_SOCKETS = Number(process.env.MAX_SOCKETS || process.env.MAX_ACTIVE || 25)
const agents = {
  'http:': new http.Agent({ keepAlive: true, maxSockets: MAX_SOCKETS }),
  'https:': new https.Agent({ keepAlive: true, maxSockets: MAX_SOCKETS })
}

export const pooledFetch = (url, options = {}) => {
  const protocol = new URL(url).protocol
  return fetch(url, { ...options, agent: agents[protocol] })
}

export const client = createClient({
  // The order of the middlewares is important !!!
  middlewares: [
    createAuthMiddlewareForClientCredentialsFlow({
      host: config.authUrl,
      projectKey: config.projectKey,
      credentials: {
        clientId: config.clientId,
        clientSecret: config.clientSecret
      }
    }),
    createHttpMiddleware({ host: config.apiUrl, fetch: pooledFetch })
  ]
})

const createProjectService = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .project
const createStoreService = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .stores
const createZonesService = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .zones
const createTaxService = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .taxCategories
const createShippingMethodsService = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .shippingMethods
const createCustomerGroupService = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .customerGroups
const createStatesService = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .states
const createCustomersService = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .customers
const createCategories = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .categories
const createOrders = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .orders
const importOrders = () =>
  createRequestBuilder({
    projectKey: config.projectKey,
    customServices: {
      orders: {
        type: 'orders',
        endpoint: '/orders/import',
        features: [features.query, features.queryOne]
      }
    }
  }).orders
const createProducts = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .products
const createProductDiscount = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .productDiscounts
const createProductTypes = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .productTypes
const createChannels = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .channels
const createTypes = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .types
const createInventory = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .inventory
const createCarts = () =>
  createRequestBuilder({ projectKey: config.projectKey })
    .carts

export const projectService = createProjectService()
export const storeService = createStoreService()
export const zonesService = createZonesService()
export const taxService = createTaxService()
export const shippingMethodService = createShippingMethodsService()
export const customerGroupService = createCustomerGroupService()
export const stateService = createStatesService()
export const customersService = createCustomersService()
export const categoriesService = createCategories()
export const ordersService = createOrders()
export const ordersImportService = importOrders()
// A function, not a shared builder: the builder keeps the version of its last withVersion(), so
// the next request built from it carried a stale ?version= and failed with 409.
export const productsService = () => createProducts()
export const productDiscountService = createProductDiscount()
export const productTypesService = createProductTypes()
export const channelsService = createChannels()
export const typesService = createTypes()
export const inventoryService = createInventory()
export const cartsService = createCarts()
