import { client, inventoryService } from './services'
require('dotenv').config()
const nconf = require('nconf')
const execSync = require('child_process').execSync
const path = require('path')

const deleteInventory = (cb) => {
  let callerName
  try { throw new Error() } catch (e) {
    const re = /(\w+)@|at (\w+) \(/g
    callerName = re.exec(e.stack)[1] || re.exec(e.stack)[2]
  };
  const request = {
    uri: inventoryService
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
            uri: inventoryService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('\x1b[32m%s\x1b[0m', 'All inventory entries are succesfully deleted')
    if (callerName === 'clean') {
      return cb()
    }
  })
    .catch(err => console.log(err))
}

const importInventory = (cmd) => {
  console.log('Importing inventory...')
  return path.join('node_modules', '.bin', cmd)
}

if (nconf.get('clean')) {
  deleteInventory()
} else if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing inventory...')
  execSync(`${importInventory('stock-import')} --logLevel debug --logSilent true --file data/inventory.csv --projectKey ${process.env.CTP_PROJECT_KEY} --clientId ${process.env.CTP_CLIENT_ID} --clientSecret ${process.env.CTP_CLIENT_SECRET} --sphereHost https://${process.env.CTP_API_URL} --sphereAuthHost https://${process.env.CTP_AUTH_URL}`)
  execSync(`${importInventory('stock-import')} --logLevel debug --logSilent true --file data/inventory-stores.csv --projectKey ${process.env.CTP_PROJECT_KEY} --clientId ${process.env.CTP_CLIENT_ID} --clientSecret ${process.env.CTP_CLIENT_SECRET} --sphereHost https://${process.env.CTP_API_URL} --sphereAuthHost https://${process.env.CTP_AUTH_URL}`)
}

module.exports = {
  deleteInventory
}
