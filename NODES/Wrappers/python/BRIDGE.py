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


class BRIDGE:
    class bridgeNODE(NODE):
        def __init__(this, baseNode: NODE, parentBridge):
            super().__init__(
                baseNode.index,
                baseNode.numOfSwitches,
                baseNode.numOfSensors,
                baseNode.numOfTransmitters,
                baseNode.switches,
                baseNode.sensors,
                baseNode.transmitters,
            )
            this.parentBridge = parentBridge

        def generateMessage(this) -> str:
            message = super().generateMessage().replace("\n", "")
            lista = this.parentBridge.getForShunt()
            for n in lista:
                if n.getType() == "C":
                    message += f"Cr_{n.getNodeIndex()}_C_{n.getElementIndex()}_{n.getIntValue()} "
                elif n.getType() == "R":
                    message += f"Cr_{n.getNodeIndex()}_R_{n.getElementIndex()}_{n.getFloatValue():.2f} "
            this.parentBridge.clearForShunt()
            return message + "\n"

        def processInstructions(this, rawMessage: str):
            if not rawMessage or rawMessage.strip().startswith("Cr"):
                return
            all_instructions = NODE_INSTRUCTION.getListOfInstructions(rawMessage)
            if not all_instructions:
                return
            for incoming in all_instructions:
                if incoming.getNodeIndex() != this.index:
                    with this.parentBridge.lock:
                        this.parentBridge.forSlaves = [
                            existing
                            for existing in this.parentBridge.forSlaves
                            if not (
                                existing.getNodeIndex() == incoming.getNodeIndex()
                                and existing.getType() == incoming.getType()
                                and existing.getElementIndex() == incoming.getElementIndex()
                            )
                        ]
                        this.parentBridge.forSlaves.append(incoming)
                else:
                    if incoming.getType() == "C" and 0 <= incoming.getElementIndex() < this.numOfSwitches:
                        this.switches[incoming.getElementIndex()] = (incoming.getIntValue() == 1)
                    elif incoming.getType() == "T" and 0 <= incoming.getElementIndex() < this.numOfTransmitters:
                        this.transmitters[incoming.getElementIndex()] = incoming.getStringValue()

        def processSlaveConfirmations(this, message: str):
            confirmations = NODE_INSTRUCTION.getListOfInstructions(message)
            for n in confirmations:
                this.parentBridge.removeFromShuntConfirmation(n)
            for n in confirmations:
                this.parentBridge.addForShuntConfirmation(n)

    def __init__(this, node: NODE, shuntIP: str, shuntPORT: int, bridgePORT: int, aesSecretKey: str = ""):
        this.lock = threading.Lock()
        this.forSlaves = []
        this.forShunt = []
        this.node = BRIDGE.bridgeNODE(node, this)
        this.shuntIP = shuntIP
        this.shuntPORT = shuntPORT
        this.bridgePORT = bridgePORT
        this.control = False
        this.aesKey = aesSecretKey or ""

    def setControl(this, value: bool):
        with this.lock:
            this.control = bool(value)

    def getControl(this) -> bool:
        with this.lock:
            return this.control

    def setAesKey(this, secretKey: str):
        with this.lock:
            this.aesKey = secretKey or ""

    def getAesKey(this) -> str:
        with this.lock:
            return this.aesKey

    def buildEncryptedPayloadForShunt(this) -> str:
        confirmations = this.node.generateMessage()
        return CRYPTO.encrypt(confirmations, this.getAesKey())

    def processEncryptedPayloadFromShunt(this, encryptedPayload: str):
        decrypted = CRYPTO.decrypt(encryptedPayload, this.getAesKey()).strip()
        if decrypted:
            this.node.processInstructions(decrypted)

    def getForShunt(this):
        with this.lock:
            return list(this.forShunt)

    def clearForShunt(this):
        with this.lock:
            this.forShunt.clear()

    def addForShuntConfirmation(this, confirmation: NODE_INSTRUCTION):
        with this.lock:
            this.forShunt.append(confirmation)

    def removeFromShuntConfirmation(this, confirmation: NODE_INSTRUCTION):
        with this.lock:
            this.forShunt = [
                n
                for n in this.forShunt
                if not (
                    n.getNodeIndex() == confirmation.getNodeIndex()
                    and n.getType() == confirmation.getType()
                    and n.getElementIndex() == confirmation.getElementIndex()
                )
            ]

    def getForSlavesByIndex(this, index: int):
        with this.lock:
            return [n for n in this.forSlaves if n.getNodeIndex() == index]

    def removeForSlavesByIndex(this, index: int):
        with this.lock:
            this.forSlaves = [n for n in this.forSlaves if n.getNodeIndex() != index]

    def getMessageForSlaveByIndex(this, index: int) -> str:
        message = ""
        lista = this.getForSlavesByIndex(index)
        for n in lista:
            message += f"I_{n.getNodeIndex()}_{n.getType()}_{n.getElementIndex()}"
            if n.getType() == "S":
                message += " "
            elif n.getType() == "C":
                message += f"_{n.getIntValue()} "
            elif n.getType() == "T":
                message += f"_{n.getStringValue()} "
        message += "\n"
        this.removeForSlavesByIndex(index)
        return message

    def initializeShuntHandler(this):
        def _run():
            while this.getControl():
                try:
                    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                        s.settimeout(60.0)
                        s.connect((this.shuntIP, this.shuntPORT))
                        encrypted_confirmations = this.buildEncryptedPayloadForShunt()
                        s.sendall(encrypted_confirmations.encode("utf-8"))
                        data = s.recv(4096)
                        if data:
                            encrypted_instructions = data.decode("utf-8", errors="replace").strip()
                            if encrypted_instructions:
                                this.processEncryptedPayloadFromShunt(encrypted_instructions)
                    time.sleep(0.2)
                except Exception as e:
                    print(f"BRIDGE shuntHandler error: {e}")
                    time.sleep(1.0)

        t = threading.Thread(target=_run, daemon=True)
        t.start()

    def initializeSlaveHandler(this):
        def _run():
            server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            server.bind(("0.0.0.0", this.bridgePORT))
            server.listen(5)
            try:
                while this.getControl():
                    try:
                        slave_client, _ = server.accept()
                        with slave_client:
                            slave_client.settimeout(5.0)
                            data = slave_client.recv(4096)
                            instructions = data.decode("utf-8", errors="replace").strip() if data else ""
                            if instructions:
                                this.node.processSlaveConfirmations(instructions)
                                lista = NODE_INSTRUCTION.getListOfInstructions(instructions)
                                if lista:
                                    idx = lista[0].getNodeIndex()
                                    reply = this.getMessageForSlaveByIndex(idx)
                                    slave_client.sendall(reply.encode("utf-8"))
                    except Exception as e:
                        print(f"BRIDGE slaveHandler error: {e}")
            finally:
                server.close()

        t = threading.Thread(target=_run, daemon=True)
        t.start()

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
