package net.arna.jojowrite.node;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.arna.jojowrite.JoJoWriteController;

import java.io.StringReader;
import java.util.Set;

/*
 * [Address Text]:[Overwrite Text][Show in ROM][Delete]
 * [Comment]
 */

/**
 * A {@link VBox} Node used for user I/O of .overwrite data.
 * todo:The {@link Overwrite#overwriteField} is coerced into a format of [HEX POINTER: HEX BYTE LIST].
 * The {@link Overwrite#commentField} is used so the user has an easier time remembering & understanding what their changes are doing.
 */

/*
 * todo: Overwrite > From ROM Differences
 *  direct overwriting (ROM view)
 *  hotkeys (Ctrl+G > GOTO | Ctrl+F > FIND)
 */
public class Overwrite extends VBox {
    private final HBox overwriteAndOptions;
    private final TextField addressField;
    private final TextField overwriteField;
    private final TextField commentField;
    private final Button showInROM;
    private final Button delete;

    private final Set<String> numerals = Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");

    public Overwrite(VBox overwrites) {
        this.getStyleClass().add("code-area");

        overwriteAndOptions = new HBox();

        addressField = new TextField("0x06");
        addressField.getStyleClass().add("main");
        addressField.setPromptText("Address");
        addressField.setMinWidth(120.0);
        //TODO: figure out how to filter user input (boo womp)

        Label addressSeparator = new Label(":");

        overwriteField = new TextField();
        overwriteField.getStyleClass().add("code-area");
        overwriteField.setPromptText("Overwrite Bytes");
        overwriteField.setPrefWidth(320.0);

        overwriteField.setOnKeyTyped(
                keyEvent -> {
                    double newLength = overwriteField.getLength() * overwriteField.getFont().getSize();
                    if (newLength < 320.0) newLength = 320.0;
                    overwriteField.setPrefWidth(newLength);
                }
        );

        showInROM = new Button("Show in ROM");
        showInROM.setMinWidth(130.0);
        showInROM.setOnAction(event -> JoJoWriteController.getInstance().showInRom(event));

        delete = new Button("Delete");
        delete.getStyleClass().add("delete-button");
        delete.setMinWidth(85.0);
        delete.setOnAction(event -> overwrites.getChildren().remove(this));

        overwriteAndOptions.getChildren().addAll(addressField, addressSeparator, overwriteField, showInROM, delete);

        commentField = new TextField();
        commentField.getStyleClass().add("code-area");
        commentField.setPromptText("Explain what this overwrite does");

        getChildren().addAll(overwriteAndOptions, commentField);

        overwrites.getChildren().add(0, this);
    }

    public void setAddressText(String text) {
        addressField.setText(text);
    }

    public void setOverwriteText(String text) {
        overwriteField.setText(text);
    }

    public void setCommentText(String text) {
        commentField.setText(text);
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
                if (hitColon) {
                    throw new IllegalStateException("Hit colon twice during Overwrite.fromCharSequence()! Should be ADDRESS: BYTES\\nCOMMENT");
                } else {
                    hitColon = true;
                }
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
}
