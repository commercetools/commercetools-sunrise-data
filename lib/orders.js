import { client, ordersService } from './services'
var nconf = require('nconf')

const deleteAllOrders = () => {
    const request = {
        uri: ordersService
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
                            uri: ordersService
                            .byId(element.id)
                            .withVersion(element.version)
                            .build(),
                            method: 'DELETE',
                        })

                })
            ) 
        }
).then(() => {
            console.log('All orders are succesfully deleted')
                })
        .catch(err => console.log(err));
}

if (nconf.get('clean')) {
    deleteAllOrders()
  }