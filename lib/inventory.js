import { inventoryService } from './services'
import {
  logAndExit,
  execute,
  createStandardDelete
} from './helpers'
import { csv } from 'csvtojson'
import { SingleBar, Presets } from 'cli-progress'

require('dotenv').config()
const nconf = require('nconf')

export const deleteInventory = createStandardDelete({
  itemName: 'inventory',
  service: inventoryService
})

export const importInventory = (
  inventoryPath = './data/inventory.csv',
  inventoryStoresPath = './data/inventory-stores.csv'
) =>
  Promise.all([
    csv().fromFile(inventoryPath),
    csv().fromFile(inventoryStoresPath)
  ]).then(([inventory, inventoryStores]) => {
    const notifySave = new SingleBar(
      {
        format:
          'Save inventory      {bar} |' +
          '| {percentage}% || {value}/{total} items',
        barCompleteChar: '\u2588',
        barIncompleteChar: '\u2591'
      },
      Presets.rect
    )
    const all = inventory.concat(inventoryStores)
    notifySave.start(all.length, 0, {})
    let processed = 0
    return Promise.all(
      all
        .map(({ sku, quantityOnStock, supplyChannel }) => {
          const supply = supplyChannel
            ? { supplyChannel: { key: supplyChannel } }
            : {}
          return {
            sku,
            quantityOnStock: Number(quantityOnStock),
            ...supply
          }
        })
        .map((inventory) => {
          return execute({
            uri: inventoryService.build(),
            method: 'POST',
            body: inventory
          }).then(() => notifySave.update(++processed))
        })
    ).then(
      () => notifySave.stop(),
      (err) => {
        notifySave.stop()
        return logAndExit(err, 'Failed to import iventory')
      }
    )
  })

if (nconf.get('clean')) {
  deleteInventory()
} else if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log('\x1b[32m%s\x1b[0m', 'Importing inventory...')
  importInventory()
}
