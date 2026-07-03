---
name: ks-smart-systems-skill
description: deals with ks smart systems automation
version: 0.0.1
platforms: [linux, macos, windows]
metadata:
  hermes:
  tags: [autonomous-ai-agents]
  requires_toolsets: [terminal, python]
---

# KS Smart Systems skill

## When is used
Whenever user asks to do something with its home or company automation that is based on KS Smart Systems. Demand could be something like "Turn on the light in the living room", "Turn off heater in the dining room." , "Make lights more red in the penthouse", "Get me temperature from the terrace"

## Exact script structure

### General rules
- Arguments in human readable form or HRF should be used by agent in order to determine property.
- Programmable variable controls behavior of component, but it should not be updated by agent, except in cases when user ask it explicitly.
- Force variable is used whenever user ask of agent to change value of the component, this variable should be affected(updated).
- Index of component on physical device in not useful to the agent
- Epoch variable argument. Agent should affect this variable only if user demands it explicitly. In regards of exact component, this variable could mean different things.
  - in case of sensors, this variable holds value in seconds of pause length in between two readings(those readings does not affect agent in any way)
  - in case of serial program, this variable holds value in seconds of activity of one switch, after this period passes, switch that is presently on is going to turn off, and next one is going to turn on.

### Script components explanation
- header example: 1("First floor", "1f", [MASTER])
  - 1 is node index
  - "First floor" is location explanation in HRF that should be used by agent in order to determine location
  - "1f" is short node name
  - MASTER is type of node
- sensor example: sensor("temp", 0, $e1, $res01)
  - sensor is declaration keyword
  - "temp" is HRF sensor name.
  - 0 is index of the sensor on physical node itself
  - $e1 is epoch variable
  - $res01 is reading variable. if agent needs to read value from the sensor, it is going to read value from this variable
- switch example: switch("light0", 0, $c1, $f1)
  - switch is declaration keyword
  - "light0" is HRF switch name.
  - 0 is switch index on physical device
  - $c1 is programmable variable. 
  - $f1 is force variable.
- trigger example: trigger("heaterValue", 0, $t1, $ft1)
  - trigger is declaration keyword
  - "heaterValue" is HRF name of the trigger
  - 0 is index of the trigger on physical device
  - $t1 is programmable variable
  - $ft1 is force variable
- list example list("lista", 1, $lv, $ln, $l1, $lf1)
  - list is is declaration keyword
  - "lista" is HRF name of the list
  - 1 is index of the component on physical device.
  - $lv is list of indices
  - $ln is list of names(list of names is list of string values in human readable form directly related to list of indices from $lv)
  - $l1 selected index. Programmable variable
  - $lf1 selected index. Force variable

### Script program explanation
- valCondition program(in text also could be referred as value condition program) example: ```valCondition(
            "light on",
            $lightsON == 1,
            $c1 = 1
        );```
  - valCondition is declaration keyword
  - "light on" is HRF name of the program
  -  conditions. Multiple conditions can be provided to a single value condition program, single condition per line. Condition lines are located between `,` after functio HRF name and the next `,` Condition example:  `$lightsON == 1`
    - $lightsON is left side of the statement and it holds name of variable that is going to be compared with variable or value from the right side
    - == is condition operator. == means equals to, > means greater than, < means less than
    - 1 is value on the right side of the expression that is compared with variable from the left side.
  - after second `,` there are results. There can be multiple results, result per line. Example of result `$c1 = 1`. This means that if all of provided conditions are met, value of $c1 variable is going to be set to 1.
- timeLim program or as it can be referred as time limit. Example timeLim($lightsON, $start, $end);
  - timeLim is declaration keyword
  - $lightsON is functional variable that is going to be affected by the program
  - $start is string variable that holds starting time provided in 24 hours format
  - $end is string variable that holds ending time provided in 24 hours format
  - when present time is in between starting and ending time, variable $lightsON is going to be set to 1 by the program, otherwise, value of this variable is going to be set to 0.
