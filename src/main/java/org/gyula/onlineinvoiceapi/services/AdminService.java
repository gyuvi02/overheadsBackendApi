package org.gyula.onlineinvoiceapi.services;

import jakarta.mail.internet.MimeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


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

    @Value("${file.create.folder}")
    private String createFolder;

    @Value("${arial.file}")
    private String arialTtfFile;

    @Value("${arial.bold.file}")
    private String arialBoldTtfFile;

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
    @Autowired
    private UserService userService;


    /**
     * Sends a registration email to the specified user, containing a unique token for completing the registration process.
     * The method generates a registration token, saves it to the database, constructs a verification link,
     * and sends an email with the link and expiration details.
     *
     * @param userEmail the email address of the user to whom the registration email will be sent
     * @return the registration link containing the unique token
     * @throws Exception if an error occurs during token generation, database save, or email sending
     */
    public String sendRegistrationEmail(String userEmail, String apartmentName, String apartmentId, String apartmentLanguage) throws Exception{

        log.info("In sendRegistrationEmail: {}", userEmail);

        log.info("apartmentLanguage: {}", apartmentLanguage);

        boolean isHungarian = "h".equals(apartmentLanguage);

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
            String subjectText = isHungarian ? "Véglegesítse az albérlet regisztrációt: " : "Complete Your Registration for the apartment: ";
            String emailText = isHungarian ? "Erre a linkre kattintva véglegesítheti a regisztrációt: " + link + "\n\nThe link expires in 24 hours (" + LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")) + ")" + "\n\nÜdvözlettel:\nIldikó Gerő" : "Click the link to complete your registration: " + link + "\n\nThe link expires in 24 hours (" + LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")) + ")" + "\n\nBest regards,\nIldikó Gerő";

            // Construct and send the email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userEmail);
            message.setSubject(subjectText + apartmentName);
            message.setText(emailText);
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

    public String sendEmailPdf(String userEmail, String apartmentAddress, String language, String pdfFile) throws Exception{

        log.info("In sendEmailPdf: {}", userEmail);

        boolean isHungarian = "h".equals(language);
        String subjectText = isHungarian ? "Összefoglaló a költségekről: " : "Cost summary: ";
        String emailText = isHungarian ? "Csatolva küldöm a pdf fájlt, ami tartalmazza a havi költségeket összefoglalva.\n\nÜdvözlettel:\nGerő Ildikó" : "Please check the attached pdf file with the summary of the monthly costs.\n\nBest regards,\nIldikó Gerő";
        
        String link;

        try {
            // Construct and send the email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(userEmail);
            helper.setSubject(subjectText + apartmentAddress);
            helper.setText(emailText);
            helper.setFrom(mailSendAddress);

            byte[] pdfBytes = Base64.getDecoder().decode(pdfFile);
            ByteArrayInputStream bis = new ByteArrayInputStream(pdfBytes);
            String fileName = apartmentAddress + "summary.pdf";
            helper.addAttachment(fileName, new ByteArrayResource(pdfBytes));

            mailSender.send(mimeMessage);

            log.info("Email sent to {}", userEmail);

        }catch (Exception e){
            log.error("An error occurred during email sending: {}", e.getMessage());
            throw e;
        }
        return ("Email sent to " + userEmail);
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

        boolean isHungarian = "h".equals(apartment.getLanguage());
        String subjectText = isHungarian ? "Emlékeztető mérőóra diktálásra" : "Remainder: submit meter values";
        String emailText = isHungarian ? "Ha eddig még nem tette meg, kérem diktálja be a mérőóra állásokat a https://omegahouses.org oladalon a következő lakáshoz: " + apartment.getCity() + ", " + apartment.getStreet() + " a következő 48 órában.\n\nÜdvözlettel:\nGerő Ildikó"  : "If you have not already done so, please record your meter readings on https://omegahouses.org website for your apartment: " + apartment.getCity() + ", " + apartment.getStreet() + " in the next 48 hours. \n\nBest regards,\nIldikó Gerő";


        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject(subjectText);
            message.setText(emailText);
            message.setFrom(mailSendAddress);
            mailSender.send(message);
            log.info("Sending email from: {}", mailSendAddress + " to " + user.getEmail());
        } catch (MailException e) {
            log.error("Error sending reminding email: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String createInvoicePdf(InvoiceItemDto invoiceData) throws RuntimeException{
        String[] gasRow;
        String[] electricityRow;
        String[] waterRow;
        List<String[]> rowList = new ArrayList<>();
        List<String[]> otherCostsList = new ArrayList<>();

        // Language-specific text mappings
        String language = invoiceData.getLanguage();
        boolean isHungarian = "h".equals(language);

        // Text translations
        String notOfficialInvoice = isHungarian ? "Nem hivatalos számla!" : "Not an official invoice!";
        String typeHeader = isHungarian ? "Típus" : "Type";
        String previousValueHeader = isHungarian ? "Előző érték" : "Previous value";
        String currentValueHeader = isHungarian ? "Jelenlegi érték" : "Current value";
        String consumptionHeader = isHungarian ? "Fogyasztás" : "Consumption";
        String sumHeader = isHungarian ? "Összeg" : "Sum";
        String itemHeader = isHungarian ? "Tétel" : "Item";
        String amountHeader = isHungarian ? "Összeg" : "Amount";
        String gasText = isHungarian ? "Gáz" : "Gas";
        String electricityText = isHungarian ? "Villany" : "Electricity";
        String waterText = isHungarian ? "Víz" : "Water";
        String rentText = isHungarian ? "Bérleti díj" : "Rent";
        String cleaningText = isHungarian ? "Takarítás" : "Cleaning";
        String maintenanceFeeText = isHungarian ? "Közös költség" : "Maintenance fee";
        String totalSumText = isHungarian ? "Végösszeg: " : "Total Sum: ";

        try (PDDocument document = new PDDocument()) {
            PDType0Font boldFont = PDType0Font.load(document, new File(arialBoldTtfFile));
            PDType0Font regularFont = PDType0Font.load(document, new File(arialTtfFile));
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            // Set starting Y position and margins
            float yStart = page.getMediaBox().getHeight() - 70;
            float margin = 50;
            float width = page.getMediaBox().getWidth() - 2 * margin;
            contentStream.setFont(regularFont, 16);
            float titleWidth = boldFont.getStringWidth(invoiceData.getApartmentAddress()) / 1000 * 16;
            float xAddress = (page.getMediaBox().getWidth() - titleWidth) / 2;
            contentStream.beginText();
            contentStream.newLineAtOffset(xAddress, yStart);
            contentStream.showText(invoiceData.getApartmentAddress());
            contentStream.endText();
            contentStream.setFont(regularFont, 12);
            float warningWidth = boldFont.getStringWidth(notOfficialInvoice) / 1000 * 12;
            float xWarning = (page.getMediaBox().getWidth() - warningWidth) / 2;
            yStart -= 30;
            contentStream.beginText();
            contentStream.newLineAtOffset(xWarning, yStart);
            contentStream.showText(notOfficialInvoice);
            contentStream.endText();

            // Space before table
            float tableY = yStart - 40;
            // Meter values table (Gas, Electricity, Water)
            String[] meterHeaders = { typeHeader, previousValueHeader, currentValueHeader, consumptionHeader, sumHeader };
            if (invoiceData.getActualGas() != null && !invoiceData.getActualGas().equals("0")) {
                int gasConsumption = Integer.parseInt(invoiceData.getActualGas()) - Integer.parseInt(invoiceData.getPreviousGas());
                rowList.add(new String[] {
                        gasText,
                        invoiceData.getPreviousGas(),
                        invoiceData.getActualGas(),
                        formatNumber(String.valueOf(gasConsumption)),
                        formatNumber(invoiceData.getGasCost()) + " HUF"
                });
            }
            if (invoiceData.getActualElectricity() != null && !invoiceData.getActualElectricity().equals("0")) {
                int electricityConsumption = Integer.parseInt(invoiceData.getActualElectricity()) - Integer.parseInt(invoiceData.getPreviousElectricity());
                rowList.add(new String[] {
                        electricityText,
                        invoiceData.getPreviousElectricity(),
                        invoiceData.getActualElectricity(),
                        formatNumber(String.valueOf(electricityConsumption)),
                        formatNumber(invoiceData.getElectricityCost()) + " HUF"
                });
            }
            if (invoiceData.getActualWater() != null && !invoiceData.getActualWater().equals("0")) {
                int waterConsumption = Integer.parseInt(invoiceData.getActualWater()) - Integer.parseInt(invoiceData.getPreviousWater());
                rowList.add(new String[] {
                        waterText,
                        invoiceData.getPreviousWater(),
                        invoiceData.getActualWater(),
                        formatNumber(String.valueOf(waterConsumption)),
                        formatNumber(invoiceData.getWaterCost()) + " HUF"
                });
            }
            String[][] meterRows = rowList.toArray(new String[rowList.size()][]);
            // Draw meter table
            tableY = drawTable(document, page, contentStream, margin, tableY, meterHeaders, meterRows);
            // Other data table (e.g., Service charges, Misc fees, etc.)
            String[] otherHeaders = { itemHeader, amountHeader };
            otherCostsList.add(new String[] {rentText, formatNumber(invoiceData.getRent()) + " HUF"});
            if (invoiceData.getCleaning() != null && !invoiceData.getCleaning().equals("0")) {
                otherCostsList.add(new String[] {cleaningText, formatNumber(invoiceData.getCleaning()) + " HUF"});
            }
            if (invoiceData.getCommonCost() != null && !invoiceData.getCommonCost().equals("0")) {
                otherCostsList.add(new String[] {maintenanceFeeText, formatNumber(invoiceData.getCommonCost()) + " HUF"});
            }
            if (invoiceData.getOtherSum() != null && !invoiceData.getOtherSum().equals("0")) {
                otherCostsList.add(new String[] {invoiceData.getOtherText(), formatNumber(invoiceData.getOtherSum()) + " HUF"});
            }
            String[][] otherRows = otherCostsList.toArray(new String[otherCostsList.size()][]);
            tableY -= 30; // Space between tables
            tableY = drawTable(document, page, contentStream, margin, tableY, otherHeaders, otherRows);
            contentStream.setFont(boldFont, 16);
            String totalLine = totalSumText + formatNumber(invoiceData.getTotalSum()) + " HUF";
            float totalWidth = boldFont.getStringWidth(totalLine) / 1000 * 16;
            float xTotal = page.getMediaBox().getWidth() - margin - totalWidth;
            contentStream.beginText();
            contentStream.newLineAtOffset(xTotal, (tableY - 50));
            contentStream.showText(totalLine);
            contentStream.endText();
            contentStream.close();
            String fileName = "Rent_" + invoiceData.getApartmentAddress() + "_" + LocalDate.now()+ ".pdf";
            int commaIndex = invoiceData.getApartmentAddress().indexOf(",");
            String addressCity = invoiceData.getApartmentAddress().substring(0, commaIndex);
            Files.createDirectories(Paths.get(createFolder + LocalDateTime.now().getYear() + "/" + addressCity));
            document.save(createFolder + LocalDateTime.now().getYear() + "/" + addressCity + "/" + fileName);
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

        PDType0Font boldFont = PDType0Font.load(doc, new File(arialBoldTtfFile));
        PDType0Font regularFont = PDType0Font.load(doc, new File(arialTtfFile));
        float rowHeight = 22f;
        float tableWidth = page.getMediaBox().getWidth() - 1 * x;
        float colWidth = tableWidth / headers.length;
        float cellMargin = 3f;

        // Draw headers
        cs.setFont(boldFont, 13);
        for (int i = 0; i < headers.length; ++i) {
            cs.beginText();
            cs.newLineAtOffset(x + i * colWidth + cellMargin, y);
            cs.showText(headers[i]);
            cs.endText();
        }
        y -= rowHeight;

        // Draw rows
        cs.setFont(regularFont, 12);
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

    private String formatNumber(String numberStr) {
        if (numberStr == null || numberStr.trim().isEmpty()) {
            return numberStr;
        }

        try {
            // Parse the number and format it with thousand separators
            long number = Long.parseLong(numberStr.trim());
            return String.format("%,d", number).replace(",", " ");
        } catch (NumberFormatException e) {
            // If parsing fails, return the original string
            return numberStr;
        }
    }


    private Map<String, String> getLast2Entries(Map<String, String> sortedMap, String meterType, Long apartmentId) {
        int unitPrice = 0;
        int consumption = 0;

        if (sortedMap.size() < 2){
            sortedMap.put("", "0"); //There must be 3 values in the Map as the frontend uses all 3 of them
            sortedMap.put(meterType, "0");
            log.info("Only one entry for {}: {}", meterType, sortedMap.get(sortedMap.keySet().iterator().next()));
            log.info("Returning sortedMap: {}", sortedMap);
            return sortedMap;
        }

        switch (meterType) {
            case "gas": unitPrice = apartmentRepository.findGasUnitPrice(apartmentId); break;
            case "electricity": unitPrice = apartmentRepository.findElectricityUnitPrice(apartmentId); break;
            case "water": unitPrice = apartmentRepository.findWaterUnitPrice(apartmentId); break;
        }
        log.info("unitPrice: {}", unitPrice);
        Map<String, String> result = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(sortedMap.keySet());
        for (int i = 0; i < 2; i++) {
            String key = keys.get(i);
            result.put(key, sortedMap.get(key));
        }
        consumption = Integer.parseInt(result.get(keys.get(0))) - Integer.parseInt(result.get(keys.get(1)));
        log.info("Last 2 entries for {}: {} - {} = {}, unit price: {}", meterType, result.get(keys.get(0)), result.get(keys.get(1)), consumption, unitPrice);
        result.put(meterType, String.valueOf((consumption*unitPrice)/100));
        log.info("Returning cost for {}: {}", meterType, (consumption*unitPrice)/100);
        return result;
    }

    public Map<String, Map<String, String>> getLast2MeterValues(Long apartmentId) {
        log.info("In getLast2MeterValues: {}", apartmentId);

        //Checking which meter values exist for this apartment
        List<Map<String, Object>> gasValues = apartmentRepository.findActiveGasMeterValues(apartmentId);
        List<Map<String, Object>> electricityValues = apartmentRepository.findActiveElectricityMeterValues(apartmentId);
        List<Map<String, Object>> waterValues = apartmentRepository.findActiveWaterMeterValues(apartmentId);

        Map<String, Map<String, String>> meterValues = new HashMap<>();
        try {
            if (!gasValues.isEmpty()) {
                Map<String, String> last2Gas = getLast2Entries(userService.sendLastYearMeterValue("gas", apartmentId), "gas", apartmentId);
                meterValues.put("gas", last2Gas);
            }
            if (!electricityValues.isEmpty()) {
                Map<String, String> last2Electricity = getLast2Entries(userService.sendLastYearMeterValue("electricity", apartmentId), "electricity", apartmentId);
                meterValues.put("electricity", last2Electricity);
            }
            if (!waterValues.isEmpty()) {
                Map<String, String> last2Water = getLast2Entries(userService.sendLastYearMeterValue("water", apartmentId), "water", apartmentId);
                meterValues.put("water", last2Water);
            }
            return meterValues;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
