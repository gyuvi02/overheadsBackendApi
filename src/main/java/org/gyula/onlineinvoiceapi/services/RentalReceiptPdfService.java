package org.gyula.onlineinvoiceapi.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.gyula.onlineinvoiceapi.model.PaymentMethod;
import org.gyula.onlineinvoiceapi.model.RentalReceipt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@Service
public class RentalReceiptPdfService {

    private static final String[] ONES = {
            "", "egy", "kettő", "három", "négy", "öt", "hat", "hét", "nyolc", "kilenc",
            "tíz", "tizenegy", "tizenkettő", "tizenhárom", "tizennégy", "tizenöt",
            "tizenhat", "tizenhét", "tizennyolc", "tizenkilenc"
    };
    private static final String[] TENS = {
            "", "", "húsz", "harminc", "negyven", "ötven", "hatvan", "hetven", "nyolcvan", "kilencven"
    };

    @Value("${file.create.folder:}")
    private String createFolder;

    @Value("${arial.file:}")
    private String arialTtfFile;

    @Value("${arial.bold.file:}")
    private String arialBoldTtfFile;

    @Value("${landlord.name:}")
    private String landlordName;

    @Value("${landlord.address:}")
    private String landlordAddress;

    @Value("${landlord.tax-identifier:}")
    private String landlordTaxIdentifier;

    public String createPdf(RentalReceipt receipt) {
        validateLandlordConfig();

        try (PDDocument document = new PDDocument()) {
            PDType0Font regularFont = PDType0Font.load(document, new File(arialTtfFile));
            PDType0Font boldFont = PDType0Font.load(document, new File(arialBoldTtfFile));

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - 55;

                y = centeredText(content, boldFont, 16, "SZÁMVITELI BIZONYLAT", y, page);
                y = centeredText(content, regularFont, 10, "Accounting receipt - not an invoice", y - 16, page);

                y -= 30;
                y = labelValue(content, boldFont, regularFont, "Bizonylat sorszáma / Receipt no.", receipt.getReceiptNumber(), margin, y);
                y = labelValue(content, boldFont, regularFont, "Kiállítás dátuma / Issue date", formatDate(receipt.getIssueDate()), margin, y);
                y = labelValue(content, boldFont, regularFont, "Időszak / Period", receipt.getPeriodYear() + ". " + receipt.getPeriodMonth() + ". hónap", margin, y);

                y -= 14;
                y = section(content, boldFont, "Bérbeadó / Landlord", margin, y);
                y = labelValue(content, boldFont, regularFont, "Név / Name", landlordName, margin, y);
                y = labelValue(content, boldFont, regularFont, "Lakcím / Address", landlordAddress, margin, y);
                y = labelValue(content, boldFont, regularFont, "Adóazonosító / Tax ID", landlordTaxIdentifier, margin, y);

                y -= 10;
                y = section(content, boldFont, "Bérlő / Tenant", margin, y);
                y = labelValue(content, boldFont, regularFont, "Név / Name", receipt.getTenantName(), margin, y);
                y = labelValue(content, boldFont, regularFont, "Lakcím / Address", receipt.getTenantAddress(), margin, y);

                y -= 10;
                y = section(content, boldFont, "Bérlemény / Property", margin, y);
                y = labelValue(content, boldFont, regularFont, "Cím / Address", receipt.getPropertyAddress(), margin, y);

                y -= 10;
                y = section(content, boldFont, "Összeg és jogcím / Amount and purpose", margin, y);
                y = moneyLine(content, boldFont, regularFont, "Bérleti díj / Rent", receipt.getRentAmount(), margin, y);
                y = moneyLine(content, boldFont, regularFont, "Rezsi / Utility costs", receipt.getUtilityAmount(), margin, y);
                y = moneyLine(content, boldFont, regularFont, "Közös költség / Maintenance fee", receipt.getMaintenanceFee(), margin, y);
                y = moneyLine(content, boldFont, regularFont, "Takarítás / Cleaning", receipt.getCleaningAmount(), margin, y);
                if (receipt.getOtherAmount().compareTo(BigDecimal.ZERO) != 0) {
                    y = moneyLine(content, boldFont, regularFont, nullToDash(receipt.getOtherText()) + " / Other", receipt.getOtherAmount(), margin, y);
                }

                y -= 8;
                y = moneyLine(content, boldFont, boldFont, "ÖSSZESEN / TOTAL", receipt.getTotalAmount(), margin, y);
                y = labelValue(content, boldFont, regularFont, "Összesen betűvel / In words", amountInHungarianWords(receipt.getTotalAmount()) + " forint", margin, y);
                y = labelValue(content, boldFont, regularFont, "Fizetés módja / Payment method", paymentMethodText(receipt.getPaymentMethod()), margin, y);
                y = labelValue(content, boldFont, regularFont, "Fizetés dátuma / Payment date", formatDate(receipt.getPaymentDate()), margin, y);

                y -= 24;
                showText(content, regularFont, 10, margin, y, "Ez a dokumentum számviteli bizonylat, nem számla.");

                y -= 60;
                signatureLine(content, regularFont, margin, y, "Bérbeadó / Landlord");
                signatureLine(content, regularFont, 330, y, "Bérlő / Tenant");
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            byte[] pdfBytes = output.toByteArray();

            Path pdfPath = savePdf(receipt, pdfBytes);
            receipt.setPdfPath(pdfPath.toString());
            receipt.setPdfSha256(sha256(pdfBytes));

            return Base64.getEncoder().encodeToString(pdfBytes);
        } catch (IOException e) {
            throw new RuntimeException("Error creating rental receipt PDF: " + e.getMessage(), e);
        }
    }

