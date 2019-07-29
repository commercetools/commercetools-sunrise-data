import { client, productsService, productTypesService } from './services'
require('dotenv').config();
const nconf = require('nconf');
const execSync = require('child_process').execSync;
const path = require("path");

const deleteAllProducts = (cb) => {
  let callerName;
  try { throw new Error(); }
  catch (e) { 
    let re = /(\w+)@|at (\w+) \(/g, st = e.stack, m;
    re.exec(st), m = re.exec(st);
    callerName = m[1] || m[2];
  };
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
      console.log('\x1b[32m%s\x1b[0m', 'All product types are succesfully deleted');
      if (callerName === 'clean'){
        return cb();
      }
    })
      .catch(err => console.log(err))
  })
    .catch(err => console.log(err))
};

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
};

const importProducts = () => {
  return path.join('node_modules', '.bin', 'product-csv-sync');
}

if (nconf.get('clean')) {
  deleteAllProducts()
} else if (nconf.get('importtypes')) {
  importProductTypes()
} else if (nconf.get('import')) {
  console.log('\x1b[32m%s\x1b[0m', 'Importing products...');
  execSync(`${importProducts()} import --matchBy sku --publish --csv data/products.csv -p ${process.env.CONFIG_PROJECT_KEY} -i ${process.env.CONFIG_CLIENT_ID}  -s ${process.env.CONFIG_CLIENT_SECRET} --sphereHost https://${process.env.CONFIG_API_URL} --sphereAuthHost https://${process.env.CONFIG_AUTH_URL}`);
}

module.exports = {
  deleteAllProducts
}