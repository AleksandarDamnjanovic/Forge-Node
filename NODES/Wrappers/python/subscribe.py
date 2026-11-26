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
    support.client.subscribe(support.topic)
    support.client.loop_start()
    try:
        while support.control:
            time.sleep(0.2)
    finally:
        support.client.loop_stop()