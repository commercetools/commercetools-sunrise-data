import { deleteAllCarts } from './carts'
import {
  deleteAllCategories,
  importCategories
} from './categories'
import { deleteChannels, importChannels } from './channels'
import {
  deleteAllCustomers,
  deleteCustomerGroups,
  importCustomerGroups,
  importCustomers
} from './customers'
import { deleteInventory } from './inventory'
import { deleteAllStates } from './lineitem-states'
import { deleteAllOrders } from './orders'
import {
  deleteAllProducts,
  importProductTypes,
  importProducts,
  deleteAllProductTypes
} from './products'
import {
  deleteAllShippingMethods,
  importShippingMethods
} from './shipping-method'
import {
  deleteTaxCategories,
  importTaxCategories
} from './tax-categories'
import { deleteTypes, importTypes } from './types'
import { deleteAllZones, importZones } from './zones'
import { importProjectData } from './project-setup'
import {
  deleteAllProductDiscounts,
  importProductDiscounts
} from './product-discount'
const nconf = require('nconf')

const taskReducer = (result, fn) => result.then(fn)

const deleteAllData = () => {
  // eslint-disable-next-line no-console
  console.log('--Deleting all project data--')
  return [
    deleteAllProductDiscounts,
    deleteAllProducts,
    deleteAllProductTypes,
    deleteAllCategories,
    deleteInventory,
    deleteAllOrders,
    deleteAllCarts,
    deleteAllStates,
    deleteAllCustomers,
    deleteCustomerGroups,
    deleteAllShippingMethods,
    deleteAllZones,
    deleteTaxCategories,
    deleteChannels,
    deleteTypes
  ].reduce(taskReducer, Promise.resolve())
}
const importNineProducts = () =>
  importProducts(undefined, undefined, undefined, 9)
const importAllData = () => {
  // eslint-disable-next-line no-console
  console.log('--Importing all project data--')
  return [
    importTypes,
    importChannels,
    importTaxCategories,
    importZones,
    importProjectData,
    importShippingMethods,
    importCustomerGroups,
    importCustomers,
    importCategories,
    importProductTypes,
    importNineProducts,
    importProductDiscounts
    // importing 150k inventory items takes too long
    //   it can be imported with npm run import:inventory
    // importInventory,
    // importOrders
  ].reduce(taskReducer, Promise.resolve())
}

const deleteAndImport = () =>
  [deleteAllData, importAllData].reduce(
    taskReducer,
    Promise.resolve()
  )

if (nconf.get('all:clean')) {
  deleteAllData().then(() =>
    // eslint-disable-next-line no-console
    console.log('--The project data is deleted--')
  )
} else if (nconf.get('all:import')) {
  importAllData().then(() =>
    // eslint-disable-next-line no-console
    console.log('--All data is imported--')
  )
} else if (nconf.get('start')) {
  deleteAndImport().then(() =>
    // eslint-disable-next-line no-console
    console.log('--All data is imported--')
  )
}
