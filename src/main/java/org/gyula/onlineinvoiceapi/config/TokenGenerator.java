package org.gyula.onlineinvoiceapi.config;

import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;

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

