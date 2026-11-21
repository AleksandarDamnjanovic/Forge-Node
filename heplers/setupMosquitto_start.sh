#!/bin/bash

# /etc/mosquitto/users.conf this file contains, users and passwords for mosquitto server
# by default you have two users. One is necessary and that is admin. user 1f is here just for an example
# you are free to change this part according to your needs
# user names and passwords are provided in this way name:password

# This script needs 1 argument and that is IP address of the mqtt broker

echo "---> Mosquitto setting up initialized..."

echo "---> Backing up original configuration..."
cp /etc/mosquitto/mosquitto.conf /etc/mosquitto/mosquitto-backup.conf