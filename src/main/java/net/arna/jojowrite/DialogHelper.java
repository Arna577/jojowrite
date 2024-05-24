package net.arna.jojowrite;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;

public class DialogHelper {
    public static Alert createStyledAlert(Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.getDialogPane().getStylesheets().add(JoJoWriteApplication.DIALOG_STYLESHEET);
        return alert;
    }

    public static <T> Dialog<T> createStyledDialog() {
        Dialog<T> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().add(JoJoWriteApplication.DIALOG_STYLESHEET);
        return dialog;
    }

    public static TextInputDialog createStyledTextInputDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.getDialogPane().getStylesheets().add(JoJoWriteApplication.DIALOG_STYLESHEET);
        return dialog;
    }
}
