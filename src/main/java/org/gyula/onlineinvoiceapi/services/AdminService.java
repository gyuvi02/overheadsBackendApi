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

    /**
     * Sends an email containing a PDF attachment with a summary of monthly costs to the specified recipient.
     *
     * @param userEmail the email address of the user to whom the email will be sent
     * @param apartmentAddress the address of the apartment, used in the email subject and attached file name
     * @param language the language code (e.g., "h" for Hungarian) to determine the email content language
     * @param pdfFile the Base64 encoded string representing the PDF file to be attached to the email
     * @return a string message confirming the email has been sent to the specified userEmail
     * @throws Exception if an error occurs during email creation or sending
     */
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
            List<Map<String, Object>> heatingValues = apartmentRepository.findActiveHeatingMeterValues(apartmentId);
            log.info("Found {} gas values, {} electricity values, {} water values and {} heating values", gasValues.size(), electricityValues.size(), waterValues.size(), heatingValues.size());

            if (!gasValues.isEmpty()) {
                latestValues.put("gas", ((Number) gasValues.get(0).get("gas_value")).intValue());
            }
            if (!electricityValues.isEmpty()) {
                latestValues.put("electricity", ((Number) electricityValues.get(0).get("electricity_value")).intValue());
            }
            if (!waterValues.isEmpty()) {
                log.info(" water value");
                latestValues.put("water", ((Number) waterValues.get(0).get("water_value")).intValue());
            }if (!heatingValues.isEmpty()) {
                log.info(" heating value");
                latestValues.put("heating", ((Number) heatingValues.get(0).get("heating_value")).intValue());
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
            List<Map<String, Object>> heatingValues = apartmentRepository.findActiveHeatingMeterValues(apartmentId);
            log.info("Found {} gas values, {} electricity values, {} water values and {} heating values", gasValues.size(), electricityValues.size(), waterValues.size(), heatingValues.size());

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

            if (!heatingValues.isEmpty()) {
                Map<String, Object> heatingRow = heatingValues.get(0);
                if (heatingRow.get("heating_value") != null) {
                    latestValues.put("heating", ((Number) heatingRow.get("heating_value")).intValue());
                }
                if (heatingRow.get("image_file") != null) {
                    latestValues.put("heating_image", heatingRow.get("image_file"));
                }
            }

            return latestValues;

        } catch (Exception e) {
            log.error("Error retrieving latest values with images for apartment {}: {}", apartmentId, e.getMessage());
            throw e;
        }
    }

    /**
     * Sends a reminder email to the user associated with the specified apartment,
     * prompting them to submit their meter readings. The email's content and language
     * are determined based on the apartment's language setting.
     *
     * @param apartment the apartment object containing details such as the associated language
     *                  and address, which are used to personalize the email.
     */
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

    /**
     * Sends a submission report for a meter reading via email. The method retrieves the apartment
     * details based on the provided apartment ID, formats the email subject and text based on
     * the apartment's language preference, and sends the email.
     *
     * @param meterType the type of the meter for which the value is being submitted
     * @param values a map containing the submission data, specifically:
     *               - "apartmentId": the identifier of the apartment (as a String) for which the submission is made
     *               - "meterValue": the submitted value for the specified meter as a String
     */
    public void sendSubmitReport(String meterType, Map<String, String> values) {

        long apartmentReference = Long.parseLong(values.get("apartmentId"));
        Apartment newApartment = apartmentRepository.findById(apartmentReference)
                .orElseThrow(() -> new IllegalArgumentException("Apartment not found for reference: " + values.get("apartmentId")));
        String valueToAdd = values.get("meterValue");

        boolean isHungarian = "h".equals(newApartment.getLanguage());
        String subjectText = isHungarian ? "Mérő állás bediktálva" : "Meter value submitted";
        String emailText = isHungarian ? "Új mérőállást diktáltak be a következő ingatlanhoz: " + newApartment.getCity() + ", " + newApartment.getStreet() + "\nAz új mérőállás: " + valueToAdd + " a következő mérőóránál: " + meterType
                : "A new meter reading was submitted for the following property: " + newApartment.getCity() + ", " + newApartment.getStreet() +  "\nThe new meter value: " + valueToAdd + " for the meter: " + meterType;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("omegahouses.mail@gmail.com");
            message.setSubject(subjectText);
            message.setText(emailText);
            message.setFrom(mailSendAddress);
            mailSender.send(message);
            log.info("Sending email from: {}", mailSendAddress + " to omegahouses.mail@gmail.com as a reminder");
        } catch (MailException e) {
            log.error("Error sending reminding email: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a PDF "invoice" based on the provided invoice data.
     * This method creates and formats the invoice content, including
     * utility consumption tables and additional cost items, then returns
     * the file path of the generated PDF file.
     *
     * @param invoiceData an {@code InvoiceItemDto} object containing
     *                    essential information such as utility readings,
     *                    costs, and additional charges required to create
     *                    the invoice.
     * @return a {@code String} representing the file path of the created
     *         PDF invoice.
     * @throws RuntimeException if an error occurs during the PDF creation
     *                          process, such as file access or data processing issues.
     */
    public String createInvoicePdf(InvoiceItemDto invoiceData) throws RuntimeException{
        String[] gasRow;
        String[] electricityRow;
        String[] waterRow;
        String[] heatingRow;
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
        String heatingText = isHungarian ? "Fűtés" : "Heating";
        String rentText = isHungarian ? "Bérleti díj" : "Rent";
        String cleaningText = isHungarian ? "Takarítás" : "Cleaning";
        String maintenanceFeeText = isHungarian ? "Közös költség" : "Maintenance fee";
        String totalSumText = isHungarian ? "Végösszeg: " : "Total Sum: ";
        String oldGasMeterConsumption = isHungarian ? "Fogyasztás a korábbi gázóra alapján" : "Gas consumption based on previous gas meter";
        String oldElectricityMeterConsumption = isHungarian ? "Fogyasztás a korábbi villanyóra alapján" : "Electricity consumption based on previous electricity meter";
        String oldWaterMeterConsumption = isHungarian ? "Fogyasztás a korábbi vízóra alapján" : "Water consumption based on previous water meter";
        String oldHeatingMeterConsumption = isHungarian ? "Fogyasztás a korábbi fűtés mérő alapján" : "Consumption based on previous heating meter";

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
            // Meter values table (Gas, Electricity, Water, Heating)
            String[] meterHeaders = { typeHeader, previousValueHeader, currentValueHeader, consumptionHeader, sumHeader };
            if (invoiceData.getActualGas() != null && !invoiceData.getActualGas().equals("0")) {
                int gasConsumption = Integer.parseInt(invoiceData.getActualGas()) - Integer.parseInt(invoiceData.getPreviousGas());
                if (invoiceData.getGasNewMeterConsumption() != null && !invoiceData.getGasNewMeterConsumption().equals("0")) rowList.add(new String[] {oldGasMeterConsumption, "" , "", invoiceData.getGasNewMeterConsumption()});
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
                if (invoiceData.getElectricityNewMeterConsumption() != null && !invoiceData.getElectricityNewMeterConsumption().equals("0")) rowList.add(new String[] {oldElectricityMeterConsumption, "" , "", invoiceData.getElectricityNewMeterConsumption()});
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
                if (invoiceData.getWaterNewMeterConsumption() != null && !invoiceData.getWaterNewMeterConsumption().equals("0")) rowList.add(new String[] {oldWaterMeterConsumption, "" , "", invoiceData.getWaterNewMeterConsumption()});
                rowList.add(new String[] {
                        waterText,
                        invoiceData.getPreviousWater(),
                        invoiceData.getActualWater(),
                        formatNumber(String.valueOf(waterConsumption)),
                        formatNumber(invoiceData.getWaterCost()) + " HUF"
                });
            }
            if (invoiceData.getActualHeating() != null && !invoiceData.getActualHeating().equals("0")) {
                int heatingConsumption = Integer.parseInt(invoiceData.getActualHeating()) - Integer.parseInt(invoiceData.getPreviousHeating());
                if (invoiceData.getHeatingNewMeterConsumption() != null && !invoiceData.getHeatingNewMeterConsumption().equals("0")) rowList.add(new String[] {oldHeatingMeterConsumption, "" , "", invoiceData.getHeatingNewMeterConsumption()});
                rowList.add(new String[] {
                        heatingText,
                        invoiceData.getPreviousHeating(),
                        invoiceData.getActualHeating(),
                        formatNumber(String.valueOf(heatingConsumption)),
                        formatNumber(invoiceData.getHeatingCost()) + " HUF"
                });
            }
            rowList.forEach(row -> log.info("Row: {}", row));
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

    /**
     * Draws a table on a given PDF page using the specified content and formatting.
     *
     * @param doc the PDF document to which the table belongs
     * @param page the PDF page on which the table will be drawn
     * @param cs the content stream used to draw the table
     * @param x the x-coordinate of the table's starting position on the page
     * @param y the y-coordinate of the table's starting position on the page
     * @param headers an array of strings representing the table headers
     * @param content a 2D array of strings representing the table content, where each sub-array is a row
     * @return the updated y-coordinate after the table is drawn to the specified content stream
     * @throws IOException if there is an error during the drawing process
     */
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

    /**
     * Formats a numeric string by adding space as a thousand separator.
     * If the input string is null, empty, or cannot be parsed as a number, it returns the original string.
     *
     * @param numberStr the string representing the numeric value to be formatted
     * @return the formatted numeric string with spaces as thousand separators,
     *         or the original string if invalid or unparsable
     */
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


    /**
     * Retrieves the last two entries from the provided sorted map and calculates the cost based on the specified meter type and apartment ID.
     * If the provided map contains less than two entries, additional default entries will be added.
     *
     * @param sortedMap a map containing meter readings sorted by key, where the key is a timestamp or sequence identifier, and the value is the reading
     * @param meterType the type of meter (e.g., gas, electricity, water, heating) for which the cost needs to be calculated
     * @param apartmentId the ID of the apartment whose meter data and unit price are being processed
     * @return a map containing the last two entries from the original map, along with the computed cost for the specified meter type
     */
    private Map<String, String> getLast2Entries(Map<String, String> sortedMap, String meterType, Long apartmentId) {
        int unitPrice = 0;
        int consumption = 0;

        log.info("sortedMap content for {} meter: {}", meterType, sortedMap);


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
            case "heating": unitPrice = apartmentRepository.findHeatingUnitPrice(apartmentId); break;
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

    /**
     * Retrieves the last two meter values for various utility meters (e.g., gas, electricity,
     * water, heating) associated with a given apartment. Incorporates additional information
     * such as updated consumption status for each utility type, if available.
     *
     * @param apartmentId the unique identifier of the apartment for which to retrieve the last two meter values
     * @return a map where the keys represent utility types (e.g., "gas", "electricity", "water", "heating")
     * and the values contain related data including the last two meter readings and status of updated consumption
     */
    public Map<String, Map<String, String>> getLast2MeterValues(Long apartmentId) {
        log.info("In getLast2MeterValues: {}", apartmentId);

        //Checking which meter values exist for this apartment
        List<Map<String, Object>> gasValues = apartmentRepository.findActiveGasMeterValues(apartmentId);
        List<Map<String, Object>> electricityValues = apartmentRepository.findActiveElectricityMeterValues(apartmentId);
        List<Map<String, Object>> waterValues = apartmentRepository.findActiveWaterMeterValues(apartmentId);
        List<Map<String, Object>> heatingValues = apartmentRepository.findActiveHeatingMeterValues(apartmentId);

        Map<String, Map<String, String>> meterValues = new HashMap<>();
        try {
            if (!gasValues.isEmpty()) {
                Map<String, String> last2Gas = getLast2Entries(userService.sendLastYearMeterValue("gas", apartmentId), "gas", apartmentId);
                last2Gas.put("gasNewMeterConsumption", checkIfNewMeterValue("gas", apartmentId));
                meterValues.put("gas", last2Gas);
            }
            if (!electricityValues.isEmpty()) {
                Map<String, String> last2Electricity = getLast2Entries(userService.sendLastYearMeterValue("electricity", apartmentId), "electricity", apartmentId);
                last2Electricity.put("electricityNewMeterConsumption", checkIfNewMeterValue("electricity", apartmentId));
                meterValues.put("electricity", last2Electricity);
            }
            if (!waterValues.isEmpty()) {
                Map<String, String> last2Water = getLast2Entries(userService.sendLastYearMeterValue("water", apartmentId), "water", apartmentId);
                last2Water.put("waterNewMeterConsumption", checkIfNewMeterValue("water", apartmentId));
                meterValues.put("water", last2Water);
            }
            if (!heatingValues.isEmpty()) {
                Map<String, String> last2Heating = getLast2Entries(userService.sendLastYearMeterValue("heating", apartmentId), "heating", apartmentId);
                last2Heating.put("heatingNewMeterConsumption", checkIfNewMeterValue("heating", apartmentId));
                meterValues.put("heating", last2Heating);
            }
            log.info("Returning meterValues: {}", meterValues);
            return meterValues;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks and retrieves the previous meter value for the specified meter type
     * and apartment ID based on the latest recorded values. If no value is found
     * or an error occurs, "0" is returned.
     *
     * @param meterType the type of the meter (e.g., "gas", "electricity", "water", "heating")
     * @param apartmentId the unique identifier of the apartment
     * @return the previous meter value as a String, or "0" if no value is available or an error occurs
     */
    private String checkIfNewMeterValue(String meterType, Long apartmentId) {
        List<Map<String, Object>> last12Values = null;
        try {
            switch (meterType) {
                case "gas": last12Values = apartmentRepository.findLatestGasMeterValues(apartmentId); break;
                case "electricity": last12Values = apartmentRepository.findLatestElectricityMeterValues(apartmentId); break;
                case "water": last12Values = apartmentRepository.findLatestWaterMeterValues(apartmentId); break;
                case "heating": last12Values = apartmentRepository.findLatestHeatingMeterValues(apartmentId); break;
            }
            log.info("last12Values: {}", last12Values);
            if (!last12Values.isEmpty() && last12Values.size() > 1 && last12Values.get(1).get("consumption") != null) {
                return last12Values.get(1).get("consumption").toString();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "0";
    }

}
