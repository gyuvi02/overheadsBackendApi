package org.gyula.onlineinvoiceapi.services;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.logging.log4j.Logger;
import org.gyula.onlineinvoiceapi.model.*;
import org.gyula.onlineinvoiceapi.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

    private static final Logger log = LogManager.getLogger(UserService.class);



    private ApartmentRepository apartmentRepository;

    @Autowired
    public void setApartmentRepository(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private GasMeterRepository gasMeterValueRepository;

    @Autowired
    public void setGasMeterValueRepository(GasMeterRepository gasMeterValueRepository) {
        this.gasMeterValueRepository = gasMeterValueRepository;
    }

    private ElectricityMeterRepository electricityMeterValueRepository;

    @Autowired
    public void setElectricityMeterValueRepository(ElectricityMeterRepository electricityMeterValueRepository) {
        this.electricityMeterValueRepository = electricityMeterValueRepository;
    }

    private WaterMeterRepository waterMeterValueRepository;

    @Autowired
    public void setWaterMeterValueRepository(WaterMeterRepository waterMeterValueRepository) {
        this.waterMeterValueRepository = waterMeterValueRepository;
    }


    @Autowired
    private HeatingMeterRepository heatingMeterValueRepository;


    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RateLimiter loginRateLimiter;
//    private final JavaMailSender mailSender;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, RegistrationTokenRepository tokenRepository, JavaMailSender mailSender, ApplicationArguments springApplicationArguments) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
//        this.mailSender = mailSender;
        this.loginRateLimiter = RateLimiter.create(3.0);
    }

    /**
     * Registers a new user in the system based on the provided registration details.
     *
     * @param registerRequest the request containing user registration details such as username, email, password,
     *                        and apartment ID
     * @throws IllegalArgumentException if the username already exists, the provided apartment ID does not exist,
     *                                  or an error occurs during registration
     */
    public void registerUser(RegisterRequest registerRequest) throws IllegalArgumentException {
        log.info("In registerUser");

        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        log.info("In registerUser");

        try {
            User user = new User();            
            user.setEmail(registerRequest.getEmail());
            user.setUsername(registerRequest.getUsername());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));  // Encrypt the password
            user.setEnabled(true);
            Apartment userApartment = apartmentRepository.findById(Long.parseLong(registerRequest.getApartmentId())).orElseThrow(() -> new IllegalArgumentException("Apartment not found with id: " + registerRequest.getApartmentId()));
            user.setApartment(userApartment);

            //Adding USER role as default
            Authority authority = new Authority();
            authority.setUser(user);
            authority.setAuthority("ROLE_USER");
            Set<Authority> authorities = new HashSet<>();
            authorities.add(authority);
            user.setAuthorities(authorities);

            userRepository.save(user);  // Update user with authorities
            log.info("User {} registered successfully.", user.getUsername());
        }catch (Exception e){
            log.error("An error occurred during registration: {}", e.getMessage());
            throw new IllegalArgumentException("An error occurred during registration: " + e.getMessage());
        }

    }

