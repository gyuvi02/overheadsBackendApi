package org.gyula.onlineinvoiceapi.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gyula.onlineinvoiceapi.model.*;
import org.gyula.onlineinvoiceapi.repositories.ElectricityMeterRepository;
import org.gyula.onlineinvoiceapi.repositories.GasMeterRepository;
import org.gyula.onlineinvoiceapi.repositories.RegistrationTokenRepository;
import org.gyula.onlineinvoiceapi.repositories.UserRepository;
import org.gyula.onlineinvoiceapi.repositories.WaterMeterRepository;
import org.gyula.onlineinvoiceapi.repositories.HeatingMeterRepository;
import org.gyula.onlineinvoiceapi.services.AuthenticationService;
import org.gyula.onlineinvoiceapi.services.CustomUserDetailsService;
import org.gyula.onlineinvoiceapi.services.UserService;
import org.gyula.onlineinvoiceapi.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MainController is a Spring REST controller that provides endpoints for handling
 * user-related functionalities such as login and registration. The controller interacts
 * with other services and repositories to perform authentication, retrieve user details,
 * process tokens, and provide necessary user or apartment-related data in responses.
 *
 * The class primarily offers secure user authentication and registration features. Several
 * layers of validations are incorporated to ensure reliability and correctness of requests
 * such as API key validation, rate limiting, and token verification.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:4200")

public class MainController {

    private static final Logger log = LogManager.getLogger(MainController.class);

    private final UserService userService;

    private final RegistrationTokenRepository tokenRepository;

    @Autowired
    private AuthenticationService authenticationService;

    private final CustomUserDetailsService customUserDetailsService;

    private final BCryptPasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final GasMeterRepository gasMeterRepository;

    private final WaterMeterRepository waterMeterRepository;

    private final ElectricityMeterRepository electricityMeterRepository;

    private final HeatingMeterRepository heatingMeterRepository;


    public MainController(UserService userService, RegistrationTokenRepository tokenRepository,
                          CustomUserDetailsService customUserDetailsService, BCryptPasswordEncoder passwordEncoder,
                          UserRepository userRepository, GasMeterRepository gasMeterRepository,
                          WaterMeterRepository waterMeterRepository, ElectricityMeterRepository electricityMeterRepository, HeatingMeterRepository heatingMeterRepository) {
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.customUserDetailsService = customUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.gasMeterRepository = gasMeterRepository;
        this.waterMeterRepository = waterMeterRepository;
        this.electricityMeterRepository = electricityMeterRepository;
        this.heatingMeterRepository = heatingMeterRepository;
    }

