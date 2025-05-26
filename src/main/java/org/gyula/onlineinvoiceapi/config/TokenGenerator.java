package org.gyula.onlineinvoiceapi.config;

import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;

/**
 * The TokenGenerator class provides utility methods to generate secure random tokens.
 *
 * This class is designed to create alphanumeric tokens of a fixed length, which
 * can be used in contexts such as authentication, session management, or any
 * other functionality requiring a unique and temporary identifier.
 *
 * Features:
 * - Generates tokens using a cryptographically secure random number generator.
 * - Tokens are composed of uppercase letters, lowercase letters, and digits.
 * - Logs the token generation process.
 *
 * Constants:
 * - CHARACTERS: The set of characters used to generate the token.
 * - TOKEN_LENGTH: The fixed length of the generated tokens.
 *
 * Methods:
 * - generateToken: Generates a secure random alphanumeric token of the specified length.
 *
 * Thread Safety:
 * - This class is thread-safe as it does not maintain any mutable shared state.
 */
public class TokenGenerator {

    public static final Logger log = org.apache.logging.log4j.LogManager.getLogger(TokenGenerator.class);

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 10;

    public static String generateToken() {

        log.info("In generateToken:");

        SecureRandom random = new SecureRandom();
        StringBuilder builder = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            builder.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return builder.toString();
    }
}

