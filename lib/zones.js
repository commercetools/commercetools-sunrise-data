import { client, zonesService } from './services'

var zones = require(process.cwd() + '/data/zones.json')

const addZones = () => {
  const updateRequest = {
    uri: zonesService.build(),
    method: 'POST',
    body: zones
  }

  client.execute(updateRequest)
    .then(result => console.log(result))
    .catch(error => console.log(error.body.errors))
}

addZones()
