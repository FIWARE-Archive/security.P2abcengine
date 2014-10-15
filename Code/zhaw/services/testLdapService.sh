#!/bin/sh
curl -X GET 'http://localhost:8888/zhaw-p2abc-webservices/schemaDump?oc=abcPerson' > in.xml
curl -X POST --header 'Content-Type: application/xml' -d @in.xml 'http://localhost:8888/zhaw-p2abc-webservices/genCredSpec' > credSpec.xml
curl -X POST --header 'Content-Type: application/xml' -d @credSpec.xml 'http://localhost:8888/zhaw-p2abc-webservices/genIssuanceAttributes' > outAttribs.xml
