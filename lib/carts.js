import { client, cartsService } from './services'
const nconf = require('nconf');

const deleteAllCarts = (cb) => {
  let callerName;
  try { throw new Error(); }
  catch (e) { 
    let re = /(\w+)@|at (\w+) \(/g, st = e.stack, m;
    re.exec(st), m = re.exec(st);
    callerName = m[1] || m[2];
  };
  const request = {
    uri: cartsService
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
            uri: cartsService
              .byId(element.id)
              .withVersion(element.version)
              .build(),
            method: 'DELETE'
          })
        })
      )
    }
  ).then(() => {
    console.log('\x1b[32m%s\x1b[0m', 'All carts are succesfully deleted')
    if (callerName === 'clean'){
      return cb();
    }
  })
    .catch(err => console.log(err))
}

if (nconf.get('clean')) {
  deleteAllCarts()
}

module.exports = {
  deleteAllCarts
}