## FN Script

### Overview
As is said in previous paragraph, AAU functionalities are based on script that is called .fn script, and understanding of how this script works in necessary for agent to be able to operate with the entire system.
First of all, scrip is divided in nodes. Every node starts with header, than { opening swirly brackets, than variable section that is contained in between opening <variables> and closing </variables> tags, than sensors section that is contained in between opening <sensors> and closing </sensors> tags, than elements section that is contained in between opening <elements> and closing </elements> tag and finally programs section that is contained in between opening <programs> and closing </programs> tag. After closing programs tag, must be closing \} bracket in order to signalize the end of node.
### Header
Header always starts with node index and then ( opening bracket, than in the form of argument wrapped with quotation marks is name of the node in human readable form, comma in between the arguments, than another argument also wrapped in quotation marks is name of the node that is going to be used for machine communication(this name can't contain empty spaces or special signs), comma again and at the end of header must be enclosed with angle brackets, a type of node(possible values are MASTER, SLAVE, SHUNT and BRIDGE)
EXAMPLE: `1("First floor", "1f", [MASTER])` so if user asks of agent to do something on the first floor by using Forge Node, agent should know that order is about this node.
As you can see, the first argument in the header is there to help agent to determine what node should be affected by command.
### Variables
The purpose of variables is to hold values that are going to be used by the system.
- **naming** Variable name always starts with $ dollar sign like `$hey`, `$iamvarname`, `$onemoreexample`. When global variable is called from another node, node index of called variable must be used; for example, if you want to refer some global variable(local name of that variable is let's say `$hey`) from another node(let's say that the variable that you want to refer is in node with index of 5) you are going to call it `$5$hey` which means variable `$hey` from node with index of 5. By the same logic, variable `$7$myint` would mean that this is variable with name of `$myint` from node with index of 7. Variable names can't hold empty spaces.
- **definition** there is a rule that says, one variable per one line like
```
    global int    $hey      = 10
           string $haha     = "haha"
           array  $nums     = [1, 2, 3]
```
- **global modifier** Variable can be local or global(if variable declaration has modifier "global", that is global variable. If variable doesn't have "global" modifier, that is local variable). Local variable can be used only in the same node, while global variable can be used from another node too. Example of global variable: `global int $op = 25`, Example of local variable: `int $op = 10`
- **types** .fn script supports multiple types of variables.
    1. int(integer, whole number) example `int $me = 50`
    2. float(number with decimal point) example `float $hey = 23.20`
    3. string(textual values), strings values must be wrapped with quotation marks like `string $bob = "bob the builder"`
    4. array can hold both, integers(`array $intarray = [0, 1, 2]`), strings(`array $stringarray = ["hi", "hey", "ho"]`) and other variable names(`array $varnames = [$c1, $c2, $d8]`). also, array can hold a range of integer values like `array $rangeexample = [0-255]`.
    5. fun, this is a special case variable. Variables of this type can't be global variables and it can store only integer type of values. This kind of variables are used solely to hold result from some function and are provided as function name arguments.
Example of entire Variables section
```
<variables>
    global int    $hey      = 10
           string $haha     = "haha"
           array  $nums     = [1, 2, 3]
           fun    $myfun    = 0
</variables>
```
### Sensors
Sensors are also defined by principle, sensor per line. Values from sensors are used only for readings. `sensor("temp", 0, $e1, $red)` In this example, you can see that sensor is defined by the function of the same name. First argument is the name of the sensor; agent is going to use this name as reference in its work; for example if client says **Give me the temperature in the dining room!**, agent should look for node that has `dining room` or some reference of it in its name and in it, the sensor with name of temperature of `temp`, `temperature` or some other reference. If you want to have multiple words in this name, you can use _ or - or capital and small letters to do so; example would be like `my_room`, `living-room`, `bathroomLight`. Next argument is index of this sensor on micro-controller or some other controlled unit(not in any interest of agent or user; system administrator should worry about this part). Next argument is epoch or name of integer variable that holds value in seconds of how long in between two readings. And finally, name of integer variable where readings are stored. Basically when agent or user want to read value from sensor, this variable is read.

### Elements
Apart from sensors, elements are those members of the node that can be both read from and write to, and in here we have 3 different types of it.

- **switch("light", 0, $c1, $fc1)** switch function is logical representation of real life switch. It can have only two different states; on and off or in our case 1(ON) and 0(OFF). The first argument of this function is name in human readable form. Second argument is index of the switch on controlled unit. Third argument is variable that holds programmable value and fourth argument is variable that holds force value. And what is the difference in between the last two. Third argument is going to be changed by programs defined in the script, but force variable will be used by user or agent in order to overwrite instructions from the program. Basically force variable is dominant state.
- **trigger("ACswitch", 1, $t1, $ft1)** trigger is value that is constantly transmitter to the controlled unit; you can call it transmitter if you like. First argument is name of the trigger in human readable form. Second is index of the transmitter on the physical unit or micro controller. Third and fourth arguments are, programmable value and forced value.
- **list("ACmodes", 0, $acModes, $acModeNames, $ACselectedMode, $fl1)** list is basically same as trigger but with one difference. While value from trigger is taken directly on controlled unit, value of list is taken as index. For example, with trigger you send value of 150 that should be used to set red element of your light; with list you have like 3 different modes of work like in the case of AC [off, heating, cooling]; when you send value 0 with list transmitter, micro controller option off is going to be selected because that option corresponds with index 0 from the provided list. First argument is the name of the list in human readable form. Second argument is index of this transmitter on the controlled unit. Third is the list of select-able elements in indexes or integers like [0, 1, 2]; forth is the same list but now in strings like [off, heating, cooling]. Fifth and sixth arguments are selected index in programmable mode and in force mode.

### Programs
Programs are where true power of .fn lies. At the moment, there are three types of programs. `valCondition`, `serial` and `timeLim`. `valCondition` and `serial` can be named, while `timeLim` can't. Type of variable that is used to name a program is fun.

- **valCondition(textName, conditions, result)**;
```
$example = valCondition(
    "temp on",
    $res01 < $tempLimDown
    $c1 == 0, 
    $c1 = 1
    $tempState = 1
);
```
In this case $example is name of this functions. Type of variable is fun and type of function is `valCondition`. If you don't need named function, simply don't name it; just write `valCondition(` without `$example` = part.
`valCondition` function has three arguments, or better to say three groups of arguments. First argument is function name in human readable form. Second argument is one or more conditions separated by new line; all of conditions must be true in order result to be executed. Result can also have one or more statements separated by new line.

- **serial(textName, listOfSwitches, epoh, timeStamp, lastIndex);**
        Program serial have purpose to turn on one switch from provided list of switches, when time comes. Program starts from switch with index provided in fifth argument, and keeps it on till the epoch provided in third argument in seconds ends. Then `timeStamp` provided in forth argument changes to current time and the new epoch starts from the beginning turning on next switch from the list... so on till the end of the list. When list ends, it starts from the beginning in the same fashion. Timestamp default value is 0, so always when you define `timeStamp` variable, you are going to set it with value of 0;
```
$fun01= serial("two switches", $listOfSwitches, $switchEp, $sTime, $lastIndex);
```
Function naming applies here too. Function of type serial is written in a single line with ; at the end.

- **timeLim(function, start, end);**
```
timeLim($fun01, $start, $end);
```
With function `timeLim`, you are able to define execution time of some other function. For example, if you want your serial program or conditional program to be executed only between 12:00 and 14:00, as a first argument to the `timeLim` function, you are going to provide function name of the function you want to limit by time, as second argument you are going to provide string variable that hold value of "12:00" and as third argument, string variable that holds value of "14:00".
Therefore default value of variable of type fun that hold function name of the function you want to limit by time is 1. That means that function is going to be executed by default except if it is provided to `timeLim` function. `timeLim` function keeps value of fun variable to 1 only in selected time; in other case, it keeps it to 0. If value of naming variable is 0 that means that that function is going to be ignored; if is 1 it is going to be executed.

Exampe of entire node:
```
1("First floor", "1f", [MASTER]){
    
    <variables>
        global      int         $e1 =               10
        global      float       $res01 =            0.0
        global      float       $res02 =            0.0
        global      int         $c1 =               0
        global      int         $c2 =               0
                    string      $f1 =               ""
                    string      $f2 =               ""
                    string      $t1 =               "3"
                    string      $ft1 =              ""
                    string      $start =            "19:20"
                    string      $end =              "6:00"
                    fun         $lightsON =         0
                    int         $opop =             0
                    array       $ha =               [$c1, $c2]
                    int         $haha =             4
                    int         $bo =               0
                    array       $listOfSwitches =   [$c1, $c2]
                    int         $switchEp =         3
                    int         $time =             0
                    int         $lastIndex =        0
                    array       $lv =               [0, 1, 2]
                    array       $ln =               ["off", "cooling", "heating"]
                    int         $l1 =               1
                    string      $lf1 =              ""

    </variables>

    <sensors>
        sensor("temp", 0, $e1, $res01)
        sensor("moist", 1, NULL, $res02)
    </sensors>

    <elements>
        switch("light0", 0, $c1, $f1)
        switch("light1", 1, $c2, $f2)
        trigger("TempValue", 0, $t1, $ft1)
        list("lista", 1, $lv, $ln, $l1, $lf1)
    </elements>

    <programs>
        valCondition(
            "light on",
            $lightsON == 1,
            $c1 = 1
        );
        valCondition(
            "light off",
            $lightsON == 0,
            $c1 = 0
        );
        timeLim($lightsON, $start, $end);
        serial("entire row", $listOfSwitches, $switchEp, $time, $lastIndex);
    </programs>

}
```