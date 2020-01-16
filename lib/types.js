import { client, typesService } from './services'
const nconf = require('nconf')

const importTypes = () => {
  const channelTypes = require(process.cwd() + '/data/channel-types.json')
  const customerTypes = require(process.cwd() + '/data/customer-types.json')
  const orderTypes = require(process.cwd() + '/data/order-types.json')
  const allTypes = [channelTypes, customerTypes, orderTypes]

  allTypes.forEach(type => {
    type.forEach(element => {
      const updateRequest = {
        uri: typesService.build(),
        method: 'POST',
        body: element
      }
      client.execute(updateRequest)
        .catch(error => console.log(error.body.errors))
    })
  })
}

const deleteTypes = (cb) => {
  const request = {
    uri: typesService
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
            uri: typesService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('\x1b[32m%s\x1b[0m', 'All types are succesfully deleted')
    if (cb) {
      return cb()
    }
  })
    .catch(err => console.log(err))
}

if (nconf.get('clean')) {
  deleteTypes()
} else if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing types...')
  importTypes()
}
module.exports = {
  deleteTypes
}
