var nconf = require('nconf')
require('dotenv').config()
var { URL } = require('url')

class Configuration {
  constructor () {
    this.config = {}
    this.init()
  }

  init () {
    this.load()
    this.print()
  }

  normalizeUrl (urlString) {
    // parse to check validity and return only protocol, host and port without path or trailing slash
    if (!/^https?:\/\//i.test(urlString)) {
      urlString = 'https://' + urlString
    }

    return new URL(urlString).origin
  }

  load () {
    nconf.argv().env()

    // required key check
    // nconf.required(['projectKey', 'clientId', 'clientSecret', 'authUrl', 'apiUrl'])
    this.config = {
      projectKey: process.env.CTP_PROJECT_KEY,
      clientId: process.env.CTP_CLIENT_ID,
      clientSecret: process.env.CTP_CLIENT_SECRET,
      authUrl: this.normalizeUrl(process.env.CTP_AUTH_URL),
      apiUrl: this.normalizeUrl(process.env.CTP_API_URL)
    }
  }

  print () {
    // eslint-disable-next-line no-console
    console.log(
      '--------------------------------------------------------'
    )
    // eslint-disable-next-line no-console
    console.log('projectKey: ' + this.config.projectKey)
    // eslint-disable-next-line no-console
    console.log('clientId: ' + this.config.clientId)
    // eslint-disable-next-line no-console
    console.log('clientSecret: ' + this.config.clientSecret)
    // eslint-disable-next-line no-console
    console.log('authUrl: ' + this.config.authUrl)
    // eslint-disable-next-line no-console
    console.log('apiUrl: ' + this.config.apiUrl)
    // eslint-disable-next-line no-console
    console.log(
      '--------------------------------------------------------'
    )
  }

  data () {
    return this.config
  }
}

export const config = new Configuration().data()
