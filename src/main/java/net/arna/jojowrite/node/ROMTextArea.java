package net.arna.jojowrite.node;

import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import net.arna.jojowrite.JJWUtils;
import org.fxmisc.richtext.StyleClassedTextArea;

public class ROMTextArea extends StyleClassedTextArea {
    boolean writingOriginal = false;

    private int address = 0x00000000;

    private static final int NUM_LINES = 33;
    private static final double BYTE_WIDTH = 19.2;
    private final Line[] lines = new Line[NUM_LINES];


    public ROMTextArea() {
        super();

        setWrapText(true);

        //addEventFilter(ScrollEvent.SCROLL, Event::consume);
        /*
        addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {

            }
        });
        */
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

    @Override
    public void clear() {
        writingOriginal = true;
        super.clear();
        writingOriginal = false;
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

            replace(start, end, text, "overwritten-text");
        }
    }

    @Override
    public void replaceSelection(String replacement) {
        replacement = replacement.toLowerCase();
        if (validateText(replacement)) {
            super.replaceSelection(replacement);
        }
    }

    public long getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }
}
