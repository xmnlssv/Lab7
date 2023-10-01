package worker;

import com.sun.istack.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * The PasswordHashed class is designed for hashing passwords using salt and the MD5 algorithm.
 */
public class PasswordHashed {

    private static SecureRandom random = new SecureRandom();

    /**
     * The generateSalt() method creates a random salt for use in password hashing.
     *
     * @return The generated salt as a hexadecimal string.
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * The hashPassword() method hashes a password using salt and the MD5 algorithm.
     *
     * @param password The password to be hashed.
     * @param salt     The salt used to enhance hashing security.
     * @return The hashed password as a hexadecimal string.
     * @throws NoSuchAlgorithmException If the MD5 algorithm is not supported.
     */
    public static String hashPassword(String password, String salt) {
        try {
            String saltedPassword = password + salt;
            MessageDigest md = MessageDigest.getInstance("MD2");
            byte[] bytes = md.digest(saltedPassword.getBytes());

            return bytesToHex(bytes);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            return "";
        }

    }

    /**
     * A helper method to convert bytes to hexadecimal representation.
     *
     * @param bytes The bytes to be converted.
     * @return A string representing the hexadecimal value of the bytes.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
