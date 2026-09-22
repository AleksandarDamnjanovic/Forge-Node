'''
*************** Name: Forge Node
*************** Part of: Python Wrapper libraries
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 22.8.2026.
*************** Location: Kragujevac, Serbia
'''

import paho.mqtt.client as mqttClient
from NODE import NODE

address= "192.168.0.24"
port = 8883
user = "1f"
password = "00004444"
topic = user
certificate = "./ca.crt"
control= True
client = mqttClient.Client()

switches = [False, False]
sensors = [float(0.1), float(24.5)]
transmitters = ["", ""]
node = NODE(1, 2, 2, 2, switches, sensors, transmitters)