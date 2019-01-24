import { client, channelsService } from './services'

const deleteChannels = () => {
    const request = {
      uri: channelsService
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
              uri: channelsService
                .byId(element.id)
                .withVersion(element.version)
                .build(),
              method: 'DELETE'
            })
          })
        )
      }
    ).then(() => {
      console.log('All channels are succesfully deleted')
    })
      .catch(err => console.log(err))
  }

  deleteChannels()
