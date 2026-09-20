## How to use wrapper

In this directory, you can find 4 different sub projects.
Two of those are codes for micro controllers(Arduino, esp32, esp8266). The main difference with those two is that in both cases, functionality is the same as main system counterparts with only one difference. In case of split system, micro controllers must work on the same network as wrappers. So, if you intent to use those, you have to set the network parameters.
There are two wrapper projects. One written in c, and one written in java. Essentially both are doing the same job(mimicking split system node functionality). You can find working example in the main executable of every sub project respectively.