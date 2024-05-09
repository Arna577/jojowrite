package net.arna.jojowrite.node;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
 *  culling of overwrites the user can't see
 *  select overwrite from ROM red text
 */
public class Overwrite extends VBox {
    private final HBox overwriteAndOptions;
    /**
     * Contains text of an 8-digit hex pointer to ROM memory.
     * Updating this fields text will cause a {@link Overwrite#separateBytes()}
     */
    private final HexTextField addressField;
    /**
     * Contains an unbounded string of hex digits, which are coerced into pairs.
     * Updating this fields text will cause a {@link Overwrite#separateBytes()}
     */
    private final HexTextField overwriteField;
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

    private static final double OVERWRITE_MIN_WIDTH = 240.0, OVERWRITE_MAX_WIDTH = 640.0;

    public Overwrite() {
        getStyleClass().add("code-area");

        overwriteAndOptions = new HBox();

        addressField = new HexTextField("00", 8);
        addressField.getStyleClass().add("address");
        addressField.setPromptText("Address");
        addressField.setMinWidth(100.0);
        addressField.setPrefWidth(100.0);

        addressField.setOnKeyTyped(keyEvent -> separateBytes());

        Label addressSeparator = new Label(":");

        overwriteField = new HexTextField();
        overwriteField.getStyleClass().add("code-area");
        overwriteField.setPromptText("Overwrite Bytes");
        overwriteField.setMinWidth(OVERWRITE_MIN_WIDTH);
        overwriteField.setMaxWidth(OVERWRITE_MAX_WIDTH);

        overwriteField.setOnKeyTyped(
                keyEvent -> {
                    int length = overwriteField.getLength();

                    // Adjust size to fit
                    double newLength = length * overwriteField.getFont().getSize() / 1.33;
                    if (newLength < OVERWRITE_MIN_WIDTH) newLength = OVERWRITE_MIN_WIDTH;
                    if (newLength > OVERWRITE_MAX_WIDTH) newLength = OVERWRITE_MAX_WIDTH;
                    overwriteField.setPrefWidth(newLength);

                    // Make sure bytes are in pairs
                    if (length % 2 == 1) {
                        if (keyEvent.getCharacter().equals("\b")) {
                            overwriteField.deletePreviousChar();
                        } else {
                            overwriteField.insertText(overwriteField.getSelection().getEnd(), "0");
                            overwriteField.selectBackward();
                        }
                    }

                    separateBytes();
                    JoJoWriteController.getInstance().refreshOverwrites();
                }
        );

        showInROM = new Button("Show in ROM");
        showInROM.setMinWidth(130.0);
        showInROM.setOnAction(event -> JoJoWriteController.getInstance().showInROM(
                getAddress(), overwriteField.getLength()
        ));

        delete = new Button("Delete");
        delete.getStyleClass().add("delete-button");
        delete.setMinWidth(85.0);

        overwriteAndOptions.getChildren().addAll(addressField, addressSeparator, overwriteField, showInROM, delete);

        commentField = new TextField();
        commentField.getStyleClass().add("code-area");
        commentField.setPromptText("Explain what this overwrite does");

        getChildren().addAll(overwriteAndOptions, commentField);
    }

    /**
     * Constructs an Overwrite and assigns it to:
     * @param overwrites a container {@link VBox}
     */
    public Overwrite(VBox overwrites) {
        this();
        delete.setOnAction(event -> {
                    overwrites.getChildren().remove(this);
                    JoJoWriteController.getInstance().refreshOverwrites();
                }
        );
        overwrites.getChildren().add(0, this);
    }

    private void separateBytes() {
        //System.out.println("Separating bytes...");
        byteMap.clear();
        String byteText = getOverwriteText();
        for (int i = 0; i < byteText.length(); i += 2) {
            // 2 digits represent 1 byte. DisplayROMAt() doubles the address to get the correct placement.
            byteMap.add(byteText.substring(i, i + 2));
        }
    }

    public ArrayList<String> getByteMap() {
        return byteMap;
    }

    public int getAddress() {
        if (addressField.getLength() == 0) return 0;
        return Integer.valueOf(addressField.getText(), 16);
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
        return addressField.getText() + ':' + overwriteField.getText() + '\n' + commentField.getText() + '\n';
    }

    // This COULD be generalized into a map of rules, but that's pointless because this will be the only String reading of this type.
    public static Overwrite fromCharSequence(VBox overwrites, CharSequence seq) throws IllegalStateException {
        Overwrite overwrite = new Overwrite(overwrites);
        StringBuilder addressText = new StringBuilder();
        StringBuilder overwriteText = new StringBuilder();
        StringBuilder commentText = new StringBuilder();
        boolean hitColon = false, hitNewline = false;
        for (int i = 0; i < seq.length(); i++) {
            char c = seq.charAt(i);
            if (c == ':') {
                if (hitColon)
                    commentText.append(':');
                else
                    hitColon = true;
            } else if (c == '\n') {
                if (hitNewline) {
                    throw new IllegalStateException("Hit newline twice during Overwrite.fromCharSequence()! Should be ADDRESS: BYTES\\nCOMMENT");
                } else {
                    hitNewline = true;
                }
            } else {
                if (hitNewline) commentText.append(c);
                else if (hitColon) overwriteText.append(c);
                else addressText.append(c);
            }
        }
        overwrite.setAddressText(addressText.toString());
        overwrite.setOverwriteText(overwriteText.toString());
        overwrite.setCommentText(commentText.toString());
        return overwrite;
    }

    /**
     * Requests focus to this Overwrites {@link Overwrite#overwriteField} and places the caret at the end.
     */
    public void focus() {
        overwriteField.requestFocus();
        overwriteField.positionCaret(overwriteField.getLength());
    }
}
