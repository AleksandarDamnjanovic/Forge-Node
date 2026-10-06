import time
import support
import threading
from accessories.ks_logger import logit

lock = threading.Lock()

scriptFile = "/home/irishpub/Programming/AAU/script/script.fn"
baskupScriptFile = "/home/irishpub/Programming/AAU/script/backup-script.fn"
logFile = "/home/irishpub/Programming/AAU/script/log.txt"
certificate = "/home/irishpub/Programming/AAU/certs/ca.crt"
serverKey = "/home/irishpub/Programming/AAU/certs/broker.key"
serverCertificate = "/home/irishpub/Programming/AAU/certs/broker.crt"

address =       "192.168.0.24"
port =          8883
user =          "admin" 
password =      "00004444"
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
        while(True):
            self.__client.loop_start()
            time.sleep(1)
            self.__client.loop_stop()
