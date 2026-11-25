## AU
List of available AU commands:
1. **write-variable** sets a new value to existing variable.
   1. index of node where variable is located
   2. name of variable itself
   3. new value.
2. **change-trigger-value** sets new value for a trigger.
    1. index of node where trigger is located
    2. name of trigger itself
    3. new trigger value.
3. **change-list-value** sets new list selected index.
    1. index of node where list is located
    2. name of list itself
    3. new index value to be selected
4. **create-variable** creates new variable.
    1. index of node where variable is located
    2. Boolean argument that says whether variable is global or not; can be True or False
    3. variable type; can be int, string, fun, array, float
    4. value for the new variable
5. **remove-variable**(index, name) removes variable.
    1. index of node where variable is located
    2. name of the variable
6. **add-value-condition-program** creates value condition program; sort of if else statement for .fn script.
    1. index of node where program is located
    2. name of the program in human readable form
    3. number of conditions
    4. number of results
    5. for every condition, additional 5 arguments must be provided
        1. left side variable
        2. left side variable type (one of variable types of .fn script)
        3. right side variable
        4. right side variable type
        5. sign(operation); can be > < ==
    6. for every result, additional 5 arguments must be provided
        1. left side variable
        2. left side variable type (one of variable types of .fn script)
        3. right side variable
        4. right side variable type
        5. sign(operation); can be > < =
    7. function name. this is used only if you want to have some variable that refers entire function. Default value is `None`
7. **remove-value-condition-program** removes val condition program
    1. index of node where the program is located
    2. name of program itself
8. **add-time-limit-program** creates new time limit program.
    1. index of node where the program is located
    2. name of program itself
    3. string variable that holds starting time
    4. string variable that holds ending time
9. **remove-time-limit-program** removes time limit program
    1. index of node where the program is located
    2. name of program itself
10. **add-serial-program** creates new serial program.
    1. index of node where the program is located
    2. name of program itself
    3. array variable that holds list of switches
    4. epoch variable. this variable holds value in seconds of pause in between two activation(switching). After the epoch ends, next switch from the list of switches is going to be activated while all the rest are going to be shut down.
    5. last selected index. this is basically the starting point
    6. function variable in the case when you need some function that is going to refer entire program. Default value is `None`
11. **remove-serial-program** removes serial program.
    1. index of node
    2. name of the program
12. **get-sensor-readings** returns readings from the sensor
    1. index of the node
    2. name of the sensor
13. **read-var** reads variable
    1. node index
    2. variable name
14. **read-node-count** returns number of existing nodes. No arguments needed.
15. **read-programs** reads all of programs from the node.
    1. node index
16. **read-elements** reads all of elements from the node.
    1. node index
17. **read-sensors** _reads all of sensors from the node
    1. node index
18. **read-variables** reads all of variables from the node
    1. node index
19. **read-header** reads header from the node
    1. node index