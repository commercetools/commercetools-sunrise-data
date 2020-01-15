import { client } from './services'
const fs = require('fs')

const COOL_DOWN_PERIOD =
  Number(process.env.COOL_DOWN_PERIOD || 10) * 1000
const MAX_ACTIVE = Number(process.env.MAX_ACTIVE || 25)
const RETRIES = Number(process.env.RETRIES || 10)
export const later = (howLong, value) =>
  new Promise(resolve =>
    setTimeout(() => resolve(value), howLong)
  )
export const createLimiter = (max = MAX_ACTIVE) => {
  let que = []
  let active = 0
  const cleanupAndNext = (key, action) => result => {
    active--
    action(result)
    if (que[0] && active < max) {
      const [key, fn, args, p] = que[0]
      execute(fn, args, key, p)
    }
  }
  const execute = (fn, args, key, p) => {
    que = que.filter(([k]) => k !== key)
    return Promise.resolve()
      .then(() => fn.apply(null, args))
      .then(
        val => cleanupAndNext(key, p.resolve)(val),
        val => cleanupAndNext(key, p.reject)(val)
      )
  }

  return fn =>
    function Processor () {
      const key = {}
      const args = [...arguments]
      const p = {}
      que.push([key, fn, args, p])
      if (active < max) {
        active++
        execute(fn, args, key, p)
      }
      return new Promise((resolve, reject) => {
        p.resolve = resolve
        p.reject = reject
      })
    }
}

export const createRetry = (retries = RETRIES) => {
  const execute = (fn, tries, args) =>
    Promise.resolve()
      .then(() => fn.apply(null, args))
      .catch(error => {
        // debugger
        tries++
        if (tries > retries) {
          throw error
        }
        return later(COOL_DOWN_PERIOD).then(() =>
          execute(fn, tries, args)
        )
      })

  return fn =>
    function retry () {
      return execute(fn, 0, [...arguments])
    }
}
export const execute = createLimiter()(
  createRetry()(client.execute.bind(client))
)
export const getAll = (getterFn, service) => (
  request,
  statusCallback = x => x
) => {
  const limit = 100
  return getterFn({
    ...request,
    uri: service
      .perPage(limit)
      .page(1)
      .build()
  }).then(response => {
    const pages = Math.ceil(
      response.body.total / response.body.limit
    )
    let pagesDone = pages === 0 ? 0 : 1
    statusCallback(pagesDone, pages)
    return Promise.all(
      response.body.results.concat(
        [...new Array(pages)]
          .map((_, index) => index + 1) // index is zero based, page parameter is not
          .slice(1) // we already fetched page 1
          .map(page =>
            getterFn({
              ...request,
              uri: service
                .perPage(limit)
                .page(page)
                .build()
            }).then(response => {
              statusCallback(++pagesDone, pages)
              return response.body.results
            })
          )
      )
    ).then(results => results.flat())
  })
}

export const NONE = {}

export const setBy = getter => items =>
  items.reduce(
    (items, item) => items.set(getter(item), item),
    new Map()
  )
export const setByKey = setBy(item => item.key)
export const groupBy = getter => items =>
  items.reduce(
    (items, item) =>
      items.set(
        getter(item),
        (items.get(getter(item)) || []).concat(item)
      ),
    new Map()
  )
export const readJson = (filePath, encoding = 'utf8') =>
  new Promise((resolve, reject) =>
    fs.readFile(filePath, encoding, (err, fileContent) =>
      err ? reject(err) : resolve(JSON.parse(fileContent))
    )
  )
