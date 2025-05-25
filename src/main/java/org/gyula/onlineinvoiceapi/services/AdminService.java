package org.gyula.onlineinvoiceapi.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.checkerframework.checker.units.qual.C;
import org.gyula.onlineinvoiceapi.config.TokenGenerator;
import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.model.InvoiceItemDto;
import org.gyula.onlineinvoiceapi.model.RegistrationToken;
import org.gyula.onlineinvoiceapi.repositories.*;
import org.gyula.onlineinvoiceapi.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.IOException;



@SuppressWarnings("unused")
@Service("adminService")
public class AdminService {

    private static final Logger log = LogManager.getLogger(AdminService.class);

    @Autowired
    RegistrationTokenRepository tokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailSendAddress;
    @Value("${frontend.address}")
    private String frontendAddress;
    @Autowired
    private GasMeterRepository gasMeterRepository;
    @Autowired
    private ElectricityMeterRepository electricityMeterRepository;

    @Autowired
    private WaterMeterRepository waterMeterRepository;
    @Autowired
    private ApartmentRepository apartmentRepository;
    @Autowired
    private UserRepository userRepository;


    /**
     * Sends a registration email to the specified user, containing a unique token for completing the registration process.
     * The method generates a registration token, saves it to the database, constructs a verification link,
     * and sends an email with the link and expiration details.
     *
     * @param userEmail the email address of the user to whom the registration email will be sent
     * @return the registration link containing the unique token
     * @throws Exception if an error occurs during token generation, database save, or email sending
     */
    public String sendRegistrationEmail(String userEmail, String apartmentName, String apartmentId) throws Exception{

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
            link = "https://" + frontendAddress + "/registerMe?token=" + token + "&ap=" + apartmentId;
            // Construct and send the email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userEmail);
            message.setSubject("Complete Your Registration for the apartment: " + apartmentName + "");
            message.setText("Click the link to complete your registration: " + link + "\n\nThe link expires in 24 hours (" + LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")) + ")");
            message.setFrom(mailSendAddress);
            log.info("Sending email from: {}", mailSendAddress);
            mailSender.send(message);

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

    /**
     * Retrieves the latest gas, electricity, and water meter values along with their respective images
     * for a given apartment.
     *
     * @param apartmentId the ID of the apartment for which the latest meter values and images are to be retrieved
     * @return a map containing the latest meter values (gas, electricity, water) and their corresponding image file paths.
     * The keys in the map include "gas", "gas_image", "electricity", "electricity_image", "water", and "water_image"
     * @throws Exception if the apartment is not found or any error occurs during the retrieval of values
     */
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

    public void sendReminderEmail(Apartment apartment) {
        User user = userRepository.findByApartmentId(apartment.getId());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Remainder: submit meter values");
        message.setText("If you have not already done so, please record your meter readings on https://omegahouses.org website for your apartment " + apartment.getCity() + ", " + apartment.getStreet() + " in the next 48 hours");
        message.setFrom(mailSendAddress);{}
        log.info("Sending email from: {}", mailSendAddress + " to " + user.getEmail());
        mailSender.send(message);
    }


    public String createInvoicePdf(InvoiceItemDto invoiceData) throws RuntimeException{
        String[] gasRow;
        String[] electricityRow;
        String[] waterRow;
        List<String[]> rowList = new ArrayList<>();
        List<String[]> otherCostsList = new ArrayList<>();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Set starting Y position and margins
            float yStart = page.getMediaBox().getHeight() - 70;
            float margin = 50;
            float width = page.getMediaBox().getWidth() - 2 * margin;

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(invoiceData.getApartmentAddress()) / 1000 * 16;
            float xAddress = (page.getMediaBox().getWidth() - titleWidth) / 2;

            contentStream.beginText();
            contentStream.newLineAtOffset(xAddress, yStart);
            contentStream.showText(invoiceData.getApartmentAddress());
            contentStream.endText();

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            float warningWidth = PDType1Font.HELVETICA_BOLD.getStringWidth("Not an official invoice!") / 1000 * 12;
            float xWarning = (page.getMediaBox().getWidth() - warningWidth) / 2;
            yStart -= 30;
            contentStream.beginText();
            contentStream.newLineAtOffset(xWarning, yStart);
            contentStream.showText("Not an official invoice!");
            contentStream.endText();


            // Space before table
            float tableY = yStart - 40;

            // Meter values table (Gas, Electricity, Water)
            String[] meterHeaders = { "Type", "Previous value", "Current value", "Consumption", "Sum" };
            if (invoiceData.getActualGas() != null && !invoiceData.getActualGas().equals("0")) rowList.add(new String[] {"Gas", invoiceData.getPreviousGas(), invoiceData.getActualGas(), String.valueOf(Integer.parseInt(invoiceData.getActualGas()) - Integer.parseInt(invoiceData.getPreviousGas())), invoiceData.getGasCost()});

            if (invoiceData.getActualElectricity() != null && !invoiceData.getActualElectricity().equals("0")) rowList.add(new String[] { "Electricity", invoiceData.getPreviousElectricity(), invoiceData.getActualElectricity(), String.valueOf(Integer.parseInt(invoiceData.getActualElectricity()) - Integer.parseInt(invoiceData.getPreviousElectricity())), invoiceData.getElectricityCost()});

            if (invoiceData.getActualWater() != null && !invoiceData.getActualWater().equals("0")) rowList.add(new String[] { "Water", invoiceData.getPreviousWater(), invoiceData.getActualWater(), String.valueOf(Integer.parseInt(invoiceData.getActualWater()) - Integer.parseInt(invoiceData.getPreviousWater())), invoiceData.getWaterCost()});

            String[][] meterRows = rowList.toArray(new String[rowList.size()][]);

            // Draw meter table
            tableY = drawTable(document, page, contentStream, margin, tableY, meterHeaders, meterRows);

            // Other data table (e.g., Service charges, Misc fees, etc.)
            String[] otherHeaders = { "Item", "Amount" };
            otherCostsList.add(new String[] {"Rent", invoiceData.getRent()});
            if (invoiceData.getCleaning() != null && !invoiceData.getCleaning().equals("0")) otherCostsList.add(new String[] {"Cleaning", invoiceData.getCleaning()});
            if (invoiceData.getCommonCost() != null && !invoiceData.getCommonCost().equals("0")) otherCostsList.add(new String[] {"Common cost", invoiceData.getCommonCost()});
            if (invoiceData.getOtherSum() != null && !invoiceData.getOtherSum().equals("0")) otherCostsList.add(new String[] {invoiceData.getOtherText(), invoiceData.getOtherSum()});

            String[][] otherRows = otherCostsList.toArray(new String[otherCostsList.size()][]);

            tableY -= 30; // Space between tables

            tableY = drawTable(document, page, contentStream, margin, tableY, otherHeaders, otherRows);

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            String totalLine = "Total Sum: " + invoiceData.getTotalSum();
            float totalWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(totalLine) / 1000 * 16;
            float xTotal = page.getMediaBox().getWidth() - margin - totalWidth;
            contentStream.beginText();
            contentStream.newLineAtOffset(xTotal, (tableY - 50));
            contentStream.showText(totalLine);
            contentStream.endText();

            contentStream.close();
            String fileName = "Rent_" + invoiceData.getApartmentAddress() + "_" + LocalDate.now()+ ".pdf";
            int commaIndex = invoiceData.getApartmentAddress().indexOf(",");
            String addressCity = invoiceData.getApartmentAddress().substring(0, commaIndex);


            Files.createDirectories(Paths.get("C:\\Users\\Szabó Gyula\\Downloads\\" + LocalDateTime.now().getYear() + "\\" + addressCity));
            document.save("C:\\Users\\Szabó Gyula\\Downloads\\" + LocalDateTime.now().getYear() + "\\" + addressCity + "\\" +  fileName);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            document.close();
            InputStream is = new ByteArrayInputStream(out.toByteArray());
            byte[] pdfArray = is.readAllBytes();
            return Base64.getEncoder().encodeToString(pdfArray);

        } catch (IOException e) {
            log.error("Error creating invoice PDF: {}", e.getMessage());
            throw new RuntimeException("Error creating invoice PDF: " + e.getMessage());
        }
    }

    // Supporting method for drawing a simple table
    private float drawTable(PDDocument doc, PDPage page, PDPageContentStream cs, float x, float y,
                            String[] headers, String[][] content) throws IOException {
        float rowHeight = 22f;
        float tableWidth = page.getMediaBox().getWidth() - 1 * x;
        float colWidth = tableWidth / headers.length;
        float cellMargin = 3f;

        // Draw headers
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        for (int i = 0; i < headers.length; ++i) {
            cs.beginText();
            cs.newLineAtOffset(x + i * colWidth + cellMargin, y);
            cs.showText(headers[i]);
            cs.endText();
        }
        y -= rowHeight;

        // Draw rows
        cs.setFont(PDType1Font.HELVETICA, 12);
        for (String[] row : content) {
            for (int i = 0; i < row.length; ++i) {
                cs.beginText();
                cs.newLineAtOffset(x + i * colWidth + cellMargin, y);
                cs.showText(row[i] != null ? row[i] : "");
                cs.endText();
            }
            y -= rowHeight;
        }
        return y;
    }


}
