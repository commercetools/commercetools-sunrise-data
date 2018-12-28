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
import {
    config
} from './config';
const request = require("request");

const deleteCategories = () => {
    var auth;
    var results;
    var tokenOptions = {
        url: 'https://auth.sphere.io/oauth/token',
  qs: 
   { grant_type: 'client_credentials',
     },
        method: 'POST',
        auth: {
            'user': config.clientId,
            'pass': config.clientSecret
        }
    };

    request(tokenOptions, function (error, response, body) {
        if (error) throw new Error(error);
        auth = JSON.parse(body);

        var options = {
        method: 'POST',
        url: 'https://api.commercetools.com/' + config.projectKey + '/graphql',
        headers: {
            Authorization: 'Bearer ' + auth.access_token,
            'Content-Type': 'application/json'
        },
        body: {
            query: 'query {categories {results {id, version}}}'
        },
        json: true
    };
    request(options, function (error, response, body) {
        if (error) throw new Error(error);

        results = body.data.categories.results;
    });
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
                
        .then(() => {
                    console.log('Сategories are succesfully deleted')
                        })
                .catch(err => console.log(err));

    });

    //     const request = {
    //         uri: categoriesService
    //         .where('parent is not defined')
    //         .build(),
    //         method: 'GET',
    //     }
    //     client.process(
    //         request,
    //         (payload) => {
    //             const results = payload.body.results;
    //             return Promise.all(
    //                 results.map((element) => {
    //                         return client.execute({
    //                             uri: categoriesService
    //                             .byId(element.id)
    //                             .withVersion(element.version)
    //                             .build(),
    //                             method: 'DELETE',
    //                         })

    //                 })
    //             ) 
    //         }
    // ).then(() => {
    //             console.log('All categories are succesfully deleted')
    //                 })
    //         .catch(err => console.log(err));
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
    }).catch(err => console.log(err));
}

const deleteStates = () => {
    client.execute({
        uri: stateService.build(),
        method: 'GET',
    }).then(result => {
        return Promise.all(
                result.body.results.map((element) => {
                    if (!element.builtIn) {
                        return client.execute({
                            uri: stateService.byId(element.id).withVersion(element.version).build(),
                            method: 'DELETE',
                        })
                    }

                })
            ).then(console.log('All states are succesfully deleted'))
            .catch(err => console.log(err))
    }).catch(err => console.log(err));
}

const deleteCustomers = () => {
    client.execute({
        uri: customersService.perPage(500).build(),
        method: 'GET',
    }).then(result => {
        return Promise.all(
                result.body.results.map((element) => {
                    return client.execute({
                        uri: customersService.byId(element.id).withVersion(element.version).build(),
                        method: 'DELETE',
                    })
                })
            ).then(console.log('All customers are succesfully deleted'))
            .catch(err => console.log(err))
    }).catch(err => console.log(err));
}

const deleteShippingMethodsAndZones = () => {
    client.execute({
        uri: shippingMethodService.perPage(500).build(),
        method: 'GET',
    }).then(result => {
        return Promise.all(
                result.body.results.map((element) => {
                    return client.execute({
                        uri: shippingMethodService.byId(element.id).withVersion(element.version).build(),
                        method: 'DELETE',
                    })
                })
            ).then(() => {
                console.log('All shipping methods are succesfully deleted')
                client.execute({
                    uri: zonesService.perPage(500).build(),
                    method: 'GET',
                }).then(result => {
                    return Promise.all(
                            result.body.results.map((element) => {
                                return client.execute({
                                    uri: zonesService.byId(element.id).withVersion(element.version).build(),
                                    method: 'DELETE',
                                })
                            })
                        ).then(console.log('All zones are succesfully deleted'))
                        .catch(err => console.log(err))
                }).catch(err => console.log(err));
            })
            .catch(err => console.log(err))
    }).catch(err => console.log(err));
}

const deleteProducts = () => {
    client.execute({
            uri: productsService.perPage(500).build(),
            method: 'GET',
        }).then(result => {
            if (result.body.count > 0) {
                console.log('Deleting ' + result.body.count + ' product(s) ..');
                Promise.all(
                        result.body.results.map((element) => {
                            return client.execute({
                                    uri: productsService.byId(element.id).build(),
                                    method: 'POST',
                                    body: {
                                        version: element.version,
                                        actions: [{
                                            action: 'unpublish'
                                        }]
                                    }
                                }).then(result => {
                                    return client.execute({
                                        uri: productsService.byId(result.body.id).withVersion(result.body.version).build(),
                                        method: 'DELETE',
                                    })
                                })
                                .catch(err => console.log(err))
                        })
                    ).then(() => {
                        deleteProducts()
                    })
                    .catch(err => console.log(err))
            } else {
                console.log('All products are successfully deleted')
            };
        })
        .catch(err => console.log(err));
}


deleteCategories();
// deleteOrders();
// deleteStates();
// deleteCustomers();
// deleteShippingMethodsAndZones();
// deleteProducts();