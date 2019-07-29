import { client, channelsService } from './services'
const nconf = require('nconf')

const deleteChannels = (cb) => {
  let callerName
  try { throw new Error() } catch (e) {
    const re = /(\w+)@|at (\w+) \(/g
    callerName = re.exec(e.stack)[1] || re.exec(e.stack)[2]
  };
  const request = {
    uri: channelsService
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
            uri: channelsService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('\x1b[32m%s\x1b[0m', 'All channels are succesfully deleted')
    if (callerName === 'clean') {
      return cb()
    }
  })
    .catch(err => console.log(err))
}

const importChannels = () => {
  var channels = require(process.cwd() + '/data/channels.json')
  channels.forEach(element => {
    const updateRequest = {
      uri: channelsService.build(),
      method: 'POST',
      body: element
    }

    client.execute(updateRequest)
      .catch(error => console.log(error.body.errors))
  })
}

if (nconf.get('clean')) {
  deleteChannels()
} else if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing channels...')
  importChannels()
}

module.exports = {
  deleteChannels
}
