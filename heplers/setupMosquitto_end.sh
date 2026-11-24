#!/bin/bash

echo "---> Generating necessary directories..."
mkdir -p /etc/mosquitto/ca_certificates
mkdir -p /etc/mosquitto/certs
mkdir -p /etc/mosquitto/conf.d

CONF_FILE=""
if [ -f "./helpers/mqtt-configuration.conf" ]; then
    CONF_FILE="./helpers/mqtt-configuration.conf"
elif [ -f "./heplers/mqtt-configuration.conf" ]; then
    CONF_FILE="./heplers/mqtt-configuration.conf"
fi

CA_SRC="./SHARED/ca.crt"
KEY_SRC="./SHARED/broker.key"
CRT_SRC="./SHARED/broker.crt"
PORT_NUM="8883"

if [ -n "$CONF_FILE" ]; then
    CFG_CA=$(grep -E '^ca\.crt=' "$CONF_FILE" | head -n1 | cut -d'=' -f2- | tr -d '"')
    CFG_KEY=$(grep -E '^broker\.key=' "$CONF_FILE" | head -n1 | cut -d'=' -f2- | tr -d '"')
    CFG_CRT=$(grep -E '^broker\.crt=' "$CONF_FILE" | head -n1 | cut -d'=' -f2- | tr -d '"')
    CFG_PORT=$(grep -E '^port=' "$CONF_FILE" | head -n1 | cut -d'=' -f2- | tr -d '"')
    [ -n "$CFG_CA" ] && [ -f "$CFG_CA" ] && CA_SRC="$CFG_CA"
    [ -n "$CFG_KEY" ] && [ -f "$CFG_KEY" ] && KEY_SRC="$CFG_KEY"
    [ -n "$CFG_CRT" ] && [ -f "$CFG_CRT" ] && CRT_SRC="$CFG_CRT"
    [ -n "$CFG_PORT" ] && PORT_NUM="$CFG_PORT"
fi

echo "---> Passing certificates..."
cp "$CA_SRC" /etc/mosquitto/ca_certificates/ca.crt
cp "$KEY_SRC" /etc/mosquitto/certs/broker.key
cp "$CRT_SRC" /etc/mosquitto/certs/broker.crt

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
echo "listener $PORT_NUM $1" >> /etc/mosquitto/mosquitto.conf
echo "allow_anonymous false" >> /etc/mosquitto/mosquitto.conf
echo "cafile /etc/mosquitto/ca_certificates/ca.crt" >> /etc/mosquitto/mosquitto.conf
echo "keyfile /etc/mosquitto/certs/broker.key" >> /etc/mosquitto/mosquitto.conf
echo "certfile /etc/mosquitto/certs/broker.crt" >> /etc/mosquitto/mosquitto.conf
echo "tls_version tlsv1.2" >> /etc/mosquitto/mosquitto.conf

echo "---> DONE"
