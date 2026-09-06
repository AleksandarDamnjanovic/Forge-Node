make:
	gcc -g example.c nodeWrapper.h -pthread -O0 -o debug.bin
	gcc example.c nodeWrapper.h -pthread -O2 -o wrapper.bin
	strip ./wrapper.bin