package net.arna.jojowrite.node;

import net.arna.jojowrite.JJWUtils;
import net.arna.jojowrite.JoJoWriteController;
import org.fxmisc.richtext.StyleClassedTextField;

import static net.arna.jojowrite.node.Overwrite.OVERWRITE_TEXT_MAX_WIDTH;
import static net.arna.jojowrite.node.Overwrite.OVERWRITE_TEXT_MIN_WIDTH;

public class OverwriteField extends StyleClassedTextField {
    public OverwriteField(Overwrite overwrite) {
        setOnKeyTyped(
                keyEvent -> {
                    int length = this.getLength();

                    // Adjust size to fit
                    double newLength = length * 16 / 1.33;
                    if (newLength < OVERWRITE_TEXT_MIN_WIDTH) newLength = OVERWRITE_TEXT_MIN_WIDTH;
                    if (newLength > OVERWRITE_TEXT_MAX_WIDTH) newLength = OVERWRITE_TEXT_MAX_WIDTH;
                    this.setPrefWidth(newLength);

                    int caretPosition = getCaretPosition();
                    // Make sure bytes are in pairs
                    if (length % 2 == 1) {
                        if (keyEvent.getCharacter().equals("\b")) {
                            if (caretPosition % 2 == 0) deleteNextChar();
                            else deletePreviousChar();
                        } else {
                            insertText(getSelection().getEnd(), "0");
                            selectRange(caretPosition, ++caretPosition);
                        }
                    }

                    overwrite.separateBytes();
                    JoJoWriteController.getInstance().refreshOverwrites();
                }
        );
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