    private Path savePdf(RentalReceipt receipt, byte[] pdfBytes) throws IOException {
        Path folder = Path.of(createFolder, String.valueOf(receipt.getPeriodYear()), "rental-receipts", String.valueOf(receipt.getApartment().getId()));
        Files.createDirectories(folder);
        Path pdfPath = folder.resolve(receipt.getReceiptNumber() + ".pdf");
        Files.write(pdfPath, pdfBytes);
        return pdfPath;
    }

    private void validateLandlordConfig() {
        if (isBlank(landlordName) || isBlank(landlordAddress) || isBlank(landlordTaxIdentifier)) {
            throw new IllegalStateException("Landlord name, address and tax identifier must be configured before creating accounting receipt PDFs");
        }
    }

    private float centeredText(PDPageContentStream content, PDType0Font font, int size, String text, float y, PDPage page) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        float x = (page.getMediaBox().getWidth() - textWidth) / 2;
        showText(content, font, size, x, y, text);
        return y;
    }

    private float section(PDPageContentStream content, PDType0Font boldFont, String title, float x, float y) throws IOException {
        showText(content, boldFont, 12, x, y, title);
        return y - 18;
    }

    private float labelValue(PDPageContentStream content, PDType0Font labelFont, PDType0Font valueFont, String label, String value, float x, float y) throws IOException {
        showText(content, labelFont, 10, x, y, label + ":");
        showText(content, valueFont, 10, x + 175, y, nullToDash(value));
        return y - 16;
    }

    private float moneyLine(PDPageContentStream content, PDType0Font labelFont, PDType0Font valueFont, String label, BigDecimal amount, float x, float y) throws IOException {
        return labelValue(content, labelFont, valueFont, label, formatAmount(amount) + " Ft", x, y);
    }

    private void signatureLine(PDPageContentStream content, PDType0Font font, float x, float y, String label) throws IOException {
        content.moveTo(x, y);
        content.lineTo(x + 180, y);
        content.stroke();
        showText(content, font, 9, x + 42, y - 14, label);
    }

    private void showText(PDPageContentStream content, PDType0Font font, int size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private String formatDate(java.time.LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy. MM. dd.", new Locale("hu", "HU")));
    }

    private String paymentMethodText(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.CASH ? "Készpénz / Cash" : "Banki átutalás / Bank transfer";
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String amountInHungarianWords(BigDecimal amount) {
        long value = amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (value == 0) {
            return "nulla";
        }
        if (value < 0 || value > 999_999_999) {
            return Long.toString(value);
        }

        long millions = value / 1_000_000;
        long thousands = (value / 1_000) % 1_000;
        long remainder = value % 1_000;

        StringBuilder words = new StringBuilder();
        appendGroup(words, millions, "millió");
        appendGroup(words, thousands, "ezer");
        if (remainder > 0) {
            words.append(threeDigitGroup((int) remainder));
        }
        if (words.charAt(words.length() - 1) == '-') {
            words.deleteCharAt(words.length() - 1);
        }
        return words.toString();
    }

    private void appendGroup(StringBuilder words, long groupValue, String suffix) {
        if (groupValue == 0) {
            return;
        }
        words.append(threeDigitGroup((int) groupValue)).append(suffix);
        if (words.length() > 0) {
            words.append("-");
        }
    }

    private String threeDigitGroup(int value) {
        StringBuilder words = new StringBuilder();
        int hundreds = value / 100;
        int rest = value % 100;

        if (hundreds > 0) {
            if (hundreds > 1) {
                words.append(ONES[hundreds]);
            }
            words.append("száz");
        }
        if (rest > 0) {
            words.append(twoDigitGroup(rest));
        }
        return words.toString();
    }

    private String twoDigitGroup(int value) {
        if (value < 20) {
            return ONES[value];
        }
        int tens = value / 10;
        int ones = value % 10;
        if (value == 20) {
            return "húsz";
        }
        if (tens == 2) {
            return "huszon" + ONES[ones];
        }
        return TENS[tens] + ONES[ones];
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String nullToDash(String value) {
        return isBlank(value) ? "-" : value.trim();
    }
}
