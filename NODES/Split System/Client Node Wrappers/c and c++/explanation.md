## How to use wrapper

There are 3 necessary header files

- _baseHeader.h_
- _clientNodeWrapper.h_
- _controlNodeWrapper.h_

_baseHeader.h_ contains all of functions and structures shared by both following headers. _clientNodeWrapper.h _holds all of necessary functions for client side functionalities while _controlNodeWrapper.h_ hold all of necessary functions of control side. In example.c, you are going to find example code of how those two sides can be used. In this case, on the same PC, both client and control wrapper are implemented.
In both cases, firs move is to create an instance of NODE, than to start it by using functions control_node_start or client_node_start, depends on the side of the process.
In order to interact with values, there you can find functions like _writeSwitch_, _writeTransmitter_, _writeSensor_, _readTransmitter_, _readSensor_ and _readSwitch_.
After operation is done, function node_stop and function free on instance of the node, must be done.