package org.gyula.onlineinvoiceapi.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gyula.onlineinvoiceapi.config.TokenGenerator;
import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.RegistrationToken;
import org.gyula.onlineinvoiceapi.repositories.*;
import org.hibernate.validator.internal.util.privilegedactions.GetAnnotationAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class responsible for managing administrative functionalities such as
 * sending registration emails to users and retrieving the latest utility meter values.
 */
@SuppressWarnings("unused")
@Service("adminService")
public class AdminService {

    private static final Logger log = LogManager.getLogger(AdminService.class);

    @Autowired
    RegistrationTokenRepository tokenRepository;

    @Value("${spring.mail.username}")
    private String mailSendAddress;
    @Autowired
    private GasMeterRepository gasMeterRepository;
    @Autowired
    private ElectricityMeterRepository electricityMeterRepository;

    @Autowired
    private WaterMeterRepository waterMeterRepository;
    @Autowired
    private ApartmentRepository apartmentRepository;


    /**
     * Sends a registration email to the specified user, containing a unique token for completing the registration process.
     * The method generates a registration token, saves it to the database, constructs a verification link,
     * and sends an email with the link and expiration details.
     *
     * @param userEmail the email address of the user to whom the registration email will be sent
     * @return the registration link containing the unique token
     * @throws Exception if an error occurs during token generation, database save, or email sending
     */
    public String sendRegistrationEmail(String userEmail) throws Exception{

        log.info("In sendRegistrationEmail: {}", userEmail);

        String link;

        try {
            String token = TokenGenerator.generateToken();
            RegistrationToken registrationToken = new RegistrationToken();
            registrationToken.setToken(token);
            registrationToken.setIsUsed(false);
            registrationToken.setExpiration(new Timestamp(System.currentTimeMillis() + (24 * 60 * 60 * 1000))); // Set expiration time
            registrationToken.setUserEmail(userEmail);

            tokenRepository.save(registrationToken);

            //This link directs to a registration website, and that website will call the API with the token
            link = "http://www.valamiweboldal.com/api/v1/register?token=" + token;

            // Construct and send the email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userEmail);
            message.setSubject("Complete Your Registration");
            message.setText("Click the link to complete your registration: " + link + "\nExpiration: " + LocalDateTime.now().plusDays(1));
            message.setFrom(mailSendAddress);
//            mailSender.send(message);

            log.info("Email sent successfully, link attached:" + link);

        }catch (Exception e){
            log.error("An error occurred during email sending: {}", e.getMessage());
            throw e;
        }
        return link;
    }


    /**
     * Retrieves the latest utility meter values for a specific apartment.
     * The method queries the database for the most recent gas, electricity, and water meter values
     * for the given apartment ID and returns them as a mapping of utility types to their respective values.
     *
     * @param apartmentId the ID of the apartment for which the utility meter values should be retrieved
     * @return a map where the keys are the utility types ("gas", "electricity", "water") and the values are the latest recorded meter values
     * @throws Exception if an error occurs while retrieving the apartment or the meter values
     */
    public Map<String, Object> getAllLatestValues(Long apartmentId) throws Exception {
        log.info("In getAllLatestValues: {}", apartmentId);
        try {
            Map<String, Object> latestValues = new HashMap<>();
            Apartment apartment = apartmentRepository.findById(apartmentId)
                    .orElseThrow(() -> new Exception("Apartment not found with id: " + apartmentId));

            log.info("Apartment found: {}", apartment.getId());

            List<Map<String, Object>> gasValues = apartmentRepository.findActiveGasMeterValues(apartmentId);
            List<Map<String, Object>> electricityValues = apartmentRepository.findActiveElectricityMeterValues(apartmentId);
            List<Map<String, Object>> waterValues = apartmentRepository.findActiveWaterMeterValues(apartmentId);
            log.info("Found {} gas values, {} electricity values, and {} water values", gasValues.size(), electricityValues.size(), waterValues.size());

            if (!gasValues.isEmpty()) {
                latestValues.put("gas", ((Number) gasValues.get(0).get("gas_value")).intValue());
            }
            if (!electricityValues.isEmpty()) {
                latestValues.put("electricity", ((Number) electricityValues.get(0).get("electricity_value")).intValue());
            }
            if (!waterValues.isEmpty()) {
                log.info(" water value");
                latestValues.put("water", ((Number) waterValues.get(0).get("water_value")).intValue());
            }

            return latestValues;

        } catch (Exception e) {
            log.error("Error retrieving latest values for apartment {}: {}", apartmentId, e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> getAllLatestValuesWithImages(Long apartmentId) throws Exception {
        log.info("In getAllLatestValuesWithImages: {}", apartmentId);
        try {
            Map<String, Object> latestValues = new HashMap<>();

            Apartment apartment = apartmentRepository.findById(apartmentId)
                    .orElseThrow(() -> new Exception("Apartment not found with id: " + apartmentId));

            log.info("Apartment found: {}", apartment.getId());

            List<Map<String, Object>> gasValues = apartmentRepository.findActiveGasMeterValues(apartmentId);
            List<Map<String, Object>> electricityValues = apartmentRepository.findActiveElectricityMeterValues(apartmentId);
            List<Map<String, Object>> waterValues = apartmentRepository.findActiveWaterMeterValues(apartmentId);
            log.info("Found {} gas values, {} electricity values, and {} water values", gasValues.size(), electricityValues.size(), waterValues.size());

            if (!gasValues.isEmpty()) {
                Map<String, Object> gasRow = gasValues.get(0);
                if (gasRow.get("gas_value") != null) {
                    latestValues.put("gas", ((Number) gasRow.get("gas_value")).intValue());
                }
                if (gasRow.get("image_file") != null) {
                    latestValues.put("gas_image", gasRow.get("image_file"));
                }
            }
            if (!electricityValues.isEmpty()) {
                Map<String, Object> elecRow = electricityValues.get(0);
                if (elecRow.get("electricity_value") != null) {
                    latestValues.put("electricity", ((Number) elecRow.get("electricity_value")).intValue());
                }
                if (elecRow.get("image_file") != null) {
                    latestValues.put("electricity_image", elecRow.get("image_file"));
                }
            }
            if (!waterValues.isEmpty()) {
                Map<String, Object> waterRow = waterValues.get(0);
                if (waterRow.get("water_value") != null) {
                    latestValues.put("water", ((Number) waterRow.get("water_value")).intValue());
                }
                if (waterRow.get("image_file") != null) {
                    latestValues.put("water_image", waterRow.get("image_file"));
                }
            }

            return latestValues;

        } catch (Exception e) {
            log.error("Error retrieving latest values with images for apartment {}: {}", apartmentId, e.getMessage());
            throw e;
        }
    }


}
