import { typesService } from './services'
import {
  logAndExit,
  execute,
  createStandardDelete
} from './helpers'
const nconf = require('nconf')

export const importTypes = () =>
  Promise.all(
    [
      require(process.cwd() + '/data/channel-types.json'),
      require(process.cwd() + '/data/customer-types.json'),
      require(process.cwd() + '/data/order-types.json')
    ].map((type) =>
      Promise.all(
        type.map((element) =>
          execute({
            uri: typesService.build(),
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

export const deleteTypes = createStandardDelete({
  itemName: 'types',
  service: typesService
})

if (nconf.get('clean')) {
  deleteTypes()
} else if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log('\x1b[32m%s\x1b[0m', 'Importing types...')
  importTypes()
}