    /**
     * Handles the login process for users by validating API keys, user credentials, and retrieving related data.
     * Upon successful login, generates a JWT token and optionally returns user-specific apartment and meter readings information.
     *
     * @param apiKey The API key provided in the request headers for authentication purposes.
     * @param loginRequest The login details containing the username and password.
     * @return A ResponseEntity containing the JWT token, a success message, and user-specific data such as apartment details
     *         and latest meter readings if applicable. Returns an error response if login fails.
     */
    @PostMapping(value = "/login")
    public ResponseEntity<?> login(
            @RequestHeader("API-KEY") String apiKey,
            @RequestBody LoginRequest loginRequest
    ) {
        log.info("/login endpoint called");
        try {
            // limiting connections per second
            if (!userService.isAllowed()) {
                throw new IllegalArgumentException("Rate limit exceeded. Try again later.");
            }

            if (authenticationService.checkApiKey(apiKey)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials or API key.");
            }

            UserDetails userDetails = customUserDetailsService.loadUserByUsername(loginRequest.getUsername());
            log.info("userdetails authorities: {}", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray());

            if (userDetails.isEnabled() && passwordEncoder.matches(loginRequest.getPassword(), userDetails.getPassword())) {
                log.info("Login successful.");

                // Generate JWT token
                long expirationMillis = 1800000; // half an hour in milliseconds
                String jwtToken = JwtUtil.generateToken(userDetails.getUsername(), expirationMillis);

                log.info("JWT Token generated: {}", jwtToken);

                // Find the apartment connected to the logged-in user
                Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());
                Apartment apartment = null;
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    if (user.getApartment() != null) {
                        apartment = user.getApartment();
                    }
                    if (apartment != null) {
                        // Convert apartment data to Map<String, String>
                        Map<String, String> apartmentData = new HashMap<>();

                        // Use reflection to get all fields and their values
                        Field[] fields = Apartment.class.getDeclaredFields();
                        for (Field field : fields) {
                            field.setAccessible(true);
                            String fieldName = field.getName();
                            try {
                                Object value = field.get(apartment);
                                if (value != null) {
                                    apartmentData.put(fieldName, value.toString());
                                } else {
                                    apartmentData.put(fieldName, "");
                                }
                            } catch (IllegalAccessException e) {
                                log.error("Error accessing field {}: {}", fieldName, e.getMessage());
                            }
                        }

                        // Check if user has ADMIN role
                        boolean isAdmin = userDetails.getAuthorities().stream()
                                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

                        // Create response with token and apartment data
                        Map<String, Object> response = new HashMap<>();
                        response.put("token", jwtToken);
                        response.put("message", "Login successful");
                        response.put("apartment", apartmentData);
                        response.put("isAdmin", isAdmin);

                        // Get latest meter values for the apartment
                        Optional<GasMeterValues> latestGas = gasMeterRepository.findByApartmentReferenceAndLatestTrue(apartment);
                        log.info("latestGas: {}", latestGas.toString());
                        Optional<WaterMeterValues> latestWater = waterMeterRepository.findByApartmentReferenceAndLatestTrue(apartment);
                        Optional<ElectricityMeterValues> latestElectricity = electricityMeterRepository.findByApartmentReferenceAndLatestTrue(apartment);
                        Optional<HeatingMeterValues> latestHeating = heatingMeterRepository.findByApartmentReferenceAndLatestTrue(apartment);

                        // Add meter values to response if they exist
                        if (latestGas.isPresent()) {
                            response.put("actualGas", latestGas.get().getGasValue().toString());
                        }

                        latestWater.ifPresent(waterMeterValues -> response.put("actualWater", waterMeterValues.getWaterValue().toString()));

                        latestElectricity.ifPresent(electricityMeterValues -> response.put("actualElectricity", electricityMeterValues.getElectricityValue().toString()));

                        latestHeating.ifPresent(heatingMeterValues -> response.put("actualHeating", heatingMeterValues.getHeatingValue().toString()));

                        return ResponseEntity.ok(response);
                    } else {

                        boolean isAdmin = userDetails.getAuthorities().stream()
                                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

                        Map<String, Object> response = new HashMap<>();
                        response.put("token", jwtToken);
                        response.put("message", "Login successful");
                        response.put("isAdmin", isAdmin);

                        return ResponseEntity.ok(response);
                    }
                } else {
                    // User not found (should not happen at this point)
                    boolean isAdmin = userDetails.getAuthorities().stream()
                            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

                    Map<String, Object> response = new HashMap<>();
                    response.put("token", jwtToken);
                    response.put("message", "Login successful");
                    response.put("isAdmin", isAdmin);

                    return ResponseEntity.ok(response);
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials or API key.");
            }
        } catch (UsernameNotFoundException unfe) {
            log.error(unfe.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(unfe.getMessage());
        } catch (IllegalArgumentException iae) {
            log.error("Login failed: {}", iae.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(iae.getMessage());
        } catch (Exception e) {
            log.error("An error occurred during login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during login.");
        }
    }

    /**
     * Handles user registration by validating API key, processing registration tokens,
     * and registering new users.
     *
     * @param apiKey the API key provided in the request header for authentication
     * @return a ResponseEntity containing the appropriate HTTP status code and a message.
     *         Possible responses are:
     *         - 201 (Created) if registration is successful
     *         - 400 (Bad Request) if the token is invalid, expired, or already used
     *         - 401 (Unauthorized) if the API key is invalid
     *         - 429 (Too Many Requests) if the rate limit is exceeded
     *         - 500 (Internal Server Error) if any unhandled error occurs during registration
     */
    @PostMapping(value = "/register")
    public ResponseEntity<?> register(
            @RequestHeader("API-KEY") String apiKey,
            @RequestBody RegisterRequest registerRequest
            ) {

        log.info("/register endpoint called");

        try {
            if (!userService.isAllowed()) {
                throw new IllegalArgumentException("Rate limit exceeded. Try again later.");
            }
            if (authenticationService.checkApiKey(apiKey)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials or API key."));
            }
            // Token comes from request body
            String token = registerRequest.getToken();
            Optional<RegistrationToken> optionalToken = tokenRepository.findByTokenAndIsUsedFalse(token);

            if (optionalToken.isPresent()) {
                RegistrationToken validToken = optionalToken.get();

                // Check if the token is expired
                if (validToken.getExpiration().toLocalDateTime().isBefore(LocalDateTime.now())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Token has expired."));
                }

                userService.registerUser(registerRequest);

                validToken.setIsUsed(true);
                tokenRepository.save(validToken);

                log.info("User registered successfully.");
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User registered successfully."));
            } else {
                log.info("Registraion failed, invalid or already used token");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid or already used token."));
            }

        }catch (IllegalArgumentException iae) {
            log.info("Registration failed: {}", iae.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", iae.getMessage()));

        } catch(Exception e){
            log.error("An error occurred during registration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An error occurred during registration."));
        }
    }
}
