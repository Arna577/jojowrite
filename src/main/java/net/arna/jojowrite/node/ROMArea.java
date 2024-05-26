package net.arna.jojowrite.node;

import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import net.arna.jojowrite.DialogHelper;
import net.arna.jojowrite.JJWUtils;
import net.arna.jojowrite.JoJoWriteController;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.util.UndoUtils;

import java.util.Collection;

import static net.arna.jojowrite.TextStyles.*;

public class ROMArea extends StyleClassedTextArea {
    boolean writingOriginal = false;
    private String originalText = "";

    private long address = 0x00000000;

    private static final int NUM_LINES = 33;
    private static final double BYTE_WIDTH = 19.2;
    private final Line[] lines = new Line[NUM_LINES];

    private final TextInputDialog findDialog = DialogHelper.createStyledTextInputDialog();
    private final TextInputDialog goToDialog = DialogHelper.createStyledTextInputDialog();

    public ROMArea() {
        super();

        initDialogs();

        // Input handlers
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode keyCode = event.getCode();
            if (keyCode == KeyCode.ENTER) {
                if (JoJoWriteController.getInstance().getOpenType() != JJWUtils.FileType.OVERWRITE) return;

                int initPos = getCaretPosition();
                int endPos = initPos;

                if (getSelection().getLength() < 2) {
                    while (true) {
                        Collection<String> styleClass = getStyleAtPosition(endPos);
                        // Style as a selector is not an amazing idea, but I'll see if it causes problems down the line :)
                        if (!styleClass.contains(TEMP_OVERWRITE_TEXT)) break;
                        if (--endPos <= 0) break;
                    }

                    // Ensures that the init and end positions are byte-aligned
                    if (initPos % 2 == 1) initPos++;
                    if (endPos % 2 == 1) endPos--;
                } else {
                    endPos = getSelection().getStart();
                    initPos = getSelection().getEnd();
                }

                JoJoWriteController.getInstance().createOverwrite(Long.toHexString(address + endPos / 2), getText(endPos, initPos), "");
                setStyleClass(endPos, initPos, OVERWRITTEN_TEXT);

                event.consume();
            }

            if (event.isControlDown()) {
                if (keyCode == KeyCode.G) { // Ctrl + G - Go to Address
                    event.consume();
                    goToDialog.showAndWait().ifPresent(addressStr -> {
                        if (addressStr.isEmpty()) return;
                        try {
                            int address = Integer.parseUnsignedInt(addressStr, 16);
                            JoJoWriteController.getInstance().romScrollBar.setValue(address);
                            selectRange(0, 0);
                        } catch (Exception ignored) {}
                    });
                }
                if (keyCode == KeyCode.F) { // Ctrl + F - Find Hex string
                    event.consume();
                    findDialog.showAndWait().ifPresent(hexStr -> {
                        if (hexStr.isEmpty()) return;
                        JoJoWriteController.getInstance().findAndDisplayInROM(hexStr);
                    });
                }
            }
        });

        //setParagraphGraphicFactory(LineNumberFactory.get(this));

        for (int i = 0; i < NUM_LINES; i++) {
            double x = i * BYTE_WIDTH + 1;
            Line line = new Line(x, 0, x, 0);
            lines[i] = line;
            line.setStroke(Paint.valueOf("#455A64"));
            line.setBlendMode(BlendMode.ADD);
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

    private void initDialogs() {
        // Styling for Find Hex String Dialog
        findDialog.setTitle("Find Hex String");
        findDialog.setHeaderText("Hex value: ");
        findDialog.getDialogPane().getStyleClass().add("help-dialog");
        findDialog.getEditor().getStyleClass().add("main");

        // Styling for Go To Dialog
        goToDialog.setTitle("Reposition");
        goToDialog.setHeaderText("Go to Address: ");
        goToDialog.getDialogPane().getStyleClass().add("help-dialog");
        goToDialog.getEditor().setTextFormatter(new TextFormatter<>(JJWUtils.limitLengthOperator(8)));
        goToDialog.getEditor().getStyleClass().add("main");
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

    public long getAddress() {
        return address;
    }

    public void setAddress(long address) {
        this.address = address;
    }
}
