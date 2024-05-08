package net.arna.jojowrite.node;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import net.arna.jojowrite.JJWUtils;

/**
 * A TextField that only accepts (lowercase) Hexadecimal digits.
 */
public class HexTextField extends TextField {

    public HexTextField() {
        super();
    }

    public HexTextField(String s, int maxLength) {
        super(s);
        setTextFormatter(new TextFormatter<HexTextField>(JJWUtils.limitLengthOperator(maxLength)));
    }

    private static boolean validateText(String text) {
        return JJWUtils.isHexadecimal(text) || text.isEmpty();
    }

    @Override
    public void replaceText(int start, int end, String text) {
        text = text.toLowerCase();
        if (validateText(text))
            super.replaceText(start, end, text);
    }

    @Override
    public void replaceSelection(String replacement) {
        replacement = replacement.toLowerCase();
        if (validateText(replacement))
            super.replaceSelection(replacement);
    }
}
