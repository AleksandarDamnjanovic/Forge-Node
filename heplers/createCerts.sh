#!/bin/bash

mkdir -p ../certs ../SHARED

if [ "${FN_NON_INTERACTIVE:-0}" = "1" ]; then
    set -e
fi

echo "---> Generating private keys for a certificate authority and a broker"
openssl genrsa -out ../SHARED/ca.key 4096
openssl genrsa -out ../SHARED/broker.key 4096

echo "---> Generating certificate authority certificate"
if [ "${FN_NON_INTERACTIVE:-0}" = "1" ]; then
    openssl req -new -x509 -days 1000 -key ../SHARED/ca.key -out ../SHARED/ca.crt \
        -batch -subj "/O=ForgeNode/OU=CA/CN=ForgeNodeCA"
else
    openssl req -new -x509 -days 1000 -key ../SHARED/ca.key -out ../SHARED/ca.crt
fi

echo "---> Generating broker's certificate request"
echo "    ---> Common Name must be valid IP address of machine where mosquitto server is going to be running"
if [ "${FN_NON_INTERACTIVE:-0}" = "1" ]; then
    COMMON_NAME="${FN_COMMON_NAME:-127.0.0.1}"
    openssl req -new -out ../SHARED/broker.csr -key ../SHARED/broker.key \
        -batch -subj "/O=ForgeNode/OU=Broker/CN=${COMMON_NAME}"
else
    openssl req -new -out ../SHARED/broker.csr -key ../SHARED/broker.key
fi

echo "---> Signing certificate"
openssl x509 -req -in ../SHARED/broker.csr -CA ../SHARED/ca.crt -CAkey ../SHARED/ca.key -CAcreateserial -out ../SHARED/broker.crt -days 1000

echo "---> Cleaning"
rm -rf ../SHARED/broker.csr ../SHARED/ca.srl

echo "DONE"
