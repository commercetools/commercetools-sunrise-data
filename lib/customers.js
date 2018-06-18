import { client, customersService } from './services'

var customersData = require(process.cwd() + '/data/customers.json')

const addCustomer = (cData) => {
  const updateRequest = {
    uri: customersService.build(),
    method: 'POST',
    body: cData
  }

  client.execute(updateRequest)
    .then(result => console.log(result))
    .catch(error => console.log(error.body.errors))
}

customersData.forEach(element => {
  addCustomer(element)
})
