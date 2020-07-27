import {
  customersService,
  customerGroupService
} from './services'
import {
  logAndExit,
  execute,
  createStandardDelete
} from './helpers'
const nconf = require('nconf')

export const importCustomers = () =>
  Promise.all(
    require(process.cwd() + '/data/customers.json').map(
      (customer) =>
        execute({
          uri: customersService.build(),
          method: 'POST',
          body: customer
        })
    )
  )
    .then(() =>
      // eslint-disable-next-line no-console
      console.log('\x1b[32m%s\x1b[0m', 'Customers imported')
    )
    .catch((error) =>
      logAndExit(error, 'Failed to add customers')
    )

export const importCustomerGroups = () =>
  Promise.all(
    require(process.cwd() +
      '/data/customer-groups.json').map((customerGroup) =>
      execute({
        uri: customerGroupService.build(),
        method: 'POST',
        body: customerGroup
      })
    )
  )
    .then(() =>
      // eslint-disable-next-line no-console
      console.log(
        '\x1b[32m%s\x1b[0m',
        'Customer groups imported'
      )
    )
    .catch((err) =>
      logAndExit(err, 'Failed to import customer groups')
    )

export const deleteAllCustomers = createStandardDelete({
  itemName: 'customers',
  service: customersService
})

export const deleteCustomerGroups = createStandardDelete({
  itemName: 'customer groups',
  service: customerGroupService
})

if (nconf.get('clean')) {
  deleteAllCustomers().then(deleteCustomerGroups)
} else if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log('\x1b[32m%s\x1b[0m', 'Importing customers...')
  importCustomerGroups().then(() => importCustomers())
}
