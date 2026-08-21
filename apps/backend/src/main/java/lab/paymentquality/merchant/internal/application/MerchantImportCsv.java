package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.internal.domain.MerchantImportMalformedException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class MerchantImportCsv {

    private MerchantImportCsv() {
    }

    static List<List<String>> parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new MerchantImportMalformedException("CSV file is empty");
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(ch);
                }
                continue;
            }
            if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                current.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n') {
                current.add(field.toString());
                field.setLength(0);
                rows.add(current);
                current = new ArrayList<>();
            } else if (ch != '\r') {
                field.append(ch);
            }
        }
        if (inQuotes) {
            throw new MerchantImportMalformedException("CSV has an unclosed quote");
        }
        if (!field.isEmpty() || !current.isEmpty()) {
            current.add(field.toString());
            rows.add(current);
        }
        return rows;
    }
}
