## How communication works
On every 10 seconds, by default, node sends its own updated state to the mosqutitto server on topic with the same name as node itself. It sends confirmations for all of its sensors and switches split by space. Message looks like: 

- **Cr_11_C_0_1 Cr_11_R_0_27.5 Cr_11_R_1_70**.

What are those? Pieces of data are separated by _. Cr stands for confirmation, number 11 is the index of node itself, C stands for switch and R stands for readings(from the sensor, real or virtual one). Next number is index of element(switch or sensor) and final number is value from that element.

When some action is necessary, AAU sends instruction to all nodes at once. Command can look like:

- **I_11_C_0_0 I_11_S_0 I_11_S_1 I_11_T_0_70**

Once again, what is this? I stands for instruction, 11 is index of node that should accept that instruction, C stands for switch, S for sensor and T for transmitter. Next number represents index of the affected element and the last number is value that should be applied to that element. Of course, you can see that sensors doesn't have value to apply, because sensor is used only for reading.

You can see that there is no confirmation for transmitter's value. That is because, transmitter is used only to send value. If you want to get present value back, you can write the code on micro controller directly to copy value from the transmitter to virtual or dummy sensor, and therefore, that value will be read on every message exchange.