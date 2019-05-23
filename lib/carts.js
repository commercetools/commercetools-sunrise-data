import {
  client,
  cartsService,
} from './services'

const deleteAllCarts = () => {
  const request = {
    uri: cartsService
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
            uri: cartsService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('\x1b[32m%s\x1b[0m', 'All carts are succesfully deleted')
  })
    .catch(err => console.log(err))
}

deleteAllCarts();
