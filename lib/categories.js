import { client, categoriesService } from './services'
require('dotenv').config()
const nconf = require('nconf')
const execSync = require('child_process').execSync
const path = require('path')

const deleteAllCategories = (cb) => {
  const request = {
    uri: categoriesService
      .where('parent is not defined')
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
            uri: categoriesService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('\x1b[32m%s\x1b[0m', 'All categories are succesfully deleted')
    if (cb) {
      return cb()
    }
  })
    .catch(err => console.log(err))
}

const importCategories = (cmd) => {
  return path.join('node_modules', '.bin', cmd)
}

if (nconf.get('clean')) {
  deleteAllCategories()
} else if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing categories...')
  execSync(`${importCategories('category-sync')} import -f data/categories.csv -p ${process.env.CTP_PROJECT_KEY} -i ${process.env.CTP_CLIENT_ID}  -s ${process.env.CTP_CLIENT_SECRET} --sphereHost ${process.env.CTP_API_URL} --sphereAuthHost ${process.env.CTP_AUTH_URL}`)
}

module.exports = {
  deleteAllCategories
}
