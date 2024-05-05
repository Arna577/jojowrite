package net.arna.jojowrite;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import net.arna.jojowrite.JJWUtils.FileType;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

public class JoJoWriteController implements Initializable {
    public FileMap files = new FileMap(this);

    public FileType openType = FileType.ROM;

    @FXML
    public Label selectedFileDisplay;

    @FXML
    public VirtualizedScrollPane<?> assemblyScrollPane;
    @FXML
    public CodeArea input;

    @FXML
    public ScrollPane overwriteScrollPane;
    @FXML
    public VBox overwrites;

    @FXML
    public StyleClassedTextArea output;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        newOverwrite();
    }

    public void setOpenType(FileType type) {
        openType = type;

        // Special editor style for overwrite files
        if (type == FileType.OVERWRITE) {
            overwriteScrollPane.setVisible(true);
            assemblyScrollPane.setVisible(false);
        } else {
            assemblyScrollPane.setVisible(true);
            overwriteScrollPane.setVisible(false);
        }
    }

    public void saveFile(FileType type) {
        try ( FileWriter outWriter = new FileWriter(files.get(type)) ) {
            switch (type) {
                case OVERWRITE -> {

                }
                case ASSEMBLY -> {

                }
                case PATCH -> {

                }
                case ROM -> {
                    System.out.println("Attempted to write to ROM file! Writing to ROMs should be done via patching.");
                }
            }
            outWriter.close();
            System.out.println("Successfully saved " + type.name() + " file.");
        } catch (IOException e) {
            System.out.println("An error occurred while saving file.");
            e.printStackTrace();
        }
    }

    /**
     * Attempts to save a file based on type.
     * If the appropriate file of the type wasn't selected, prompts the user to select.
     */
    public void trySaveFile(FileType type) {
        if (files.get(type) == null) {
            selectAndSaveFile(type);
        } else {
            saveFile(type);
        }
    }

    /**
     * Selects a new {@link FileType} of file.
     * @return The previous file from {@link JoJoWriteController#files}.
     */
    public File selectFile(FileType type) {
        return files.put(type, JoJoWriteApplication.chooseFile(type));
    }

    public void selectAndSaveFile(FileType type) {
        if (selectFile(type) != null)
            saveFile(type);
    }

    /** PATCH **/
    public void trySavePatch(ActionEvent actionEvent) {
        trySaveFile(FileType.PATCH);
    }

    public File selectPatch(ActionEvent actionEvent) {
        return selectFile(FileType.PATCH);
    }

    public void selectAndSavePatch(ActionEvent actionEvent) {
        selectAndSaveFile(FileType.PATCH);
    }

    public void patchROM(ActionEvent actionEvent) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    /** ROM **/
    public File selectROM(ActionEvent actionEvent) {
        return selectFile(FileType.ROM);
    }

    /** OVERWRITE **/
    public void newOverwriteFile(ActionEvent actionEvent) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void openOverwriteFile(ActionEvent actionEvent) {
        setOpenType(FileType.OVERWRITE);
        input.clear();
        input.replace(0, input.getLength(), "", "basic-text");
    }

    public File selectOverwriteFile(ActionEvent actionEvent) {
        return selectFile(FileType.OVERWRITE);
    }

    public void saveOverwriteFile(ActionEvent actionEvent) {
        saveFile(FileType.OVERWRITE);
    }

    public void selectAndSaveOverwriteFile(ActionEvent actionEvent) {
        selectAndSaveFile(FileType.OVERWRITE);
    }

    /** ASSEMBLY **/
    public void newAssembly(ActionEvent actionEvent) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void openAssembly(ActionEvent actionEvent) {
        if (!files.containsKey(FileType.ASSEMBLY)) {
            selectAssembly(actionEvent);
            if (!files.containsKey(FileType.ASSEMBLY)) {
                return;
            }
        }

        setOpenType(FileType.ASSEMBLY);
        input.clear();

        try
        {
            FileReader reader = new FileReader( files.get(FileType.ASSEMBLY) );
            BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                input.appendText(line);
            }
            br.close();
            input.requestFocus();
        }
        catch (Exception e) {
            System.out.println("An error occurred while opening assembly file.");
            e.printStackTrace();
        }
    }

    public File selectAssembly(ActionEvent actionEvent) {
        return selectFile(FileType.ASSEMBLY);
    }

    public void saveAssembly(ActionEvent actionEvent) {
        saveFile(FileType.ASSEMBLY);
    }

    public void selectAndSaveAssembly(ActionEvent actionEvent) {
        selectAndSaveFile(FileType.ASSEMBLY);
    }

    /** OTHER **/
    public void updateSelectedFileDisplay() {
        StringBuilder out = new StringBuilder();
        files.forEach(
                (key, value) -> out.append(key.name()).append(": ").append(value).append(" | ")
        );
        System.out.println(out);
        selectedFileDisplay.setText(out.toString());
    }

    public void newOverwrite() {
        TextField overwriteField = new TextField("0x06");
        overwriteField.getStyleClass().add("code-area");
        overwriteField.setPromptText("ADDRESS, OVERWRITE");

        overwrites.getChildren().add(0, overwriteField);
    }
}