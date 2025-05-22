package org.gyula.onlineinvoiceapi.services;

import org.apache.logging.log4j.Logger;
import org.gyula.onlineinvoiceapi.model.User;
import org.gyula.onlineinvoiceapi.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * CustomUserDetailsService is an implementation of the Spring Security UserDetailsService
 * interface, responsible for loading user-specific data during authentication.
 *
 * This service is used to fetch user details such as username, password, and authorities
 * from the database via the injected UserRepository.
 *
 * The class provides the logic to look up a user by their username, retrieve associated
 * user data, and return an instance of UserDetails. It is a key component of the application’s
 * security layer, enabling the authentication and authorization process.
 *
 * Components:
 * - Logs user-specific actions for debugging and monitoring purposes.
 * - Fetches user information from the database using UserRepository.
 * - Throws UsernameNotFoundException if the user cannot be found.
 *
 * Dependencies:
 * - UserRepository: Provides access to the database for fetching user data.
 *
 * Key Methods:
 * - loadUserByUsername(String username): Retrieves user details by username,
 *   constructs a UserDetails object, and returns it.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    public static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        log.info("In loadUserByUsername: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getAuthorities());
    }
}

