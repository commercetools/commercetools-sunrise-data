import { client, categoriesService, ordersService } from './services'

const deleteCategories = () => {
    client.execute({
        uri: categoriesService.build(),
        method: 'GET'
    }).then(result => 
        result.body.results.forEach(element => {
            client.execute({
                uri: categoriesService.byId(element.id).withVersion(element.version).build(),
        method: 'DELETE',
              })
        console.log("Deleted category " + element.name.en)
        }))
        .catch(err => console.log(err));
}

const deleteOrders = () => {
    client.execute({
        uri: ordersService.perPage(500).build(),
        method: 'GET',
    }).then(result => 
        Promise.all(
            result.body.results.map((element) => {
                client.execute({
                    uri: ordersService.byId(element.id).withVersion(element.version).build(),
                    method: 'DELETE',
                })  
            })
        ).then(console.log('All orders are succesfully deleted'))
        .catch(err => console.log(err))
        ).catch(err => console.log(err));
}

deleteCategories();
deleteOrders();