//    public boolean login(String username, String rawPassword) throws IllegalArgumentException {
//        // Fetch the user by username
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new IllegalArgumentException("User not found"));
//        if (!user.isEnabled()) {
//            throw new IllegalArgumentException("User is disabled");
//        }
//        // Check if the raw password matches the encrypted password
//        return passwordEncoder.matches(rawPassword, user.getPassword());  // Authentication successful
//    }

    public boolean isAllowed() {
        return loginRateLimiter.tryAcquire(); // Check if the request can proceed
    }

    /**
     * Adds a new meter value for the specified meter type and associates it with an apartment reference.
     * Depending on the meter type, it updates the respective repository and database table to track the latest meter value.
     *
     * @param meterType the type of meter (e.g., "gas", "electricity", or "water")
     * @param values a map containing the data required for the meter value, including "apartmentId" and "meterValue"
     * @param file an optional file containing additional information (e.g., an image file) associated with the meter value
     * @throws IllegalArgumentException if the `values` map is null or empty, or if the provided meterType is invalid
     * @throws Exception if an error occurs during the operation, such as file processing or database update issues
     */
    public void addMeterValue(String meterType, Map<String, String> values, MultipartFile file, int consumption) throws Exception {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Values map cannot be null or empty");
        }
        byte[] fileContent;
        if(file != null){
            fileContent = file.getBytes();
        }else{
            fileContent = null;
        }

        // Extracting necessary values from the map
        long apartmentReference = Long.parseLong(values.get("apartmentId"));
        Apartment newApartment = apartmentRepository.findById(apartmentReference)
                .orElseThrow(() -> new IllegalArgumentException("Apartment not found for reference: " + values.get("apartmentId")));
        String valueToAdd = values.get("meterValue");

        try {
            String tableName;
            String updateQuery;

            switch (meterType) {
                case "gas":
                    GasMeterValues gasMeterValue = new GasMeterValues();
                    tableName = "gas_meter_values";
                    gasMeterValue.setLatest(true);
                    gasMeterValue.setApartmentReference(newApartment);
                    gasMeterValue.setDateOfRecording(LocalDateTime.now());
                    gasMeterValue.setGasValue(Integer.parseInt(valueToAdd));
                    if (consumption != 0) gasMeterValue.setConsumption(consumption);
                    if(fileContent != null) gasMeterValue.setImageFile(fileContent);
                    gasMeterValueRepository.save(gasMeterValue);
                    //Modify the previous latest value only if the save wass successful
                    updateQuery = "UPDATE " + tableName + " SET latest = false WHERE apartment_reference = " + apartmentReference + " AND id NOT IN (SELECT id FROM " + tableName + " WHERE apartment_reference = " + apartmentReference + " ORDER BY date_of_recording DESC LIMIT 1)";
                    jdbcTemplate.update(updateQuery);
                    break;
                case "electricity":
                    ElectricityMeterValues electricityMeterValue = new ElectricityMeterValues();
                    tableName = "electricity_meter_values";
                    electricityMeterValue.setLatest(true);
                    electricityMeterValue.setApartmentReference(newApartment);
                    electricityMeterValue.setDateOfRecording(LocalDateTime.now());
                    electricityMeterValue.setElectricityValue(Integer.parseInt(valueToAdd));
                    electricityMeterValue.setImageFile(fileContent);
                    if (consumption != 0) electricityMeterValue.setConsumption(consumption);
                    electricityMeterValueRepository.save(electricityMeterValue);
                    //Modify the previous latest value only if the save wass successful
                    updateQuery = "UPDATE " + tableName + " SET latest = false WHERE apartment_reference = " + apartmentReference + " AND id NOT IN (SELECT id FROM " + tableName + " WHERE apartment_reference = " + apartmentReference + " ORDER BY date_of_recording DESC LIMIT 1)";
                    jdbcTemplate.update(updateQuery);
                    break;
                case "water":
                    WaterMeterValues waterMeterValue = new WaterMeterValues();
                    tableName = "water_meter_values";
                    waterMeterValue.setLatest(true);
                    waterMeterValue.setApartmentReference(newApartment);
                    waterMeterValue.setDateOfRecording(LocalDateTime.now());
                    waterMeterValue.setWaterValue(Integer.parseInt(valueToAdd));
                    waterMeterValue.setImageFile(fileContent);
                    if (consumption != 0) waterMeterValue.setConsumption(consumption);
                    waterMeterValueRepository.save(waterMeterValue);
                    //Modify the previous latest value only if the save was successful
                    updateQuery = "UPDATE " + tableName + " SET latest = false WHERE apartment_reference = " + apartmentReference + " AND id NOT IN (SELECT id FROM " + tableName + " WHERE apartment_reference = " + apartmentReference + " ORDER BY date_of_recording DESC LIMIT 1)";
                    jdbcTemplate.update(updateQuery);
                    break;
                case "heating":
                    HeatingMeterValues heatingMeterValue = new HeatingMeterValues();
                    tableName = "heating_meter_values";
                    heatingMeterValue.setLatest(true);
                    heatingMeterValue.setApartmentReference(newApartment);
                    heatingMeterValue.setDateOfRecording(LocalDateTime.now());
                    heatingMeterValue.setHeatingValue(Integer.parseInt(valueToAdd));
                    heatingMeterValue.setImageFile(fileContent);
                    if (consumption != 0) heatingMeterValue.setConsumption(consumption);
                    heatingMeterValueRepository.save(heatingMeterValue);
                    updateQuery = "UPDATE " + tableName + " SET latest = false WHERE apartment_reference = " + apartmentReference + " AND id NOT IN (SELECT id FROM " + tableName + " WHERE apartment_reference = " + apartmentReference + " ORDER BY date_of_recording DESC LIMIT 1)";
                    jdbcTemplate.update(updateQuery);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid meterType: " + meterType);
            }

        }catch (Exception e){
            log.error("An error occurred during meter value submission: {}", e.getMessage());
            throw new Exception("An error occurred during meter value submission." + e.getMessage());

        }
    }

    /**
     * Retrieves the most recent meter value for the specified meter type and apartment ID.
     * The method queries active meter values for the given apartment and returns the corresponding
     * meter value if available.
     *
     * @param meterType the type of meter for which the value is to be retrieved. Valid values are "gas", "electricity", and "water".
     * @param apartmentId the ID of the apartment whose meter value is being requested.
     * @return the most recent meter value as a String for the specified meter type and apartment ID.
     * @throws Exception if the meterType is invalid, no meter value is found for the apartment,
     *                   or if any other error occurs during the retrieval process.
     */
    public String sendOldMeterValue(String meterType, Long apartmentId) throws Exception {

        List<Map<String, Object>> foundMeterValues;

        try{
            return switch (meterType) {
                case "gas" -> {
                    foundMeterValues = apartmentRepository.findActiveGasMeterValues(apartmentId);
                    yield foundMeterValues.get(0).get("gas_value").toString();
                }
                case "electricity" -> {
                    foundMeterValues = apartmentRepository.findActiveElectricityMeterValues(apartmentId);
                    yield foundMeterValues.get(0).get("electricity_value").toString();
                }
                case "water" -> {
                    foundMeterValues = apartmentRepository.findActiveWaterMeterValues(apartmentId);
                    yield foundMeterValues.get(0).get("water_value").toString();
                }
                case "heating" -> {
                    foundMeterValues = apartmentRepository.findActiveHeatingMeterValues(apartmentId);
                    yield foundMeterValues.get(0).get("heating_value").toString();
                }
                default -> throw new Exception("Invalid meterType: " + meterType);
            };
        }catch (IndexOutOfBoundsException e){
            throw new Exception("No meter value found for apartment: " + apartmentId);
        }
    }

    /**
     * Retrieves the last 12 meter values submitted for a specified type of meter
     * (gas, electricity, or water) for a given apartment.
     *
     * @param meterType the type of meter for which the values are retrieved. Acceptable values are "gas", "electricity", and "water".
     * @param apartmentId the unique identifier of the apartment for which the meter values are to be fetched.
     * @return a map where the keys are the dates of recording (in their string representation),
     *         and the values are the corresponding meter readings.
     * @throws Exception if an invalid meter type is provided or if any issue occurs while retrieving the data.
     */
    public Map<String, String> sendLastYearMeterValue(String meterType, Long apartmentId) throws Exception {
        log.info("In sendLastYearMeterValue");
        List<Map<String, Object>> result;
        switch (meterType) {
            case "gas" -> {result = apartmentRepository.findLatestGasMeterValues(apartmentId); break;}
            case "electricity" -> {result = apartmentRepository.findLatestElectricityMeterValues(apartmentId); break;}
            case "water" -> {result = apartmentRepository.findLatestWaterMeterValues(apartmentId); break;}
            case "heating" -> {result = apartmentRepository.findLatestHeatingMeterValues(apartmentId); break;}
            default -> throw new Exception("Invalid meterType: " + meterType);
        }

        Map<String, String> resultMap = new LinkedHashMap<>();
        result.stream()
                .sorted((m1, m2) -> m2.get("date_of_recording").toString().compareTo(m1.get("date_of_recording").toString()))
                .forEach(map -> {
                    resultMap.put(map.get("date_of_recording").toString(), map.get(meterType + "_value").toString());
                });

        result.forEach((r) -> System.out.println("result outcome: " + r.values()));

        return resultMap;
    }

    /**
     * Retrieves the last year's meter values along with associated images for a given apartment and meter type.
     * The data is sorted by the recording date in descending order.
     *
     * @param meterType The type of meter for which to retrieve values. Options are "gas", "electricity", or "water".
     * @param apartmentId The ID of the apartment to fetch meter values for.
     * @return A map containing the recorded meter values and associated data. Keys are formatted as "date_<recording_date>"
     *         and values contain a map with the meter value, recording date, and an optional image file.
     * @throws Exception If an invalid meterType is provided or if an issue occurs during data retrieval.
     */
    public Map<String, Object> sendLastYearMeterValueWithImage(String meterType, Long apartmentId) throws Exception {
        log.info("In sendLastYearMeterValueWithImage");
        List<Map<String, Object>> result = null;
        switch (meterType) {
            case "gas" -> {result = apartmentRepository.findLatestGasMeterValues(apartmentId);}
            case "electricity" -> {result = apartmentRepository.findLatestElectricityMeterValues(apartmentId);}
            case "water" -> {result = apartmentRepository.findLatestWaterMeterValues(apartmentId);}
            case "heating" -> {result = apartmentRepository.findLatestHeatingMeterValues(apartmentId);}
            default -> throw new Exception("Invalid meterType: " + meterType);
        }

        Map<String, Object> resultMap = new LinkedHashMap<>();
        result.stream()
                .sorted((m1, m2) -> m2.get("date_of_recording").toString().compareTo(m1.get("date_of_recording").toString()))
                .forEach(map -> {
                    log.info("Found meter value: {} for date: ", map.get(meterType + "_value"));
                    Map<String, Object> valueMap = new HashMap<>();
                    valueMap.put("value", map.get(meterType + "_value"));
                    valueMap.put("date", map.get("date_of_recording"));
                    if (map.get("image_file") != null) {
                        valueMap.put("image", map.get("image_file"));
                    }else{
                        valueMap.put("image", null);
                    }
                    resultMap.put("date_" + map.get("date_of_recording"), valueMap);
                });

        return resultMap;
    }
}

