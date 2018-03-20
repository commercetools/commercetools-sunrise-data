# Sunrise Data

[![Build Status](https://travis-ci.org/commercetools/commercetools-sunrise-data.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-sunrise-data)


### Requirements

- Install [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Clone](https://help.github.com/articles/cloning-a-repository/) this repository to your computer
- Create an empty project (without sample data) in the [Admin Center](https://admin.commercetools.com)

### Configuration

Adapt [`src/main/resources/application.properties`](src/main/resources/application.properties) with the credentials of the commercetools project where to run the import. 

Optionally, in the same file you can also modify the location of the resources or disable some steps.

If you require a further modification of the steps, you can modify them directly in [`/src/main/java/com/commercetools/dataimport/DataImportJobConfiguration.java`](/src/main/java/com/commercetools/dataimport/DataImportJobConfiguration.java)

### Run

Now simply run the application with maven:
```bash
./mvnw spring-boot:run
``` 
Congratulations! If no error was raised, your project should contain the data from Sunrise.