- serial program. Example: serial("entire row", $listOfSwitches, $switchEp, $time, $lastIndex);
  - serial is declaration keyword 
  - "entire row" is HRF name
  - $listOfSwitches is variable of type array that holds programmable variables of switches that are going to be affected by the program.
  - $switchEp is epoch variable
  - $time is timestamp variable. Integer variable, always initialized with 0 value.
  - $lastIndex is integer variable that holds the last active switch of this program by the index from $listOfSwitches

## App functions
App can take 16 different commands with or without arguments in order to process user's request. Arguments are provided in the way `command` `firstArgument` `secondArgument`; examples `write-variable 1 $var1 25`, `change-list-value 3 $newVal 2`. Down is provided list of functions(commands) with arguments.

1. **write-variable** sets a new value to existing variable.
   1. index of node where variable is located
   2. name of variable itself
   3. new value.

2. **create-variable** creates new variable.
    1. index of node where variable is located
    2. Name of the variables(must start with $)
    3. Boolean argument that says whether variable is global or not; can be True or False
    4. variable type; can be int, string, fun, array, float
    5. value for the new variable

3. **remove-variable**(index, name) removes variable.
    1. index of node where variable is located
    2. name of the variable

4. **add-value-condition-program** creates value condition program; sort of if else statement for .kst script.
    1. index of node where program is located
    2. name of the program in human readable form
    3. number of conditions
    4. number of results
    5. for every condition, additional 5 arguments must be provided
        1. left side variable
        2. left side variable type (one of variable types of .kst script)
        3. right side variable
        4. right side variable type
        5. sign(operation); can be > < ==
    6. for every result, additional 5 arguments must be provided
        1. left side variable
        2. left side variable type (one of variable types of .kst script)
        3. right side variable
        4. right side variable type
        5. sign(operation); can be > < =
    7. function name. this is used only if you want to have some variable that refers entire function. Default value is `None`

5. **remove-value-condition-program** removes val condition program
    1. index of node where the program is located
    2. name of program itself

6. **add-time-limit-program** creates new time limit program.
    1. index of node where the program is located
    2. functional variable that is going to hold the state
    3. string variable that holds starting time
    4. string variable that holds ending time

7. **remove-time-limit-program** removes time limit program
    1. index of node where the program is located
    2. name of program itself

8. **add-serial-program** creates new serial program.
    1. index of node where the program is located
    2. name of program itself
    3. array variable that holds list of switches
    4. epoch variable. this variable holds value in seconds of pause in between two activation(switching). After the epoch ends, next switch from the list of switches is going to be activated while all the rest are going to be shut down.
    5. Timestamp. Integer variable, always equals to 0.
    6. last selected index. this is basically the starting point
    7. function variable in the case when you need some function that is going to refer entire program. Default value is `None`

9. **remove-serial-program** removes serial program.
    1. index of node
    2. name of the program

10. **read-var** reads variable
    1. node index
    2. variable name

11. **read-node-count** returns number of existing nodes. No arguments needed.

12. **read-programs** reads all of programs from the node.
    1. node index

13. **read-elements** reads all of elements from the node.
    1. node index

14. **read-sensors** _reads all of sensors from the node
    1. node index

15. **read-variables** reads all of variables from the node
    1. node index

16. **read-header** reads header from the node
    1. node index

## Script path and execution
Script PATH = path is located in this skill directory, `scripts` sub-directory

## Warning
In front of every $, you must use \ like \$

## Global variables naming convention
When is necessary to from one node to affect global variable from another node, naming convention needs to be followed.
    - in node x, global variable $y from node with index of z must be named like `$z$y`. Example of naming variable $var from node 4 in node 1 is `$4$var`. Example of naming variable $var from node 3 in node 6 is `$3$var`. So the rule is $ than index of node of origin, than full variable name.

## Variables named by agent rule
When agent has to create variable, this **naming convention** must be followed. Agent will always name its variables as $agent and then first available index like `$agent1`, `$agent2`, `$agent3` and so on, so that name of the variable will always be unique on the node level.

