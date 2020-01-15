import { client, categoriesService } from './services'
import { execute } from './helpers'
require('dotenv').config()

const nconf = require('nconf')
const deleteAllCategories = cb => {
  const request = {
    uri: categoriesService.where('parent is not defined').build(),
    method: 'GET'
  }
  client
    .process(request, payload => {
      const results = payload.body.results
      return Promise.all(
        results.map(element => {
          return client.execute({
            uri: categoriesService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    })
    .then(() => {
      console.log('\x1b[32m%s\x1b[0m', 'All categories are succesfully deleted')
      if (cb) {
        return cb()
      }
    })
    .catch(err => console.log('Failed with error:', err))
}
const setParent = categories => {
  const byExternalId = categories.reduce(
    (result, category) => result.set(category.externalId, category),
    new Map()
  )
  return Array.from(byExternalId.values()).map(category => {
    const categoryCopy = { ...category }
    categoryCopy.parent = categoryCopy.parentId
      ? byExternalId.get(categoryCopy.parentId).key
      : undefined
    delete categoryCopy.parentId
    return categoryCopy
  })
}
const groupByParent = categories => {
  const recur = (categories, map, keys, level) => {
    if (!categories.length) {
      return map
    }
    // add children only AFTER a possible parent has been added
    categories.forEach(category => {
      if (keys.includes(category.parent)) {
        map.set(level, map.get(level).concat(category))
      }
    })
    const currentKeys = Array.from(map.values())
      .flat()
      .map(category => category.key)
    // recursively call with categories that are in the map removed
    return recur(
      categories.filter(category => !currentKeys.includes(category.key)),
      map.set(level + 1, []), // set the next level
      currentKeys,
      level + 1
    )
  }

  return Array.from(
    recur(categories, new Map([[0, []]]), [undefined], 0).values()
  ).filter(categories => categories.length) // filter out empty ones
}
const saveRecursive = groupedCategories => {
  const recur = (groupedCategories, index, result) => {
    if (index === groupedCategories.length) {
      return result
    }
    return Promise.all(
      groupedCategories[index]
        .map(category =>
          category.parent
            ? {
              ...category,
              parent: { key: category.parent }
            }
            : category
        )
        .map(category => {
          const request = {
            uri: categoriesService.build(),
            method: 'POST',
            body: category
          }
          return execute(request).catch(err => {
            // debugger
            throw err
          })
        })
    ).then(result => recur(groupedCategories, index + 1, result.concat(result)))
  }
  return recur(groupedCategories, 0, [])
}
const importCategories = csvFilePath => {
  const csv = require('csvtojson')
  csv()
    .fromFile(csvFilePath)
    .then(rawJson => saveRecursive(groupByParent(setParent(rawJson))))
}

if (nconf.get('clean')) {
  deleteAllCategories()
} else if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing categories...')
  importCategories(nconf.get('csv'))
}

module.exports = {
  deleteAllCategories
}
