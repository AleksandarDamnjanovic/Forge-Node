## How to use wrapper

The real magic is happening in nodeWrapper.h In example.c you can find example code of how to use this feature.
```
NODE_CLIENT* client= getNode(1, 10001, 0, "192.168.0.101", 2, 2, 2);

    client_node_start(client);
    client_node_writeSensors(0, 12.4, client);
    client_node_writeSensors(1, 21.05, client);
   
    sleep(2);
    char* t1= client_node_read_transmitter_value(0, client);
    char* t2 = client_node_read_transmitter_value(1, client);
    printf("---\nsensor 0: %.2f\nsensor: 1: %.2f\nswitch 0: %d\nswitch 1: %d\ntransmitter 0: %s\ntransmitter 1: %s\n----",
    client_node_readSensors(0, client), client_node_readSensors(1, client)
    ,client_node_readSwitch(0, client)?1:0,client_node_readSwitch(1, client)?1:0, t1, t2);
    client_node_stop(client);
    client_node_freeNode(client);
    return 0;
```
with function getNode, you are providing node index, access port, control variable that should always be 0, string that represents control node IP address, and numbers of switches, sensors and transmitters respectively.
With client_node_start function provide node client that you just created to start communication. Function client_node_writeSensors is going to set value from the second argument to sensor with index from the first argument on the node that you provide as third argument.
There are also functions client_node_read_transmitter_value, client_node_readSensors and client_node_readSwitch that read respective elements. All of those 3 function expect first argument to be index of the element to be read and node where reading is performed.
In order to stop execution on the node, use function client_node_stop with argument of the node.
At the end, function client_node_freeNode clears all of elements of the node from the memory.