This folder contains extensions and additions for the p2abcengine.

 * services/ contains the webservices (GUI and REST-API) for the p2abcengine.
 * components/ contains some common components required by services/ and/or integration-tests
 * integration-tests/ contains tests that can be manually launched.

## How to build the core-abce

Before building the webservices you must/should build the core-abce first.

    cd Code/core-abce
    mvn clean install -DskipTests

## How to build the webservices

To build the web-services you should do the following:

    cd Code/zhaw/components
    mvn clean install
    cd Code/zhaw/services 
    mvn clean install
    
You can then deploy the war (which currently contains ALL the services) to a tomcat server:

    mvn -Dtomcat.username=munt -Dtomcat.password=**** tomcat7:deploy

(use ```tomcat7:redeploy``` if you want to redeploy instead).

