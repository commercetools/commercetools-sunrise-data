import { client, customersService, customerGroupService } from './services'
const nconf = require('nconf')
const customersData = require(process.cwd() + '/data/customers.json')

const addCustomer = (cData) => {
  const updateRequest = {
    uri: customersService.build(),
    method: 'POST',
    body: cData
  }

  client.execute(updateRequest)
    .catch(error => console.log(error.body.errors))
}

const importCustomerGroups = () => {
  var customerGroups = require(process.cwd() + '/data/customer-groups.json')
  customerGroups.forEach(element => {
    const updateRequest = {
      uri: customerGroupService.build(),
      method: 'POST',
      body: element
    }

    client.execute(updateRequest)
      .catch(error => console.log(error.body.errors))
  })
}

const deleteAllCustomers = (cb) => {
  const request = {
    uri: customersService
      .build(),
    method: 'GET'
  }
  client.process(
    request,
    (payload) => {
      const results = payload.body.results
      return Promise.all(
        results.map((element) => {
          return client.execute({
            uri: customersService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('\x1b[32m%s\x1b[0m', 'All customers are succesfully deleted')
    if (cb) {
      return cb()
    }
  })
    .catch(err => console.log(err))
}

const deleteCustomerGroups = (cb) => {
  const request = {
    uri: customerGroupService
      .build(),
    method: 'GET'
  }
  client.process(
    request,
    (payload) => {
      const results = payload.body.results
      return Promise.all(
        results.map((element) => {
          return client.execute({
            uri: customerGroupService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('\x1b[32m%s\x1b[0m', 'All customer groups are succesfully deleted')
    return cb()
  })
    .catch(err => console.log(err))
}

if (nconf.get('clean')) {
  deleteAllCustomers()
  deleteCustomerGroups()
} else if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing customers...')
  importCustomerGroups()
  customersData.forEach(element => {
    addCustomer(element)
  })
}

module.exports = {
  deleteAllCustomers,
  deleteCustomerGroups
}
