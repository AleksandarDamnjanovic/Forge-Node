'''
*************** Name: KS Administrative Unit
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 01.06.2026.
*************** Location: Kragujevac, Serbia
*************** Description:
                    This unit comminucates with MQTT server via admin topic, what gives it right to issue commands
                    to AAU. KSAU is can also be used as midle point in between user and KS AAU
'''

import paho.mqtt.client as mqttClient
import time
import ssl
import threading
from threading import Thread, Lock
import sys, os
import traceback
import accessories.support
from accessories.help import help
from accessories.parser import parseCommand
import socket

client = None
CONN = None

# 📎 if we get some connection error, we are going to print out warning
def on_connect(client, data, flags, returnCode):
    if(returnCode!=0):
        print(f"Client connection error... return code: {returnCode}")

# 📎 MQTT function that reacts on messages sent by another party to topic that we listen
def on_message(client, data, message):
    try:
        text = message.payload.decode("utf-8")
        
        if message.topic != "admin":
            return

        if not str(text).startswith("--response--"):
            return

        temp = ""
        for line in text.splitlines():
            if line!="" and line != "--response--" and line != "--end response--":
                line = line.strip()
                if temp == "":
                    temp = line
                else:
                    temp = f"{temp}\n{line}"
        text = f"{temp}\r\n" if temp != "" else "(empty)\r\n"

        if mode == "cli":
            print(text)
        elif mode == "tcp":
            accessories.support.lock.acquire()
            accessories.support.TEXT = text
            accessories.support.lock.release()
    except Exception as e:
        traceback.print_exc()
        print(f"--onMessage function-- error message:{e}")

if __name__=="__main__":

    mode = "cli"
    HOST = "0.0.0.0"
    PORT = -1
    th = None

    try:
    
        if len(sys.argv) < 2:
            raise Exception("Wrong arguments. Provide argument help for explanation")
        elif sys.argv[1]== "help":
            print(accessories.help.arguments)
            sys.exit(0)
        elif sys.argv[1]=="cli":
            pass
        elif sys.argv[1]=="tcp":
            if len(sys.argv) < 3:
                raise Exception("TCP mode requires a port argument (e.g., python3 au.py tcp 10010 [host])")
            PORT = int(sys.argv[2])
            if len(sys.argv) >= 4:
                HOST = sys.argv[3]
            mode = "tcp"
        else:
            raise Exception("Wrong arguments. Provide argument help for explanation")
    
        client = mqttClient.Client()
        client.on_connect= on_connect
        client.on_message= on_message
        context = ssl._create_unverified_context()
        context.load_verify_locations(cafile=accessories.support.certificate)
        client.tls_set_context(context)
        client.tls_insecure_set(True)
        client.username_pw_set(accessories.support.user, accessories.support.password)

        th = accessories.support.communication(accessories.support.topic, client)
        th.start()

        time.sleep(1)
        print(f"KS Administrative Unit, ready to serve...")
        if mode == "cli":
            while(True):
                command = input()
                if command == "exit":
                    print("KS Administrative Unit, terminated")
                    accessories.support.kill= True
                    th.join()
                    sys.exit(0)
                elif command == "help":
                    print(help)
                else:
                    command = parseCommand(command)
                    if not command.startswith("Error"):
                        client.publish(accessories.support.topic, command)
                    else:
                        print(command)
        elif mode == "tcp":
            server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            server.bind((HOST, PORT))
            server.listen()

            while True:
                CONN, addr = server.accept()
                try:
                    with CONN:
                        data = CONN.recv(4096)
                        command = data.decode().strip()
                        if command == "":
                            continue
                        if command == "exit":
                            response = f"Error: 'exit' command is disabled in TCP mode. Stop AU via the Forge Node UI or process manager.\r\n"
                            CONN.sendall(response.encode())
                        elif command == "help":
                            response = f"{help}\r\n"
                            accessories.support.lock.acquire()
                            CONN.sendall(response.encode())
                            accessories.support.lock.release()
                        else:
                            command = parseCommand(command)
                            if not command.startswith("Error"):
                                accessories.support.lock.acquire()
                                accessories.support.TEXT = ""
                                accessories.support.lock.release()
                                client.publish(accessories.support.topic, command)
                                response = ""
                                deadline = time.monotonic() + 10.0
                                while response == "" and time.monotonic() < deadline:
                                    time.sleep(0.1)
                                    accessories.support.lock.acquire()
                                    response = accessories.support.TEXT
                                    accessories.support.lock.release()
                                accessories.support.lock.acquire()
                                accessories.support.TEXT = ""
                                if response == "":
                                    response = "Error: Timed out waiting for AAU response\r\n"
                                CONN.sendall(response.encode())
                                accessories.support.lock.release()
                            else:
                                response = f"{command}\r\n"
                                accessories.support.lock.acquire()
                                CONN.sendall(response.encode())
                                accessories.support.lock.release()
                except SystemExit:
                    raise
                except Exception as conn_err:
                    print(f"--tcp connection-- error message:{conn_err}")
                            
    except Exception as e:
        print(f"KS Administrative Unit configuration error\r\n\t{e}")
        accessories.support.kill= True
        if th is not None and th.is_alive():
            th.join()
        sys.exit(1)