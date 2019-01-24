import { client, taxService } from './services'

const deleteTaxCategories = () => {
    const request = {
      uri: taxService
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
              uri: taxService
                .byId(element.id)
                .withVersion(element.version)
                .build(),
              method: 'DELETE'
            })
          })
        )
      }
    ).then(() => {
      console.log('All customer groups are succesfully deleted')
    })
      .catch(err => console.log(err))
  }

deleteTaxCategories()