## How it works
Mosquitto MQTT server is used for entire communication exchange. Every node has access only to its own topic named after name of the node itself. Every node has to have its unique index that is used as identification in the script and in instructions. AAU has access to all topics + admin topic and it processes both incoming and outgoing messages according to rules set in .fn script.

Entire communication is encrypted. By default, self-signed certificate is used.

There are multiple types of nodes, although at the moment only one type is implemented (MASTER is implemented).

- **MASTER**; this type of node, has direct access to its own topic and its messages are processed directly by AAU.

- **SHUNT**; same as master type, but with additional functionality. In case when smart system has some remote group of nodes shunt serves as communication node towards remote group from the server side. All of messages for the shunt, bridge and other slave nodes in the remote group are grouped in single message on the AAU and sent to the shunt. Shunt removes from this message only those instructions with its own index and all the rest are sent to the bridge.

- **BRIDGE**; is same as shunt just on the side of remote group. It receives message from the shunt, splits all of instructions by the index and reform messages for slaves connected to it. While shunt acts as a master, bridge acts as a slave. Responses from those slaves are again grouped together and as a single message sent back to the shunt that passes that message from entire remote group to the AAU.

- **SLAVE**; slave is just an node in the remote group that has no direct access to the AAU but communicates with it through BRIDGE-SHUNT channel.