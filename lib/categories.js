import { categoriesService } from './services'
import {
  execute,
  logAndExit,
  createStandardDelete
} from './helpers'
require('dotenv').config()

const nconf = require('nconf')
export const deleteAllCategories = createStandardDelete({
  itemName: 'categories',
  service: () =>
    categoriesService.where('parent is not defined')
})
const setCateoryData = category => (
  { ...category,orderHint:category.key }
)
const setParent = categories => {
  const byExternalId = categories.reduce(
    (result, category) =>
      result.set(category.externalId, category),
    new Map()
  )
  return Array.from(byExternalId.values()).map(category => {
    const categoryCopy = setCateoryData(category)
    categoryCopy.parent = categoryCopy.parentId
      ? byExternalId.get(categoryCopy.parentId).key
      : undefined
    delete categoryCopy.parentId
    return categoryCopy
  })
}
const groupByParent = (categories) => {
  const recur = (categories, map, keys, level) => {
    if (!categories.length) {
      return map
    }
    // add children only AFTER a possible parent has been added
    categories.forEach((category) => {
      if (keys.includes(category.parent)) {
        map.set(level, map.get(level).concat(category))
      }
    })
    const currentKeys = Array.from(map.values())
      .flat()
      .map((category) => category.key)
    // recursively call with categories that are in the map removed
    return recur(
      categories.filter(
        (category) => !currentKeys.includes(category.key)
      ),
      map.set(level + 1, []), // set the next level
      currentKeys,
      level + 1
    )
  }

  return Array.from(
    recur(
      categories,
      new Map([[0, []]]),
      [undefined],
      0
    ).values()
  ).filter((categories) => categories.length) // filter out empty ones
}
const saveRecursive = (groupedCategories) => {
  const recur = (groupedCategories, index, result) => {
    if (index === groupedCategories.length) {
      return result
    }
    return Promise.all(
      groupedCategories[index]
        .map((category) =>
          category.parent
            ? {
              ...category,
              parent: { key: category.parent }
            }
            : category
        )
        .map((category) => {
          const request = {
            uri: categoriesService.build(),
            method: 'POST',
            body: category
          }
          return execute(request)
        })
    ).then((result) =>
      recur(
        groupedCategories,
        index + 1,
        result.concat(result)
      )
    )
  }
  return recur(groupedCategories, 0, [])
}
export const importCategories = (
  csvFilePath = './data/categories.csv'
) =>
  require('csvtojson')()
    .fromFile(csvFilePath)
    .then((rawJson) =>
      saveRecursive(
        groupByParent(
          setParent(
            rawJson.map((category, index) => ({
              ...category,
              orderHint: String(index)
            }))
          )
        )
      )
    )
    .then(() =>
      // eslint-disable-next-line no-console
      console.log(
        '\x1b[32m%s\x1b[0m',
        'Categories imported'
      )
    )
    .catch((err) =>
      logAndExit(err, 'Failed to import categories')
    )

if (nconf.get('clean')) {
  deleteAllCategories()
} else if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log(
    '\x1b[32m%s\x1b[0m',
    'Importing categories...'
  )
  importCategories(nconf.get('csv'))
}
