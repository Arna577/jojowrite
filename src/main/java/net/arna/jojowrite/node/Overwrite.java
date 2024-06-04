package net.arna.jojowrite.node;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.arna.jojowrite.JoJoWriteController;

import java.util.ArrayList;

/*
 * [Address Text]:[Overwrite Text][Show in ROM][Delete]
 * [Comment]
 */

/***
 * A lazy-loaded {@link VBox} Node used for user I/O of overwrite data.
 * The {@link Overwrite#addressField} is coerced to an 8-digit Hex string.
 * The {@link Overwrite#overwriteField} is coerced into a Hex string.
 * The {@link Overwrite#commentField} is used so the user has an easier time remembering & understanding what their changes are doing.
 */

/*
 * todo: Overwrite > From ROM Differences
 *  more hotkeys (?)
 *  selecting amount of ROM to put into page
 *  help menus
 *  ROMTextArea width snapping
 *  integration with xcopy
 *  romsplitting?
 *  select overwrite from ROM red text
 *  Assembly: show as LUA, show as Overwrite
 *  Add a LOGGER
 *  fix overwrites not displaying on start page of ROM
 */
public class Overwrite extends VBox {
    /**
     * Contains text of an 8-digit hex pointer to ROM memory.
     * Updating this fields text will cause a {@link Overwrite#separateBytes()}
     */
    private HexTextField addressField;
    /**
     * Contains an unbounded string of hex digits, which are coerced into pairs.
     * Updating this fields text will cause a {@link Overwrite#separateBytes()}
     */
    private OverwriteField overwriteField;
    /**
     * May contain most characters, used by the user to annotate what this {@link Overwrite} does.
     */
    private TextField commentField;
    /**
     * A HashMap containing indices and byte Strings, used for rendering Overwrites in the {@link ROMArea}.
     */
    private final ArrayList<String> byteStrings = new ArrayList<>();
    //private final ArrayList<Byte> bytes = new ArrayList<>();

    public static final double OVERWRITE_TEXT_MIN_WIDTH = 240.0, OVERWRITE_TEXT_MAX_WIDTH = 640.0, OVERWRITE_MIN_HEIGHT = 68.0;

    private boolean loaded = false;

    private OverwriteBox overwrites = null;

