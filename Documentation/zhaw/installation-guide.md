Privacy GEri Installation and Administration Guide
===============================

# Before You Start

You should familiarise yourself with the concepts of the Privacy GE as laid out in the Privacy GE architecture description.  That document also has a complete glossary of all the terms that we will be using in this document.

# How to Build and Deploy

## Requirements and Prerequisites

* A [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) for Java version 8 (or probably higher). The runtime environment (JRE) will *not* suffice. (This is a prerequisite also for building IBM's P2ABC engine.)
* A recent [git](http://www.git-scm.com/) client.
* [Maven 3](https://maven.apache.org/) (this is a prerequisite also for building IBM's P2ABC engine).
* The current version of the Privacy GEri, which is available from the [ZHAW github](https://github.engineering.zhaw.ch/neut/p2abcengine).

You will also need either a functioning .NET environment or a mono executable; we have found no way to disable .NET support in the libary that does all the crypto.  The .NET environment will not be needed, it just has to look as if it was there. If you don't already have .NET and don't want mono, you could execute these commands in a directory on the servlet engine's execution path:

    if [ ! -f mono ]; then
      echo : > mono
      chmod 755 mono
    fi

Next, build the P2ABC engine according to its [https://github.com/p2abcengine/p2abcengine/wiki/How-to-Build-the-ABC-Engine build instructions]. This will install the required JAR files in your local Maven repository. 

**Please NOTE** that ZHAW does *not* maintain the P2ABC engine, nor do we maintain its documentation. Should you find errors in the installation instructions or in the software, please address them to the maintainers of the P2ABC engine, not to us. If you find any, we *cannot* do anything about them even if we wanted to, except perhaps sympathise with you.

### Obtaining the Source Code

The latest version can always be obtained from ZHAW's [P2ABC github](https://github.engineering.zhaw.ch/neut/p2abcengine):

    git clone https://github.engineering.zhaw.ch/neut/p2abcengine.git
    mv p2abcengine p2abcengine-zhaw

That last step, renaming the directory, is necessary because IBM's p2abcengine will clone into the same directory.

### Building the Privacy GE

Once this is in place, building the Privacy GE is as simple as

    cd p2abcengine-zhaw
    mvn package

(You can add ```-DskipTests``` to the Maven invocation if you would like to skip the unit tests.)

Per default this will generate a war-file with all the services included. If you prefer a war-file with only one involved party you can
do this by specifing a maven profile such as:

     mvn package -P user
     mvn package -P issuance
     mvn package -P verification

which will produce war-files using only the corresponding REST-service and GUI-service.

### Deploying the GE

The result of the previous step is that a file ```services/target/zhaw-p2abc-webservices.war``` (or ```services/target/zhaw-webservices-4.1.3-*PROFILE*.war```) has been created. This is the Privacy GE. You deploy it like you would deploy any other WAR; the precise mechanics depend on the servlet engine and your preferred mode of deployment.

## Providers

### Authentication-Providers

The Authentication-Providers are used by the issuance service to authenticate users to then later gather
the correct user's data from the attribute source. 

Currently authentication through LDAP, JDBC or KEYROCK is supported. ```_UID_``` when using
JDBC is the sha1 hash of the username provided during the authentication process. When using KEYROCK
```_UID_``` is the sha1 hash of the e-mail value returned by Keyrock. For LDAP ```_UID_``` is the
raw username provided during authentication however limited to characters ```a-zA-Z```.

#### LDAP

When using LDAP as an authentication source the *bindQuery* is used to find the Bind-DN of the user in the LDAP
directory. Therefore the *bindQuery* must be an LDAP search filter capable of finding the correct user.

#### JDBC

For JDBC the *bindQuery* must be an SQL-Query that returns the password (as a sha1 hash) together with it's salt.
Such a query could for example be ```SELECT password, salt FROM users WHERE username='_UID_'```. The JDBCAuthenticationProvider
will perform the comparison ```sha1(salt + authPassword) == sha1(password)``` (where authPassword is the password given to the
issuance service by the user). Please be aware that the order of the columns returned by the *bindQuery* is relevant.
```SELECT salt, password``` doesn't obey the required order. First column must be the password and the second column must be the salt.

#### KEYROCK

The KeyrockAuthenticationProvider ignores the *bindQuery* since it is not required and therefore unused.

### Attribute-Providers

The AttributeValueProviders are capable of extracting values from an underlying attribute source whereas AttributeInfoProviders
are used to extract meta-information from and underlying attribute source. AttributeInfoProviders are used when
generating CredentialSpecifications and AttributeValueProviders are used when issuing Credentials.

Currently support for LDAP and JDBC is available. We recommend using a JDBC database as an attribute source. When using LDAP
the *name* parameter for generating CredentialSpecifications refers to an *objectClass* in LDAP. When using JDBC the *name*
parameter for generating CredentialSpecifications refers to the name of a table in the database.

For LDAP the *queryRule* is an LDAP search filter capable of finding an entry in LDAP that is somehow bound to the user (i.e.
contains the user's attributes). For JDBC the *queryRule* is an SQL-Query that queries all attributes of the user
(e.g. an example query would be ```SELECT * FROM userAttributes WHERE user = '_UID_';```). 

#### Mappings

Mappings determine how the data types of the underlying attribute source will be mapped (by the Attribute-Providers)
to p2abc data types. 

Note: If you plan on using dates we recommend using timestamps stored
as integers. 

##### LDAP-Mappings

The LDAP-Provider is capable of understanding the following types:

* ```1.3.6.1.4.1.1466.115.121.1.15``` will be mapped to ```xs:string``` with encoding ```urn:abc4trust:1.0:encoding:string:sha-256```
* ```1.3.6.1.4.1.1466.115.121.1.50``` will be mapped to ```xs:string``` with encoding ```urn:abc4trust:1.0:encoding:string:sha-256```
* ```1.3.6.1.4.1.1466.115.121.1.27``` will be mapped to ```xs:integer``` with ```encoding urn:abc4trust:1.0:encoding:integer:signed```

##### JDBC-Mappings

The JDBC-Provider is capable of understanding the following types:

* ```VARCHAR``` will be mapped to xs:string with encoding ```urn:abc4trust:1.0:encoding:string:sha-256```
* ```BIGINT```, ```INTEGER``` and ```SMALLINT``` will be mapped to ```xs:integer with encoding urn:abc4trust:1.0:encoding:integer:signed``` 

```VARCHAR```, ```BIGINT```, ```INTEGER``` and ```SMALLINT``` refer to ```java.sql.Types.*``` types.

## Configuration

Configuration is done through ```web.xml``` and ```context.xml``` (which reside in the ```WEB-INF``` and ```META-INF``` directories, respectively).  Some entries exist for more than one service.

```IMPORTANT```: URLs that are generated by the Privacy GE may contain encoded slashes. These are sometimes disabled in application servers and then *the Privacy GE will not work properly*. To enable this in Tomcat, put this line in Tomcat's configuration file:

    org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true

If you run another application server, please consult its manual.

### Issuance Service

The components of the issuer that communicate with either an [[Privacy - Architecture Description#Identity Source|identity source]] or an [[Privacy - Architecture Description#Attribute Source|attribute source]] are called [[Privacy - Architecture Description#Authentication Provider|authentication providers]] and [[Privacy - Architecture Description#Attribute Provider|attribute providers]], respectively. [[Privacy - Architecture Description#Authentication Provider|authentication providers]] extend the abstract class ```ch.zhaw.ficore.p2abc.services.issuance.AuthenticationProvider```.

Attribute providers are implemented by extending two abstract classes.  The first abstract class, ```ch.zhaw.ficore.p2abc.services.issuance.AttributeInfoProvider```, provides meta-information about an attribute source, such as the names and types of the attributes that are available in that source.  The second abstract class, ```ch.zhaw.ficore.p2abc.services.issuance.AttributeValueProvider```, finally implements the mechanism to retrieve the attributes elonging to a name.  Using this mechanism, it is possible to write a provider that uses a CSV file as both an identity source and as an attribute source).  If you do that, you will need to modify the factory methods of the extended abstract classes.

In ```web.xml```:

* ```cfg/useDbLocking```: If set to ```true``` the software will not make concurrent requests to the underlying JDBC database. Locking for example might be necessary when using SQLite (in certain circumstances).
* ```cfg/Source/attributes```: The attribute source to use (java.lang.String). Example values: ```LDAP```, ```JDBC```. If you downloaded the service from github, the value will be ```FAKE```, which is ```NOT SUITABLE FOR PRODUCTION``` and ```MUST BE REPLACED BEFORE DEPLOYING THE SERVICE```!
* ```cfg/Source/authentication```': The authentication source to use (java.lang.String). Example values: ```FAKE```, ```LDAP```, ```JDBC```.
* ```cfg/bindQuery```: The so called *Bind Query*. This is used by the authentication provider. One plausible example is: ```SELECT password FROM users WHERE username='_UID_'```. Please refer to the documentation of the underlying *AuthenticationProvider* because the *Bind Query* is provider specific.
* ```cfg/useDbLocking```: If set to true the software will not make concurrent requests to the underlying JDBC-database. Locking for example might be necessary when using SQLite-Databases (in certain circumstances).
* ```cfg/issuanceServiceURL```: The URL of the issuance-service. This URL is used by the issuance-gui to talk with the issuance-resource. URL should end with a ```/```. Example value: ```http://srv-lab-t-425.zhaw.ch:8080/zhaw-p2abc-webservices/issuance/```. (Issuance-GUI)
* ```cfg/restAuthPassword```: The password to use to authenticate to the REST-API of the services. The REST-API of the (non-gui) services is protected by *Basic HTTP Auth*. (All GUI services).
* ```cfg/restAuthUser```: The user to use to authenticate to the REST-API of the services. The REST-API of the (non-gui) services is protected by *Basic HTTP Auth*. (All GUI services).
* ```cfg/keyrock/baseURL```: URL to the Keyrock OAuth API. Defaults to ```https://account.lab.fiware.org/```.

In ```context.xml```:

    <Resource
	    name="jdbc/URIBytesStorage"
	    type="javax.sql.DataSource"
	    scope="Unshareable"
	    driverClassName="org.sqlite.JDBC"
	    factory="org.apache.tomcat.jdbc.pool.DataSourceFactory"
	    url="jdbc:sqlite:/tmp/blabla.db"/>

This resource entry is to configure the storage of the services. The web-services can be configured to use any JDBC-DataSource (in the example above we use SQLite).

    <Resource
        name="cfg/ConnectionParameters/attributes"
        type="ch.zhaw.ficore.p2abc.configuration.ConnectionParameters"
        factory="org.apache.naming.factory.BeanFactory"
        serverName="localhost"
        serverPort="10389"
        authenticationMethod="simple"
        user="uid=admin, ou=system"
        password="secret"
        connectionString="jdbc:sqlite:/home/mroman/test.db"
        driverString="org.sqlite.JDBC"
        useTls="false"/>
        
    <Resource
        name="cfg/ConnectionParameters/authentication"
        type="ch.zhaw.ficore.p2abc.configuration.ConnectionParameters"
        factory="org.apache.naming.factory.BeanFactory"
        serverName="localhost"
        serverPort="10389"
        authenticationMethod="simple"
        user="uid=admin, ou=system"
        password="secret"
        connectionString="jdbc:sqlite:/home/mroman/test.db"
        driverString="org.sqlite.JDBC"
        useTls="false"/>


These two *Resource* entries are to configure the connection parameters for the *Authentication-* and *Attribute Providers*. Depending on the provider not all settings are actually used.
For example the LDAP-Providers ignore *connectionString* and *driverString*. Please refer to the documentation of the underlying provider for more information about what settings are required and what
values it expects.

For LDAP the Query is an LDAP Search Query. For SQL the Query is an SQL-Statement. 
In Queries the String ```_UID_``` will be replaced with the user name as given in the
authentication request by an end user. User names are restricted ASCII letters only.

### Verification Service

In ```web.xml```:

* ```cfg/verifierIdentity```: The *Verifier Identity* is a String that should uniquely identify a verifier (for example use the URL of your verification-service). *Presentation policies* and *Presentation tokens* can contain a *Verifier Identity* which the verification-service will check if the *Verifier Identity* given by the user in the *Presentation Token* matches his own *Verifier Identity* (i.e. if the presentation token was actually intended for said verification-service and not for another verification-service. This is to prevent man-in-the-middle attacks where a verification-service proxies another verification-service. (```HOWEVER```: There seems to be an issue in the implementation of the underlying engine we use which appears to ```NOT``` sign the *Verifier Identity* which means that an attacker can alter the *Verifier Identity* of a created *Presentation Token* which means that it actually can't *really* protect against man-in-the-middle attacks. This issue is still under investigation.)
* ```cfg/useDbLocking```: If set to true the software will not make concurrent requests to the underlying JDBC-database. Locking for example might be necessary when using SQLite-Databases (in certain circumstances). (ALL)
* ```cfg/verificationServiceURL```: The URL of the verification-service. This URL is used by the verification-gui to talk with the verification-service. URL should end with a ```/```. Example value: ```http://srv-lab-t-425.zhaw.ch:8080/zhaw-p2abc-webservices/verification/```. (Verification-GUI)
* ```cfg/restAuthPassword```: The password to use to authenticate to the REST-API of the services. The REST-API of the (non-gui) services is protected by *Basic HTTP Auth*. (All GUI services).
* ```cfg/restAuthUser```: The user to use to authenticate to the REST-API of the services. The REST-API of the (non-gui) services is protected by *Basic HTTP Auth*. (All GUI services).
* ```cfg/allowFakeAccesstoken```: If set an accesstoken ```FAKE``` will always allow access to a resource named ```FAKE```. Don't use this in production.

### User Service

In ```web.xml```:

* ```cfg/useDbLocking```: If set to true the software will not make concurrent requests to the underlying JDBC-database. Locking for example might be necessary when using SQLite-Databases (in certain circumstances). (ALL)
* ```cfg/userServiceURL```: The URL of the user-service. This URL is used by the user-gui to talk with the user-service. URL should end with a ```/```. Example value: ```http://srv-lab-t-425.zhaw.ch:8080/zhaw-p2abc-webservices/user/```. (User-GUI)
* ```cfg/restAuthPassword```: The password to use to authenticate to the REST-API of the services. The REST-API of the (non-gui) services is protected by *Basic HTTP Auth*. (All GUI services).
* ```cfg/restAuthUser```: The user to use to authenticate to the REST-API of the services. The REST-API of the (non-gui) services is protected by *Basic HTTP Auth*. (All GUI services).


### User-GUI Service

In ```web.xml```:

* ```cfg/userGui/keyrockEnabled```: Boolean. If set to true then the user-gui will perform the neccessary intermediate steps required if the issuance-service requires keyrock authentication. This flag only affects the gui service.
* ```cfg/keyrock/clientId```: The ClientID as provided under "Credentials" in Keyrock. 
* ```cfg/keyrock/clientSecret```: The client secret as provided under "Credentials" in Keyrock.
* ```cfg/keyrock/baseURL```: URL to the Keyrock OAuth API. Defaults to ```https://account.lab.fiware.org/```.

*clientId* and *clientSecret* are generated by Keyrock when you register an application. The p2abc-webservices will use what is called *Resource Owner Password Credentials Grant* in OAuth. 
The accesstoken received from Keyrock is sent together with the issuance request to the issuance-service.

### P2ABC-Filter

* ```cfg/p2abc-filter/callbackRegex```: The callback regex for the P2ABC-Filter. If the path of a request matches this regex the filter will look for the accesstoken and try to verify it at the verification service.
* ```cfg/p2abc-filter/pathRegex```: This regex specifies when the P2ABC-Filter shall intercept requests and deny or allow access.
* ```cfg/p2abc-filter/resourceName```: This specifies the name of the resource. When the P2ABC-Filter performs the verifyAccessToken-lookup at the verification service it will match this ```resourceName``` with the output of the verification service.
* ```cfg/p2abc-filter/verifierURL```: The url of the verification service.

#### How does the P2ABC-Filter work?

The P2ABC-Filter will analyze requests and deny them if neccessary. If a request matches ```callbackRegex``` the P2ABC-Filter will require that an accesstoken is present in the request and will try to verify
it at the verification service and check if this accesstoken unlocks access to the resource ```resourcName```. If verification succeeds the P2ABC-Filter will inject a session cookie. 
If a request matches ```pathRegex``` the P2ABC-Filter will deny the request unless a valid session cookie is present.

#### How to use?

Given below is an example configuration (WEB-INF/web.xml):

    <env-entry>
        <env-entry-name>cfg/p2abc-filter/callbackRegex</env-entry-name>
        <env-entry-value>^demo-resource/page$</env-entry-value>
        <env-entry-type> java.lang.String </env-entry-type>
    </env-entry>
    <env-entry>
        <env-entry-name>cfg/p2abc-filter/pathRegex</env-entry-name>
        <env-entry-value>^demo-resource(.*)$</env-entry-value>
        <env-entry-type> java.lang.String </env-entry-type>
    </env-entry>
    <env-entry>
        <env-entry-name>cfg/p2abc-filter/resourceName</env-entry-name>
        <env-entry-value>FAKE</env-entry-value>
        <env-entry-type> java.lang.String </env-entry-type>
    </env-entry>
    <env-entry>
        <env-entry-name>cfg/p2abc-filter/verifierURL</env-entry-name>
        <env-entry-value>https://localhost:8443/zhaw-p2abc-webservices/verification</env-entry-value>
        <env-entry-type> java.lang.String </env-entry-type>
    </env-entry>

and to activate the P2ABC-Filter (which consists of a Request- and a ResponseFilter) (also WEB-INF/web.xml):

    <init-param>
        <param-name>com.sun.jersey.spi.container.ContainerRequestFilters</param-name>
        <param-value>ch.zhaw.ficore.p2abc.filters.PrivacyReqFilter</param-value>
    </init-param>
    <init-param>
        <param-name>com.sun.jersey.spi.container.ContainerResponseFilters</param-name>
        <param-value>ch.zhaw.ficore.p2abc.filters.PrivacyRespFilter</param-value>
    </init-param>

### Protecting the services from unwanted access

You will most likely want to prevent access to some of the REST-methods exposed by the services. 
All methods that should not be exposed to the public in the verification service and the issuance service have a path prefix
```/protected/*```. You can restrict access to these methods through the ```web.xml```. Given below is an example which
shows how to protect the issuance service.

    <security-constraint> 
      <!-- web resources that are protected -->
      <web-resource-collection>
        <web-resource-name>A Protected Page</web-resource-name>
        <url-pattern>/issuance/protected/*</url-pattern>
      </web-resource-collection>

      <auth-constraint>
        <!-- role-name indicates roles that are allowed
             to access the web resource specified above -->
        <role-name>p2abc-manager</role-name>
      </auth-constraint>
    </security-constraint> 

## Integrating P2ABC with Resource Owners

Resource owners such as hosters of web-applications or similar can very easily integrate the use of privacy-preserving credentials. A resource owner must register his resource at a cooperating verifier. For this purpose, the resource owner will need:

* the resource's URI;
* a presentation policy that controls access to that resource; and
* a redirect URI to which an end user is redirected with an access token if he can satisfy the given presentation policy.

This registration requires no change at all at the resource owner's web-application. The web-application need only extract the access token from the URI and perform a REST call to the verifier to verify it. 

For example, a resource owner might register his resource with a redirect URI of ```http://myresource.com/access```. Assuming that the end user passes verification of his credentials, he will be redirected to ```http://myresource.com/access/?accesstoken=xyz```.  The web-application now needs to extract the token from the query and make a REST call to the verifier ```http://myverifier.com/verification/verifyAccessToken?accesstoken=xyz```.  If successful, this will return the name/URI of the resource the end user requested, and an HTTP error otherwise.

Given below is some example code in Java (using ```javax.ws.rs```) which will verify that an end user has a valid access token and requested
the resource named *resource*. After an access token has been verified the verifier will forget the access token, this means that the application
should perform its own session handling/user tracking after it got the confirmation from the verifier that the access token was valid. 

    @GET()
    @Path("/page/")
    public Response resource(@QueryParam("accesstoken") String accessToken)
            throws Exception {
        try {
            String result = (String) RESTHelper
                    .getRequestUnauth("http://your-verifier.your-company.com/zhaw-p2abc-webservices/verification/verifyAccessToken?accesstoken="
                            + URLEncoder.encode(accessToken, "UTF-8")); //performs a HTTP-GET request

            if (result.equals("resource")) {
                // Your response goes here
                return Response.ok("You are allowed to access this page: " + result)
                        .build();
            } else {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("You are not allowed to access this page: " + result).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("You are not allowed to access this page!").build();
        }
    }


If you don't want or can't change your applications code you can use the P2ABC-Filter (see above). The P2ABC-Filter consists of a Response- and Request-Filter which you can use for example through tomcat. 


# Sanity check procedures

The Sanity Check Procedures are the steps that a System Administrator will take to verify that an installation is ready to be tested. This is therefore a preliminary set of tests to ensure that obvious or basic malfunctioning is fixed before proceeding to unit tests, integration tests and user validation.

Unit and integration tests of the p2abcengine run as self-hosted applications through the use of jersey. You can just run them through ```mvn test```. However, this does of course not test a live installation.
The tests are self-embedded and self-configuring. 

## End to End testing

* This is basically quick testing to check that everything is up and running. It may be composed of a single test or a few of them.
* E.g.: login on a web site and doing a basic query on a web form or API (provide URL and user/password)

## List of Running Processes

This depends on how you host the RESTful services. Usually you'll want to do this using a tomcat and therefore your tomcat should be running. 

## Network interfaces Up & Open

This also depends on how you host the services. Most likely you'll want them to run on port 443 using https but this is up to you. 

## Databases

This depends entirely on your configuration and what attribute and/or authentication providers you make use of.

# Diagnosis Procedures

The Diagnosis Procedures are the first steps that a System Administrator will take to locate the source of an error in a GE. Once the nature of the error is identified with these tests, the system admin will very often have to resort to more concrete and specific testing to pinpoint the exact point of error and a possible solution. Such specific testing is out of the scope of this section.

The following sections have to be filled in with the information or an “N/A” (“Not Applicable”) where needed. Do not delete section titles in any case. 

## Resource Availability

State the amount of available resources in terms of RAM and hard disk that are necessary to have a healthy enabler. This means that bellow these thresholds the enabler is likely to experience problems or bad performance. 

## Remote Service Access

State what enablers talk with your enabler and give the parameters to characterise such connection. The administrator will use this information to verify that such links are available.

## Resource Consumption

State the amount of resources that are abnormally high or low. This applies to 
RAM, CPU and i/o.

## I/O flows

State what a normal i/o flow is (in terms of flows in firewalls)
