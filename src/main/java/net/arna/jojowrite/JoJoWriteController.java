package net.arna.jojowrite;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.arna.jojowrite.JJWUtils.FileType;
import net.arna.jojowrite.node.AssemblyArea;
import net.arna.jojowrite.node.Overwrite;
import net.arna.jojowrite.node.ROMTextArea;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

import static net.arna.jojowrite.TextStyles.BASIC_TEXT;
import static net.arna.jojowrite.TextStyles.OVERWRITTEN_TEXT;

public class JoJoWriteController implements Initializable {
    private static JoJoWriteController instance;

    private RandomAccessFile romFile;

    public FileMap files = new FileMap(this);

    public FileType openType = null;

    @FXML
    public Label selectedFileDisplay;

    @FXML
    public VirtualizedScrollPane<?> assemblyScrollPane;
        @FXML
        public AssemblyArea input;

    @FXML
    public ScrollPane overwriteScrollPane;
        @FXML
        public VBox overwrites;
        @FXML
        public HBox overwriteControls;

    @FXML
    public VirtualizedScrollPane<?> outputScrollPane;
        @FXML
        public StyleClassedTextArea output;

    private static Set<Node> romNodes, overwriteNodes, assemblyNodes;

    @FXML
    public HBox romTextBox;
        @FXML
        public VirtualizedScrollPane<?> precisionROMScrollPane;
        @FXML
        public ScrollBar romScrollBar;
        @FXML
        public ROMTextArea romArea;

    public static JoJoWriteController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;

        assemblyNodes = Set.of(assemblyScrollPane, outputScrollPane);
        overwriteNodes = Set.of(romTextBox, overwriteScrollPane, overwrites, overwriteControls);
        romNodes = Set.of(romTextBox);

        romScrollBar.valueProperty().addListener(
            (observable, oldValue, newValue) -> {
                try {
                    displayROMAt(newValue.intValue());
                } catch (IOException e) {
                    JJWUtils.printException(e, "Something went wrong while displaying ROM.");
                }
            }
        );