    public Overwrite() {
        setMinHeight(OVERWRITE_MIN_HEIGHT);

        visibleProperty().setValue(false);
        visibleProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (!loaded && newValue) load();
                }
        );
    }

    public double getLayoutHeight() {
        if (loaded) return getBoundsInLocal().getHeight();
        return OVERWRITE_MIN_HEIGHT;
    }

    /**
     * Constructs an Overwrite and assigns it to:
     * @param overwrites a container {@link OverwriteBox}
     */
    public Overwrite(OverwriteBox overwrites) {
        this();
        assignOverwriteBox(overwrites, true);
    }

    public void assignOverwriteBox(OverwriteBox overwrites, boolean prepend) {
        this.overwrites = overwrites;
        if (prepend) overwrites.add(0, this);
        else overwrites.add(this);
    }

    private void load() {
        if (loaded) throw new IllegalStateException("Tried to load Overwrite more than once!");
        if (overwrites == null) throw new IllegalStateException("Tried to load Overwrite without an assigned OverwriteBox!");
        HBox overwriteAndOptions = new HBox();

        addressField = new HexTextField(bufferAddressText == null ? "00000000" : bufferAddressText, 8);
        addressField.getStyleClass().add("address");
        addressField.setPromptText("Address");
        addressField.setMaxWidth(98.0);
        addressField.setOnKeyTyped(
                keyEvent -> {
                    separateBytes();
                    JoJoWriteController.getInstance().refreshOverwrites();
                }
        );

        overwriteField = new OverwriteField(this);
        overwriteField.setText(bufferOverwriteText);
        overwriteField.getStyleClass().add("overwrite-field");
        overwriteField.setMinWidth(OVERWRITE_TEXT_MIN_WIDTH);
        overwriteField.setMaxWidth(OVERWRITE_TEXT_MAX_WIDTH);

        Button showInROM = new Button("Show in ROM");
        //showInROM.setMinWidth(130.0);
        showInROM.setOnAction(event -> JoJoWriteController.getInstance().showInROM(getAddress(), overwriteField.getLength()));

        Button delete = new Button("Delete");
        delete.getStyleClass().add("delete-button");
        delete.setOnAction(event -> {
            overwrites.removeAndUpdate(this);
            JoJoWriteController.getInstance().refreshOverwrites();
        });
        //delete.setMinWidth(85.0);

        overwriteAndOptions.getChildren().addAll(addressField, overwriteField, showInROM, delete);

        commentField = new TextField(bufferCommentText);
        commentField.getStyleClass().add("code-area");
        commentField.setPromptText("Explain what this overwrite does");

        getChildren().addAll(overwriteAndOptions, commentField);

        separateBytes();

        loaded = true;
        //System.out.println("Loaded new overwrite; " + this);
    }

    void separateBytes() {
        //System.out.println("Separating bytes...");
        String byteText = getOverwriteText();
        int byteTextLength = byteText.length();
        if (byteTextLength % 2 != 0) throw new IllegalStateException("Overwrite field with uneven character count; " + overwriteField);
        byteStrings.clear();
        //todo: optimize??
        for (int i = 0; i < byteTextLength; i += 2) {
            // 2 digits represent 1 byte. DisplayROMAt() doubles the address to get the correct placement.
            char first = byteText.charAt(i);
            char second = byteText.charAt(i + 1);
            String byteString = String.valueOf(first) + second;
            byteStrings.add(byteString);
            //bytes.add( (byte) ((Character.digit(first, 16) << 4) + Character.digit(second, 16)) );
        }
    }

    public ArrayList<String> getByteStrings() {
        return byteStrings;
    }

    public int getAddress() {
        String toParse = loaded ? addressField.getText() : bufferAddressText;
        if (toParse.length() == 0) return 0;
        return Integer.parseUnsignedInt(toParse, 16);
    }

    public void setAddressText(String text) {
        addressField.setText(text);
        separateBytes();
    }

    public void setOverwriteText(String text) {
        overwriteField.setText(text);
        separateBytes();
    }

    public void setCommentText(String text) {
        commentField.setText(text);
    }

    public String getAddressText() {
        if (loaded) return addressField.getText();
        return bufferAddressText;
    }

    public String getOverwriteText() {
        if (loaded) return overwriteField.getText();
        return bufferOverwriteText;
    }

    public String getCommentText() {
        if (loaded) return commentField.getText();
        return bufferCommentText;
    }

    @Override
    public String toString() {
        return getAddressText() + ';' + getOverwriteText() + ';' + getCommentText();
    }

    public void bufferText(String address, String overwrite, String comment) {
        bufferCommentText = comment;
        bufferOverwriteText = overwrite;
        bufferAddressText = address;
    }

    private String bufferAddressText = null;
    private String bufferOverwriteText = null;
    private String bufferCommentText = null;
    public static Overwrite fromString(String raw) {
        //long ms = System.currentTimeMillis();
        Overwrite overwrite = new Overwrite();
        String[] content = raw.split(";");
        if (content.length > 1) {
            overwrite.bufferAddressText = content[0];
            overwrite.bufferOverwriteText = content[1];
            if (content.length > 2) {
                overwrite.bufferCommentText = content[2];
            }
            overwrite.separateBytes();
        } else {
            throw new IllegalArgumentException("Invalid string for generating Overwrite; " + raw);
        }
        //System.out.println("COMPLETE; took " + (System.currentTimeMillis() - ms) + "ms");
        return overwrite;
    }

    /**
     * Requests focus to this Overwrites {@link Overwrite#overwriteField} and places the caret at the end.
     */
    public void focus() {
        if (!loaded) load();
        overwriteField.requestFocus();
        overwriteField.displaceCaret(overwriteField.getLength());
    }
}
