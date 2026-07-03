help = '''
\tEntire administrative protocol has 19 functions. Argumens like index and name are common.
\tIndex is used for node index and name as name of the particular element.
\tArguments should follow function name like in example section.
\tKeep in mind that in most of casses, you can't pass value but variable name.
\tVariable that you want to affect with some function, must be already present in the script.
\tIf variable is not present, you have to create it before you use it in some function.

\twrite-var index name value
\t\tsets a new value to existing variable. with value arguments, a new value is provided

\tchange-trigger-value index name triggerValue
\t\twith triggerValue argument, a new value for trigger is provided

\tchange-list-value index name listValue
\t\twith listValue argument, new value from the list is selected.
\t\tIn here, you should not provide value for the trigger but index from the list

\tcreate-variable index name globalVar varType varValue
\t\tcreates new variable.
\t\tglobalVar is boolean argument that says whether variable is global or not.
\t\tvarType sets variable type and varValue, starting value for the new variable

\tremove-variable index name
\t\tremoves variable with provided name from the node with provided index

\tadd-val-condition-program index name numOfConditions numOfResults...
\t...5 arguments for every condition, 5 arguments for every result, funVariableName
\t\tthis is the most complicated case because num of results and num of conditions is not fix.
\t\tthere are 5 arguments for every condition and result.
\t\tleft side, left type, right side, right tyle and sign

\tremove-val-condition-program index name
\t\tremoves val condition program by provided name and from node with provided index

\tadd-time-lim-program index name start end
\t\tcreates new time limit program where start and end are the starting and ending time

\tremove-time-lim-program index name
\t\tremoves time limit program by privided name from node with provided index

\tadd-serial-program index name listOfSwitches epoh lastIndex functionName=None
\t\t creates new serial program.
\t\tArgument listOfSwitches must be array that holds all of switches that should be affected by this program.
\t\tepoh is time when change is commited.
\t\tfirst switch that is going to be activated is one after last index.
\t\tfunction name is name of function variable that could be connected to this program and used in later code

\tremove-serial-program index name
\t\tremoves serial program by provided name and from node with provided index

\tget-sensor-readings index name
\t\treturns readings from the sensor by provided name and from node by provided index

\tread-var index name
\t\treads variable by provided name and from node by provided index

\tread-node-count
\t\treturns number of existing nodes

\tread-programs index
\t\treads all of programs from the node-index

\tread-elements index
\t\treads all of elements from the node-index

\tread-sensors index
\t\treads all of sensors from the node-index

\tread-variables index
\t\treads all of variables from the node-index

\tread-header index
\t\treads header from the node-index

Example:
\t add-serial-program 2 mySerialProgram $lst $eph $lstind None
\t read-programs 3
\t get-sensor-reading 2 sensor1
\t read-var 4 $opop
'''

arguments = '''
KS Administrative Unit provides two modes.
    1. CLI mode with stdin/stdout stream. Design to be used by user directly.
        - in order to use the app with this mode, provide argument 'cli'
    2. TCP mode limited only to local machine for the security reasons. Purpose of the second mode is to be used for an AI integration.
        - in order to use the app with this mode, provide the first argument 'tcp', and the second argument is port where the service is going to listen on
'''