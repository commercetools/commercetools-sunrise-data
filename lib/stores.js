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
            })
          )
        )
    )
  )
    .then(() =>
      // eslint-disable-next-line no-console
      console.log('\x1b[32m%s\x1b[0m', 'Types imported')
    )
    .catch((err) =>
      logAndExit(err, 'Failed to import channel types')
    )

export const deleteStores = createStandardDelete({
  itemName: 'stores',
  service: storeService
})

if (nconf.get('clean')) {
  deleteStores()
} else if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log('\x1b[32m%s\x1b[0m', 'Importing types...')
  importStores()
}
