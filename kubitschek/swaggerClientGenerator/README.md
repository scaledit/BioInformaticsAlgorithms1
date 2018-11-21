# Project for public configuration API
#
# This project requires the swagger-js-codegen module:
#
#    https://github.com/wcandillon/swagger-js-codegen
#

This project runs a Node.js application that processes a Swagger api.json file into an Angular sample client JavaScript file.
The project only runs Node for the purpose of using the Swagger file to generate an angular JavaScript file.

NOTE: you must have Node installed and you must run 'npm install' prior to executing the SBT commands.

The sbt script target

```
sbt genAngularClient
```

runs the Node application 'angularClient.js' against the api.json file and creates 

```
src/main/resources/js/RulesApiSwaggerAngularClient.js
```

That path is the SBT standard path for resources so that the 'sbt package' command can produce a JAR file
without any extra scripts or build stages.

#Publish the artifact

Any checkins will cause CircleCI build to run, and a new NPM 'tgz' is built and published to Artifactory.



