import { client, productsService, productTypesService } from './services'
var nconf = require('nconf')

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
    console.log('\x1b[32m%s\x1b[0m', 'All products are succesfully deleted')
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
      console.log('\x1b[32m%s\x1b[0m', 'All product types are succesfully deleted')
    })
      .catch(err => console.log(err))
  })
    .catch(err => console.log(err))
}

const importProductTypes = () => {
  var productTypes = require(process.cwd() + '/data/product-type.json')
  productTypes.forEach(element => {
    const updateRequest = {
      uri: productTypesService.build(),
      method: 'POST',
      body: element
    }

    client.execute(updateRequest)
      .catch(error => console.log(error.body.errors))
  })
}

if (nconf.get('clean')) {
  deleteAllProducts()
} else if (nconf.get('import')) {
  importProductTypes()
}