        setOpenType(FileType.ROM);
    }

    public void setOpenType(FileType type) {
        openType = type;

        // Special editor style for overwrite files
        switch (type) {
            case OVERWRITE -> {
                overwriteNodes.forEach(node -> node.setVisible(true));
                assemblyNodes.forEach(node -> node.setVisible(false));
            }
            case ASSEMBLY -> {
                overwriteNodes.forEach(node -> node.setVisible(false));
                assemblyNodes.forEach(node -> node.setVisible(true));
            }
            case ROM -> {
                overwriteNodes.forEach(node -> node.setVisible(false));
                assemblyNodes.forEach(node -> node.setVisible(false));
                romNodes.forEach(node -> node.setVisible(true));
            }
        }
    }

    public void saveFile(FileType type) {
        try ( FileWriter outWriter = new FileWriter(files.get(type)) ) {
            switch (type) {
                case OVERWRITE -> {
                    for (int i = overwrites.getChildren().size() - 1; i >= 0; i--)
                        if (overwrites.getChildren().get(i) instanceof Overwrite overwrite)
                            outWriter.append(overwrite.toString());
                }

                case ASSEMBLY, PATCH -> outWriter.append(input.getText());

                case ROM -> System.out.println("Attempted to write to ROM file! Writing to ROMs should be done via patching.");
            }
            outWriter.close();
            System.out.println("Successfully saved " + type.name() + " file.");
        } catch (IOException e) {
            JJWUtils.printException(e, "An error occurred while saving file.");
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
     * @return The selected file.
     */
    public File selectFile(FileType type) {
        File selected = JoJoWriteApplication.chooseFile(type);
        files.put(type, selected);
        return selected;
    }

    /**
     * Selects a new {@link FileType} of file to save.
     * @return A new {@link File}.
     */
    public File newFile(FileType type) {
        File toSave = JoJoWriteApplication.saveFile(type);
        files.put(type, toSave);
        return toSave;
    }

    public void selectAndSaveFile(FileType type) {
        if (newFile(type) != null)
            saveFile(type);
    }

    /** PATCH **/
    public void trySavePatch() {
        trySaveFile(FileType.PATCH);
    }

    public void openPatch() {
        if (selectPatch() == null) return;

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
            JJWUtils.printException(e, "An error occurred while opening assembly file.");
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
    public void openROMFile() {
        File ROM = selectROMFile();
        if (ROM == null) return;

        if (openType == FileType.OVERWRITE || openType == FileType.ROM) {
            romScrollBar.setMax(ROM.length());
            try {
                romFile = new RandomAccessFile(ROM, "r");
                displayROMAt(0);
            } catch (Exception e) {
                JJWUtils.printException(e, "An error occurred while opening ROM file.");
            }
        }
    }

    public File selectROMFile() {
        return selectFile(FileType.ROM);
    }

    /** OVERWRITE **/
    public void newOverwriteFile() {
        if (files.get(FileType.OVERWRITE) != newFile(FileType.OVERWRITE)) // Selected new file
            overwrites.getChildren().clear();
        setOpenType(FileType.OVERWRITE);
    }

    public void openOverwriteFile() {
        if (selectOverwriteFile() == null)
            return;

        setOpenType(FileType.OVERWRITE);
        //TODO: figure out java destructors
        overwrites.getChildren().removeIf(node -> node instanceof Overwrite);

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

            romArea.requestFocus();
            refreshOverwrites();
        }
        catch (Exception e) {
            JJWUtils.printException(e, "An error occurred while opening assembly file.");
        }

        /*
        Random random = new Random();
        for (int i = 0; i < 2048; i++) {
            var ovr = new Overwrite(overwrites);
            ovr.setAddressText(Integer.toHexString(random.nextInt(0x07fffff)));
            String testText = Long.toHexString(random.nextLong(0x7fffffffffffffffL));
            if (testText.length() % 2 == 1)
                testText = testText.substring(1);
            ovr.setOverwriteText(testText);
        }

        for (int i = 0; i < 128; i++) {
            var ovr = new Overwrite(overwrites);
            ovr.setAddressText(Integer.toHexString(i));
            ovr.setOverwriteText("FF");
        }
         */
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
        newFile(FileType.ASSEMBLY);
        setOpenType(FileType.ASSEMBLY);
    }

    public void openAssembly() {
        if (newFile(FileType.ASSEMBLY) == null)
            return;

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
            JJWUtils.printException(e, "An error occurred while opening assembly file.");
        }
    }

    public File selectAssembly() {
        return selectFile(FileType.ASSEMBLY);
    }

    public void trySaveAssembly() {
        trySaveFile(FileType.ASSEMBLY);
    }

    public void selectAndSaveAssembly() {
        selectAndSaveFile(FileType.ASSEMBLY);
    }

    /**
     * Displays the currently selected files at the bottom of the screen.
     */
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
        throw new UnsupportedOperationException("Not implemented!");
    }

    public void showInROM(int address, int length) {
        if (romArea.getText().isEmpty()) return;
        romScrollBar.setValue(address); // Causes displayROMAt(address) via ChangeListener
        romArea.selectRange(0, length);
    }

    // ADDRESS INCREMENTS PER BYTE (OR TWO HEX DIGITS)
    private static final int MAX_ROM_DISPLAY_LENGTH = 512; //4096
    // Self-explanatory.
    private void displayROMAt(int address) throws IOException {
        if (romFile == null) return;

        romArea.setAddress(address);
        romArea.clear();

        try {
            byte[] bytes = new byte[MAX_ROM_DISPLAY_LENGTH];
            if (address > romFile.length()) {
                throw new IOException("Attempted to read outside file bounds!");
            }

            romFile.seek(address);
            int bytesRead = romFile.read(bytes);
            if (bytesRead != -1) {
                byte[] newBytes = new byte[bytesRead];
                System.arraycopy(bytes, 0, newBytes, 0, bytesRead);
                //System.out.println("Read " + bytesRead + " bytes.");

                romArea.setWritingOriginal(true);
                romArea.append(JJWUtils.bytesToHex(newBytes), BASIC_TEXT);
                romArea.setWritingOriginal(false);

                displayROMOverwrites();
            }
        } catch (IOException e) {
            JJWUtils.printException(e, "Something went wrong while reading ROM file!");
        }
    }

    /**
     * Clears out any temporary changes to the {@link JoJoWriteController#romArea} and displays all in-bounds {@link JoJoWriteController#overwrites}.
     */
    public void refreshOverwrites() {
        romArea.restoreOriginal();
        displayROMOverwrites();
    }

    /**
     * Displays all Overwrites that reside within the currently rendered {@link JoJoWriteController#romArea} as text styled with {@link TextStyles#OVERWRITTEN_TEXT}.
     */
    private void displayROMOverwrites() {
        int address = romArea.getAddress();

        for (Node node : overwrites.getChildren()) {
            if (node instanceof Overwrite overwrite) {
                var byteMap = overwrite.getByteMap();
                int overwriteAddress = overwrite.getAddress();
                for (int i = 0; i < byteMap.size(); i++) {
                    // Bytes
                    int byteTextAddress = overwriteAddress - address + i; // Relativize
                    if (byteTextAddress < 0) continue;
                    if (byteTextAddress >= romArea.getLength() / 2) continue;

                    // Characters (2/Byte)
                    int characterIndex = byteTextAddress * 2;
                    romArea.replace(characterIndex, characterIndex + 2, byteMap.get(i), OVERWRITTEN_TEXT);
                }
            }
        }

        romArea.resetUndoManager();
    }

    public void showOverwriteHelp(ActionEvent actionEvent) {
        var dialog = new Alert(Alert.AlertType.INFORMATION,
                """
                        Overwrites are the building blocks of a rom-hack.
                        In JoJoWrite they are marked as red text, and are coerced into aligning with byte data (i.e. their characters come in pairs).
                        
                        It is highly recommended to comment ones overwrites to ensure understanding of their purpose.
                        
                        Yellow text is a temporary overwrite that will be dismissed if:
                            ●   another overwrite is updated
                            ●   the current ROM block is moved
                        Temporary overwrites are confirmed by pressing enter.
                        If the caret is at a temporary overwrite, every preceding byte containing uninterrupted yellow characters will be extracted to an overwrite.
                        It is also possible to select a segment of a temporary overwrite to extract, requiring at least two characters to be selected.
                        
                        When placed inside a .overwrite file, they take the form of:
                        [ADDRESS]:[DATA]\\n[COMMENT]\\n""");
        dialog.setTitle("Overwrite Info");
        dialog.showAndWait();
    }
}