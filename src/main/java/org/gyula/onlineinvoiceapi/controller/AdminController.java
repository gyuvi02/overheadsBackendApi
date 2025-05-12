package org.gyula.onlineinvoiceapi.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.repositories.ApartmentRepository;
import org.gyula.onlineinvoiceapi.services.AdminService;
import org.gyula.onlineinvoiceapi.services.AuthenticationService;
import org.gyula.onlineinvoiceapi.services.CustomUserDetailsService;
import org.gyula.onlineinvoiceapi.services.UserService;
import org.gyula.onlineinvoiceapi.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The AdminController class provides REST API endpoints for administrative tasks
 * related to user management and apartment operations. It includes operations such
 * as sending registration emails, adding new apartments, retrieving apartment details,
 * fetching all apartments, and editing existing apartment information.
 * This controller is secured and requires valid API keys and administrative
 * privileges to access its endpoints.
 *
 * Endpoints:
 * - /sendEmail: Sends a registration email to a user if the request is authorized.
 * - /addApartment: Adds a new apartment entry to the database (Admin-only).
 * - /getApartment: Fetches details of a single apartment using its ID (Admin-only).
 * - /getAllApartments: Retrieves a list of all apartments (Admin-only).
 * - /editApartment: Updates the details of an existing apartment (Admin-only).
 *
 * Security:
 * Each endpoint in the AdminController is protected by validating API keys and
 * ensuring the user has administrative credentials.
 *
 * Dependencies:
 * This class uses services and repositories such as:
 * - UserService
 * - AdminService
 * - AuthenticationService
 * - CustomUserDetailsService
 * - ApartmentRepository
 *
 * Logging:
 * Every endpoint logs relevant operations for debugging and tracking purposes.
 */
