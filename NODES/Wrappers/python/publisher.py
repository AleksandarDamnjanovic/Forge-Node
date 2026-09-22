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
import support

def run():
    while(support.control):
        message = support.node.generateMessage()
        support.client.publish(support.topic, message)
        time.sleep(1)