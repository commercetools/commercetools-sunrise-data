import { client } from './services'
import { SingleBar, Presets } from 'cli-progress'

const fs = require('fs')
const COOL_DOWN_PERIOD =
  Number(process.env.COOL_DOWN_PERIOD || 15) * 1000
const MAX_ACTIVE = Number(process.env.MAX_ACTIVE || 25)
const RETRIES = Number(process.env.RETRIES || 5)
export const later = (howLong, value) =>
  new Promise((resolve) =>
    setTimeout(() => resolve(value), howLong)
  )
const promiseLike = (x) =>
  x !== undefined && typeof x.then === 'function'
const ifPromise = (fn) => (x) =>
  promiseLike(x) ? x.then(fn) : fn(x)
export const createLimiter = (max = MAX_ACTIVE) => {
  var que = []
  var queIndex = -1
  var running = 0
  const wait = (resolve, fn, arg) => () =>
    resolve(ifPromise(fn)(arg)) || true // should always return true
  const nextInQue = () => {
    ++queIndex
    if (typeof que[queIndex] === 'function') {
      return que[queIndex]()
    } else {
      que = []
      queIndex = -1
      running = 0
      return 'Does not matter, not used'
    }
  }
  const queItem = (fn, arg) =>
    new Promise((resolve) =>
      que.push(wait(resolve, fn, arg))
    )
  return (fn) => (arg) => {
    const p = queItem(fn, arg).then((x) => nextInQue() && x)
    running++
    if (running <= max) {
      nextInQue()
    }
    return p
  }
}

export const createRetry = (retries = RETRIES) => {
  const execute = (fn, tries, args) =>
    Promise.resolve()
      .then(() => fn.apply(null, args))
      .catch((error) => {
        // debugger
        tries++

        if (error.body && error.body.statusCode === 401) {
          throw error
        }

        if (tries > retries) {
          throw error
        }
        return later(COOL_DOWN_PERIOD).then(() =>
          execute(fn, tries, args)
        )
      })

  return (fn) =>
    function retry () {
      return execute(fn, 0, [...arguments])
    }
}
export const execute = createLimiter()(
  createRetry()(client.execute.bind(client))
)
export const getAll = (getterFn, service) => (
  request,
  statusCallback = (x) => x
) => {
  const limit = 100
  const recur = (
    result,
    getterFn,
    service,
    goOn,
    lastId,
    total
  ) => {
    if (!goOn) {
      return result
    }
    let uriBuilder = service
      .perPage(limit)
      .sort('id', true)
      .withTotal(total === undefined)
    if (lastId) {
      uriBuilder = uriBuilder.where(`id>"${lastId}"`)
    }
    return getterFn({
      ...request,
      uri: uriBuilder.build()
    }).then((response) => {
      const all = result.concat(response.body.results)
      statusCallback(
        all.length,
        total || response.body.total
      )
      return recur(
        all,
        getterFn,
        service,
        response.body.count === limit,
        response.body.results.slice(-1)[0]?.id,
        total || response.body.total
      )
    })
  }
  return recur([], getterFn, service, true)
}

export const NONE = {}

export const setBy = (getter) => (items) =>
  items.reduce(
    (items, item) => items.set(getter(item), item),
    new Map()
  )
export const setByKey = setBy((item) => item.key)
export const groupBy = (getter) => (items) =>
  items.reduce(
    (items, item) =>
      items.set(
        getter(item),
        (items.get(getter(item)) || []).concat(item)
      ),
    new Map()
  )

export const readFile = (filePath, encoding = 'utf8') =>
  new Promise((resolve, reject) =>
    fs.readFile(filePath, encoding, (err, fileContent) =>
      err ? reject(err) : resolve(fileContent)
    )
  )
export const writeFile = (filePath, content) =>
  new Promise((resolve, reject) =>
    fs.writeFile(filePath, content, (err) =>
      err ? reject(err) : resolve()
    )
  )
export const readJson = (filePath, encoding = 'utf8') =>
  readFile(filePath, encoding).then((fileContent) =>
    JSON.parse(fileContent)
  )
export const logAndExit = (error, message) => {
  // eslint-disable-next-line no-console
  console.error(
    `${message}, see import-error.log for more details`
  )
  const log = Object.keys(error).length
    ? JSON.stringify(error, undefined, 2)
    : JSON.stringify(
      { message: error.massage, stack: error.stack },
      undefined,
      2
    )
  const exit = () => {
    process.exit(1)
  }
  return writeFile('import-error.log', log).then(exit, exit)
}
export const processAll = (
  getterFn,
  service,
  request,
  processor = (x) => x
) => {
  const limit = 100
  const recur = (
    result,
    getterFn,
    service,
    goOn,
    lastId,
    total
  ) => {
    if (!goOn) {
      return result
    }
    let uriBuilder =
      typeof service === 'function'
        ? service()
        : service
          .perPage(limit)
          .sort('id', true)
          .withTotal(total === undefined)
    if (lastId) {
      uriBuilder = uriBuilder.where(`id>"${lastId}"`)
    }
    const uri = uriBuilder.build()
    // console.log('getting:', uri)
    return getterFn({
      ...request,
      uri
    }).then((response) => {
      // console.log(JSON.stringify(response, undefined, 2))
      return Promise.all(
        response.body.results.map((item) =>
          processor(item, total || response.body.total)
        )
      ).then((processed) => {
        const all = result.concat(processed)
        return recur(
          all,
          getterFn,
          service,
          response.body.count === limit,
          response.body.results.slice(-1)[0]?.id,
          total || response.body.total
        )
      })
    })
  }
  return recur([], getterFn, service, true)
}
const processWithNotify = (
  message,
  getterFn,
  service,
  request,
  processor = (x) => x
) => {
  let notify = { stop: (x) => x }
  let started = false
  const updateStatus = (done, total) => {
    if (!started) {
      notify = new SingleBar(
        {
          format: message,
          barCompleteChar: '\u2588',
          barIncompleteChar: '\u2591'
        },
        Presets.rect
      )
      notify.start(total)
      started = true
    }
    notify.update(done)
  }
  let processed = 0
  return processAll(
    getterFn,
    service,
    request,
    (current, total) =>
      Promise.resolve()
        .then(() => processor(current, total))
        .then((result) => {
          updateStatus(++processed, total)
          return result
        })
  ).then(
    (result) => {
      notify.stop()
      return result
    },
    (err) => {
      notify.stop()
      return Promise.reject(err)
    }
  )
}

export const createStandardDelete = ({
  itemName,
  service,
  deleteFunction = (item) =>
    execute({
      uri: (typeof service === 'function'
        ? service()
        : service
      )
        .byId(item.id)
        .withVersion(item.version)
        .build(),
      method: 'DELETE'
    })
}) => () => {
  // eslint-disable-next-line no-console
  console.log('\x1b[32m%s\x1b[0m', `Deleting ${itemName}`)
  const spaces = (amount) =>
    [...new Array(amount)].map(() => ' ').join('')
  return processWithNotify(
    `Delete ${itemName}${spaces(
      20 - itemName.length
    )}{bar} |` + '| {percentage}% || {value}/{total} items',
    execute,
    service,
    {
      method: 'GET'
    },
    deleteFunction
  ).catch((err) =>
    logAndExit(err, `Failed to delete ${itemName}`)
  )
}

// // Logging what requests are made can be convenient when debugging
// function requestLogger (httpModule){
//   var original = httpModule.request
//   httpModule.request = function (options, callback){
//     console.log(options.url)
//     return original(options, callback)
//   }
// }

// requestLogger(require('http'))
// requestLogger(require('https'))
