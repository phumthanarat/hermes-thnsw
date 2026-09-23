package hk.hku.cecid.edi.sfrm.util;

import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.piazza.commons.security.KeyStoreManager;

/**
 * FTPCredentialCipher encrypts/decrypts FTP channel passwords at rest,
 * using the RSA key pair already provisioned for SFRM message security
 * (corvus.p12, the same "keystore-manager" component SFRM uses to sign and
 * encrypt messages) rather than introducing a second key management story
 * just for this. This only protects the password while sitting in the
 * database (e.g. a DB dump/backup leak) -- FTP itself still sends the
 * password in the clear over the wire unless FTPS is used, which is a
 * separate, orthogonal concern.
 *
 * @author Hermes2+ (FTP port type)
 */
public class FTPCredentialCipher {

    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    public static String encrypt(String plaintext) throws Exception {
        if (plaintext == null || plaintext.length() == 0) {
            return "";
        }
        PublicKey publicKey = getKeyStoreManager().getPublicKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
        return java.util.Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String ciphertextBase64) throws Exception {
        if (ciphertextBase64 == null || ciphertextBase64.length() == 0) {
            return "";
        }
        PrivateKey privateKey = getKeyStoreManager().getPrivateKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(java.util.Base64.getDecoder().decode(ciphertextBase64));
        return new String(decrypted, "UTF-8");
    }

    private static KeyStoreManager getKeyStoreManager() throws Exception {
        return (KeyStoreManager) SFRMProcessor.getInstance().getSystemModule().getComponent("keystore-manager");
    }
}
