import { projectService } from './services'
import { logAndExit, execute } from './helpers'
const nconf = require('nconf')
const projectData = require(process.cwd() +
  '/data/project.json')

// Helper to execute a project action with fresh version (retries on conflict)
const executeProjectAction = (action, retries = 3) =>
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
          actions: [action]
        }
      })
    )
    .catch((err) => {
      if (err.statusCode === 409 && retries > 0) {
        // eslint-disable-next-line no-console
        console.log(`Version conflict on ${action.action}, retrying...`)
        return executeProjectAction(action, retries - 1)
      }
      throw err
    })

export const importProjectData = () =>
  executeProjectAction({
    action: 'changeCurrencies',
    currencies: projectData.currencies
  })
    .then(() =>
      executeProjectAction({
        action: 'changeCountries',
        countries: projectData.countries
      })
    )
    .then(() =>
      executeProjectAction({
        action: 'changeLanguages',
        languages: projectData.languages
      })
    )
    .then(() =>
      executeProjectAction({
        action: 'changeProductSearchIndexingEnabled',
        enabled: true
      })
    )
    .then(() =>
      // eslint-disable-next-line no-console
      console.log('\x1b[32m%s\x1b[0m', 'Project set up')
    )
    .catch((err) =>
      logAndExit(err, 'Failed to set up project')
    )

export const setStandaloneVariantMode = (retries = 3) =>
  execute({
    uri: projectService.build(),
    method: 'GET'
  })
    .then((result) => {
      // Skip if already in StandaloneVariants mode
      if (result.body.variantMode === 'StandaloneVariants') {
        // eslint-disable-next-line no-console
        console.log('\x1b[32m%s\x1b[0m', 'Project already in StandaloneVariants mode')
        return Promise.resolve()
      }
      return execute({
        uri: projectService.build(),
        method: 'POST',
        body: {
          version: result.body.version,
          actions: [
            {
              action: 'setVariantMode',
              variantMode: 'StandaloneVariants'
            }
          ]
        }
      }).then(() =>
        // eslint-disable-next-line no-console
        console.log('\x1b[32m%s\x1b[0m', 'Project variant mode set to StandaloneVariants')
      )
    })
    .catch((err) => {
      // Retry on version conflict
      if (err.statusCode === 409 && retries > 0) {
        // eslint-disable-next-line no-console
        console.log('Version conflict, retrying...')
        return setStandaloneVariantMode(retries - 1)
      }
      return logAndExit(err, 'Failed to set variant mode')
    })

if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log(
    '\x1b[32m%s\x1b[0m',
    'Importing project data...'
  )
  importProjectData()
}
