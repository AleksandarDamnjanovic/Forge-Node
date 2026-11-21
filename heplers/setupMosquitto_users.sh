
#!/bin/bash

echo "---> Generating users..."
touch /etc/mosquitto/users.conf

echo "admin:00007070" >> /etc/mosquitto/users.conf
echo "northEntrance:00007070" >> /etc/mosquitto/users.conf
echo "1ffirstPanel:00007070" >> /etc/mosquitto/users.conf
echo "1fsecondPanel:00007070" >> /etc/mosquitto/users.conf
echo "1fsleepingRoom:00007070" >> /etc/mosquitto/users.conf
echo "terraceLight:00007070" >> /etc/mosquitto/users.conf
echo "2ffirstRoom:00007070" >> /etc/mosquitto/users.conf
echo "2fmainHall:00007070" >> /etc/mosquitto/users.conf
echo "2f1ledControl:00007070" >> /etc/mosquitto/users.conf
echo "2f2ledControl:00007070" >> /etc/mosquitto/users.conf
echo "2f3ledControl:00007070" >> /etc/mosquitto/users.conf
mosquitto_passwd -U /etc/mosquitto/users.conf