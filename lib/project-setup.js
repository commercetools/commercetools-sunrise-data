import { projectService } from './services'
import { logAndExit, execute } from './helpers'
const nconf = require('nconf')
const projectData = require(process.cwd() +
  '/data/project.json')

export const importProjectData = () =>
  execute({
    uri: projectService.build(),
    method: 'GET'
  })
    .then((result) =>
      execute({
        uri: projectService.build(),
        method: 'POST',
        body: {
          version: result.body.version,
          actions: [
            {
              action: 'changeCurrencies',
              currencies: projectData.currencies
            }
          ]
        }
      })
    )
    .then((result) =>
      execute({
        uri: projectService.build(),
        method: 'POST',
        body: {
          version: result.body.version,
          actions: [
            {
              action: 'changeCountries',
              countries: projectData.countries
            }
          ]
        }
      })
    )
    .then((result) =>
      execute({
        uri: projectService.build(),
        method: 'POST',
        body: {
          version: result.body.version,
          actions: [
            {
              action: 'changeLanguages',
              languages: projectData.languages
            }
          ]
        }
      })
    )
    .then(() =>
      // eslint-disable-next-line no-console
      console.log('\x1b[32m%s\x1b[0m', 'Project set up')
    )
    .catch((err) =>
      logAndExit(err, 'Failed to set up project')
    )

if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log(
    '\x1b[32m%s\x1b[0m',
    'Importing project data...'
  )
  importProjectData()
}
