/*
*************** Name: KS Node
*************** Part of: KS Smart Systems
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
*/

#include "crypto.hpp"
#include <bearssl/bearssl_block.h>
#include <bearssl/bearssl_hash.h>
#include <string.h>

void CRYPTO::deriveKey(const String &secretKey, uint8_t keyOut[16]) {
    uint8_t digest[32];
    br_sha256_context ctx;
    br_sha256_init(&ctx);
    br_sha256_update(&ctx, secretKey.c_str(), secretKey.length());
    br_sha256_out(&ctx, digest);
    memcpy(keyOut, digest, 16);
}

int CRYPTO::hexCharToNibble(char c) {
    if (c >= '0' && c <= '9')
        return c - '0';
    if (c >= 'a' && c <= 'f')
        return c - 'a' + 10;
    if (c >= 'A' && c <= 'F')
        return c - 'A' + 10;
    return -1;
}

String CRYPTO::encrypt(String plainText, String secretKey) {
    if (secretKey.length() == 0)
        return plainText;

    uint8_t key[16];
    deriveKey(secretKey, key);

    uint8_t iv[16];
    uint8_t ivCopy[16];
    for (int i = 0; i < 16; i++) {
        iv[i] = (uint8_t)os_random();
        ivCopy[i] = iv[i];
    }

    size_t plainLen = plainText.length();
    size_t padLen = 16 - (plainLen % 16);
    size_t totalLen = plainLen + padLen;

    uint8_t *buf = new uint8_t[totalLen];
    if (plainLen > 0)
        memcpy(buf, plainText.c_str(), plainLen);
    for (size_t i = plainLen; i < totalLen; i++)
        buf[i] = (uint8_t)padLen;

    br_aes_small_cbcenc_keys encCtx;
    br_aes_small_cbcenc_init(&encCtx, key, 16);
    br_aes_small_cbcenc_run(&encCtx, ivCopy, buf, totalLen);

    static const char hexDigits[] = "0123456789abcdef";
    String out = "";
    out.reserve((16 + totalLen) * 2 + 2);

    for (int i = 0; i < 16; i++) {
        out += hexDigits[(iv[i] >> 4) & 0x0F];
        out += hexDigits[iv[i] & 0x0F];
    }
    for (size_t i = 0; i < totalLen; i++) {
        out += hexDigits[(buf[i] >> 4) & 0x0F];
        out += hexDigits[buf[i] & 0x0F];
    }
    out += "\n";

    delete[] buf;
    return out;
}

String CRYPTO::decrypt(String cipherTextHex, String secretKey) {
    if (secretKey.length() == 0)
        return cipherTextHex;

    cipherTextHex.trim();
    size_t hexLen = cipherTextHex.length();
    // Must contain at least 16-byte IV (32 hex chars) + 1 AES block (32 hex chars) = 64 hex chars
    if (hexLen < 64 || (hexLen % 32) != 0)
        return "";

    size_t byteLen = hexLen / 2;
    uint8_t *raw = new uint8_t[byteLen];
    for (size_t i = 0; i < byteLen; i++) {
        int hi = hexCharToNibble(cipherTextHex.charAt(i * 2));
        int lo = hexCharToNibble(cipherTextHex.charAt(i * 2 + 1));
        if (hi < 0 || lo < 0) {
            delete[] raw;
            return "";
        }
        raw[i] = (uint8_t)((hi << 4) | lo);
    }

    uint8_t key[16];
    deriveKey(secretKey, key);

    uint8_t iv[16];
    memcpy(iv, raw, 16);

    uint8_t *cipherPayload = raw + 16;
    size_t payloadLen = byteLen - 16;

    br_aes_small_cbcdec_keys decCtx;
    br_aes_small_cbcdec_init(&decCtx, key, 16);
    br_aes_small_cbcdec_run(&decCtx, iv, cipherPayload, payloadLen);

    uint8_t padLen = cipherPayload[payloadLen - 1];
    if (padLen == 0 || padLen > 16 || padLen > payloadLen) {
        delete[] raw;
        return "";
    }
    for (size_t i = payloadLen - padLen; i < payloadLen; i++) {
        if (cipherPayload[i] != padLen) {
            delete[] raw;
            return "";
        }
    }

    size_t plainLen = payloadLen - padLen;
    char *plainBuf = new char[plainLen + 1];
    if (plainLen > 0)
        memcpy(plainBuf, cipherPayload, plainLen);
    plainBuf[plainLen] = '\0';

    String result = String(plainBuf);
    delete[] plainBuf;
    delete[] raw;
    return result;
}