## How to initialize service
1. You are going to use `client.py` from Script PATH with python like `python PATH/client.py`. As first argument, you are always going to provide port number 11111. For second argument and on, you are going to use app functions and its arguments; for example, command would look like `python PATH/client.py 11111 read-variables 1`
2. get number of nodes from entire system by executing command `read-node-count`. keep in mind that nodes are enumerated from 1 on ...
3. for every node index execute command `read-header` with node index as argument in order to get header text. Structure of header text is better explained in skill documentation `header.md`. The most important piece of data for the beginning is the first argument of the header function. That piece of data will be called `Location`
4. memorize all of node names(Locations) and corresponding indexes. those information are necessary for all other processes.
5. second most important piece of data is called `property`. Property is the first argument of functions sensor, switch, trigger, list. You have to memorize all of those properties for the future use. for reading sensors, function `read-sensors` is going to be used, and for switches, triggers and lists, function `read-elements`. Both of those functions are using index of the Location as argument.
### How to pass arguments to client.py???
Arguments to client.py are passed in cli, not programming style, so if 2 arguments must be passed to client. py, do it like `python PATH/client.py argument1 argument2`

## Query analysis core rules
1. First thing that agent should do, regardless of exact demand of user is to determine locations that should be affected(node indices) and properties(exact switch, sensor, trigger, list or even program).
    - example: user's query is `I want to turn on second switch in the main room`. From the query, it is clear that location is `main room`. So, agent should look for node that in its header in the first argument holds some text that refers to the `main room`. Next in order to find out property, agent must check, what object should be affected by user's demand; in this query it is clear that that object is second switch, so we can conclude that our property is switch on the location that has in its first argument some value that refers `second switch`.
    - the previous query is simple, because single user's query can affect multiple locations and multiple properties like in case `Turn off light 1 in bathroom and turn on both lights on the main room`. in here, agent must realize that location `main room`, either has two switches designated as lights or there are two nodes that deal with lights in the main room.
2. Second thing that agent should realize from the query is exact action or actions that user demands.
    - switching or turning on and off, in most of cases deal with switch element.
    - reading values from sensors deals with sensors
    - transmitting or sending usually refers to transmitters
    - selecting values from some range or from group of values, usually refers to lists
    - if user wants some sequential switching, usually deal is with serial program
    - if user wants to have some value or event affected by time, time limit should be used
    - if user wants some value to affect some other value, val conditions should be used
3. What variables to affect? If user wants to create some program that is to affect the system, then programmable variables should be used. In case when user wants to make some direct change in regards of elements, force variable should be used.
4. When all previous steps are done, agent must know all nodes(names and indices) and all of variables that should be affected by the action.

## Specific situations
  - It could happen that user don't specify location. In this case, when necessary, agent should ask explicitly for location, but in another case, agent should look through all nodes in order to determine exact location.

## Program handling core rules

### Program writing location rule
- time limit program is going to be written in node where starting and ending time variables are defined
- serial program is going to be written in node where epoch, time stamp and last index variables are defined
- value condition program is going to be written in node where most of variables used to create this program are defined
In case of tie, use node 1.

### Program names rule
Program names for programs `valCondition` and `serial` are mandatory. If user don't provide those names, agent must ask explicitly. Rule for program name is maximum 20 characters, only alpha-numeric

