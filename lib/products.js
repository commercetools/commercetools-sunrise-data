import { client, productsService, productTypesService } from './services'

const deleteAllProducts = () => {
  const request = {
    uri: productsService
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
            uri: productsService
              .byId(element.id)
              .build(),
            method: 'POST',
            body: {
              version: element.version,
              actions: [{
                action: 'unpublish'
              }]
            }
          }).then(result => {
            return client.execute({
              uri: productsService
                .byId(result.body.id)
                .withVersion(result.body.version)
                .build(),
              method: 'DELETE'
            })
          })
            .catch(err => console.log(err))
        })
      )
    }
  ).then(() => {
    console.log('All products are succesfully deleted')
  })
    .catch(err => console.log(err))
}

const deleteProductTypes = () => {
  const request = {
    uri: productTypesService
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
            uri: productTypesService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('All product types are succesfully deleted')
  })
    .catch(err => console.log(err))
}

deleteAllProducts();
deleteProductTypes()
