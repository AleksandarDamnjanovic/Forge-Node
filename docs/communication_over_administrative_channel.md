## Communication over the administrative channel

Now, we are in completely different dimension. Now, we are talking about sending commands to AAU over the administrative channel, or how to communicate with AAU in commanding way 👮🏻

At the moment, there are 19 functions that you can sent to admin channel on your mqtt server in order to make AAU to obey.

In all of cases argument index is index of the node that is affected. name is actual element that is affected like switch or variable.

1. **writeVar**(index, name, value) sets a new value to existing variable. *with value arguments, a new value is provided*
2. **changeTriggerValue**(index, name, triggerValue) *with triggerValue argument, a new value for trigger is provided*
3. **changeListValue**(index, name, listValue) *with listValue argument, new value from the list is selected. In here, you should not provide value for the trigger but index from the list
4. **createVariable**(index, name, globalVar, varType, varValue) *creates new variable. globalVar is boolean argument that says whether variable is global or not. varType sets variable type and varValue, starting value for the new variable*
5. **removeVariable**(index, name) *removes variable with provided name from the node with provided index*
6. **addValConditionProgram**(index, name, numOfConditions, numOfResults, 5 arguments for every condition, 5 arguments for every result, funVariableName) *this is the most complicated case because num of results and num of conditions is not fix. there are 5 arguments for every condition and result. left side, left type, right side, right tyle and sign*
7. **removeValConditionProgram**(index, name) *removes val condition program by provided name and from node with provided index*
8. **addTimeLimProgram**(index, name, start, end) *creates new time limit program where start and end are the starting and ending time*
9. **removeTimeLimProgram**(index, name) *removes time limit program by privided name from node with provided index*
10. **addSerialProgram**(index, name, listOfSwitches, epoh, lastIndex, functionName=None) *creates new serial program. Argument listOfSwitches must be array that holds all of switches that should be affected by this program. epoh is time when change is commited. first switch that is going to be activated is one after last index. function name is name of function variable that could be connected to this program and used in later code*
11. **removeSerialProgram**(index, name) *removes serial program by provided name and from node with provided index*
12. **getSensorReadings**(index, name) *returns readings from the sensor by provided name and from node by provided index*
13. **readVar**(index, name) *reads variable by provided name and from node by provided index*
14. **readNodeCount**() *returns number of existing nodes*
15. r**eadPrograms**(index) *reads all of programs from the node-index*
16. **readElements**(index) *reads all of elements from the node-index*
17. **readSensors**(index) *reads all of sensors from the node-index*
18. **readVariables**(index) *reads all of variables from the node-index*
19. **readHeader**(index) *reads header from the node-index*