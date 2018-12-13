import {
    client,
    categoriesService,
    ordersService
} from './services'

const deleteCategories = () => {
    client.execute({
        uri: categoriesService.build(),
        method: 'GET',
    }).then(result => {
            return Promise.all(
                    result.body.results.map((element) => {
                        if (!element.parent) {
                            return client.execute({
                                uri: categoriesService.byId(element.id).withVersion(element.version).build(),
                                method: 'DELETE',
                            })
                        }
                        
                    })
                ).then(console.log('All categories are succesfully deleted'))
                .catch(error => console.log(error))
                }
        ).catch(error => console.log(error))
    ;
}

const deleteOrders = () => {
    client.execute({
        uri: ordersService.perPage(500).build(),
        method: 'GET',
    }).then(result => {
        return Promise.all(
            result.body.results.map((element) => {
                return client.execute({
                    uri: ordersService.byId(element.id).withVersion(element.version).build(),
                    method: 'DELETE',
                })
            })
        ).then(console.log('All orders are succesfully deleted'))
        .catch(err => console.log(err))
        }
    ).catch(err => console.log(err));
}

deleteCategories();
deleteOrders();