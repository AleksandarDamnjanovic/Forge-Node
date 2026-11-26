'''
*************** Name: Forge Node
*************** Part of: Python Wrapper libraries
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 22.8.2026.
*************** Location: Kragujevac, Serbia
'''

class NODE:
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
        if not instructions or not instructions.strip():
            return
        tokens = instructions.strip().split()

        for t in tokens:
            if not t:
                continue
            e = t.split("_")
            if len(e) < 4:
                print(f"Instruction {t}, not properly formated\n")
                continue

            if e[0] != 'I':
                print(f"Instruction {t}, not properly formated\n")
                continue

            if e[1] != str(this.index):
                continue

            try:
                el_idx = int(e[3])
            except ValueError:
                continue

            if e[2] == 'S':
                continue
            elif e[2] == 'C' and len(e) >= 5:
                if 0 <= el_idx < this.numOfSwitches:
                    val = (str(e[4]).strip() == "1" or str(e[4]).strip().lower() == "true")
                    this.switches[el_idx] = val
            elif e[2] == 'T' and len(e) >= 5:
                if 0 <= el_idx < this.numOfTransmitters:
                    this.transmitters[el_idx] = "_".join(e[4:])

    def getSwitchValue(this, elIndex):
        if 0 <= elIndex < this.numOfSwitches:
            return this.switches[elIndex]
        return False

    def getSensorValue(this, elIndex):
        if 0 <= elIndex < this.numOfSensors:
            return this.sensors[elIndex]
        return -1.0

    def getTransmitterValue(this, elIndex):
        if 0 <= elIndex < this.numOfTransmitters:
            return this.transmitters[elIndex]
        return ""

    def setSensorValue(this, elIndex, value: float):
        if 0 <= elIndex < this.numOfSensors:
            this.sensors[elIndex] = float(value)