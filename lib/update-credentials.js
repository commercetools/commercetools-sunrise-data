var fs = require('fs');
var path = require('path');
var readline = require('readline');
import { config } from './config';


const createSphereCredentialsFile = (config) => {
    const data = config.project + ':' + config.client_id + ':' +config.client_secret;
    fs.writeFile('.sphere-project-credentials', data, function(err) {
        if(err) {
            return console.log(err);
        }
    }); 
    console.log('configuration file saved! filename: ' + '.sphere-project-credentials');
}

createSphereCredentialsFile(config);
