package net.arna.jojowrite;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import net.arna.jojowrite.JJWUtils.FileType;
import net.arna.jojowrite.node.Overwrite;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

public class JoJoWriteController implements Initializable {
    private static JoJoWriteController instance;

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

    public static JoJoWriteController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
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
                    for (int i = overwrites.getChildren().size() - 1; i >= 0; i--) {
                        if (overwrites.getChildren().get(i) instanceof Overwrite overwrite)
                            outWriter.append(overwrite.toString());
                    }
                }

                case ASSEMBLY -> {

                }

                case PATCH -> outWriter.append(input.getText());

                case ROM -> System.out.println("Attempted to write to ROM file! Writing to ROMs should be done via patching.");
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
        if (files.get(type) == null)
            selectFile(type);
        if (files.get(type) != null)
            saveFile(type);
    }

    /** PATCH **/
    public void trySavePatch() {
        trySaveFile(FileType.PATCH);
    }

    public void openPatch(ActionEvent actionEvent) {
        if (files.get(FileType.PATCH) == null) {
            selectPatch();
            if (files.get(FileType.PATCH) == null) {
                return;
            }
        }

        setOpenType(FileType.PATCH);
        input.clear();

        try
        {
            FileReader reader = new FileReader( files.get(FileType.PATCH) );
            BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
                input.appendText(line);
                input.appendText("\n");
            }
            br.close();
            input.requestFocus();
        }
        catch (Exception e) {
            System.out.println("An error occurred while opening assembly file.");
            e.printStackTrace();
        }
    }

    public File selectPatch() {
        return selectFile(FileType.PATCH);
    }

    public void selectAndSavePatch() {
        selectAndSaveFile(FileType.PATCH);
    }

    public void patchROM() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    /** ROM **/
    public File selectROM() {
        return selectFile(FileType.ROM);
    }

    /** OVERWRITE **/
    public void newOverwriteFile() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void openOverwriteFile() {
        if (files.get(FileType.OVERWRITE) == null) {
            selectOverwriteFile();
            if (files.get(FileType.OVERWRITE) == null) {
                return;
            }
        }

        setOpenType(FileType.OVERWRITE);
        overwrites.getChildren().removeIf(
                node -> node instanceof Overwrite
        );

        try
        {
            FileReader reader = new FileReader( files.get(FileType.OVERWRITE) );
            BufferedReader br = new BufferedReader(reader);
            boolean readingOverwrite = true;
            String line, overwriteText = "";
            while ((line = br.readLine()) != null) {
                if (readingOverwrite) {
                    overwriteText = line;
                } else {
                    overwriteText += '\n';
                    overwriteText += line;
                    Overwrite.fromCharSequence(overwrites, overwriteText);
                }

                readingOverwrite = !readingOverwrite;
            }
            br.close();
            input.requestFocus();
        }
        catch (Exception e) {
            System.out.println("An error occurred while opening assembly file.");
            e.printStackTrace();
        }
    }

    public File selectOverwriteFile() {
        return selectFile(FileType.OVERWRITE);
    }

    public void trySaveOverwriteFile() {
        trySaveFile(FileType.OVERWRITE);
    }

    public void selectAndSaveOverwriteFile() {
        selectAndSaveFile(FileType.OVERWRITE);
    }

    /** ASSEMBLY **/
    public void newAssembly() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    public void openAssembly(ActionEvent actionEvent) {
        if (files.get(FileType.ASSEMBLY) == null) {
            selectAssembly(actionEvent);
            if (files.get(FileType.ASSEMBLY) == null) {
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

    public void trySaveAssembly(ActionEvent actionEvent) {
        trySaveFile(FileType.ASSEMBLY);
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
        //System.out.println(out);
        selectedFileDisplay.setText(out.toString());
    }

    public void newOverwrite() {
        new Overwrite(overwrites);
    }

    public void showAsLua() {
        output.clear();

    }

    public void showInRom(ActionEvent event) {

    }
}