### Serial program pause rule
If user needs some pause in between two switches, that can't be done directly but by using certain procedure. First of all, if value of pause is divided by value of the epoch, the result must be integer. Variable of type integer with default value of 0 must be created for the purpose of pause(dummy variable). In switching array, whenever pause is necessary, name of dummy variable is going to be placed; name of dummy variable is going to be placed (length of pause)/epoch times.
  - example: if user wants serial program to affect switches light 1 and light 2 where epoch value is 2 seconds, and user wants pause of 2 seconds in between light 1 and light 2, and user needs pause of 6 seconds after light 2 is done, switching array for this program is going to be like(taking into account that programmable variable for light 1 is named `$ss1` and for light 2 is `$ss2`. In this same case, dummy variable is named `$pause`) `$ss1`, `$pause`, `$ss2`, `$pause`, `$pause`,`$pause`; so in this case we are going to get, light 1 that is going to be turned on for 2 seconds, than 2 seconds pause, than 2 seconds light 2 is going go be turned on, and finally we are going to have 3 pauses of 2 seconds each.
  - in order to make pause, epoch variable for the program can't be changed. The only way to approach to solution is through dummy variable as explained in this rule.

### Serial program multiplication of active state rule
If user needs some switch to be active longer that others, same principle like in serial program pause rule. Once again, length of activity of one switch, when divided by epoch, must be integer. So, if user needs light 1(programmable variable name `$ss1`) to be active for 2 seconds and light 2(programmable variable name `$ss2`) to be active for 4 seconds in case when epoch value is 2 seconds, switching array would look like `$ss1`, `$ss2`, `$ss2`.

### Rest of program handling rules
1. After query is properly analyzed and agent determines that the next step is dealing with program, first step in that process is to determine, with what kind of program agent must work.
2. Next step is to create all of necessary variables.
    - For time limits program
        - first functional variable that is going to hold the result(initial value is always 0)
        - starting time(string variable that is going to represent time in 24 hours format like `16:45`)
        - ending time(same formatting style as with starting time)
    - For serial program
       - before continuing to creation of rest of necessary variables, agent must check whether user needs to create a pause in between some switches. If that is true, agent must act according to predefined rules.
       - next is epoch variable, of course integer that is going to hold pause length in seconds
       - time stamp, again integer with value of 0
       - function variable, default value is None.
       - when user demand to create serial program, agent is going to create new serial program except if user explicitly demands to update existing
          - array of switches, or epoch from another serial program can't be reused, but dedicated variable for the new program must be created.
    - For value condition program
        - function name and that only if user ask for it explicitly 

## How to determine property
- if user wants something to switch or to turn to on or off, that usually relates to some switch
- if user wants to read some value or get readings, that usually relates to sensors
- if user wants to switch some state or to select some other options among multiple, that usually relates to lists
- switching of two different states, usually relates to switch, multiple states relates to list
- if user wants to send some value or to set some value to be send, that usually relates to trigger
- if user wants to set or change some value if some other value changes in regards of some rule, usually relates to value condition program
- if user wants to make some sequential switching, that usually refers to serial program
- if user wants to turn something on in regards of time, time limit program should be used

## Exact procedure
In all of cases, always first perform `Query analysis core rules`, and in case of programs `Program handling core rules` must be implemented too.
1.  In case of **switches** user can say something like `Turn on first light in my living room`, `Switch the first light in livin room` or `Turn livin room light 1 off` or any variation of those. Use command `write-variable` in order to set force variable of desired switch to wished value(0 for off and 1 for on).
2. In case of sensors, agent must know that direct updating of values of sensors is off limits. In case of **sensors**, user can ask 3 different things.
    - User can ask you do deal with epoch in one of two ways
        - to update epoch value agent should use function `write-variable` with arguments of node index, epoch variable for the exact sensor and new value for this variable that user provided.
        - to read epoch value agent should use function `read-var` with arguments of node index and epoch variable for the exact sensor.
    - User can ask to read value from the sensor. If user asks something like, `I would like to know value`(temperature, intensity… or simply value) `from some sensor`; example: `Give me temperature on the first floor`.
        1. Agent should look for the fourth argument of the sensor because fourth argument is name of variable that holds reading values from the sensor.
        2. Then agent should read value from the variable by using function `read-var`.
    - user can also have some general question about sensors
        1. `Get me a list of sensors from some node` or similar; in which case agent should execute `read-sensors` function for that particular location-node.
        2. `Give me data about sensor that reads value greater, less, or equals of x` ; in this case agent should execute `read-sensors` on all nodes. Than for every reading variable of particular sensor to execute `read-var` in order to get value. If value fulfills user's condition(greater, less or equals to x, where x is some value provided by the user), data about that sensor should be returned to the user as result, alongside the rest of sensors that fulfill the same condition.
        3. Taking into account second example from `reading sensor` case, user doesn't have to look through sensors by reading value only. It can have conditions based on name of sensor or index on working unit(second argument), or even about epoch. User can ask something like `give me all sensors that deals with temperature`; in this case, agent should execute `read-sensors` function on all nodes and all sensors with name that have something to do with temperature(like `temp`, `temp01`, `temperature`,`temperature`,`temperature-sensor`, `temp-readings` ) should be returned to the user as the result.
