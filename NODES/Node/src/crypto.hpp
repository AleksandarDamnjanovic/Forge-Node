/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
*/

#ifndef CRYPTO_H
#define CRYPTO_H

#include <Arduino.h>

class CRYPTO {
    public:
        static String encrypt(String plainText, String secretKey);
        static String decrypt(String cipherTextHex, String secretKey);

    private:
        static void deriveKey(const String &secretKey, uint8_t keyOut[16]);
        static int hexCharToNibble(char c);
};

#endif
