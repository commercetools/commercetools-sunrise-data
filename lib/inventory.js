import {
  client,
  inventoryService
} from './services'

const deleteInventory = () => {
  const request = {
    uri: inventoryService
      .build(),
    method: 'GET'
  }
  client.process(
    request,
    (payload) => {
      const results = payload.body.results
      return Promise.all(
        results.map((element) => {
          return client.execute({
            uri: inventoryService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('All inventory entries are succesfully deleted')
  })
    .catch(err => console.log(err))
}

deleteInventory()
