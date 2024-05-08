package net.arna.jojowrite;

import javafx.scene.control.TextFormatter;

import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public interface JJWUtils {
    enum FileType {
        ROM,
        OVERWRITE,
        ASSEMBLY,
        PATCH
    }

    String OVERWRITE_FILE_EXTENSION = ".overwrite";
    String ASSEMBLY_FILE_EXTENSION = ".x16asm";
    String PATCH_FILE_EXTENSION = ".patch";

    Pattern hex = Pattern.compile("[0-9a-fA-F]+");
    static boolean isHexadecimal(String text) {
        return hex.matcher(text).matches();
    }

    static UnaryOperator<TextFormatter.Change> limitLengthOperator(int maxLength) {
        return c -> {
            if (c.isContentChange()) {
                int newLength = c.getControlNewText().length();
                if (newLength > maxLength) {
                    // replace the input text with the last maxLength chars
                    String tail = c.getControlNewText().substring(newLength - maxLength, newLength);
                    c.setText(tail);
                    // replace the range to complete text
                    // valid coordinates for range is in terms of old text
                    int oldLength = c.getControlText().length();
                    c.setRange(0, oldLength);
                }
            }
            return c;
        };
    }

    byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    static void printException(Exception e, String msg) {
        System.out.println(msg);
        e.printStackTrace();
    }
}
