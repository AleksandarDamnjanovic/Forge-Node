'''
*************** Name: AAU
*************** Part of: Forge Node
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2024.
*************** Location: Kragujevac, Serbia
*************** Description:
                    This is the main part of the project. This file should be executed in order for program to work.
'''

import paho.mqtt.client as mqttClient
from accessories.admin import processAdmin
import time
import ssl
import threading
from threading import Thread
import support
import script
import sys, os
from accessories.ks_logger import logit
import traceback

# 📎 function that does all the work
def rerun():
    
    while support.START == True:
        time.sleep(0.1)

    with support.rerunLock:
        support.START = True
        try:
            support.factory.clearMessages()
            script.refresh()
            support.factory.packMessages()
            support.factory.calculateFactoryEpoh()
            epoh = support.factory.getFactoryEpoh()
            sendMessages()
        finally:
            support.START = False
        logit(f"epoh until next rerun: {epoh}", 0)

        return epoh
        
# 📎 if we get some connection error, we are going to log warning
def on_connect(client, data, flags, returnCode):
    if(returnCode!=0):
        logit(f"Client connection error... return code: {returnCode}", 2)

# 📎 MQTT function that reacts on messages sent by another party to topic that we listen
def on_message(client, data, message):
    try:
        text = message.payload.decode("utf-8")

        if str(text).__contains__("--no response--") or str(text).startswith("--response--"):
            return
        
        if message.topic== "admin":
            with support.rerunLock:
                mess = processAdmin(text)
            if mess != "":
                client = clients.get(0)
                if client != None:
                    client.publish("admin", mess)
        else:
            if text.__contains__("\r"):
                text= str(text).replace("\r", " ")

            if text.__contains__("\n"):
                text= str(text).replace("\n", " ")

            while text.__contains__("  "):
                text= str(text).replace("  ", " ")

            text = str(text).strip()
            if text != "":
                __lines= text.split(" ")
                __lines= list(filter(None, __lines))
                __tokens= text.split("_")
                __tokens= list(filter(None, __tokens))

                if len(__tokens) == 0 or __tokens[0]=="I" or __tokens[0]== "Q":
                    return

                __result = True
                demand = -1
                with support.rerunLock:
                    for l in __lines:
                        res = support.factory.processConfirmation(l)
                        if res == None:
                            continue
                        valid, d = res
                        if valid == False:
                            __result= False
                        if d != -1:
                            demand = d
                if demand != -1:
                    logit(f"update called by sensor on node: {demand}", 0)
                    rerun()

    except Exception as e:
        traceback.print_exc()
        logit(f"--onMessage function-- error message:{e}", 2)

# 📎 this function sens messages from all nodes to coresponding MQTT topics
def sendMessages():
    for n in support.nodes:
        ind = n.getNodeIndex()
        if n.getType() == "MASTER" or n.getType() == "SHUNT":
            message = support.factory.getMessagesByIndex(ind)
            if len(message)>0:
                message = message[0]
            else:
                continue
            client= clients.get(ind)
            if client == None:
                continue
            try:
                client.publish(n.getTopic(), message)
            except Exception as e:
                logit(f"--sendMessage function-- error message:{e}", 2)

# 📎 the main purpose of this theread is to run indefinitelly and to call function rerun periodically.
# 📎 function rerun is what actually does all the work
class running(threading.Thread):
    def __init__(self):
        super().__init__()

    def run(self):
        while True:
            if support.START == False:
                epoh= rerun()
                time.sleep(epoh)
            else:
                time.sleep(0.1)

  # 📎 entry point
if __name__ == "__main__":
    script.buildAll()
    script.refresh()

    clients = dict()

    adminClient = mqttClient.Client()
    adminClient.on_connect= on_connect
    adminClient.on_message= on_message
    context = ssl._create_unverified_context()
    context.load_verify_locations(cafile=support.certificate)
    adminClient.tls_set_context(context)
    adminClient.tls_insecure_set(True)
    adminClient.username_pw_set(support.user, support.password)
    clients[0] = adminClient
    th = support.communication("admin", adminClient)
    th.start()

    for n in support.nodes:
        if n.getType() == "BRIDGE" or n.getType() == "SLAVE":
            continue
        ind = n.getNodeIndex()
        client = mqttClient.Client()
        client.on_connect= on_connect
        client.on_message= on_message
        context = ssl._create_unverified_context()
        context.load_verify_locations(cafile=support.certificate)
        client.tls_set_context(context)
        client.tls_insecure_set(True)
        client.username_pw_set(support.user, support.password)
        clients[ind] = client
        th = support.communication(n.getTopic(), client)
        th.start()

    time.sleep(2)
    run = running()
    run.start()