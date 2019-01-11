import { client, categoriesService } from './services'
var nconf = require('nconf')

const deleteAllCategories = () => {
    const request = {
        uri: categoriesService
        .where('parent is not defined')
        .build(),
        method: 'GET',
    }
    client.process(
        request,
        (payload) => {
            const results = payload.body.results;
            return Promise.all(
                results.map((element) => {
                        return client.execute({
                            uri: categoriesService
                            .byId(element.id)
                            .withVersion(element.version)
                            .build(),
                            method: 'DELETE',
                        })

                })
            )
        }
).then(() => {
            console.log('All categories are succesfully deleted')
                })
        .catch(err => console.log(err));
}

if (nconf.get('clean')) {
    deleteAllCategories()
  }
