import { storeService } from './services'
import {
  logAndExit,
  execute,
  createStandardDelete
} from './helpers'
const nconf = require('nconf')

export const importStores = () =>
  Promise.all(
    [require(process.cwd() + '/data/stores.json')].map(
      (type) =>
        Promise.all(
          type.map((element) =>
            execute({
              uri: storeService.build(),
              method: 'POST',
              body: element
            }).catch((err) => {
              // Ignore duplicate key errors (stores already exist)
              if (err.statusCode === 400 && err.body && err.body.errors) {
                const duplicateError = err.body.errors.find(e => e.code === 'DuplicateField' && e.field === 'key')
                if (duplicateError) {
                  // eslint-disable-next-line no-console
                  console.log(`Store '${element.key}' already exists, skipping...`)
                  return Promise.resolve()
                }
              }
              throw err
            })
          )
        )
    )
  )
    .then(() =>
      // eslint-disable-next-line no-console
      console.log('\x1b[32m%s\x1b[0m', 'Stores imported')
    )
    .catch((err) =>
      logAndExit(err, 'Failed to import stores')
    )

export const deleteStores = createStandardDelete({
  itemName: 'stores',
  service: storeService
})

// Only execute when this file is run directly (not when imported)
if (require.main === module) {
  if (nconf.get('clean')) {
    deleteStores()
  } else if (nconf.get('import')) {
    // eslint-disable-next-line no-console
    console.log('\x1b[32m%s\x1b[0m', 'Importing stores...')
    importStores()
  }
}
