package net.arna.jojowrite;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import net.arna.jojowrite.JJWUtils.FileType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class JoJoWriteController {
    public FileMap files = new FileMap(this);

    public FileType currentlyOpen = FileType.ROM;

    @FXML
    public Label lineCounter;
    @FXML
    public TextArea input;
    @FXML
    public TextArea output;
    @FXML
    public Label selectedFileDisplay;

    public JoJoWriteController() {
        System.out.println(input);
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
            System.out.println("An error occurred.");
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
    public void newOverwrite(ActionEvent actionEvent) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void openOverwrite(ActionEvent actionEvent) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public File selectOverwrite(ActionEvent actionEvent) {
        return selectFile(FileType.OVERWRITE);
    }

    public void saveOverwrite(ActionEvent actionEvent) {
        saveFile(FileType.OVERWRITE);
    }

    public void selectAndsaveOverwrite(ActionEvent actionEvent) {
        selectAndSaveFile(FileType.OVERWRITE);
    }

    /** ASSEMBLY **/
    public void newAssembly(ActionEvent actionEvent) {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void openAssembly(ActionEvent actionEvent) {
        throw new UnsupportedOperationException("Not implemented.");
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
}