'''
*************** Name: Forge Node
*************** Part of: Python Wrapper libraries
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
'''


class NODE_INSTRUCTION:
    def __init__(this, raw: str = ""):
        this.nodeIndex = -1
        this.elementIndex = -1
        this.type = ""
        this.intValue = 0
        this.floatValue = 0.0
        this.stringValue = ""

        if not raw:
            return
        cleaned = raw.replace("\x00", "").strip()
        if not cleaned:
            return

        tokens = cleaned.split("_")
        if len(tokens) < 4:
            return

        try:
            this.nodeIndex = int(tokens[1].strip())
            this.type = tokens[2].strip()[0]
            this.elementIndex = int(tokens[3].strip())
            if this.type == "C" and len(tokens) >= 5:
                this.intValue = int(tokens[4].strip())
            elif this.type == "R" and len(tokens) >= 5:
                this.floatValue = float(tokens[4].strip())
            elif this.type == "T" and len(tokens) >= 5:
                this.stringValue = "_".join(tokens[4:]).strip()
        except Exception:
            this.nodeIndex = -1

    @staticmethod
    def getListOfInstructions(rawMessage: str):
        lista = []
        if not rawMessage:
            return lista
        cleaned = rawMessage.replace("\x00", "").strip()
        if not cleaned:
            return lista
        for t in cleaned.split():
            if t.startswith("I_") or t.startswith("Cr_"):
                inst = NODE_INSTRUCTION(t)
                if inst.getNodeIndex() > 0:
                    lista.append(inst)
        return lista

    def getNodeIndex(this) -> int:
        return this.nodeIndex

    def getElementIndex(this) -> int:
        return this.elementIndex

    def getType(this) -> str:
        return this.type

    def getIntValue(this) -> int:
        return this.intValue

    def getFloatValue(this) -> float:
        return this.floatValue

    def getStringValue(this) -> str:
        return this.stringValue
