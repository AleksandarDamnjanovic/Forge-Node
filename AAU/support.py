import sys
import time
from unittest import case
import support
import threading
from accessories.ks_logger import logit

scriptFile = ""
backupScriptFile = ""
logFile = ""
certificate = ""
serverKey = ""
serverCertificate = ""
address = ""
port = 0
user = ""
password = ""

lock = threading.Lock()
rerunLock = threading.Lock()

file = open("./configuration.conf", "r")
configuration = file.read()
configuration = configuration.split("\n")
file.close()

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
        case "scriptFile":
            scriptFile = value
        case "backupScriptFile":
            backupScriptFile = value
        case "logFile":
            logFile = value
        case "certificate":
            certificate = value
        case "serverKey":
            serverKey = value
        case "serverCertificate":
            serverCertificate = value
        case "address":
            address = value
        case "port":
            port = int(value)
        case "user":
            user = value
        case "password":
            password = value

if(scriptFile == "" or logFile == "" or certificate == "" or serverKey == "" or serverCertificate == "" or address == "" or port == 0 or user == "" or password == ""):
    logit("Configuration file is not valid", 2)
    sys.exit(1)

START =         False
factory = None
nodes = None

class communication(threading.Thread):
    def __init__(self, topic, client):
        super().__init__()
        self.__topic = topic
        self.__client = client
    
    def run(self):
        done= False
        while(not done):
            logit(f"Trying to connect to topic {self.__topic}", 0)
            time.sleep(0.5)
            try:
                self.__client.connect(support.address, support.port)
                done = True
            except Exception as e:
                logit(f"--communication loop-- error message:{e}", 2)

        self.__client.subscribe(self.__topic)
        logit(f"{self.__topic} ... listening")
        self.__client.loop_forever()
