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
from CRYPTO import CRYPTO
from NODE import NODE
from NODE_INSTRUCTION import NODE_INSTRUCTION


class SHUNT:
    class shuntNode(NODE):
        def __init__(this, baseNode: NODE, bridgePort: int, aesSecretKey: str = ""):
            super().__init__(
                baseNode.index,
                baseNode.numOfSwitches,
                baseNode.numOfSensors,
                baseNode.numOfTransmitters,
                baseNode.switches,
                baseNode.sensors,
                baseNode.transmitters,
            )
            this.instructions = []
            this.towardsBridge = []
            this.confirmations = []
            this.bridgingControl = False
            this.lock = threading.Lock()
            this.bridgePort = bridgePort
            this.aesKey = aesSecretKey or ""

        def setAesKey(this, secretKey: str):
            with this.lock:
                this.aesKey = secretKey or ""

        def getAesKey(this) -> str:
            with this.lock:
                return this.aesKey

        def generateMessage(this) -> str:
            message = super().generateMessage().replace("\n", "")
            with this.lock:
                for n in this.confirmations:
                    if n.getType() == "C":
                        message += f"Cr_{n.getNodeIndex()}_C_{n.getElementIndex()}_{n.getIntValue()} "
                    elif n.getType() == "R":
                        message += f"Cr_{n.getNodeIndex()}_R_{n.getElementIndex()}_{n.getFloatValue():.2f} "
                this.confirmations = []
            return message + "\n"

        def processInstructions(this, rawMessage: str):
            if not rawMessage or rawMessage.strip().startswith("Cr"):
                return
            this.instructions = NODE_INSTRUCTION.getListOfInstructions(rawMessage)
            with this.lock:
                this.towardsBridge.clear()
            for inst in this.instructions:
                if inst.getNodeIndex() != this.index:
                    with this.lock:
                        this.towardsBridge.append(inst)
                else:
                    if inst.getType() == "C" and 0 <= inst.getElementIndex() < this.numOfSwitches:
                        this.switches[inst.getElementIndex()] = (inst.getIntValue() == 1)
                    elif inst.getType() == "T" and 0 <= inst.getElementIndex() < this.numOfTransmitters:
                        this.transmitters[inst.getElementIndex()] = inst.getStringValue()

        def generateMessageForTheBridge(this) -> str:
            message = ""
            with this.lock:
                for n in this.towardsBridge:
                    message += f"I_{n.getNodeIndex()}_{n.getType()}_{n.getElementIndex()}"
                    if n.getType() == "S":
                        message += " "
                    elif n.getType() == "C":
                        message += f"_{n.getIntValue()} "
                    elif n.getType() == "T":
                        message += f"_{n.getStringValue()} "
                this.towardsBridge.clear()
            return message + "\n"

        def appendBridgeConfirmations(this, rawMessage: str):
            lista = NODE_INSTRUCTION.getListOfInstructions(rawMessage)
            with this.lock:
                for incoming in lista:
                    this.confirmations = [
                        existing
                        for existing in this.confirmations
                        if not (
                            existing.getNodeIndex() == incoming.getNodeIndex()
                            and existing.getType() == incoming.getType()
                            and existing.getElementIndex() == incoming.getElementIndex()
                        )
                    ]
                    this.confirmations.append(incoming)

        def exchangeBridgePayload(this, encryptedIncomingFromBridge: str) -> str:
            key = this.getAesKey()
            decrypted = CRYPTO.decrypt(encryptedIncomingFromBridge, key).strip()
            if decrypted.startswith("Cr"):
                this.appendBridgeConfirmations(decrypted)
            plain_for_bridge = this.generateMessageForTheBridge()
            return CRYPTO.encrypt(plain_for_bridge, key)

        def getBridgingControl(this) -> bool:
            with this.lock:
                return this.bridgingControl

        def setBridgingControl(this, value: bool):
            with this.lock:
                this.bridgingControl = bool(value)

        def startBridging(this):
            def _run():
                server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                server.bind(("0.0.0.0", this.bridgePort))
                server.listen(5)
                try:
                    while this.getBridgingControl():
                        try:
                            client, _ = server.accept()
                            with client:
                                client.settimeout(60.0)
                                data = client.recv(4096)
                                incoming = data.decode("utf-8", errors="replace").strip() if data else ""
                                encrypted_reply = this.exchangeBridgePayload(incoming)
                                if encrypted_reply:
                                    client.sendall(encrypted_reply.encode("utf-8"))
                                time.sleep(0.2)
                        except Exception as e:
                            print(f"SHUNT bridge handler error: {e}")
                finally:
                    server.close()

            t = threading.Thread(target=_run, daemon=True)
            t.start()

    def __init__(this, node: NODE, master: int, bridge: int, bridgePort: int, aesSecretKey: str = ""):
        this.node = SHUNT.shuntNode(node, bridgePort, aesSecretKey)
        this.master = master
        this.bridge = bridge

    def getNode(this):
        return this.node

    def getMasterIndex(this) -> int:
        return this.master

    def getBridgeIndex(this) -> int:
        return this.bridge