3. In case of **variables**
    1. User can ask `I want all of variables on certain location or node` ; in this case agent should execute `read-variables` with argument of node(locations) index in order to get list of all variables.
    2. User can have more specific demand like, getting variables under some condition like `all of variables of certain variable type` in what case, user should execute `read-variables` function with argument of node(location) index for certain node(if user demands variables only from one node) or for all nodes(if user wants variables from entire script. In this case, agent should specify at the end, what variable belongs to what node). User can also specify other conditions like local or global variables, arrays that hold certain elements, numerical value that is greater, less or equal to some specific value or string variable with some specific content.
    3. User can ask you to create a new variable. In this case, agent should call function `create-variable`. For this function arguments, user must provide exact values. Agent must know index of node, where variable is created(first argument), variable name for second argument, `True` or `False` value for third argument(True if variable is global, False if variable is local), variable type for fourth argument and initial value for fifth argument. Variable is always going to be local except in case when that variable must be used in different node than one where is created.
    4. User can ask for existing variable deletion. However, for deletion, some conditions must be fulfilled in order not to break the system.
        1. If variable that user wants to delete is global, agent must check first:
            1. All of array type variables in the entire script by executing `read-variables` function. All of array variables that hold variable name for deletion as array member must be remembered for the future use.
            2. Later in the text, we are going to talk about how agent should process programs, but in here first I will explain, how to deal with programs in regards of deleting global variable. Agent must read all of programs in the entire script by execution function `read-programs` with argument of index node for every node. Every program that holds any reference to variable for deletion(either it has deletion variable as argument or has array variable as arguments that has deletion variable as its element) is going to be remembered for later use.
            3. Global variable for deletion must apply all of conditions for local variable too, and conditions for local variable are going to be provided later in the text.
        2. If variable that user wants to delete is local
            - Agent must check presence of variable for deletion in all of sensors by executing `read-sensors` for local node(node where variable is located), in all of elements by executing `read-elements` for local node and in all of programs by executing `read-programs` for local node. Every occurrence of variable for deletion must be remembered for later use.
        3. When presence of variable for deletion is known, agent must determine whether variable for deletion is essential or system variable or not. Essential of system variables are those variables that are present as argument in sensors, switches, triggers or lists. If variable for deletion is essential, it can not be deleted by agent by any means. In this case, agent must inform user that variable that user wants to delete is essential variable and it can not be deleted by agent. The only way for this variable to be deleted is to be deleted by system administrator directly. All other steps in regards of variable deletion process, can be jump over in this case.
        4. If variable for deletion is present in programs but not essential, it can be deleted, but user must be informed that variable for deletion is used in specific programs and before deleting of the variable, those programs that hold variable as argument or that hold array variable that has variable for deletion as element, must be deleted first. If user persist that variable for deletion must be deleted, agent must delete all of programs that have some reference to variable for deletion(detail explanation of how to delete programs are going to be found later in the text), before proceeding to variable deletion.
        5. Before proceeding to variable deletion, the same variable must be remove from all of arrays where is contained as element. Agent should do that by updating array with new value, where variable for deletions is excluded. For this, function `write-variable` must be used.
        6. Variable deletion is going to be performed by calling function `remove-variable` with arguments of node index where variable for deletion is located and name of the variable itself.
    5.  User can ask to update value of existing variable. In this case `write-variable` function should be used with arguments of node index, existing variable name and new value respectively.