@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    private static final Logger log = LogManager.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private AdminService adminServce;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ApartmentRepository apartmentRepository;


    /**
     * Generates and sends a registration email to the user if the provided API key
     * and authorization header are valid, and the user has administrative privileges.
     *
     * @param apiKey the API key used for authentication
     * @param authorizationHeader the Authorization header containing user credentials
     * @return ResponseEntity containing the status and message of the email operation
     */
    @GetMapping(value = "/sendEmail")
    public ResponseEntity<?> generateToken(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader)
    {

        String username = null;
        log.info("/sendEmail endpoint called");

        try {
            username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrator can send email, user {} is not authorized.", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(username + " is not authorized to send email.");
            }

            String email = userService.getUserByUsername(username).getEmail();
            if (email == null) {
                log.error("The user {} does not exist.", username);
                throw new Exception("The user does not exist:" + username);
            }

            String link = adminServce.sendRegistrationEmail(email);
            return ResponseEntity.ok("Email sent successfully, link attached:" + link);
        } catch (RuntimeException ex) {
            log.error("\"Could not send email, the user {} does not exist.", username);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not send email, the user does not exist");
        }catch (Exception e){
            log.error("An error occurred during email sending: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during email sending." + e.getMessage());
        }
    }

    /**
     * Handles the addition of a new apartment to the system. Validates the request's API key
     * and authorization header, ensuring only administrators carry out this operation. If
     * the validation is successful, the provided {@code Apartment} object is saved to the
     * database.
     *
     * @param apiKey the API key provided in the request header for authentication
     * @param authorizationHeader the authorization token provided in the request header
     * @param newApartment the {@code Apartment} object containing details of the apartment to be added
     * @return a {@code ResponseEntity} containing the status of the operation. Returns HTTP
     *         status code 201 (Created) upon successful addition, 401 (Unauthorized) if the
     *         user is not authorized, or 403 (Forbidden) if an error occurs during processing.
     */
    @PostMapping(value = "/addApartment")
    public ResponseEntity<String> addApartment(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Apartment newApartment)

    {
        log.info("/addApartment endpoint called");
        String username = null;
        try {
            username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrator can add a new apartment, user {} is not authorized.", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(username + " is not authorized to add a new apartment.");
            }

            // Save the new apartment to the database
            apartmentRepository.save(newApartment);
            log.info("Apartment added successfully");

        }catch (Exception e){
            log.error("An error occurred during adding  a new apartment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Retrieves information about an apartment based on the provided apartment ID.
     * The method verifies the API key and authorization header, checks if the user
     * has the necessary administrative authority, and fetches the apartment details
     * from the database.
     *
     * @param apiKey the API key used for authentication
     * @param authorizationHeader the authorization header containing the user's credentials
     * @param apartmentId a map containing the ID of the apartment to fetch, provided
     *                    with the key "apartmentId"
     * @return a {@code ResponseEntity<Object>} containing the apartment details if found,
     *         an error message if the apartment is not found, or if the user is not
     *         authorized to access this information
     */
    @GetMapping(value = "/getApartment")
    public ResponseEntity<Object> getApartment(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> apartmentId)

    {
        log.info("/getApartment endpoint called");
        String username = null;
        try {
            username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrator can get apartment information, user {} is not authorized.", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(username + " is not authorized to get apartment information.");
            }

                Long id = Long.parseLong(apartmentId.get("apartmentId"));

            Apartment apartment = apartmentRepository.findById(id).orElse(null);

            if (apartment == null) {
                log.warn("Apartment with ID {} was not found", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Apartment not found");
            }

            log.info("Apartment with ID {} retrieved successfully by user: {}", id, username);
            return ResponseEntity.ok(apartment);

        }catch (Exception e){
            log.error("An error occurred during login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * Retrieves a list of all apartments.
     * This method requires administrator access to retrieve the data.
     *
     * @param apiKey the API key provided to authenticate the request
     * @param authorizationHeader the authorization header containing authentication information
     * @return a ResponseEntity containing the list of all apartments and an appropriate HTTP status code
     */
    @GetMapping(value = "/getAllApartments")
    public ResponseEntity<List<Apartment>> getAllApartments(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("/getAllApartments endpoint called");

        List<Apartment> apartments = null;;

        try {
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrator can get information on all apartments, user {} is not authorized.", username);
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

                apartments = apartmentRepository.findAll();
            if (apartments.isEmpty()) {
                return new ResponseEntity<>(apartments, HttpStatus.NO_CONTENT);
            }

        } catch (Exception e) {
            log.error("An error occurred during getting all apartments: {}", e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(apartments, HttpStatus.OK);
    }

    /**
     * Edits an existing apartment in the system based on the provided details. This endpoint requires
     * administrator-level authorization to execute. The edited apartment must already exist in the database.
     *
     * @param apiKey the API key for authenticating the request
     * @param authorizationHeader the authorization header containing user credentials or tokens
     * @param editedApartment the apartment object containing updated details; the ID must be provided to identify the apartment to be edited
     * @return a ResponseEntity containing a success message with the updated apartment ID if successful,
     *         a 401 status if the user is unauthorized,
     *         a 404 status if the apartment ID is not found, or
     *         a 500 status if an internal server error occurs
     */
    @PostMapping(value = "/editApartment")
    public ResponseEntity<?> editApartment (
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Apartment editedApartment) {

        log.info("/editApartment endpoint called");

        try{
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrator can edit an apartment, user {} is not authorized.", username);
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            Long apartmentId = editedApartment.getId();
            if (apartmentId == null || !apartmentRepository.findById(apartmentId).isPresent()) {
                log.warn("Apartment with ID {} not found for editing.", apartmentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"Apartment not found for editing.\"}");
            }

            Apartment updatedApartment = apartmentRepository.save(editedApartment);
            log.info("Apartment updated successfully with ID: {}", updatedApartment.getId());
            return new ResponseEntity<>(Map.of("message", "Apartment updated successfully with ID: " + updatedApartment.getId()), HttpStatus.OK);
//            return new ResponseEntity<>("Apartment updated successfully with ID: " + updatedApartment.getId(), HttpStatus.OK);

        }catch(Exception e){
            log.error("An error occurred while saving the edited apartment {}", e.getMessage());
            return new ResponseEntity<>("An error occurred while saving the edited apartment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Deletes an apartment based on the provided apartment ID. Requires the request to be authorized
     * and the user to have administrative privileges. This method validates the request, checks if the
     * user has the appropriate permissions, and deletes the apartment if it exists.
     *
     * @param apiKey the API key for authenticating the request
     * @param authorizationHeader the Authorization header containing the user's credentials
     * @param deleteApartmentId the ID of the apartment to be deleted
     * @return ResponseEntity containing the HTTP status and optional message indicating the result of the operation
     */
    @PostMapping(value = "deleteApartment")
    public ResponseEntity<String> deleteApartment (
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody String deleteApartmentId) {

        log.info("/deleteApartment endpoint called");

        try{
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrator can delete an apartment, user {} is not authorized.", username);
                return new ResponseEntity<>("Only administrator can delete an apartment, user " + username + " is not authorized.", HttpStatus.UNAUTHORIZED);
            }

            if (apartmentRepository.findById(Long.parseLong(deleteApartmentId)).isPresent()) {
                apartmentRepository.deleteById(Long.parseLong(deleteApartmentId));
                log.info("Apartment with the id {} deleted successfully", deleteApartmentId);
                return new ResponseEntity<>(HttpStatus.OK);
            }

            log.info("Apartment with the id {} could not be found", deleteApartmentId);
            return new ResponseEntity<>("Apartment could not be found", HttpStatus.BAD_REQUEST);

        }catch(Exception e){
            log.error("An error occurred while saving the edited apartment {}", e.getMessage());
            return new ResponseEntity<>("An error occurred while saving the edited apartment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Retrieves the most recent meter values for a given apartment. This method requires
     * authentication and administrative permission to access the requested data.
     *
     * @param apiKey the API key provided in the request header to validate the request source
     * @param authorizationHeader the token provided in the request header for user authentication
     * @param apartmentId a map containing the key "apartmentId" with the ID of the apartment
     *                    whose meter values are to be retrieved
     * @return a ResponseEntity containing either a map of the latest meter values for the specified
     *         apartment in case of success, or an error message with the corresponding HTTP status
     */
    @PostMapping(value = "getAllLastMeterValues")
    public ResponseEntity<?> getAllLastMeterValues (
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> apartmentId) {
        log.info("/getAllLastMeterValues endpoint called");
        Map<String, Object> allLatestValues = new HashMap<>();
        try{
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);
            log.info("User {} is trying to retriev all last meter values for apartment {}", username, apartmentId.get("apartmentId"));
            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrators can delete an apartment, user {} is not authorized.", username);
                return new ResponseEntity<>("Only administrators can delete an apartment, user " + username + " is not authorized.", HttpStatus.UNAUTHORIZED);
            }
            if (Objects.equals(apartmentId.get("withImage"), "0")) {
                allLatestValues = adminServce.getAllLatestValues(Long.parseLong(apartmentId.get("apartmentId")));

            }else{
                allLatestValues = adminServce.getAllLatestValuesWithImages(Long.parseLong(apartmentId.get("apartmentId")));

            }
            return ResponseEntity.ok(allLatestValues);

        }catch(Exception e){
            log.error("An error occurred while searching for the latest meter values {}", e.getMessage());
            return new ResponseEntity<>("An error occurred while searching for the latest meter values: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
