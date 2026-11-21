'''
*************** Name: KS Administrative Unit
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 01.06.2026.
*************** Location: Kragujevac, Serbia
'''

import time
import threading
import os
import sys
from pathlib import Path

localDir = os.getcwd()
_AU_DIR = Path(__file__).resolve().parent.parent
_PROJECT_ROOT = _AU_DIR.parent
_CONF_PATH = _PROJECT_ROOT / "AAU" / "script" / "au configuration" / "au-configuration.conf"

lock = threading.Lock()

certificate = ""
serverKey = ""
serverCertificate = ""

address =       ""
port =          0
user =          "" 
password =      ""
kill =          False
topic =         "admin"

if not _CONF_PATH.is_file():
    print(f"Configuration file not found: {_CONF_PATH}", file=sys.stderr)
    sys.exit(1)

with open(_CONF_PATH, "r", encoding="utf-8") as file:
    configuration = file.read().splitlines()

for i in range(len(configuration)):
    if(str(configuration[i]).strip() == ""):
        continue
    configuration[i] = configuration[i].strip()
    if "=" not in configuration[i]:
        continue
    name, value = configuration[i].split("=", 1)
    name = name.strip()
    value = value.strip()
    if len(value) >= 2 and value.startswith('"') and value.endswith('"'):
        value = value[1:-1]
    match name:
        case "ca":
            certificate = value
        case "serverKey":
            serverKey = value
        case "serverCertificate":
            serverCertificate = value
        case "address":
            address = value
        case "port":
            try:
                port = int(value)
            except ValueError:
                port = 0
        case "user":
            user = value
        case "password":
            password = value

if(certificate == "" or serverKey == "" or serverCertificate == "" or address == "" or port <= 0 or user == "" or password == ""):
    print("Configuration file is not valid", file=sys.stderr)
    sys.exit(1)

if(not os.path.isfile(certificate) or not os.path.isfile(serverKey) or not os.path.isfile(serverCertificate)):
    print("AU certificate or key file does not exist", file=sys.stderr)
    sys.exit(1)

TEXT = ""

class communication(threading.Thread):
    def __init__(self, topic, client):
        super().__init__()
        self.__topic = topic
        self.__client = client
    
    def run(self):
        done= False
        while(not done):
            print(f"Trying to connect to topic {self.__topic}")
            time.sleep(0.5)
            try:
                self.__client.connect(address, port)
                done = True
            except Exception as e:
                print(f"--communication loop-- error message:{e}")

        self.__client.subscribe(self.__topic)
        print(f"{self.__topic} ... connected")
        while(not kill):
            self.__client.loop_start()
            if kill:
                self.__client.disconnect()
                break
            time.sleep(1)
            self.__client.loop_stop()
