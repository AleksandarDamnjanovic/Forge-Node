'''
*************** Name: KS Administrative Unit
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 01.06.2026.
*************** Location: Kragujevac, Serbia
'''

import re

def parseCommand(text):
    try:
        message= ""
        rawTokens = re.findall(r'"[^"]*"|\'[^\']*\'|\[[^\]]*\]|\S+', str(text))
        temp = list()
        for a in rawTokens:
            if a != " " and a != "":
                if str(a).__contains__("\\$"):
                    a= str(a).replace("\\$", "$")
                if len(a) >= 2 and ((a.startswith('"') and a.endswith('"')) or (a.startswith("'") and a.endswith("'"))):
                    a = a[1:-1]
                temp.append(a)

        s = temp
        
        # 📖 only node index needed as argumen
        if(s[0]=="read-header"):
            message = "readHeader("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    val = int(s[i])
                    message = f"{message}{val})"

        # 📖 only node index needed as argumen
        elif(s[0]=="read-variables"):
            message = "readVariables("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    val = int(s[i])
                    message = f"{message}{val})"
        
        # 📖 only node index needed as argumen
        elif(s[0]=="read-sensors"):
            message = "readSensors("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    val = int(s[i])
                    message = f"{message}{val})"
        
        # 📖 only node index needed as argumen
        elif(s[0]=="read-elements"):
            message = "readElements("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    val = int(s[i])
                    message = f"{message}{val})"
        
        # 📖 only node index needed as argumen
        elif(s[0]=="read-programs"):
            message = "readPrograms("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    val = int(s[i])
                    message = f"{message}{val})"
        
        # 📖 no arguments needed; function returns number of nodes in the script/system
        elif(s[0]=="read-node-count"):
            message = "readNodeCount()"
        
        # 📖 two arguments needed; node index and variable name
        elif(s[0]=="read-var"):
            counter = 1
            index = 0
            name = ""
            message = "readVar("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    counter = counter + 1
            message = f"{message}{index},{name})"
        
        # 📖 two arguments needed; node index and sensor name
        elif(s[0]=="get-sensor-readings"):
            counter = 1
            index = 0
            name = ""
            message = "getSensorReadings("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    counter = counter + 1
            message = f"{message}{index},{name})"
        
        # 📖 six or seven arguments needed; node index, program name, variable that holds list of switches, integer variable
        # 📖 that holds epoch time, optional timestamp variable, integer varaible that holds the last index and function name that is None by default
        elif(s[0]=="add-serial-program"):
            args = [x for x in s[1:] if x != ""]
            index = int(args[0])
            name = args[1]
            listOfSwitches = args[2]
            epoh = args[3]
            if len(args) >= 7:
                switchTime = args[4]
                lastIndex = args[5]
                functionName = args[6]
                message = f"addSerialProgram({index}, {name}, {listOfSwitches}, {epoh}, {switchTime}, {lastIndex}, {functionName})"
            else:
                lastIndex = args[4]
                functionName = args[5] if len(args) >= 6 else "None"
                message = f"addSerialProgram({index}, {name}, {listOfSwitches}, {epoh}, {lastIndex}, {functionName})"
        
        # 📖 two arguments needed; node index and serial program name
        elif(s[0]=="remove-serial-program"):
            counter = 1
            index = 0
            name = ""
            message = "removeSerialProgram("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    counter = counter + 1
            message = f"{message}{index}, {name})"
        
        # 📖 four arguments necessary; first is node index, than name of the program, start time variable and end time variable
        elif(s[0]=="add-time-limit-program" or s[0]=="add-time-lim-program"):
            counter = 1
            index = 0
            name = ""
            start = ""
            end = ""
            message = "addTimeLimProgram("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    elif counter == 3:
                        start = s[i]
                    elif counter == 4:
                        end = s[i]
                    counter = counter + 1
            message = f"{message}{index}, {name}, {start}, {end})"

        # 📖 two arguments necessary; first is node index and second is name of time limit program
        elif(s[0]=="remove-time-limit-program" or s[0]=="remove-time-lim-program"):
            counter = 1
            index = 0
            name = ""
            message = "removeTimeLimProgram("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    counter = counter + 1
            message = f"{message}{index}, {name})"

        # 📖 the most complicated case because of variable number of arguments. first argument is node index, secon
        # 📖 argument is name of program, third is number of conditions, forth is number of results and then
        # 📖 for every condition + 5 arguments where first is name of variable, second is type of variable, third is
        # 📖 name of variable on right side, forth is type of variable on right side and fifth is sign or operation
        # 📖 same rooles for results and at the end function name
        # 📖 EXAMPLE in case of val conditon program with 2 conditions and 2 results, function call would look like this
        # 📖 add-value-condition-program 1 myProg 2 2 $op int $op1 int == $ha int $ha1 int > $res int $rr1 int = $rk int 
        # 📖 $ro int = None  
        elif(s[0]=="add-value-condition-program"):
            counter = 1
            index = 0
            name = ""
            numOfConditions = 0
            numOfResults = 0
            funName = "None"
            message = "addValConditionProgram("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    elif counter == 3:
                        numOfConditions = int(s[i])
                    elif counter == 4:
                        numOfResults = int(s[i])
                    counter = counter + 1
                    if counter == 5:
                        break

            conditions = [""] * (numOfConditions * 5)
            last = counter
            for i in range(0, numOfConditions * 5):
                if s[last + i] != "":
                    conditions[i] = s[last + i]
                    counter = counter + 1

            con_text = ""
            for ss in conditions:
                con_text = f"{con_text}, {ss}"

            results = [""] * (numOfResults * 5)
            last = counter
            for i in range(0, numOfResults * 5):
                if s[last + i] != "":
                    results[i] = s[last + i]
                    counter = counter + 1

            res_text = ""
            for ss in results:
                res_text = f"{res_text}, {ss}"

            for i in range(counter, s.__len__()):
                if s[i] != "":
                    funName = s[i]

            message = f"{message}{index}, {name},{numOfConditions},{numOfResults}{con_text}{res_text},{funName})"

        # 📖 two necessary arguments; node index and program name
        elif(s[0]=="remove-value-condition-program" or s[0]=="remove-valuel-condition-program"):
            counter = 1
            index = 0
            name = ""
            message = "removeValConditionProgram("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    counter = counter + 1
            message = f"{message}{index}, {name})"

        # 📖 5 necessary arguments; node index, variable name, global declaration provided as boolean in string form,
        # 📖 variable type and variable value
        elif(s[0]=="create-variable"):
            counter = 1
            index = 0
            name = ""
            globalVar = "False"
            varType = ""
            varValue = ""
            message = "createVariable("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    elif counter == 3:
                        globalVar = s[i]
                    elif counter == 4:
                        varType = s[i]
                    elif counter == 5:
                        varValue = s[i]
                    counter = counter + 1
            message = f"{message}{index}, {name}, {globalVar}, {varType}, {varValue})"

        # 📖 two necessary arguments; node index and variable name
        elif(s[0]=="remove-variable"):
            counter = 1
            index = 0
            name = ""
            message = "removeVariable("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    counter = counter + 1
            message = f"{message}{index}, {name})"

        # 📖 three necessary arguments; node index, list name and list selected index 
        elif(s[0]=="change-list-value"):
            counter = 1
            index = 0
            name = ""
            listValue = ""
            message = "changeListValue("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    elif counter == 3:
                        listValue = s[i]
                    counter = counter + 1
            message = f"{message}{index}, {name}, {listValue})"

        # 📖 three necessary arguments; node index, trigger name and trigger value 
        elif(s[0]=="change-trigger-value"):
            counter = 1
            index = 0
            name = ""
            triggerValue = ""
            message = "changeTriggerValue("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    elif counter == 3:
                        triggerValue = s[i]
                    counter = counter + 1
            message = f"{message}{index}, {name}, {triggerValue})"

        # 📖 three necessary arguments; node index, variable name and variable value 
        elif(s[0]=="write-variable"):
            counter = 1
            index = 0
            name = ""
            varValue = ""
            message = "writeVar("
            for i in range(1, s.__len__()):
                if s[i] != "":
                    if counter == 1:
                        index = int(s[i])
                    elif counter == 2:
                        name = s[i]
                    elif counter == 3:
                        varValue = s[i]
                    counter = counter + 1
            message = f"{message}{index}, {name}, {varValue})"

        else:
            raise Exception()

    except Exception as e:
        message = f"Error: Command not properly formated!\r\n\t{e}"

    return message