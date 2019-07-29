import { client, zonesService } from './services'
const nconf = require('nconf')
const zones = require(process.cwd() + '/data/zones.json')

const addZones = () => {
  zones.forEach(element => {
    const updateRequest = {
      uri: zonesService.build(),
      method: 'POST',
      body: element
    }

    client.execute(updateRequest)
      .catch(error => console.log(error.body.errors))
  })
}

const deleteAllZones = (cb) => {
  let callerName
  try { throw new Error() } catch (e) {
    const re = /(\w+)@|at (\w+) \(/g
    callerName = re.exec(e.stack)[1] || re.exec(e.stack)[2]
  };
  const request = {
    uri: zonesService
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
            uri: zonesService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('\x1b[32m%s\x1b[0m', 'All zones are succesfully deleted')
    if (callerName === 'clean') {
      return cb()
    }
  })
    .catch(err => console.log(err))
}

if (nconf.get('clean')) {
  deleteAllZones()
} else if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing zones...')
  addZones()
}

module.exports = {
  deleteAllZones
}
