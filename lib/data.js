const carts = require('./carts')
const categories = require('./categories')
const channels = require('./channels')
const customers = require('./customers')
const inventory = require('./inventory')
const lineitemStates = require('./lineitem-states')
const orders = require('./orders')
const products = require('./products')
const shippingMethod = require('./shipping-method')
const taxCategories = require('./tax-categories')
const types = require('./types')
const zones = require('./zones')

const deleteAllData = (cb) => {
  console.log('--Deleting all project data--')
  const tasks = [
    categories.deleteAllCategories,
    products.deleteAllProducts,
    inventory.deleteInventory,
    orders.deleteAllOrders,
    carts.deleteAllCarts,
    lineitemStates.clearAllStatesWithTransaction,
    customers.deleteAllCustomers,
    customers.deleteCustomerGroups,
    shippingMethod.deleteAllShippingMethods,
    zones.deleteAllZones,
    taxCategories.deleteTaxCategories,
    channels.deleteChannels,
    types.deleteTypes
  ]

  if (!tasks.length) {
    return cb()
  }
  let index = 0

  function clean () {
    var task = tasks[index++]
    task(index === tasks.length ? cb : clean)
  }

  clean()
}

deleteAllData(function () {
  console.log('--The project data is deleted--')
})
