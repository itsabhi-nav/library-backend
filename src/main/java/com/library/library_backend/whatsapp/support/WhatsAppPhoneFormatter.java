package com.library.library_backend.whatsapp.support;

public final class WhatsAppPhoneFormatter {

    private WhatsAppPhoneFormatter() {
    }

    /** Format phone number for WhatsApp API (India +91 default). */
    public static String format(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        String cleaned = phoneNumber.replaceAll("\\D", "");
        if (cleaned.length() == 10) {
            cleaned = "91" + cleaned;
        } else if (cleaned.length() == 11 && cleaned.startsWith("0")) {
            cleaned = "91" + cleaned.substring(1);
        }
        return cleaned;
    }
}
