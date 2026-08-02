package org.gyula.onlineinvoiceapi.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.gyula.onlineinvoiceapi.model.PaymentMethod;
import org.gyula.onlineinvoiceapi.model.RentalReceipt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import org.apache.pdfbox.util.Matrix;

@Service
public class RentalReceiptPdfService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final float POINTS_PER_MM = 72f / 25.4f;

    private static final String[] ONES = {
            "", "egy", "kett\u0151", "h\u00e1rom", "n\u00e9gy", "\u00f6t", "hat", "h\u00e9t", "nyolc", "kilenc",
            "t\u00edz", "tizenegy", "tizenkett\u0151", "tizenh\u00e1rom", "tizenn\u00e9gy", "tizen\u00f6t",
            "tizenhat", "tizenh\u00e9t", "tizennyolc", "tizenkilenc"
    };
    private static final String[] TENS = {
            "", "", "h\u00fasz", "harminc", "negyven", "\u00f6tven", "hatvan", "hetven", "nyolcvan", "kilencven"
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
                drawReceipt(document, content, page, regularFont, boldFont, receipt);
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

    private void drawReceipt(PDDocument document, PDPageContentStream content, PDPage page, PDType0Font regularFont,
                             PDType0Font boldFont, RentalReceipt receipt) throws IOException {
        float margin = 50;
        float width = page.getMediaBox().getWidth() - 2 * margin;
        float y = page.getMediaBox().getHeight() - 42;

        float headerBottom = y - 70;
        float headerHeight = 64;
        drawBox(content, margin, headerBottom, width, headerHeight, 0.94f);
        centeredTextInBox(content, boldFont, 18, "SZ\u00c1MVITELI BIZONYLAT", headerBottom, headerHeight, 7, page);
        centeredTextInBox(content, regularFont, 10, "Accounting receipt - not an invoice", headerBottom, headerHeight, -10, page);
        y -= 78;

        y = beginSection(content, boldFont, "Bizonylat adatai / Receipt details", margin, y, width);
        y = twoColumnLabelValue(content, boldFont, regularFont,
                "Sorsz\u00e1m / Receipt no.", receipt.getReceiptNumber(),
                "Id\u0151szak / Period", receipt.getPeriodYear() + ". " + receipt.getPeriodMonth() + ". h\u00f3nap",
                margin, y);
        y = twoColumnLabelValue(content, boldFont, regularFont,
                "Ki\u00e1ll\u00edt\u00e1s / Issue date", formatDate(receipt.getIssueDate()),
                "Fizet\u00e9s / Payment date", formatDate(receipt.getPaymentDate()),
                margin, y);
        y = endSection(y);

        y = beginSection(content, boldFont, "Felek / Parties", margin, y, width);
        y = subsection(content, boldFont, "B\u00e9rbead\u00f3 / Landlord", margin, y);
        y = labelValue(content, boldFont, regularFont, "N\u00e9v / Name", landlordName, margin, y);
        y = labelValue(content, boldFont, regularFont, "Lakc\u00edm / Address", landlordAddress, margin, y);
        y = labelValue(content, boldFont, regularFont, "Ad\u00f3azonos\u00edt\u00f3 / Tax ID", landlordTaxIdentifier, margin, y);
        y -= 5;
        y = subsection(content, boldFont, "B\u00e9rl\u0151 / Tenant", margin, y);
        y = labelValue(content, boldFont, regularFont, "N\u00e9v / Name", receipt.getTenantName(), margin, y);
        y = labelValue(content, boldFont, regularFont, "Lakc\u00edm / Address", receipt.getTenantAddress(), margin, y);
        y = endSection(y);

        y = beginSection(content, boldFont, "B\u00e9rlem\u00e9ny / Property", margin, y, width);
        y = labelValue(content, boldFont, regularFont, "C\u00edm / Address", receipt.getPropertyAddress(), margin, y);
        y = endSection(y);

        y = beginSection(content, boldFont, "T\u00e9telek / Items", margin, y, width);
        y = tableHeader(content, boldFont, margin, y, width);
        y = tableRow(content, regularFont, "B\u00e9rleti d\u00edj / Rent", receipt.getRentAmount(), margin, y, width);
        y = totalRow(content, boldFont, "\u00d6SSZESEN / TOTAL", receipt.getRentAmount(), margin, y, width);
        y = endSection(y);

        y = beginSection(content, boldFont, "Fizet\u00e9s / Payment", margin, y, width);
        y = labelValue(content, boldFont, regularFont, "\u00d6sszesen bet\u0171vel / In words", amountInHungarianWords(receipt.getRentAmount()) + " forint", margin, y);
        y = labelValue(content, boldFont, regularFont, "Fizet\u00e9s m\u00f3dja / Payment method", paymentMethodText(receipt.getPaymentMethod()), margin, y);
        y = endSection(y);

        y = beginSection(content, boldFont, "B\u00e9rbead\u00f3i al\u00e1\u00edr\u00e1s / Landlord signature", margin, y, width);
        drawLandlordSignature(document, content, margin + width / 2, y - 23);
        y -= 64;

        showText(content, regularFont, 10, margin, y, "Ez a dokumentum sz\u00e1mviteli bizonylat, nem sz\u00e1mla.");
        content.setLineWidth(0.25f);
        content.moveTo(margin, y - 8);
        content.lineTo(margin + width, y - 8);
        content.stroke();
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

    private void centeredTextInBox(PDPageContentStream content, PDType0Font font, int size, String text,
                                   float boxBottom, float boxHeight, float yOffset, PDPage page) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        float x = (page.getMediaBox().getWidth() - textWidth) / 2;
        float y = boxBottom + boxHeight / 2 - size / 3f + yOffset;
        showText(content, font, size, x, y, text);
    }

    private float beginSection(PDPageContentStream content, PDType0Font boldFont, String title, float x, float y, float width) throws IOException {
        content.setLineWidth(0.8f);
        content.moveTo(x, y);
        content.lineTo(x + width, y);
        content.stroke();
        y -= 17;
        showText(content, boldFont, 12, x, y, title);
        y -= 10;
        content.setLineWidth(0.25f);
        content.moveTo(x, y);
        content.lineTo(x + width, y);
        content.stroke();
        return y - 11;
    }

    private float endSection(float y) {
        return y - 9;
    }

    private float subsection(PDPageContentStream content, PDType0Font boldFont, String title, float x, float y) throws IOException {
        showText(content, boldFont, 10, x, y, title);
        return y - 18;
    }

    private float labelValue(PDPageContentStream content, PDType0Font labelFont, PDType0Font valueFont, String label, String value, float x, float y) throws IOException {
        showText(content, labelFont, 9, x, y, label + ":");
        showText(content, valueFont, 10, x + 175, y, nullToDash(value));
        return y - 14;
    }

    private float twoColumnLabelValue(PDPageContentStream content, PDType0Font labelFont, PDType0Font valueFont,
                                      String leftLabel, String leftValue, String rightLabel, String rightValue,
                                      float x, float y) throws IOException {
        showText(content, labelFont, 9, x, y, leftLabel + ":");
        showText(content, valueFont, 10, x, y - 13, nullToDash(leftValue));
        showText(content, labelFont, 9, x + 285, y, rightLabel + ":");
        showText(content, valueFont, 10, x + 285, y - 13, nullToDash(rightValue));
        return y - 29;
    }

    private float tableHeader(PDPageContentStream content, PDType0Font font, float x, float y, float width) throws IOException {
        drawBox(content, x, y - 14, width, 20, 0.90f);
        showText(content, font, 10, x + 8, y - 8, "Megnevez\u00e9s / Item");
        showText(content, font, 10, x + width - 98, y - 8, "\u00d6sszeg / Amount");
        return y - 24;
    }

    private float tableRow(PDPageContentStream content, PDType0Font font, String label, BigDecimal amount, float x, float y, float width) throws IOException {
        content.setLineWidth(0.2f);
        content.moveTo(x, y - 6);
        content.lineTo(x + width, y - 6);
        content.stroke();
        showText(content, font, 10, x + 8, y - 20, label);
        showText(content, font, 10, x + width - 88, y - 20, formatAmount(amount) + " Ft");
        return y - 24;
    }

    private float totalRow(PDPageContentStream content, PDType0Font font, String label, BigDecimal amount, float x, float y, float width) throws IOException {
        drawBox(content, x, y - 22, width, 24, 0.95f);
        showText(content, font, 11, x + 8, y - 15, label);
        showText(content, font, 11, x + width - 98, y - 15, formatAmount(amount) + " Ft");
        return y - 30;
    }

    private void drawLandlordSignature(PDDocument document, PDPageContentStream content, float centerX, float centerY) throws IOException {
        BufferedImage signature = loadSignatureImage();
        PDImageXObject image = LosslessFactory.createFromImage(document, signature);

        float imageWidth = 130f;
        float imageHeight = imageWidth * signature.getHeight() / signature.getWidth();
        float offsetX = randomOffsetPoints(10);
        float offsetY = randomOffsetPoints(5);
        float rotation = (RANDOM.nextFloat() * 10f - 5f);
        float drawCenterX = centerX + offsetX;
        float drawCenterY = centerY + offsetY;
        float x = -imageWidth / 2;
        float y = -imageHeight / 2;

        content.saveGraphicsState();
        content.transform(Matrix.getTranslateInstance(drawCenterX, drawCenterY));
        content.transform(Matrix.getRotateInstance(Math.toRadians(rotation), 0, 0));
        content.drawImage(image, x, y, imageWidth, imageHeight);
        content.restoreGraphicsState();
    }

    private BufferedImage loadSignatureImage() throws IOException {
        return ImageIO.read(new ClassPathResource("alairas.png").getInputStream());
    }

    private float randomOffsetPoints(int maxMillimeters) {
        return (RANDOM.nextFloat() * 2f - 1f) * maxMillimeters * POINTS_PER_MM;
    }

    private void drawBox(PDPageContentStream content, float x, float y, float width, float height, float gray) throws IOException {
        content.setNonStrokingColor(gray, gray, gray);
        content.addRect(x, y, width, height);
        content.fill();
        content.setNonStrokingColor(0f, 0f, 0f);
        content.setLineWidth(0.35f);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void showText(PDPageContentStream content, PDType0Font font, int size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy. MM. dd.", new Locale("hu", "HU")));
    }

    private String paymentMethodText(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.CASH ? "K\u00e9szp\u00e9nz / Cash" : "Banki \u00e1tutal\u00e1s / Bank transfer";
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
        appendGroup(words, millions, "milli\u00f3");
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
        words.append(threeDigitGroup((int) groupValue)).append(suffix).append("-");
    }

    private String threeDigitGroup(int value) {
        StringBuilder words = new StringBuilder();
        int hundreds = value / 100;
        int rest = value % 100;

        if (hundreds > 0) {
            if (hundreds > 1) {
                words.append(ONES[hundreds]);
            }
            words.append("sz\u00e1z");
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
            return "h\u00fasz";
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
