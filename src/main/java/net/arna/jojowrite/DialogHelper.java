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

    public static TextInputDialog createStyledTextInputDialog(String title, String header) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.getDialogPane().getStylesheets().add(DIALOG_STYLESHEET);
        return dialog;
    }

    public static TextInputDialog createFindDialog(String title, String header) {
        TextInputDialog dialog = createStyledTextInputDialog(title, header);
        dialog.initModality(Modality.NONE);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().removeAll(ButtonType.OK);
        dialogPane.getButtonTypes().addAll(ButtonType.NEXT);
        //final Button nextButton = (Button) dialogPane.lookupButton(ButtonType.NEXT);
        return dialog;
    }
}
