## What this solution contains

Forge-Node is a complete, self-contained system. It includes the following main components:

- **AAU (Auxiliary Automation Unit)**  
  The core of the system, written in Python. It interprets the `.fn` script, coordinates all nodes over MQTT, applies logic, and handles real-time administrative commands. It has full access to every node topic plus a dedicated admin channel that allows live changes to the running script without restarting the system.

- **NODE**  
  Firmware for ESP32 and ESP8266 microcontrollers, built with the Arduino framework and PlatformIO. After the initial flash and configuration in `node.hpp`, the nodes operate under the control of the AAU and the `.fn` script.

- **Wrappers**
  Software that behaves like node. Written in multiple programming languages like java, c and python. You can create app of your own that can be integrated into entire ecosystem. 

- **Administrative Unit (AU)**  
  A separate Python application that communicates with the AAU over dedicated mqtt channel. Its purpose is to respond to requests of AI agents.

- **Web UI**  
  A modern web interface (located in `UI/forge-node`) that gives you a convenient graphical way to monitor and control the system. When running via Docker it is exposed by default on port 10011.

- **Android Application**  
  A native Android app (in the `Android` folder) that lets you interact with your Forge-Node system from your phone. Useful for quick checks and control when you’re away from a computer.

- **AI Skill**  
  A ready-to-use skill (tested with Hermes and OpenCode) that enables AI agents to understand and operate the system through the AU’s TCP interface. Works well even with smaller models such as Gemma 4B.

- **Docker Setup**  
  A complete Docker + Docker Compose configuration is included. You can build and run the entire backend (AAU, MQTT broker, UI, and supporting services) with just a few commands. The compose file lets you easily adjust credentials, exposed ports, and SSL settings. Entire system is designed with docker in mind, and using it through docker is advisable and recommended.