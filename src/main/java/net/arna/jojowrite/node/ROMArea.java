package net.arna.jojowrite.node;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import net.arna.jojowrite.DialogHelper;
import net.arna.jojowrite.JJWUtils;
import net.arna.jojowrite.JoJoWriteApplication;
import net.arna.jojowrite.JoJoWriteController;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.util.UndoUtils;

import java.util.Collection;

import static net.arna.jojowrite.TextStyles.*;

public class ROMArea extends StyleClassedTextArea {
    boolean writingOriginal = false;
    private String originalText = "";

    private long address = 0x00000000;

    private static final int NUM_LINES = 34;
    public static final double BYTE_WIDTH = 19.25, BYTE_HEIGHT = 22.75;
    private final Line[] lines = new Line[NUM_LINES];

    private final TextInputDialog findDialog = DialogHelper.createFindDialog("Find Hex string", "");
    private final TextInputDialog goToDialog = DialogHelper.createStyledTextInputDialog("Go to", "");

    private ScrollBar scrollBar;
    private Label addressOutputLabel;

    public ROMArea() {
        super();

        setWrapText(true);

        initDialogs();

        caretPositionProperty().addListener(observable -> {
            if (addressOutputLabel == null) return;
            String hex = Long.toHexString(getAddress() + getCaretPosition() / 2);
            addressOutputLabel.setText(
                    ("00000000" + hex).substring(hex.length())
            );
        });

        //setOnScroll(null);
        addEventFilter(ScrollEvent.ANY, event -> {
            if (event.getDeltaY() > 0) scrollBar.decrement();
            else scrollBar.increment();
        });

        //todo: disable dragging bytes around ffs

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
                switch (keyCode) {
                    case F -> {
                        event.consume();
                        if (findDialog.isShowing()) {
                            ((Stage) findDialog.getDialogPane().getScene().getWindow()).toFront();
                        } else {
                            findDialog.show();
                        }
                    }
                    case G -> {
                        event.consume();
                        goToDialog.showAndWait().ifPresent(addressStr -> {
                            if (addressStr.isEmpty()) return;
                            try {
                                int address = Integer.parseUnsignedInt(addressStr, 16);
                                scrollBar.setValue(address);
                                selectRange(0, 0);
                            } catch (Exception ignored) {
                            }
                        });
                    }
                    case S -> {
                        event.consume();
                        JoJoWriteApplication.saveFile(JJWUtils.FileType.ROM);
                    }
                }
            }
        });

        //setParagraphGraphicFactory(LineNumberFactory.get(this));

        for (int i = 0; i < NUM_LINES; i++) {
            double x = i * BYTE_WIDTH;
            Line line = new Line(x, 0, x, 0);
            lines[i] = line;
            line.setStroke(Paint.valueOf("#455A64"));
            line.setBlendMode(BlendMode.ADD);
            getChildren().add(line);
        }

        widthProperty().addListener((observable, oldValue, newValue) -> {
            for (int i = 0; i < NUM_LINES; i++) lines[i].setVisible(i * BYTE_WIDTH <= newValue.doubleValue());
            calculateByteCapacity();
        });

        heightProperty().addListener((observable, oldValue, newValue) -> {
            for (int i = 0; i < NUM_LINES; i++) lines[i].setEndY(newValue.doubleValue());
            calculateByteCapacity();
        });

        //needsLayoutProperty().addListener(observable -> calculateByteCapacity());
    }

    public void setAddressOutputLabel(Label addressOutputLabel) {
        this.addressOutputLabel = addressOutputLabel;
    }

    public void setScrollBar(ScrollBar scrollBar) {
        this.scrollBar = scrollBar;
    }

    private int byteCapacity = 0x0400;

    /**
     * Calculates how many bytes would fit neatly into the viewport.
     * Works for lower heights but fucks up on higher ones for some reason
     */
    public void calculateByteCapacity() {
        double x = getWidth(), y = getViewportHeight();
        int newByteCapacity = (int) (x / BYTE_WIDTH) * (int) (y / BYTE_HEIGHT);
        // Due to minimum size limitations, this is a reasonable check
        // Its actual purpose is to stop the ROMArea from loading 0 bytes consistently
        if (newByteCapacity > 0)
            byteCapacity = newByteCapacity;
    }

    public int getByteCapacity() {
        return byteCapacity;
    }

    private void initDialogs() {
        // Styling for Find Hex String Dialog
        final DialogPane findDialogPane = findDialog.getDialogPane();
        findDialogPane.getStyleClass().add("help-dialog");
        final TextField findDialogEditor = findDialog.getEditor();
        findDialogEditor.getStyleClass().add("main");
        findDialogEditor.setMaxWidth(Double.POSITIVE_INFINITY);
        findDialogEditor.setTextFormatter(JJWUtils.hexadecimalTextFormatter());
        final Button nextButton = (Button) findDialogPane.lookupButton(ButtonType.NEXT);
        nextButton.addEventFilter(ActionEvent.ACTION,
                event -> {
                    event.consume();
                    String hexStr = findDialogEditor.getText();
                    if (hexStr.isEmpty()) return;
                    int selectedLocalByte = Math.min(getAnchor(), getCaretPosition()) / 2; // 2 characters -> 1 byte

                    Platform.runLater(() -> // Find next
                            JoJoWriteController.getInstance().findAndDisplayInROM(hexStr, getAddress() + selectedLocalByte + 1)
                    );
                }
        );
        findDialog.setGraphic(null);
        findDialogPane.setMinWidth(600.0);

        // Styling for Go To Dialog
        final DialogPane goToDialogPane = goToDialog.getDialogPane();
        goToDialogPane.getStyleClass().add("help-dialog");
        goToDialog.getEditor().setTextFormatter(new TextFormatter<>(JJWUtils.limitLengthOperator(8)));
        goToDialog.getEditor().getStyleClass().add("main");
        goToDialog.setGraphic(null);
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