4. In case of **triggers**. When user wants to deal with triggers, it is going to ask for some value to be transmitted or some value to be sent. Trigger is basically, value transmitted.
    1. User can ask for list of triggers. It can ask triggers from one node or from multiple if not all of nodes. Or it can ask for triggers by some specific condition. In all of cases, agent should use function `read-elements` and that from all of elements to take only trigger functions. If user wants triggers from one node, function `read-elements` should be executed only for node in question. If user wants triggers from multiple nodes, `read-elements` function should be executed for all necessary nodes. If user provide some condition, first all of triggers must be read from all of requested locations(nodes) and than from that list, must be removed all of triggers that are not fulfilling users conditions.
    2. User can ask about some specifics in regards of one trigger. Example `get me trigger value for red color component in light in bathroom`
        1. Start always with performing query analysis core rules on given case
        2. Than agent should execute function `read-elements` for that particular node and take into consideration only trigger elements from this response.
        3. Than from all of triggers in this list, take into consideration only the one that has logical connection with user's demand; in this case that is name like `red color`, `red light`, `red` or something in similar fashion.
        4. At the end, use function `read-var` for third argument of the selected trigger and forth argument of the selected trigger. This is important in order to see both value for programmable variable and force variable.
        5. User can also ask about trigger index on the unit itself; in that case, agent should check second argument from the selected trigger.
    3. User can ask to update trigger value. If that is the case, agent should keep in check that if value of a trigger is updated directly, then that should be by using fourth argument of trigger function, or force value. So take variable name from fourth argument and use function `write-var` in order to update its value to value desired by the user.
