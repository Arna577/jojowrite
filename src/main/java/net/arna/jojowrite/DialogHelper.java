package net.arna.jojowrite;

import javafx.scene.control.*;
import javafx.stage.Modality;

import static net.arna.jojowrite.JoJoWriteApplication.DIALOG_STYLESHEET;

public class DialogHelper {
    public static Alert createStyledAlert(Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.getDialogPane().getStylesheets().add(DIALOG_STYLESHEET);
        return alert;
    }

    public static <T> Dialog<T> createStyledDialog() {
        Dialog<T> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().add(DIALOG_STYLESHEET);
        return dialog;
    }

    public static TextInputDialog createStyledTextInputDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.getDialogPane().getStylesheets().add(DIALOG_STYLESHEET);
        return dialog;
    }

    public static TextInputDialog createFindDialog() {
        TextInputDialog dialog = createStyledTextInputDialog();
        dialog.initModality(Modality.NONE);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().clear();
        dialogPane.getButtonTypes().addAll(ButtonType.NEXT, ButtonType.CLOSE);
        //final Button nextButton = (Button) dialogPane.lookupButton(ButtonType.NEXT);
        return dialog;
    }
}
