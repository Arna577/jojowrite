package net.arna.jojowrite.node;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import net.arna.jojowrite.JJWUtils;
import net.arna.jojowrite.JoJoWriteController;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.util.UndoUtils;

import java.util.Collection;

import static net.arna.jojowrite.TextStyles.BASIC_TEXT;
import static net.arna.jojowrite.TextStyles.TEMP_OVERWRITE_TEXT;

public class ROMTextArea extends StyleClassedTextArea {
    boolean writingOriginal = false;
    private String originalText = "";

    private int address = 0x00000000;

    private static final int NUM_LINES = 33;
    private static final double BYTE_WIDTH = 19.2;
    private final Line[] lines = new Line[NUM_LINES];

    public ROMTextArea() {
        super();

        setWrapText(true);

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (JoJoWriteController.getInstance().openType != JJWUtils.FileType.OVERWRITE) return;

                int initPos = getCaretPosition();
                int endPos = initPos;

                if (getSelection().getLength() < 2) {
                    while (true) {
                        Collection<String> styleClass = getStyleAtPosition(endPos);
                        // Style as a selector is not an amazing idea, but I'll see if it causes problems down the line :)
                        if (!styleClass.contains(TEMP_OVERWRITE_TEXT))
                            break;
                        endPos--;
                        if (endPos <= 0)
                            break;
                    }

                    if (initPos % 2 == 1)
                        initPos++;
                    if (endPos % 2 == 1)
                        endPos--;
                } else {
                    endPos = getSelection().getStart();
                    initPos = getSelection().getEnd();
                }

                Overwrite overwrite = new Overwrite(JoJoWriteController.getInstance().overwrites);
                overwrite.setAddressText(Integer.toHexString(address + endPos / 2));
                overwrite.setOverwriteText(getText(endPos, initPos));
                overwrite.focus();

                JoJoWriteController.getInstance().refreshOverwrites();

                event.consume();
            }
        });

        //setParagraphGraphicFactory(LineNumberFactory.get(this));

        for (int i = 0; i < NUM_LINES; i++) {
            double x = i * BYTE_WIDTH + 1;
            Line line = new Line(x, 0, x, 0);
            lines[i] = line;
            line.setStroke(Paint.valueOf("#455A64"));
            getChildren().add(line);
        }

        widthProperty().addListener((observable, oldValue, newValue) -> {
            for (int i = 0; i < NUM_LINES; i++)
                lines[i].setVisible(i * BYTE_WIDTH + 1 <= newValue.doubleValue());
        });

        heightProperty().addListener((observable, oldValue, newValue) -> {
            for (int i = 0; i < NUM_LINES; i++)
                lines[i].setEndY(newValue.doubleValue());
        });
    }

    @Override
    protected void setWidth(double value) {
        super.setWidth(value);
    }

    private boolean validateText(String text) {
        return JJWUtils.isHexadecimal(text) || writingOriginal;
    }

    public void setWritingOriginal(boolean writingOriginal) {
        this.writingOriginal = writingOriginal;
    }

    public void restoreOriginal() {
        replace(0, getLength(), originalText, BASIC_TEXT);
    }

    @Override
    public void clear() {
        writingOriginal = true;
        originalText = "";
        super.clear();
        writingOriginal = false;
    }

    @Override
    public void paste() {
        // TODO: unfuck pasting
    }

    public void resetUndoManager() {
        setUndoManager(UndoUtils.defaultUndoManager(this));
    }

    @Override
    public void append(String text, String styleClass) {
        super.append(text, styleClass);
        if (writingOriginal) {
            originalText += text;
            resetUndoManager();
        }
    }

    @Override
    public void replaceText(int start, int end, String text) {
        if (writingOriginal) {
            super.replaceText(start, end, text);
            return;
        }

        text = text.toLowerCase();
        if (validateText(text)) {
            int selectionLength = end - start;
            selectionLength = Math.min(selectionLength, text.length());
            end = start + selectionLength;

            if (start == end) { // Don't allow single character typing
                if (++end > getLength())
                    return;
            }

            replace(start, end, text, TEMP_OVERWRITE_TEXT);
        }
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }
}
