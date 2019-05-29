import {
  client,
  customersService,
  customerGroupService
} from './services'
var nconf = require('nconf')

var customersData = require(process.cwd() + '/data/customers.json')

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

const deleteAllCustomers = () => {
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
  })
    .catch(err => console.log(err))
}

const deleteCustomerGroups = () => {
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
  })
    .catch(err => console.log(err))
}

if (nconf.get('clean')) {
  deleteAllCustomers()
  deleteCustomerGroups()
} else if (nconf.get('import')) {
  importCustomerGroups()
  customersData.forEach(element => {
    addCustomer(element)
  })
}
