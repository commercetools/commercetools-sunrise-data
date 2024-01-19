import { stateService } from './services'
import {
  logAndExit,
  execute,
  createStandardDelete
} from './helpers'
const nconf = require('nconf')

export const deleteAllLineItemStates = () =>
  createStandardDelete({
    itemName: 'transition states',
    service: () =>
      stateService.where(
        'transitions is not empty and type = "LineItemState"'
      ),
    deleteFunction: (result) =>
      execute({
        uri: stateService
          .byId(result.id)
          .withVersion(result.version)
          .build(),
        method: 'POST',
        body: {
          version: result.version,
          actions: [
            {
              action: 'setTransitions',
              transitions: []
            }
          ]
        }
      })
  })().then(
    createStandardDelete({
      itemName: 'states',
      service: () =>
        stateService.where(
          'builtIn = false and type = "LineItemState"'
        )
    })
  )

const createStateDraft = (key, nameEN, nameDE, nameFR) => ({
  key: key,
  type: 'LineItemState',
  initial: false,
  name: { en: nameEN, de: nameDE, fr: nameFR }
})

const createTransactionsDraft = (toIds) => {
  const transactionObj = []
  toIds.forEach((item) => {
    transactionObj.push({ typeId: 'state', id: item })
  })

  return [
    {
      action: 'setTransitions',
      transitions: transactionObj
    }
  ]
}

const createTransactions = (fromId, toIds) =>
  execute({
    uri: stateService.byId(fromId).build(),
    method: 'GET'
  }).then((res) => {
    execute({
      uri: stateService.byId(fromId).build(),
      method: 'POST',
      body: {
        version: res.body.version,
        actions: createTransactionsDraft(toIds)
      }
    })
  })

// return a promise
const createState = ([key, nameEN, nameDE, nameFR]) =>
  execute({
    uri: stateService.build(),
    method: 'POST',
    body: createStateDraft(key, nameEN, nameDE, nameFR)
  }).then(({ body: { id } }) => id)

export const importLineItemStates = () =>
  execute({
    uri: stateService.where('builtIn = true').build(),
    method: 'GET'
  })
    .then((response) => response.body?.results[0]?.id)
    .then((initialId) =>
      Promise.all(
        [
          ['readyToShip', 'Ready to Ship', 'Versandfertig', 'Prêt pour l\'expédition'],
          ['backorder', 'In replenishment', 'Wird nachbestellt', 'En réapprovisionnement'],
          ['shipped', 'Shipped', 'Versandt', 'Expédié'],
          ['canceled', 'Canceled', 'Storniert', 'Annulé'],
          ['picking', 'Picking', 'Picking', 'Préparation'],
          ['returned', 'Returned', 'Retourniert', 'Returné'],
          ['returnApproved', 'Return approved', 'Retoure akzeptiert', 'Retour approuvé'],
          ['returnNotApproved', 'Return not approved', 'Retoure nicht akzeptiert', 'Retour non approuvé'],
          ['closed', 'Closed', 'Abgeschlossen', 'Complété'],
          ['lost', 'Lost', 'Verloren gegangen', 'Perdu'],
          ['lossApproved', 'Loss Approved', 'Verlust bestätigt', 'Perte approuvée'],
          ['lossNotApproved', 'Loss not Approved', 'Wieder gefunden', 'Perte non approuvée']
        ]
          .map(createState)
          .concat(initialId)
      )
    )
    .then(
      ([
        readyToShipId,
        backOrderId,
        shippedId,
        cancelledId,
        pickingId,
        returnedId,
        returnApprovedId,
        returnNotApprovedId,
        closedId,
        lostId,
        lossApprovedId,
        lossNotApprovedId,
        initialId
      ]) =>
        Promise.all([
          createTransactions(initialId, [
            pickingId,
            backOrderId
          ]),
          createTransactions(pickingId, [
            readyToShipId,
            backOrderId
          ]),
          createTransactions(backOrderId, [
            pickingId,
            cancelledId
          ]),
          createTransactions(readyToShipId, [shippedId]),
          createTransactions(shippedId, [
            returnedId,
            lostId,
            closedId
          ]),
          createTransactions(returnedId, [
            returnApprovedId,
            returnNotApprovedId
          ]),
          createTransactions(returnApprovedId, [closedId]),
          createTransactions(lostId, [
            lossNotApprovedId,
            lossApprovedId
          ]),
          createTransactions(lossApprovedId, [closedId]),
          createTransactions(lossNotApprovedId, [closedId])
        ])
    )
    .catch((err) =>
      logAndExit(err, 'Failed to import states')
    )

if (nconf.get('clean')) {
  deleteAllLineItemStates()
} else if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log(
    '\x1b[32m%s\x1b[0m',
    'Importing line item states...'
  )
  importLineItemStates()
}
