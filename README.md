# Sunrise Data

[![Build Status](https://travis-ci.org/commercetools/commercetools-sunrise-data.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sunrise-data)

## How to create a project with Sunrise data

Before starting the import, make sure you have access to the [Admin Center](https://admin.commercetools.com). You will also need to run the application in this repository, check that you meet the [requirements](#requirements) and [clone](https://help.github.com/articles/cloning-a-repository/) this repository to your computer.

### 1. Create your project
1. Open the [Admin Center](https://admin.commercetools.com) and create an empty project (without sample data).

### 2. Prepare node environemnt for Import
1. Go to the root of this project, were the package.json is located.
2. Install all node dependencies
    ```js
        npm install
    ```
3. Set commercetools project credentials as npm config values:
    <pre>
        npm config set sunrise:authUrl <i>authUrl</i> - <i>(i.e. auth.commercetools.com)</i>
        npm config set sunrise:apiUrl <i>apiUrl</i> - <i>(i.e. api.commercetools.com)</i>
        npm config set sunrise:httpAuthUrl <i>https://[authUrl]</i> - <i>(i.e. https://auth.commercetools.com)</i>
        npm config set sunrise:httpApiUrl <i>https://[apiUrl]</i> - <i>(i.e. https://api.commercetools.com)</i>
        npm config set sunrise:projectKey <i>projectKey</i>
        npm config set sunrise:clientId <i>clientId</i>
        npm config set sunrise:clientSecret <i>clientSecret</i>
    </pre>
    *Url variables expect only the host name, not a full URL. 

### 3. Import basic data
1. import project setting, delivery zones / tax setting, categories and products
    ```js
        npm run start
    ```

### 4. Import additional data (optional)
1. import inventories
    ```js
        npm run import:inventory
    ```

## How to use the application

### Requirements

- Install [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- Install [Node.js](https://nodejs.org/en/download/current/) ^8 or ^9  (node 10 is not compatible)

On Issues with the node version (e.g. after switching the version), run `npm rebuild`
