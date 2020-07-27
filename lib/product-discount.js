import { productDiscountService } from './services'
import {
  execute,
  logAndExit,
  createStandardDelete
} from './helpers'
require('dotenv').config()

export const deleteAllProductDiscounts = createStandardDelete(
  {
    itemName: 'discounts',
    service: productDiscountService
  }
)

export const importProductDiscounts = () =>
  execute({
    uri: productDiscountService.build(),
    method: 'POST',
    body: {
      value: {
        type: 'relative',
        permyriad: 5000
      },
      predicate: 'sku = "M0E20000000DX1Y"',
      name: {
        en: 'test',
        de: ''
      },
      description: {
        en: 'test',
        de: ''
      },
      isActive: true,
      sortOrder: '0.1',
      references: [],
      attributeTypes: {}
    }
  }).catch((error) =>
    logAndExit(error, 'Failed to import product discounts')
  )
