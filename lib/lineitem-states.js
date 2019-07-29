import { client, stateService } from './services'
const nconf = require('nconf')

let initialId
let callerName

const loadInitialStateId = () => {
  client.execute({
    uri: stateService.where('builtIn = true').build(),
    method: 'GET'
  }).then(result => { initialId = result.body.results[0].id })
    .catch(error => console.log(error))
}

const clearAllStatesWithTransaction = (cb) => {
  try { throw new Error() } catch (e) {
    const re = /(\w+)@|at (\w+) \(/g
    callerName = re.exec(e.stack)[1] || re.exec(e.stack)[2]
  };
  client.execute({
    uri: stateService.where('transitions is not empty').build(),
    method: 'GET'
  }).then(result => unsetTransitions(result.body.results, cb))
    .catch(error => console.log(error))
}

const unsetTransitions = (results, cb) => {
  const promises = []
  const action = [{
    /* eslint quote-props: ["error", "consistent"] */
    'action': 'setTransitions',
    'transitions': []
  }]
  for (let i = 0, len = results.length; i < len; i++) {
    promises.push(client.execute({
      uri: stateService.byId(results[i].id).withVersion(results[i].version).build(),
      method: 'POST',
      body: { version: results[i].version, actions: action }
    }))
  }
  Promise.all(promises)
    .then(res => {
      client.execute({
        uri: stateService.where('builtIn = false').build(),
        method: 'GET'
      }).then(response => {
        const deletePromises = []
        for (let i = 0, len = response.body.results.length; i < len; i++) {
          deletePromises.push(deleteStates(response.body.results[i]))
        }

        Promise.all(deletePromises)
          .then(res => {
            console.log('\x1b[32m%s\x1b[0m', 'All states are succesfully deleted')
            if (callerName === 'clean') {
              return cb()
            }
          })
          .catch(err => console.log(err))
      })
    })
    .catch(err => console.log(err))
}

const deleteStates = (item) => {
  console.log('delete promise for state: ' + item.id)
  return client.execute({
    uri: stateService.byId(item.id).withVersion(item.version).build(),
    method: 'DELETE'
  })
}

const createStateDraft = (key, nameEN, nameDE) => {
  return {
    'key': key,
    'type': 'LineItemState',
    'initial': false,
    'name': { 'en': nameEN, 'de': nameDE }
  }
}

const createTransactionsDraft = (toIds) => {
  const transactionObj = []
  toIds.forEach(item => {
    transactionObj.push({ 'typeId': 'state', 'id': item })
  })

  return [{
    'action': 'setTransitions',
    'transitions': transactionObj
  }]
}

const createTransactions = (fromId, toIds) => {
  client.execute({
    uri: stateService.byId(fromId).build(),
    method: 'GET'
  })
    .then(res => {
      client.execute({
        uri: stateService.byId(fromId).build(),
        method: 'POST',
        body: { version: res.body.version, actions: createTransactionsDraft(toIds) }
      })
        .then(res => console.log('transaction(s) created for: ' + fromId))
        .catch(err => console.log(err))
    })
    .catch(err => console.log(err))
}

// return a promise
const createState = (key, nameEN, nameDE) => {
  return client.execute({
    uri: stateService.build(),
    method: 'POST',
    body: createStateDraft(key, nameEN, nameDE)
  })
}

const AddStateToProject = () => {
  let readyToShipId, backOrderId, shippedId, cancelledId, pickingId, returnedId, returnApprovedId,
    returnNotApprovedId, closedId, lostId, lossApprovedId, lossNotApprovedId

  Promise.all([createState('readyToShip', 'Ready to Ship', 'Versandfertig'),
    createState('backorder', 'In replenishment', 'Wird nachbestellt'),
    createState('shipped', 'Shipped', 'Versandt'),
    createState('canceled', 'Canceled', 'Storniert'),
    createState('picking', 'Picking', 'Picking'),
    createState('returned', 'Returned', 'Retourniert'),
    createState('returnApproved', 'Return approved', 'Retoure akzeptiert'),
    createState('returnNotApproved', 'Return not approved', 'Retoure nicht akzeptiert'),
    createState('closed', 'Closed', 'Abgeschlossen'),
    createState('lost', 'Lost', 'Verloren gegangen'),
    createState('lossApproved', 'Loss Approved', 'Verlust bestätigt'),
    createState('lossNotApproved', 'Loss not Approved', 'Wieder gefunden')])
    .then(response => {
      readyToShipId = response[0].body.id
      backOrderId = response[1].body.id
      shippedId = response[2].body.id
      cancelledId = response[3].body.id
      pickingId = response[4].body.id
      returnedId = response[5].body.id
      returnApprovedId = response[6].body.id
      returnNotApprovedId = response[7].body.id
      closedId = response[8].body.id
      lostId = response[9].body.id
      lossApprovedId = response[10].body.id
      lossNotApprovedId = response[11].body.id

      createTransactions(initialId, [pickingId, backOrderId])
      createTransactions(pickingId, [readyToShipId, backOrderId])
      createTransactions(backOrderId, [pickingId, cancelledId])
      createTransactions(readyToShipId, [shippedId])
      createTransactions(shippedId, [returnedId, lostId, closedId])
      createTransactions(returnedId, [returnApprovedId, returnNotApprovedId])
      createTransactions(returnApprovedId, [closedId])
      createTransactions(lostId, [lossNotApprovedId, lossApprovedId])
      createTransactions(lossApprovedId, [closedId])
      createTransactions(lossNotApprovedId, [closedId])
    })
    .catch(err => console.log(err))
}

if (nconf.get('clean')) {
  clearAllStatesWithTransaction()
} else if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing line item states...')
  loadInitialStateId()
  AddStateToProject()
}

module.exports = {
  clearAllStatesWithTransaction
}
