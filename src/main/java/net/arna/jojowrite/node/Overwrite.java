package net.arna.jojowrite.node;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import net.arna.jojowrite.JoJoWriteController;

import java.util.ArrayList;

/*
 * [Address Text]:[Overwrite Text][Show in ROM][Delete]
 * [Comment]
 */

/**
 * A {@link VBox} Node used for user I/O of .overwrite data.
 * The {@link Overwrite#addressField} is coerced to an 8-digit Hex string.
 * The {@link Overwrite#overwriteField} is coerced into a Hex string.
 * The {@link Overwrite#commentField} is used so the user has an easier time remembering & understanding what their changes are doing.
 */

/*
 * todo: Overwrite > From ROM Differences
 *  hotkeys (Ctrl+G > GOTO | Ctrl+F > FIND)
 *  selecting amount of ROM to put into page
 *  help menus
 *  ROMTextArea width snapping
 *  patch files
 *  select overwrite from ROM red text
 */
public class Overwrite extends VBox {
    /**
     * Contains text of an 8-digit hex pointer to ROM memory.
     * Updating this fields text will cause a {@link Overwrite#separateBytes()}
     */
    private final HexTextField addressField;
    /**
     * Contains an unbounded string of hex digits, which are coerced into pairs.
     * Updating this fields text will cause a {@link Overwrite#separateBytes()}
     */
    private final OverwriteField overwriteField;
    /**
     * May contain most characters, used by the user to annotate what this Overwrite does.
     */
    private final TextField commentField;
    private final Button showInROM;
    private final Button delete;
    /**
     * A HashMap containing indices and byte Strings, used for rendering Overwrites in the {@link ROMTextArea}.
     */
    private final ArrayList<String> byteMap = new ArrayList<>();

    public static final double OVERWRITE_MIN_WIDTH = 240.0, OVERWRITE_MAX_WIDTH = 640.0;

    public Overwrite() {
        HBox overwriteAndOptions = new HBox();

        addressField = new HexTextField("00000000", 8);
        addressField.getStyleClass().add("address");
        addressField.setPromptText("Address");
        addressField.setMaxWidth(98.0);
        addressField.setOnKeyTyped(keyEvent -> separateBytes());

        overwriteField = new OverwriteField(this);
        overwriteField.getStyleClass().add("overwrite-field");
        overwriteField.setMinWidth(OVERWRITE_MIN_WIDTH);
        overwriteField.setMaxWidth(OVERWRITE_MAX_WIDTH);

        showInROM = new Button("Show in ROM");
        //showInROM.setMinWidth(130.0);
        showInROM.setOnAction(event -> JoJoWriteController.getInstance().showInROM(getAddress(), overwriteField.getLength()));

        delete = new Button("Delete");
        delete.getStyleClass().add("delete-button");
        //delete.setMinWidth(85.0);

        overwriteAndOptions.getChildren().addAll(addressField, overwriteField, showInROM, delete);

        commentField = new TextField();
        commentField.getStyleClass().add("code-area");
        commentField.setPromptText("Explain what this overwrite does");

        getChildren().addAll(overwriteAndOptions, commentField);
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
        delete.setOnAction(event -> {
            overwrites.remove(this);
            JoJoWriteController.getInstance().refreshOverwrites();
        });

        if (prepend) overwrites.add(0, this);
        else overwrites.add(this);
    }

    void separateBytes() {
        //System.out.println("Separating bytes...");
        String byteText = getOverwriteText();
        int byteTextLength = byteText.length();
        if (byteTextLength % 2 != 0) throw new IllegalStateException("Overwrite field with uneven character count; " + overwriteField);
        byteMap.clear();
        for (int i = 0; i < byteTextLength; i += 2) {
            // 2 digits represent 1 byte. DisplayROMAt() doubles the address to get the correct placement.
            byteMap.add(byteText.substring(i, i + 2));
        }
    }

    public ArrayList<String> getByteMap() {
        return byteMap;
    }

    public int getAddress() {
        if (addressField.getLength() == 0) return 0;
        return Integer.parseUnsignedInt(addressField.getText(), 16);
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

    public String getOverwriteText() {
        return overwriteField.getText();
    }

    @Override
    public String toString() {
        return addressField.getText() + ':' + overwriteField.getText() + '\n' + commentField.getText();
    }

    public static Overwrite fromStrings(String addressAndOverwrite, String comment) {
        //long ms = System.currentTimeMillis();
        Overwrite overwrite = new Overwrite();
        int colonIndex = addressAndOverwrite.indexOf(':');
        if (colonIndex == -1) throw new IllegalArgumentException("Invalid string for generating Overwrite; " + addressAndOverwrite);
        overwrite.setAddressText(addressAndOverwrite.substring(0, colonIndex));
        overwrite.setOverwriteText(addressAndOverwrite.substring(colonIndex + 1));
        overwrite.setCommentText(comment);
        //System.out.println("COMPLETE; took " + (System.currentTimeMillis() - ms) + "ms");
        return overwrite;
    }

    /**
     * Requests focus to this Overwrites {@link Overwrite#overwriteField} and places the caret at the end.
     */
    public void focus() {
        overwriteField.requestFocus();
        overwriteField.displaceCaret(overwriteField.getLength());
    }
}
