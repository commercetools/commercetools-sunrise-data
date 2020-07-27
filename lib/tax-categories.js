import { taxService } from './services'
import {
  logAndExit,
  execute,
  createStandardDelete
} from './helpers'
const nconf = require('nconf')

export const deleteTaxCategories = createStandardDelete({
  itemName: 'tax categories',
  service: taxService
})

export const importTaxCategories = () =>
  Promise.all(
    require(process.cwd() + '/data/tax-category.json').map(
      (element) =>
        execute({
          uri: taxService.build(),
          method: 'POST',
          body: element
        })
    )
  )
    .then(() =>
      // eslint-disable-next-line no-console
      console.log(
        '\x1b[32m%s\x1b[0m',
        'Tax categories imported'
      )
    )
    .catch((err) =>
      logAndExit(err, 'Failed to import tax categories')
    )

if (nconf.get('clean')) {
  deleteTaxCategories()
} else if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log(
    '\x1b[32m%s\x1b[0m',
    'Importing tax categories...'
  )
  importTaxCategories()
}
