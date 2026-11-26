'''
*************** Name: Forge Node
*************** Part of: Python Wrapper libraries
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
'''

import socket
import threading
import time
from NODE import NODE


class SLAVE:
    def __init__(this, node: NODE, bridgeAddress: str, bridgePORT: int):
        this.node = node
        this.bridgeAddress = bridgeAddress
        this.bridgePORT = bridgePORT
        this.control = False
        this.lock = threading.Lock()
        this.handler = None

    def setControl(this, value: bool):
        with this.lock:
            this.control = bool(value)

    def getControl(this) -> bool:
        with this.lock:
            return this.control

    def startHandler(this) -> bool:
        def _run():
            while this.getControl():
                try:
                    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
                        sock.settimeout(5.0)
                        sock.connect((this.bridgeAddress, this.bridgePORT))
                        message = this.node.generateMessage()
                        if not message.endswith("\n"):
                            message += "\n"
                        sock.sendall(message.encode("utf-8"))
                        data = sock.recv(4096)
                        if data:
                            incoming = data.decode("utf-8", errors="replace").strip()
                            if incoming:
                                this.node.processInstructions(incoming)
                    time.sleep(0.2)
                except Exception as e:
                    print(f"SLAVE handler error: {e}")
                    time.sleep(1.0)

        this.handler = threading.Thread(target=_run, daemon=True)
        this.handler.start()
        return True

    def writeSwitch(this, index: int, value: bool):
        with this.lock:
            if 0 <= index < this.node.numOfSwitches:
                this.node.switches[index] = bool(value)

    def writeSensor(this, index: int, value: float):
        with this.lock:
            this.node.setSensorValue(index, value)

    def writeTransmitter(this, index: int, value: str):
        with this.lock:
            if 0 <= index < this.node.numOfTransmitters:
                this.node.transmitters[index] = str(value)

    def getSwitchValue(this, index: int) -> bool:
        with this.lock:
            return this.node.getSwitchValue(index)

    def getSensorValue(this, index: int) -> float:
        with this.lock:
            return this.node.getSensorValue(index)

    def getTransmitterValue(this, index: int) -> str:
        with this.lock:
            return this.node.getTransmitterValue(index)
