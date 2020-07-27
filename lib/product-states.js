import { stateService } from './services'
import {
  logAndExit,
  execute,
  createStandardDelete
} from './helpers'
const nconf = require('nconf')

export const deleteAllProductStates = () =>
  createStandardDelete({
    itemName: 'transition states',
    service: () =>
      stateService.where(
        'transitions is not empty and type = "ProductState"'
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
        stateService.where('type = "ProductState"')
    })
  )

const createStateDraft = (
  key,
  nameEN,
  nameDE,
  intialState
) => ({
  key: key,
  type: 'ProductState',
  initial: intialState,
  name: { en: nameEN, de: nameDE }
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
const createState = ([key, nameEN, nameDE, intialState]) =>
  execute({
    uri: stateService.build(),
    method: 'POST',
    body: createStateDraft(key, nameEN, nameDE, intialState)
  }).then(({ body: { id } }) => id)

export const importProductStates = () =>
  execute({
    uri: stateService.build(),
    method: 'POST',
    body: createStateDraft(
      'productStateInitial',
      'Open',
      'Open',
      true
    )
  })
    .then((response) => response.body?.id)
    .then((initialId) =>
      Promise.all(
        [
          [
            'readyForReview',
            'Ready for Review',
            'Ready for Review',
            false
          ],
          ['approved', 'Approved', 'Approved', false]
        ]
          .map(createState)
          .concat(initialId)
      )
    )
    .then(([readyForReviewId, approvedId, initialId]) =>
      Promise.all([
        createTransactions(initialId, [readyForReviewId]),
        createTransactions(readyForReviewId, [
          initialId,
          approvedId
        ])
      ])
    )
    .catch((err) =>
      logAndExit(err, 'Failed to import product states')
    )

if (nconf.get('clean')) {
  deleteAllProductStates()
} else if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log(
    '\x1b[32m%s\x1b[0m',
    'Importing line item states...'
  )
  importProductStates()
}
