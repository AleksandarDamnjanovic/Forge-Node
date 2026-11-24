import os
import shlex
import sys

filename = "./setupMosquitto_users.sh"
l = len(sys.argv)
content = '''#!/bin/bash

echo "---> Generating users..."
rm -f /etc/mosquitto/users.conf
touch /etc/mosquitto/users.conf

'''

if l < 3:
    print("Usage: python createUserList.py <name> <password>")
    sys.exit(1)

for i in range(1, l, 2):
    name = sys.argv[i]
    password = sys.argv[i + 1]
    content += f"printf '%s\\n' {shlex.quote(f'{name}:{password}')} >> /etc/mosquitto/users.conf\n"

content = f"{content}mosquitto_passwd -U /etc/mosquitto/users.conf\n"

f = open(filename, "w")
f.write(content)
f.close()

os.chmod(filename, 0o755)