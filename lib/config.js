var nconf = require('nconf');
var path = require('path');
var fs = require('fs');
var { URL } = require('url');

class Configuration{

        constructor(){
            this.config = {};
            this.init();
        }

        init() {
            this.load();
            this.print();
        }

        normalizeUrl(urlString) {
            // parse to check validity and return only protocol, host and port without path or trailing slash
            return new URL(urlString).origin;
        }

        load() {
            nconf.argv()
            .env();

            //required key check
            nconf.required(['projectKey', 'clientId', 'clientSecret', 'authUrl', 'apiUrl']);
            this.config = {
                projectKey: nconf.get('projectKey'),
                clientId: nconf.get('clientId'),
                clientSecret: nconf.get('clientSecret'),
                authUrl: this.normalizeUrl(nconf.get('authUrl')),
                apiUrl: this.normalizeUrl(nconf.get('apiUrl'))
            };
            
        }

        print(){
            console.log('--------------------------------------------------------');            
            console.log('projectKey: ' + this.config.projectKey);
            console.log('clientId: ' + this.config.clientId);
            console.log('clientSecret: ' + this.config.clientSecret);
            console.log('authUrl: ' + this.config.authUrl);
            console.log('apiUrl: ' + this.config.apiUrl);
            console.log('--------------------------------------------------------');
        }

       
        data() {
            return this.config;
        }
}

export const config = new Configuration().data();




