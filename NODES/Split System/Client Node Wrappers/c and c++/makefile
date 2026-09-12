make:
	gcc -g example.c ./headers/clientNodeWrapper.h ./headers/controlNodeWrapper.h ./headers/baseHeader.h -pthread -O0 -o debug.bin
	gcc example.c ./headers/clientNodeWrapper.h ./headers/controlNodeWrapper.h ./headers/baseHeader.h -pthread -O2 -o wrapper.bin
	strip ./wrapper.bin