package net.arna.jojowrite.node;

import net.arna.jojowrite.JJWUtils;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.File;
import java.util.regex.Pattern;

import static net.arna.jojowrite.TextStyles.*;

/**
 * A StyleClassedTextArea used for .patch files that validates whether the input file paths are valid.
 */
public class PatchArea extends StyleClassedTextArea {
    final Pattern SLASH = Pattern.compile("/");

    public PatchArea() {
        setOnKeyTyped(event -> update());
    }

    private static boolean notROMPath(String string) {
        return string.endsWith(JJWUtils.ASSEMBLY_FILE_EXTENSION) || string.endsWith(JJWUtils.OVERWRITE_FILE_EXTENSION);
    }

    public void update() {
        String[] paragraphs = getText().split("\n");

        int character = 0;
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];
            int length = paragraph.length();

            boolean validFile = new File(paragraph).isFile();

            if (validFile) {
                if (i == 0 || (i == paragraphs.length - 1 && !notROMPath(paragraph))) { // Highlight ROM
                    setStyleClass(character, character + length, ADDRESS_TEXT);
                } else {
                    setStyleClass(character, character + length,
                            notROMPath(paragraph) ?
                                    TEMP_OVERWRITE_TEXT : OVERWRITTEN_TEXT);
                }
            } else {
                setStyleClass(character, character + length, OVERWRITTEN_TEXT);
            }

            character += length + 1;
        }
    }

    @Override
    public void replaceText(int start, int end, String text) {
        super.replaceText(start, end, SLASH.matcher(text).replaceAll("\\\\"));
    }
}
