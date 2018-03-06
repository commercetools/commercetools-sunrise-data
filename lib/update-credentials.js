var fs = require('fs');
var path = require('path');
var readline = require('readline');
import { config } from './config';




const createSpringBootCredentialsFile = (config) => {
     
    var fs = require('fs');
    var lines = fs.readFileSync('src/main/resources/application.properties').toString().split("\n");
    for(var i in lines) {
        lines[i] = lines[i].replace('#ctp.projectKey=your-project-key', 'ctp.projectKey=' + config.project);
        lines[i] = lines[i].replace(/ctp.projectKey=.*/, 'ctp.projectKey=' + config.project);
        lines[i] = lines[i].replace('#ctp.clientId=your-client-id', 'ctp.clientId=' + config.client_id);
        lines[i] = lines[i].replace(/ctp.clientId=.*/, 'ctp.clientId=' + config.client_id);
        lines[i] = lines[i].replace('#ctp.clientSecret=your-client-secret', 'ctp.clientSecret=' + config.client_secret);
        lines[i] = lines[i].replace(/ctp.clientSecret=.*/, 'ctp.clientSecret=' + config.client_secret);
        console.log(lines[i]);
    }

    var file = fs.createWriteStream('src/main/resources/application.properties');
    lines.map(function(v) { file.write(v + '\n'); });
    file.end();
    
}

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
createSpringBootCredentialsFile(config);