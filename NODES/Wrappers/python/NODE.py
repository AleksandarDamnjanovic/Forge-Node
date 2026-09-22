'''
*************** Name: Forge Node
*************** Part of: Python Wrapper libraries
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 22.8.2026.
*************** Location: Kragujevac, Serbia
'''

class NODE:
    index = -1

    numOfSwitches = 0
    numOfSensors = 0
    numOfTransmitters = 0

    switches = None
    sensors = None
    transmitters = None

    def __init__(this, index, numOfSwitches, numOfSensors, numOfTransmitters, 
            switches: list[bool], sensors: list[float], transmitters: list[str]):
        this.index = index
        this.numOfSensors = numOfSensors
        this.numOfSwitches = numOfSwitches
        this.numOfTransmitters = numOfTransmitters
        this.switches = switches
        this.sensors = sensors
        this.transmitters = transmitters

    def generateMessage(this):
        message = ""
        for i in range(0, this.numOfSwitches):
            val = 0
            if this.switches[i]:
                val = 1
            message = f"{message}Cr_{this.index}_C_{str(i)}_{str(val)} "

        for i in range(0, this.numOfSensors):
            message = f"{message}Cr_{this.index}_R_{str(i)}_{str(this.sensors[i])} "

        return message

    def processInstructions(this, instructions: str):
        tokens = instructions.split(" ")

        for t in tokens:
            e = t.split("_")

            if e[0]!= 'I':
                print(f"Instruction {instructions}, not properly formated\n")
                return

            if e[1]!= str(this.index):
                print(f"Instruction {instructions}, holds wrong node index\n")
                return

            if e[2]=='C':
                val = bool(e[4])
                this.switches[int(e[3])] = val
            elif e[2]=='T':
                this.transmitters[int(e[3])] = str(e[4])

    def getSwitchValue(this, elIndex):
        return this.switches[elIndex]

    def getSensorValue(this, elIndex):
        return this.sensors[elIndex]

    def getTransmitterValue(this, elIndex):
        return this.transmitters[elIndex]

    def setSensorValue(this, elIndex, value: float):
        this.sensors[elIndex]= value