import {
  client,
  customersService
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
    .then(result => console.log('All customers are succesfully imported'))
    .catch(error => console.log(error.body.errors))
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
    console.log('All customers are succesfully deleted')
  })
    .catch(err => console.log(err))
}

if (nconf.get('clean')) {
  deleteAllCustomers()
} else if (nconf.get('import')) {
  customersData.forEach(element => {
    addCustomer(element)
  })
}
