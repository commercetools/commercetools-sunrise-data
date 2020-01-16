import { client, projectService } from './services'
const nconf = require('nconf')
const projectData = require(process.cwd() + '/data/project.json')

const importProjectData = () => {
  const request = {
    uri: projectService
      .build(),
    method: 'GET'
  }
  client.execute(request)
    .then(result => {
      client.execute({
        uri: projectService.build(),
        method: 'POST',
        body: {
          version: result.body.version,
          actions: [{
            action: 'changeCurrencies',
            currencies: projectData.currencies
          }]
        }
      })
        .then(result => {
          client.execute({
            uri: projectService.build(),
            method: 'POST',
            body: {
              version: result.body.version,
              actions: [{
                action: 'changeCountries',
                countries: projectData.countries
              }]
            }
          })
            .then(result => {
              client.execute({
                uri: projectService.build(),
                method: 'POST',
                body: {
                  version: result.body.version,
                  actions: [{
                    action: 'changeLanguages',
                    languages: projectData.languages
                  }]
                }
              })
                .catch(err => console.log(err))
            })
            .catch(err => console.log(err))
        })
        .catch(err => console.log(err))
    })
    .catch(err => console.log(err))
}

if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing project data...')
  importProjectData()
}