5. If user wants to handle lists, it is usually going to say something in regards of selection some option or changing some selection, or mode. In case of lists, all of steps like in case of triggers are the same, just apply it for lists instead of trigger. There is only one difference to take into consideration. Function list has 6 arguments instead of 4. Note that first and second argument in trigger and list are the same, fifth and sixth from list are the same as third and fourth from trigger. So only third and fourth argument of list function are new in here. Third argument of list function is array that holds indexes for selection(basically some range of numbers), while fourth argument is also array, but this array holds strings corresponding with numbers from previous array. In some cases, fourth argument can also hold numbers but the point is that arrays from third and fourth argument of list function must have the same number of arguments.
6. Case of programs.
    1. Program `serial` are generally used when user wants to have multiple switches turning on sequentially. User's demand could be something like
        -  `I want light in living room, then light on the balcony and light in bathroom to be turned on sequentially or one ofter the another.`
        -   `I need heater on firts pannel than heater on second pannel and then heater in bedroom to be turned one after the another.`
    The key point is that multiple switches have to be turned on and off, one after the another.
        - another key feature of serial program is that user needs to provide an epoch. Epoch is actually time of, how long one switch is going to be turned on, before it turns off and next one is turned on.
        - agent must know that serial program is basically a infinite loop. When last switch from the array is off, first switch is going to be turned on again.
        - at the end, when all of data is gathered and all of necessary variables are created, agent will call function add-serial-program with arguments
            1. node of index where program is going to be created
            2. name of program that user defined at the start of this process
            3. switching array
            4. epoch variable
            5. timestamp variable
            6. last index variable
            7. None for functional variable(going to be explained later in the text)
    2. If user wants to delete serial program, it's gonna provide request in a fashion of `I want to remove sequential switching of switch in my bathroom and switch in my living room`. When agent determine what serial program should be deleted, it must ask user by using serial program name for confirmation.
    3. User can also ask for some changes or updates to serial program.
            - If user said something like `I want to add one switch to list of switches` it is clear that user wants to update list of switches by adding one more switch. Then if user didn't already, agent must ask for exact switch and how change is going to look. When all of changes that user wants to make are clear, agent should use write-variable function to update switching array to desirable content.
            - if user says that it wants to update epoch to a new value. agent should use write-variable function to change epoch variable to desirable value.
            - user can also ask to change last index variable in order to change default starting point. in that case, agent should also use write-variable function to change value of last index variable to desired value. Desired value in this case is index in the switching list of programmable variable of the switch that user wants to be used as default starting point.
    4. User can ask of agent to make a program to change some value if some condition arise. For that purpose is used valCondition program. This program is something like if else statement in various programming languages. One of most used case in regards of KS Smart Systems is with time limit programs that is going to be explained later in the text when we will have a word about timeLim programs.
        - user can ask to create a program in order to turn on some switch or to change some other value if some other value changes in some way. Example of it can be if user asks that if reading value from sensor goes over some limit that some switch should be turned on and according to that if value of the same sensor goes bellow some limit, that switch that is previously turned on, will be turned off. Example `If temperature in my living room goes below 21 degrees, turn on header number 1 in my living room` in this case, if user don't specify, agent should ask for turning off condition; acceptable option in this case would be 20 degrees.
        - user can also have multiple condition that could produce multiple results. It could ask for something like this `If temperature both in my living room and in my bathroom go below 21 degree, turn on heaters in my entire top floor.` In this case it is easy to realize that user wants to check values from two different sensors that are in most cases located on different nodes, and if, for example, there are 3 different heaters on the top floor, all 3 heaters must be turned on if conditions are fulfilled. So in this case, we would have two conditions and three results. Ways of getting variable for next step are explained in previous example.
        - now agent must determine exact operators that are going to be used in order program to be created.
            - if user says something like `if temperature is 21 degree` or `if temperature equals 21 degree` or something in fashion that means that value should be equal to demanded, than operator is `==`
            - if user says something like `if temperature is bellow 21 degree` or `temperature is not yet 21 degree` or anything in fashion that means that value that we compare is less than value that user gave as reference, operator will be `<`
            - if user demands something opposite that in previous case like `if temperature is bigger or greater than 12 degrees`, operator will be `>`
        - as you have seen, in case of conditions we have 3 different operators in regards of situation. In case of results, operator is always `=`.
        - now, we are going to list all of values, agent must know before proceeding to program creation
            - index of node where program is going to be written to.
            - name of the program in human readable form.
            - number of conditions
            - number of results
            - and now, per every condition 5 arguments sequentially
                - variable that is compared
                - compared variable type
                - variable or value for comparison
                - right side variable or value type(must be the same with compared variable)
                - operator
            - and now, per every result 5 arguments sequentially
                - variable that is compared
                - compared variable type
                - variable or value for comparison
                - right side variable or value type(must be the same with compared variable)
                - operator
            - function variable name(by default value `None` should be provided). If user explicitly don't say that it wants function variable name to be set, agent should provide default value.
        - When all of values are prepared, agent should use function add-value-condition-program with all prepared following arguments.
    5.  User can ask of agent to remove some val condition program. When program that should be delete is determined, function remove-value-condition-program
    6.  User can ask of agent to make some correction in val condition program. The same like in previous case, agent should first find program that should be changed and what variable or value should be changed. Change is going to be achieved by changing value of variable that user wants to affect
    7. User can ask to create time limit or timeLim program. This kind of program has purpose to change value of some fun variable in regards of time.
           - user can ask something like `I want light in the main holl to be turned on in between 2 and 4 am.` In order this program to be created, function add-time-limit-program must be used.
           - But in order for result that user requested to be achieved, agent must create two val condition programs. One that is going to set switch variable to 0 when functional variable from this time limit program turns to 1 and other that is going to be set switch programmable variable to 0 when functional variable from this time limit program gets to 0.
    8. Changes to time limit program can be done in the sense of changing value of starting and ending time. For that function write-variable must be used.
7. At the end, when you get the result from the terminal, inform user about it.
    