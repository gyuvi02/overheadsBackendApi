package org.gyula.onlineinvoiceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gyula.onlineinvoiceapi.repositories.ApartmentRepository;
import org.gyula.onlineinvoiceapi.services.AuthenticationService;
import org.gyula.onlineinvoiceapi.services.CustomUserDetailsService;
import org.gyula.onlineinvoiceapi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * UserController is a REST controller that handles various endpoints related to user operations,
 * including retrieving and submitting meter values for apartments. This class provides functionality
 * for managing meter data using different endpoints. The controller uses dependency-injected services
 * for authentication and user-related operations.
 *
 * Annotation Description:
 * - @RestController: Specifies that this class is a RESTful web controller.
 * - @RequestMapping: Binds the controller to base URL "/api/v1/user".
 * - @CrossOrigin: Allows cross-origin requests from the specified origin.
 *
 * Dependencies:
 * - UserService: Provides business logic related to user and apartment data.
 * - AuthenticationService: Handles request validation and authentication logic.
 * - CustomUserDetailsService: Manages custom user-related operations.
 * - ApartmentRepository: Interface for database operations related to apartments.
 *
 * Logging:
 * - A logger is used to track significant events during the execution of requests, such as method calls
 *   and error occurrences.
 *
 * Key Endpoints:
 * - /sendOldMeterValue: Retrieves the old meter value for a specific apartment and meter type.
 * - /submitMeterValue: Submits meter data, including optional file uploads.
 * - /getLastMeterValues: Retrieves the last 12 submitted meter values for a specified apartment and meter type.
 */
@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private static final Logger log = LogManager.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ApartmentRepository apartmentRepository;

    /**
     * Endpoint to send the old meter value for a specific apartment and meter type.
     * Validates the request headers and processes the input values to retrieve and send the latest meter value.
     *
     * @param apiKey the API key provided in the request header for authorization purposes.
     * @param authorizationHeader the authorization token provided in the request header for authentication.
     * @param values a map containing the key-value pairs, where 'meterType' specifies the type of the meter
     *               (e.g., electricity, gas, water) and 'apartmentId' indicates the ID of the apartment.
     * @return ResponseEntity containing a String with the latest meter value if successful, or an error message
     *         if an exception occurs.
     */
    @GetMapping(value = "/sendOldMeterValue")
    public ResponseEntity<String> sendOldMeterValue(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String,String> values)
    {
        log.info("/sendOldMeterValue endpoint called");

        try {
            authenticationService.validateRequest(apiKey, authorizationHeader);

            String latestValue = userService.sendOldMeterValue(values.get("meterType"), Long.valueOf(values.get("apartmentId")));

            log.info("Latest meter value found successfully: {}", latestValue);
            return ResponseEntity.ok(latestValue);

        }catch (Exception e){
            log.error("An error occurred during sending the old value: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during sending data." + e.getMessage());
        }
    }

    /**
     * Handles the submission of a meter value.
     *
     * This endpoint validates the request headers, parses the `values` parameter, and processes an optional file.
     * Supported meter types and their data are managed through this function. A file, if included, is validated for size constraints.
     *
     * @param apiKey the API key provided in the request header for authentication
     * @param authorizationHeader the authorization token included in the request header
     * @param meterType the type of the meter (e.g., electricity, gas, water) as a request parameter
     * @param values the map of key-value pairs representing the meter data to be submitted
     * @param file an optional multipart file containing additional meter data; maximum allowed size is 10 MB
     * @return a ResponseEntity containing a success message with HTTP status `200 OK` when successful, or an error message with the appropriate HTTP status in case of a failure
     */
    @PostMapping(value = "/submitMeterValue")
    public ResponseEntity<String> submitMeterValue(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam String meterType,
            @RequestParam Map<String, String> values,
            @RequestParam(value = "file", required = false) MultipartFile file)
    {
        log.info("/submitMeterValue endpoint called");
        log.info("values:" + values.toString());

        try{
            if (!file.isEmpty()) {
                if (file.getSize() > 10 * 1024 * 1024) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File size exceeds the maximum allowed limit of 10 MB.");
                }
                String contentType = file.getContentType();
                authenticationService.validateRequest(apiKey, authorizationHeader);

            }
        }catch (NullPointerException e){
            log.error("File was not sent.");
            file = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        try {
            authenticationService.validateRequest(apiKey, authorizationHeader);

            ObjectMapper objectMapper = new ObjectMapper();
            // Parse the "values" key if it contains JSON
            String rawJson = values.get("values");
            log.info("loadUserByUsername: {}", rawJson);
            Map<String, String> parsedValues = objectMapper.readValue(rawJson, Map.class);
            userService.addMeterValue(meterType, parsedValues, file);
            log.info("Meter value submitted successfully");

        }catch (Exception e){
            log.error("An error occurred during submitting new values: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.OK).body("Meter value submitted successfully.");
    }


    /**
     * Retrieves the last 12 meter values for a specified apartment and meter type.
     * Validates the request headers for authentication before proceeding.
     *
     * @param apiKey The API key used for authentication.
     * @param authorizationHeader The authorization token used for authentication.
     * @param body A map containing the request payload:
     *             - "apartmentId": The unique identifier of the apartment.
     *             - "meterType": The type of meter (e.g., electricity, water, gas).
     * @return A ResponseEntity containing a map of the last 12 meter values or an error message in case of failure.
     */
    //Get the last 12 meter values from the selected apartment
    @PostMapping(value = "/getLastMeterValues")
    public ResponseEntity<Map<String, String>> getLastMeterValues(
            @RequestHeader("API-KEY") String apiKey,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> body
    ) {
        log.info("/getLastMeterValues endpoint called");

        try {
            authenticationService.validateRequest(apiKey, authorizationHeader);
            String apartmentId = body.get("apartmentId");
            String meterType = body.get("meterType");
            
            Map<String,Object>lastMeterValuesWithImages = userService.sendLastYearMeterValueWithImage(meterType, Long.valueOf(apartmentId));
            lastMeterValuesWithImages.forEach((key, value) -> log.info("Key: {}, Value: {}", key, value));


            Map<String, String> lastMeterValues = userService.sendLastYearMeterValue(meterType, Long.valueOf(apartmentId));
            log.info("Last meter values retrieved successfully");
            return new ResponseEntity<>(lastMeterValues, HttpStatus.OK);
        } catch (Exception e) {
            log.error("An error occurred during getting last 12 meter values: {}", e.getMessage());
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("Error", e.getMessage());
            return new ResponseEntity<>(errorMap, HttpStatus.UNAUTHORIZED);
        }
    }

}
