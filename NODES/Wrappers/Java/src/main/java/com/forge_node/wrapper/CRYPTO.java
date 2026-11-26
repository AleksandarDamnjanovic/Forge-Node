/*
*************** Name: Forge Node
*************** Part of: Java Wrapper libraries
*************** Author: Aleksandar Damnjanovic AKA Kind Spirit
*************** YouTube channel: Kind Spirit Technology
*************** Date: 26.11.2026.
*************** Location: Kragujevac, Serbia
*/

package com.forge_node.wrapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CRYPTO {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private static SecretKeySpec deriveKey(String secretKey) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha256.digest(secretKey.getBytes(StandardCharsets.UTF_8));
        byte[] key16 = Arrays.copyOf(digest, 16);
        return new SecretKeySpec(key16, "AES");
    }

    private static String bytesToHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            chars[i * 2] = HEX_DIGITS[v >>> 4];
            chars[i * 2 + 1] = HEX_DIGITS[v & 0x0F];
        }
        return new String(chars);
    }

    private static int hexCharToNibble(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) return null;
        byte[] out = new byte[len / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = hexCharToNibble(hex.charAt(i * 2));
            int lo = hexCharToNibble(hex.charAt(i * 2 + 1));
            if (hi < 0 || lo < 0) return null;
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    public static String encrypt(String plainText, String secretKey) {
        if (secretKey == null || secretKey.isEmpty()) {
            return plainText;
        }
        try {
            SecretKeySpec keySpec = deriveKey(secretKey);
            byte[] iv = new byte[16];
            RANDOM.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(iv) + bytesToHex(encrypted) + "\n";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String decrypt(String cipherTextHex, String secretKey) {
        if (secretKey == null || secretKey.isEmpty()) {
            return cipherTextHex;
        }
        if (cipherTextHex == null) {
            return "";
        }
        String trimmed = cipherTextHex.replace("\0", "").trim();
        if (trimmed.length() < 64 || (trimmed.length() % 32) != 0) {
            return "";
        }
        try {
            byte[] raw = hexToBytes(trimmed);
            if (raw == null || raw.length < 32) {
                return "";
            }
            byte[] iv = Arrays.copyOfRange(raw, 0, 16);
            byte[] cipherPayload = Arrays.copyOfRange(raw, 16, raw.length);

            SecretKeySpec keySpec = deriveKey(secretKey);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] plainBytes = cipher.doFinal(cipherPayload);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
