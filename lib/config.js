var nconf = require('nconf');
var path = require('path');
var fs = require('fs');

class Configuration{

        constructor(){
            this.config = {};
            this.init();
        }

        init() {
            this.load();
            this.print();
        }

        load() {
            nconf.argv()
            .env();

            //required key check
            nconf.required(['project', 'client_id', 'client_secret']);
            this.config = {
                project: nconf.get('project'),
                client_id: nconf.get('client_id'),
                client_secret: nconf.get('client_secret'),
                authUrl: nconf.get('authUrl'),
                apiUrl: nconf.get('apiUrl')
            };
            
            
            // if(nconf.get('save') !== undefined) {
            //     this.wrrite();
            // } else if(nconf.get('csv-cred-save')) {
            //     this.writeSVC();
            // }
            console.log('this is the save value:' + this.save);
        }

        print(){
            console.log('--------------------------------------------------------');            
            console.log('project: ' + this.config.project);
            console.log('client_id: ' + this.config.client_id);
            console.log('client_secret: ' + this.config.client_secret);
            console.log('authUrl' + this.config.authUrl);
            console.log('apiUrl' + this.config.apiUrl);
            console.log('--------------------------------------------------------');
        }

       
        data() {
            return this.config;
        }
}

export const config = new Configuration().data();




