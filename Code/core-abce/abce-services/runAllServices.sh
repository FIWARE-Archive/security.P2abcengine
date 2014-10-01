#!/bin/bash

echo "Starting all services... (selfcontained)"

cd ./target/

java -jar selfcontained-issuance-service.war 9100 1>/tmp/issuance.log 2>/tmp/issuance.err.log &

java -jar selfcontained-user-service.war 9200 1>/tmp/user.log 2>/tmp/user.err.log &

java -jar selfcontained-verification-service.war 9300 1>/tmp/verification.log 2>/tmp/verification.err.log &

java -jar selfcontained-inspection-service.war 9400 1>/tmp/inspection.log 2>/tmp/inspection.err.log &

java -jar selfcontained-revocation-service.war 9500 1>/tmp/revocation.log 2>/tmp/revocation.err.log &

ps axuw | grep java

echo "Waiting...."
read -p "Press [Enter] to die..."
jobs -p | xargs kill -9
