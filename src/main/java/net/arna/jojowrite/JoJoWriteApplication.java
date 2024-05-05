package net.arna.jojowrite;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;

public class JoJoWriteApplication extends Application {
    private static Stage stage;
    private static FileChooser ROMFileChooser;
    private static FileChooser patchFileChooser;
    private static FileChooser assemblyFileChooser;
    private static FileChooser overwriteFileChooser;
    private static CodeArea codeArea;

    @Override
    public void start(final Stage stage) throws IOException {
        // Initialization
        FXMLLoader fxmlLoader = new FXMLLoader(JoJoWriteApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);

        String css = JoJoWriteApplication.class.getResource("stylesheet.css").toExternalForm();
        scene.getStylesheets().add(css);

        Font.loadFont(JoJoWriteApplication.class.getResourceAsStream("fonts/CourierPrime-Regular.ttf"), 16);

        initCodeArea(scene);

        stage.setTitle("JoJoWrite");
        stage.setScene(scene);
        stage.show();

        // Assignment
        JoJoWriteApplication.stage = stage;

        ROMFileChooser = new FileChooser();
        ROMFileChooser.setTitle("Select ROM File");

        patchFileChooser = new FileChooser();
        patchFileChooser.setTitle("Select Patch File");
        patchFileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JoJoWrite Patch Files", '*' + JJWUtils.PATCH_FILE_EXTENSION)
        );

        assemblyFileChooser = new FileChooser();
        assemblyFileChooser.setTitle("Select Assembly File");
        assemblyFileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("x16 RISC Assembly Files", '*' + JJWUtils.ASSEMBLY_FILE_EXTENSION)
        );

        overwriteFileChooser = new FileChooser();
        overwriteFileChooser.setTitle("Select Overwrite File");
        overwriteFileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JoJoWrite Overwrite Files", '*' + JJWUtils.OVERWRITE_FILE_EXTENSION)
        );
    }

    private void initCodeArea(Scene scene) throws InvalidObjectException {
        codeArea = (CodeArea) scene.lookup("#input");
        if (codeArea == null) throw new InvalidObjectException("No CodeArea found!");


    }

    public static File chooseFile(JJWUtils.FileType type) {
        return switch (type) {
            case ROM -> ROMFileChooser.showOpenDialog(stage);
            case PATCH -> patchFileChooser.showOpenDialog(stage);
            case ASSEMBLY -> assemblyFileChooser.showOpenDialog(stage);
            case OVERWRITE -> overwriteFileChooser.showOpenDialog(stage);
        };
    }


    public static void main(String[] args) {
        launch();
    }
}