import {
    client,
    categoriesService,
    ordersService,
    stateService,
    customersService,
    shippingMethodService,
    zonesService,
    productsService
} from './services'

const deleteCategories = () => {
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

const deleteOrders = () => {
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

const deleteStates = () => {
    const request = {
        uri: stateService
        .where('transitions is not empty')
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
                            uri: stateService
                            .byId(element.id)
                            .build(),
                            method: 'POST',
                            body: {
                                version: element.version,
                                actions: [{
                                    action: 'setTransitions',
                                    transitions: []
                                }]
                            }
                        }).then(result => {
                            if (!result.body.builtIn) {
                            return client.execute({
                                uri: stateService
                                .byId(result.body.id)
                                .withVersion(result.body.version)
                                .build(),
                                method: 'DELETE',
                            })
                        }
                        })
                        .catch(err => console.log(err))
                    
                })
            ) 
        }
).then(() => {

    const request = {
        uri: stateService
        .build(),
        method: 'GET',
    }
    client.process(
        request,
        (payload) => {
            const results = payload.body.results;
            return Promise.all(
                results.map((element) => {
                    if (!element.builtIn) {
                        return client.execute({
                            uri: stateService
                            .byId(element.id)
                            .withVersion(element.version)
                            .build(),
                            method: 'DELETE',
                        })
                    }
                })
            ) 
        }
).then(() => {
            console.log('All states are succesfully deleted')
                })
        .catch(err => console.log(err));
                })
        .catch(err => console.log(err));
}

const deleteCustomers = () => {
    const request = {
        uri: customersService
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
                            uri: customersService
                            .byId(element.id)
                            .withVersion(element.version)
                            .build(),
                            method: 'DELETE',
                        })

                })
            ) 
        }
).then(() => {
            console.log('All customers are succesfully deleted')
                })
        .catch(err => console.log(err));
}

const deleteShippingMethodsAndZones = () => {
    
    const request = {
        uri: shippingMethodService
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
                            uri: shippingMethodService
                            .byId(element.id)
                            .withVersion(element.version)
                            .build(),
                            method: 'DELETE',
                        })

                })
            ) 
        }
).then(() => {
            console.log('All shipping methods are succesfully deleted')
            const request = {
                uri: zonesService
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
                                    uri: zonesService
                                    .byId(element.id)
                                    .withVersion(element.version)
                                    .build(),
                                    method: 'DELETE',
                                })
        
                        })
                    ) 
                }
        ).then(() => {
                    console.log('All zones are succesfully deleted')
                        })
                .catch(err => console.log(err));
                })
        .catch(err => console.log(err));
}

const deleteProducts = () => {

    const request = {
        uri: productsService
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
                            method: 'DELETE',
                        })
                    })
                    .catch(err => console.log(err))

                })
            ) 
        }
).then(() => {
            console.log('All products are succesfully deleted')
                })
        .catch(err => console.log(err));
}


deleteCategories();
deleteOrders();
deleteStates();
deleteCustomers();
deleteShippingMethodsAndZones();
deleteProducts();