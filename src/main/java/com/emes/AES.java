package com.emes;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {

  private static final String KEY_DERIVATION_FUNCTION = "PBKDF2WithHmacSHA512";
  private static final Integer KEY_DERIVATION_ITERATIONS = 210_000;
  private static final Integer AES_KEY_SIZE = 128;
  private static final String AES_MODE = "AES/GCM/NoPadding";
  private static final String AES = "AES";

  private static final Integer AES_SALT_LENGTH = 16;
  private static final Integer AAD_LENGTH = 12;
  private static final Integer PASSWORD_SALT_LENGTH = 16;

  public byte[] encrypt(char[] password, byte[] content)
      throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
      BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException,
      InvalidKeySpecException {

    Objects.requireNonNull(password);
    Objects.requireNonNull(content);
    Precondition.require(password.length > 0);
    Precondition.require(content.length > 0);

    var secureRandom = new SecureRandom();
    var aesSalt = new byte[AES_SALT_LENGTH];
    secureRandom.nextBytes(aesSalt);
    var aad = new byte[AAD_LENGTH];
    secureRandom.nextBytes(aad);
    var passwordSalt = new byte[PASSWORD_SALT_LENGTH];
    secureRandom.nextBytes(passwordSalt);
    var key = getKey(password, passwordSalt);

    var cipher = Cipher.getInstance(AES_MODE);
    var keySpec = new SecretKeySpec(key, AES);
    var gcmParameterSpec = new GCMParameterSpec(AES_KEY_SIZE, aesSalt);

    cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
    cipher.updateAAD(aad);
    var ciphertext = cipher.doFinal(content);

    return joinArrays(passwordSalt, aesSalt, aad, ciphertext);
  }

  public byte[] decrypt(char[] password, byte[] aesResult)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
      InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {

    Objects.requireNonNull(password);
    Objects.requireNonNull(aesResult);
    Precondition.require(password.length > 0);

    ByteBuffer byteBuffer = ByteBuffer.wrap(aesResult);

    byte[] passwordSalt = new byte[PASSWORD_SALT_LENGTH];
    byte[] aesSalt = new byte[AES_SALT_LENGTH];
    byte[] aad = new byte[AAD_LENGTH];
    byte[] ciphertext = new byte[aesResult.length - PASSWORD_SALT_LENGTH - AES_SALT_LENGTH
        - AAD_LENGTH];

    byteBuffer.get(passwordSalt, 0, passwordSalt.length);
    byteBuffer.get(aesSalt, 0, aesSalt.length);
    byteBuffer.get(aad, 0, aad.length);
    byteBuffer.get(ciphertext, 0, ciphertext.length);

    var key = getKey(password, passwordSalt);

    var cipher = Cipher.getInstance(AES_MODE);
    var keySpec = new SecretKeySpec(key, AES);
    var gcmParameterSpec = new GCMParameterSpec(AES_KEY_SIZE, aesSalt);

    cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
    cipher.updateAAD(aad);

    return cipher.doFinal(ciphertext);
  }

  private byte[] getKey(char[] password, byte[] salt)
      throws NoSuchAlgorithmException, InvalidKeySpecException {

    var derivationSpec =
        new PBEKeySpec(password, salt, KEY_DERIVATION_ITERATIONS, AES_KEY_SIZE);
    return SecretKeyFactory.getInstance(KEY_DERIVATION_FUNCTION).generateSecret(derivationSpec)
        .getEncoded();
  }

  private byte[] joinArrays(byte[] passwordSalt, byte[] aesSalt, byte[] aad, byte[] ciphertext) {
    ByteBuffer result = ByteBuffer.allocate(PASSWORD_SALT_LENGTH + AES_SALT_LENGTH + AAD_LENGTH
        + ciphertext.length);

    result.put(passwordSalt);
    result.put(aesSalt);
    result.put(aad);
    result.put(ciphertext);

    return result.array();
  }
}
