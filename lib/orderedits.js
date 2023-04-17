import { orderEditsService } from './services'
import {
  createStandardDelete
} from './helpers'
const nconf = require('nconf')

export const deleteOrderEdits = createStandardDelete({
  itemName: 'order-edits',
  service: orderEditsService
})


if (nconf.get('clean')) {
  deleteOrderEdits()
}
