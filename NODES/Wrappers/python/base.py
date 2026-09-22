'''
*************** Name: Forge Node
*************** Part of: Python Wrapper libraries
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 22.8.2026.
*************** Location: Kragujevac, Serbia
'''

import paho.mqtt.client as mqttClient
import time
import threading
import subscribe
import publisher
import support

def onConnect(client, data, flags, returnCode):
    if(returnCode==0):
        print(f"Connected");
    else:
        print(f"Connection error {str(returnCode)}")

def onMessage(client, data, message):
    messageText = message.payload.decode("utf-8")
    if str(messageText).startswith("Cr"):
        return
    support.node.processInstructions(messageText)
    print(messageText)

class publisherThread(threading.Thread):
    def __init__(self):
        super().__init__()

    def run(self):
        publisher.run()

class subscriberThread(threading.Thread):
    def __init__(self):
        super().__init__()

    def run(self):
        subscribe.run()

if __name__ == "__main__":
    support.client.on_connect = onConnect
    support.client.on_message = onMessage
    support.client.tls_set(support.certificate)
    support.client.tls_insecure_set(True)
    support.client.username_pw_set(support.user, support.password)
    support.client.connect(support.address, support.port)

    publisherThread().start()
    subscriberThread().start()

    time.sleep(5)
    support.control = False