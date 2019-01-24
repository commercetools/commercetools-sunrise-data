import { client, typesService } from './services'

const deleteTypes = () => {
    const request = {
        uri: typesService
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
                uri: typesService
                  .byId(element.id)
                  .withVersion(element.version)
                  .build(),
                method: 'DELETE'
              })
            })
          )
        }
      ).then(() => {
        console.log('All types are succesfully deleted')
      })
        .catch(err => console.log(err))
}

deleteTypes()