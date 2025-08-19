package org.gyula.onlineinvoiceapi.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gyula.onlineinvoiceapi.model.*;
import org.gyula.onlineinvoiceapi.repositories.ApartmentRepository;

import java.util.Optional;

import org.gyula.onlineinvoiceapi.repositories.UserRepository;
import org.gyula.onlineinvoiceapi.services.AdminService;
import org.gyula.onlineinvoiceapi.services.AuthenticationService;
import org.gyula.onlineinvoiceapi.services.CustomUserDetailsService;
import org.gyula.onlineinvoiceapi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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

    private final UserService userService;
    private final AdminService adminService;
    private final AuthenticationService authenticationService;
//    private final CustomUserDetailsService customUserDetailsService;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;

    public AdminController(
            UserService userService,
            AdminService adminService,
            AuthenticationService authenticationService,
//            CustomUserDetailsService customUserDetailsService,
            ApartmentRepository apartmentRepository,
            UserRepository userRepository) {
        this.userService = userService;
        this.adminService = adminService;
        this.authenticationService = authenticationService;
//        this.customUserDetailsService = customUserDetailsService;
        this.apartmentRepository = apartmentRepository;
        this.userRepository = userRepository;
    }


    /**
     * Generates and sends a registration email to the user if the provided API key
     * and authorization header are valid, and the user has administrative privileges.
     *
     * @param apiKey the API key used for authentication
     * @param authorizationHeader the Authorization header containing user credentials
     * @return ResponseEntity containing the status and message of the email operation
     */
    @PostMapping(value = "/sendEmail")
    public ResponseEntity<?> generateToken(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> body)
    {

        String username = null;
        log.info("/sendEmail endpoint called");

        try {
            username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrator can send email, user {} is not authorized.", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(username + " is not authorized to send email.");
            }
            Apartment apartment = apartmentRepository.findById(Long.parseLong(body.get("apartment"))).orElse(null);
            if (apartment == null) {
                log.warn("Apartment with ID {} was not found", body.get("apartment"));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Apartment not found");
            }

            String email = body.get("email");
            log.info("Sending email to user {}", email);
            String apartmentName = apartment.getCity() + " " + apartment.getStreet();
//            String apartmentName = apartmentRepository.findById(Long.parseLong(body.get("apartment")))
//                    .map(apt -> apt.getCity() + " " + apt.getStreet())
//                    .orElse("");
            String apartmentLanguage = String.valueOf(apartment.getLanguage());
            log.info("Apartment name: {}", apartmentName);

            String link = adminService.sendRegistrationEmail(email, apartmentName, body.get("apartment"), apartmentLanguage);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Email sent successfully");
            return ResponseEntity.ok(response);
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

        apartments.forEach(apartment -> {
            log.info("Apartment with language {} retrieved successfully", apartment.getLanguage());
        });

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
            @RequestBody Apartment editedApartment,
            @RequestParam String meterType,
            @RequestParam String lastMeterValue) {

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

            if (meterType.equals("null")) {     // "Normal" update
                apartmentRepository.save(editedApartment);
                log.info("Apartment updated successfully with ID: {}", apartmentId);
                return new ResponseEntity<>(Map.of("message", "Apartment updated successfully with ID: " + apartmentId), HttpStatus.OK);
            }else {     // Update because one of the meters was changed
                log.info("A new {} meter is to be registered for apartment with ID: {}", meterType, apartmentId);
                //First, I need to save the final consumption value for the old meter
                Map<String, Map<String, String>> results = adminService.getLast2MeterValues(Long.parseLong(String.valueOf(apartmentId)));
                String oldMeterValue = results.get(meterType).entrySet().iterator().next().getValue(); //get the first value from the Map, it is the latest
                Map<String,String> meterMap = Map.of("meterValue", "0", "apartmentId", apartmentId.toString());
                int consumptionCalculated = oldMeterValue.equals(lastMeterValue) ? 0 : Integer.parseInt(lastMeterValue) - Integer.parseInt(oldMeterValue);
                log.info("A new {} meter is to be registered for apartment with ID: {}. Old meter value: {}, new meter value: {}, calculated consumption: {}", meterType, apartmentId, oldMeterValue, lastMeterValue, consumptionCalculated);
                userService.addMeterValue(meterType, meterMap, null, consumptionCalculated);
                apartmentRepository.save(editedApartment);
                return new ResponseEntity<>(Map.of("message", "Apartment updated successfully with ID: " + apartmentId), HttpStatus.OK);

            }

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
                log.error("Only administrators can delete an apartment, user {} is not authorized.", username);
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
            log.error("An error occurred while deleting the apartment {}", e.getMessage());
            return new ResponseEntity<>("An error occurred while deleting the apartment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
        Map<String, Object> allLatestValues;
        try{
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);
            log.info("User {} is trying to retriev all last meter values for apartment {}", username, apartmentId.get("apartmentId"));
            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrators can get information on an apartment, user {} is not authorized.", username);
                return new ResponseEntity<>("Only administrators can get information on an apartment, user " + username + " is not authorized.", HttpStatus.UNAUTHORIZED);
            }
            if (Objects.equals(apartmentId.get("withImage"), "0")) {
                allLatestValues = adminService.getAllLatestValues(Long.parseLong(apartmentId.get("apartmentId")));

            }else{
                allLatestValues = adminService.getAllLatestValuesWithImages(Long.parseLong(apartmentId.get("apartmentId")));

            }
            return ResponseEntity.ok(allLatestValues);

        }catch(Exception e){
            log.error("An error occurred while searching for the latest meter values {}", e.getMessage());
            return new ResponseEntity<>("An error occurred while searching for the latest meter values: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves the last meter values for a specific apartment and meter type.
     * It validates the incoming request with the provided API key and authorization header.
     *
     * @param apiKey the API key provided in the request header for authentication
     * @param authorizationHeader the authorization token provided in the request header for authentication
     * @param body a map containing the request body, where:
     *             - "apartmentId" is the ID of the apartment
     *             - "meterType" is the type of meter (e.g., electricity, water, gas)
     * @return a ResponseEntity containing the last meter values with additional data if the operation completes successfully.
     *         If an error occurs, it returns a ResponseEntity containing an error message.
     */
    @PostMapping(value = "/getLastMeterValues")
    public ResponseEntity<?> getLastMeterValues(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> body
    ) {
        log.info("/getLastMeterValues endpoint called");
//        log.info("body:" + body.toString());

        try {
            authenticationService.validateRequest(apiKey, authorizationHeader);
            String apartmentId = body.get("apartmentId");
            String meterType = body.get("meterType");

            Map<String,Object>lastMeterValuesWithImages = userService.sendLastYearMeterValueWithImage(meterType, Long.valueOf(apartmentId));
//            log.info("last meter values with meter type: " + lastMeterValuesWithImages.toString());
            Map<String, String> lastMeterValues = userService.sendLastYearMeterValue(meterType, Long.valueOf(apartmentId));
//            log.info("Last meter values retrieved successfully: " + lastMeterValues.toString());
            return new ResponseEntity<>(lastMeterValuesWithImages, HttpStatus.OK);
        } catch (Exception e) {
            log.error("An error occurred during getting last 12 meter values: {}", e.getMessage());
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("Error", e.getMessage());
            return new ResponseEntity<>(errorMap, HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Retrieves a list of all users in the system. This method checks whether
     * the provided API key and authorization header grant administrator rights.
     * If authorized, it fetches basic user details from the database. The response
     * includes user ID, email, username, and enabled status, excluding sensitive information.
     *
     * @param apiKey the API key provided in the request header to authenticate the request
     * @param authorizationHeader the authorization header containing user credentials or token for validation
     * @return a ResponseEntity containing a list of users if the request is authorized, or an appropriate
     *         HTTP status with no content or an error explanation if unauthorized or an exception occurs
     */
    @GetMapping(value = "/getAllUsers")
    public ResponseEntity<?>getAllUsers(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("/getAllUsers endpoint called");

        List<UserListItemDto> users;
        try {
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if (!authenticationService.checkAdminAuthority(username)) {
                log.error("Only administrators can get information on users, user {} is not authorized.", username);
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }
            users = userRepository.findAll().stream()
                    .map(user -> new UserListItemDto(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getApartment() != null ? user.getApartment().getId() : null
                    )).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("An error occurred during getting all users: {}", e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * Deletes a user based on the provided user ID. This operation is restricted to administrators only.
     *
     * @param apiKey the API key provided in the request header for authentication
     * @param authorizationHeader the authorization token provided in the request header
     * @param deleteUserId the ID of the user to be deleted, passed in the request body
     * @return a ResponseEntity containing a success or error message along with the appropriate HTTP status code
     */
    @PostMapping(value = "/deleteUser")
    public ResponseEntity<String> deleteUser (
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody String deleteUserId) {

        log.info("/deleteUser endpoint called");

        try{
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrators can delete a user, user {} is not authorized.", username);
                return new ResponseEntity<>("Only administrators can delete a user, user " + username + " is not authorized.", HttpStatus.UNAUTHORIZED);
            }

            Optional<User> userToDelete = userRepository.findById(Long.parseLong(deleteUserId));
            if (userToDelete.isPresent()) {
                User user = userToDelete.get();
                if (user.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                    log.warn("Attempted to delete admin user with ID {}", deleteUserId);
                    return new ResponseEntity<>("Admin users cannot be deleted", HttpStatus.FORBIDDEN);
                }
                userRepository.deleteById(Long.parseLong(deleteUserId));
                log.info("User with the id {} deleted successfully", deleteUserId);
                return new ResponseEntity<>(HttpStatus.OK);
            }
            
            

            log.info("User with the id {} could not be found", deleteUserId);
            return new ResponseEntity<>("User could not be found", HttpStatus.BAD_REQUEST);

        }catch(Exception e){
            log.error("An error occurred while deleting the user {}", e.getMessage());
            return new ResponseEntity<>("An error occurred while deleting the user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Handles the request to edit an existing user in the system. This endpoint requires API key
     * and authorization headers for authentication and permits only administrators to perform the operation.
     * The method updates user details like username, email, and associated apartment, if provided.
     *
     * @param apiKey the system-generated API key passed in the request headers for validation
     * @param authorizationHeader the authorization token from the request headers for validating the user's identity
     * @param modifiedUser a data transfer object (DTO) containing the updated user details
     * @return a ResponseEntity containing the result of the operation. The response could be:
     *         - HTTP 200 (OK) with a message indicating the user was updated successfully
     *         - HTTP 401 (Unauthorized) if the user is not allowed to perform this operation
     *         - HTTP 404 (Not Found) if the user to be edited does not exist
     *         - HTTP 500 (Internal Server Error) if an exception occurs while processing the request
     */
    @PostMapping(value = "/editUser")
    public ResponseEntity<?> editUser (
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody UserListItemDto modifiedUser) {

        log.info("/editUser endpoint called");

        try{
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrators can edit an user, user {} is not authorized.", username);
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }

            Long userId = modifiedUser.getId();
            User originalUser = userRepository.findById(userId).orElse(null);
            if (!userRepository.findById(userId).isPresent()) {
                log.warn("User with ID {} not found for editing.", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"User not found for editing.\"}");
            }
            assert originalUser != null;
            log.info("User with ID {} ", originalUser.getApartment().getCity());

            originalUser.setUsername(modifiedUser.getUsername());
            originalUser.setEmail(modifiedUser.getEmail());
            if (modifiedUser.getApartmentId() != null) {
                Apartment apartment = apartmentRepository.findById(modifiedUser.getApartmentId()).orElse(null);
                originalUser.setApartment(apartment);
            }

            User updatedUser = userRepository.save(originalUser);
            log.info("User updated successfully with ID: {}", updatedUser.getId());
            return new ResponseEntity<>(Map.of("message", "User updated successfully with ID: " + updatedUser.getId()), HttpStatus.OK);

        }catch(Exception e){
            log.error("An error occurred while saving the edited user {}", e.getMessage());
            return new ResponseEntity<>("An error occurred while saving the edited user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/createInvoice")
    public ResponseEntity<?> createInvoice (
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody InvoiceItemDto invoiceData) {

        log.info("/createInvoice endpoint called");

        try{
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrators can create an invoice, user {} is not authorized.", username);
                return new ResponseEntity<>("Only administrator can create an invoice, user " + username + " is not authorized.", HttpStatus.UNAUTHORIZED);
            }
            
            String invoicePdf64 = adminService.createInvoicePdf(invoiceData);
            log.info("Invoice created successfully: {}", invoiceData.getEmail());
            return new ResponseEntity<>(Map.of("message", "Invoice created successfully",
                    "invoicePdf64", invoicePdf64,
                    "email", invoiceData.getEmail(),
                    "language", invoiceData.getLanguage(),
                    "apartmentAddress", invoiceData.getApartmentAddress()),
                    HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred while creating the invoice: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping(value = "/getUserByApartmentId")
    public ResponseEntity<Map<String, String>> getUserByApartmentId(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> apartmentId){

        log.info("/getUserByApartmentId endpoint called");

        try{
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrators can get the user, user {} is not authorized.", username);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Only administrator can get the user, user " + username + " is not authorized.");
                return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
            }

            User user = userRepository.findByApartmentId(Long.parseLong(apartmentId.get("apartmentId")));
            if (user == null) {
                log.warn("No user is connected to this apartment: {}", apartmentId.get("apartmentId"));
                Map<String, String> userNotFound = new HashMap<>();
                userNotFound.put("error", "No user is connected to this apartment");
                return new ResponseEntity<>(userNotFound, HttpStatus.NOT_FOUND);
            }
            log.info("User email: {}", user.getEmail());

            Map<String, String> response = new HashMap<>();
            response.put("email", user.getEmail());
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "An error occurred while searching for the user: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping(value = "/getLast2values")
    public ResponseEntity<?> getLast2values(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> apartmentId){

        log.info("/getLast2values endpoint called");

        try{
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrators can get the meter values, user {} is not authorized.", username);
                return new ResponseEntity<>("Only administrator can get the meter values, user " + username + " is not authorized.", HttpStatus.UNAUTHORIZED);
            }

            log.info("Apartment id: {}", apartmentId);

            Map<String, Map<String, String>> results = adminService.getLast2MeterValues(Long.parseLong(apartmentId.get("apartmentId")));

            log.info("Results: {}", results);

            return new ResponseEntity<>(Map.of("results", results), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred while searching for the user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping(value = "/sendPdfEmail")
    public ResponseEntity<Map<String, String>> sendPdfEmail(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody emailPdfDTO requestPdf){
        log.info("/sendPdfEmail endpoint called");

        try {
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrators can send the pdf document, user {} is not authorized.", username);
                return new ResponseEntity<>(Map.of("error", "Only administrator send the pdf document, user " + username + " is not authorized."), HttpStatus.UNAUTHORIZED);
            }

            log.info("Request email: {}", requestPdf.getEmail());

            String response;
            try {
                response = adminService.sendEmailPdf(requestPdf.getEmail(), requestPdf.getApartmentAddress(), requestPdf.getLanguage(), requestPdf.getPdfBase64());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            log.info("Sent email to the address {}", requestPdf.getEmail());

            return new ResponseEntity<>(Map.of("message", response), HttpStatus.OK);

        }catch (Exception e){
            log.error("An error occurred while sending the email {}", e.getMessage());
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/newMeterInstalled")
    public ResponseEntity<?> newMeterInstalled(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Apartment modifiedApartment,
            @RequestBody int lastMeterValue){
        log.info("/newMeterInstalled endpoint called");

        try {
            String username = authenticationService.validateRequest(apiKey, authorizationHeader);

            if(!authenticationService.checkAdminAuthority(username)){
                log.error("Only administrators can register the new meter, user {} is not authorized.", username);
                return new ResponseEntity<>(Map.of("error", "Only administrator can register the new meter, user " + username + " is not authorized."), HttpStatus.UNAUTHORIZED);
            }

            String response = "";
            try {

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return new ResponseEntity<>(Map.of("message", response), HttpStatus.OK);

        }catch (Exception e){
            log.error("An error occurred while registering the new meter {}", e.getMessage());
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
