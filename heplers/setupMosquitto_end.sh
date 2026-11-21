#!/bin/bash

echo "---> Generating necessary directories..."
mkdir -p /etc/mosquitto/ca_certificates
mkdir -p /etc/mosquitto/certs
mkdir -p /etc/mosquitto/conf.d

echo "---> Passing certificates..."
cp ./SHARED/ca.crt /etc/mosquitto/ca_certificates/ca.crt
cp ./SHARED/broker.key /etc/mosquitto/certs/broker.key
cp ./SHARED/broker.crt /etc/mosquitto/certs/broker.crt

echo "---> Changing ownership and permissions for new files and directories..."
chown -R mosquitto:mosquitto /etc/mosquitto
chmod 755 /etc/mosquitto
chmod 755 /etc/mosquitto/certs
chmod 755 /etc/mosquitto/ca_certificates
chmod 644 /etc/mosquitto/*.conf
chmod 640 /etc/mosquitto/users.conf
chmod 640 /etc/mosquitto/ca_certificates/ca.crt
chmod 640 /etc/mosquitto/certs/broker.key
chmod 640 /etc/mosquitto/certs/broker.crt

echo "---> Generating configuration..."
touch /etc/mosquitto/mosquitto.conf
echo "password_file /etc/mosquitto/users.conf" > /etc/mosquitto/mosquitto.conf
echo "listener 8883 $1" >> /etc/mosquitto/mosquitto.conf
echo "allow_anonymous false" >> /etc/mosquitto/mosquitto.conf
echo "cafile /etc/mosquitto/ca_certificates/ca.crt" >> /etc/mosquitto/mosquitto.conf
echo "keyfile /etc/mosquitto/certs/broker.key" >> /etc/mosquitto/mosquitto.conf
echo "certfile /etc/mosquitto/certs/broker.crt" >> /etc/mosquitto/mosquitto.conf
echo "tls_version tlsv1.2" >> /etc/mosquitto/mosquitto.conf

echo "---> DONE"
