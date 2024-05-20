package net.arna.jojowrite;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.HBox;
import javafx.stage.StageStyle;
import net.arna.jojowrite.JJWUtils.FileType;
import net.arna.jojowrite.asm.Compiler;
import net.arna.jojowrite.node.*;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.*;
import java.net.URL;
import java.util.*;

import static net.arna.jojowrite.JJWUtils.ASSEMBLY_FILE_EXTENSION;
import static net.arna.jojowrite.JJWUtils.OVERWRITE_FILE_EXTENSION;
import static net.arna.jojowrite.TextStyles.BASIC_TEXT;
import static net.arna.jojowrite.TextStyles.OVERWRITTEN_TEXT;

public class JoJoWriteController implements Initializable {
    private static JoJoWriteController instance;

    private RandomAccessFile romRAF, outRomRAF;
    private File outROM;
    public final FileMap files = new FileMap(this);

    public FileType openType = null;

    @FXML
    public Label selectedFileDisplay;

    @FXML
    public VirtualizedScrollPane<?> assemblyScrollPane;
        @FXML
        public AssemblyArea assemblyArea;
    @FXML
    public ScrollPane errorScrollPane;
        @FXML
        public TextArea errorArea;

    @FXML
    public ScrollPane overwriteScrollPane;
        @FXML
        public OverwriteBox overwrites;
    @FXML
    public HBox overwriteControls;

    @FXML
    public VirtualizedScrollPane<?> outputScrollPane;
        @FXML
        public StyleClassedTextArea output;

    @FXML
    public HBox patchControls;
    @FXML
    public VirtualizedScrollPane<?> patchScrollPane;
        @FXML
        public PatchArea patchArea;

    @FXML
    public HBox romTextBox;
        @FXML
        public VirtualizedScrollPane<?> precisionROMScrollPane;
        @FXML
        public ScrollBar romScrollBar;
        @FXML
        public ROMArea romArea;

    private Map<FileType, Set<Node>> fileTypeNodeMap;

    private final Timer overwriteLoadTimer = new Timer();
    public static JoJoWriteController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;

        Set<Node> assemblyNodes = Set.of(errorScrollPane, assemblyScrollPane, outputScrollPane);
        Set<Node> overwriteNodes = Set.of(romTextBox, overwriteScrollPane, overwrites, overwriteControls);
        //Set<Node> romNodes = Set.of(romTextBox);
        Set<Node> patchNodes = Set.of(patchControls, patchScrollPane);

        fileTypeNodeMap = Map.of(
                FileType.ASSEMBLY, assemblyNodes,
                FileType.PATCH, patchNodes,
                FileType.OVERWRITE, overwriteNodes
                //FileType.ROM, romNodes
        );

        romScrollBar.valueProperty().addListener(
                (observable, oldValue, newValue) -> displayROMAt(newValue.intValue())
        );

        //todo: figure out VirtualizedScrollPane hooks
        //assemblyScrollPane.addEventHandler(DragEvent.ANY, event -> assemblyArea.updateVisualsOnly(false));

        overwriteLoadTimer.scheduleAtFixedRate(new OverwriteLoadTask(), 0, 2);
        overwrites.assignParentPane(overwriteScrollPane);

        Compiler.setErrorOutputArea(errorArea);

        setOpenType(FileType.ROM);
    }

    public void setOpenType(FileType type) {
        openType = type;

        fileTypeNodeMap.forEach((key, value) -> {
            boolean keyMatches = type == key;
            value.forEach(
                    node -> {
                        node.setVisible(keyMatches);
                        node.setManaged(keyMatches);
                    }
            );
        });
    }

    public void saveFile(FileType type) {
        try ( FileWriter outWriter = new FileWriter(files.get(type)) ) {
            switch (type) {
                case OVERWRITE -> {
                    for (int i = 0; i < overwrites.size(); i++)
                        if (overwrites.get(i) instanceof Overwrite overwrite)
                            outWriter.append(overwrite.toString()).append("\n");
                }

                case ASSEMBLY -> outWriter.append(assemblyArea.getText());

                case PATCH -> outWriter.append(patchArea.getText());

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
    public void newPatch() {
        newFile(FileType.PATCH);
        setOpenType(FileType.PATCH);
    }

    public void trySavePatch() {
        trySaveFile(FileType.PATCH);
    }

    public void openPatch() {
        if (selectPatch() == null) return;

        setOpenType(FileType.PATCH);
        patchArea.clear();

        try
        {
            FileReader reader = new FileReader( files.get(FileType.PATCH) );
            BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
                patchArea.appendText(line);
                patchArea.appendText("\n");
            }
            br.close();
            patchArea.requestFocus();
        }
        catch (Exception e) {
            JJWUtils.printException(e, "An error occurred while opening patch file.");
        }
    }

    public File selectPatch() {
        return selectFile(FileType.PATCH);
    }

    public void selectAndSavePatch() {
        selectAndSaveFile(FileType.PATCH);
    }

    public void addLoadedToPatch() {
        File rom = files.get(FileType.ROM);
        if (rom == null) {
            patchArea.appendText("\nNo ROM selected!");
            patchArea.selectLine();
            patchArea.requestFocus();
            return;
        }

        patchArea.appendText(rom.getPath() + '\n');

        files.forEach(
                (type, file) -> {
                    if (type != FileType.PATCH && type != FileType.ROM) {
                        patchArea.appendText(file.getPath() + '\n');
                    }
                }
        );
    }

    public void patchROM() {
        // Ensure patch and output file are set
        if (outROM == null) selectOutROMFile();
        if (outROM == null) return;
        File patch = files.get(FileType.PATCH);
        if (patch == null) patch = selectPatch();
        if (patch == null) return;

        File sourceROM = null;
        Map<File, FileType> toProcess = new HashMap();
        try {
            FileReader reader = new FileReader(patch);
            BufferedReader br = new BufferedReader(reader);
            sourceROM = new File(br.readLine());
            String line;
            while ((line = br.readLine()) != null) {
                FileType type;
                if (line.endsWith(OVERWRITE_FILE_EXTENSION)) {
                    type = FileType.OVERWRITE;
                } else if (line.endsWith(ASSEMBLY_FILE_EXTENSION)) {
                    type = FileType.ASSEMBLY;
                } else {
                    System.out.println("Wrong file type referenced in patch file; " + line);
                    continue;
                }
                toProcess.put(new File(line), type);
            }
            br.close();
        }
        catch (Exception e) {
            JJWUtils.printException(e, "An error occurred while reading patch file.");
        }

        if (sourceROM == null || !sourceROM.exists()) {
            System.out.println("Couldn't locate source ROM for patching.");
            return;
        }
        System.out.println("SRC: " + sourceROM);
        toProcess.forEach(
                (file, type) -> System.out.println(file + "; " + type)
        );
        System.out.println("DEST: " + outROM);
    }

    /** ROM **/
    public void openROMFile() {
        File ROM = selectROMFile();
        if (ROM == null) return;

        if (openType == FileType.OVERWRITE || openType == FileType.ROM) {
            romScrollBar.setMax(ROM.length());
            try {
                romRAF = new RandomAccessFile(ROM, "r");
                displayROMAt(0);
            } catch (Exception e) {
                JJWUtils.printException(e, "An error occurred while opening ROM file.");
            }
        }
    }

    public File selectROMFile() {
        return selectFile(FileType.ROM);
    }

    public void selectOutROMFile() {
        outROM = JoJoWriteApplication.chooseFile(FileType.ROM);
        try {
            outRomRAF = new RandomAccessFile(outROM, "rw");
        } catch (FileNotFoundException e) {
            System.out.println("Selected invalid file!");
        }
    }

    /** OVERWRITE **/
    public void newOverwriteFile() {
        if (files.get(FileType.OVERWRITE) != newFile(FileType.OVERWRITE)) // Selected new file
            overwrites.clear();
        setOpenType(FileType.OVERWRITE);
    }

    private class OverwriteLoadTask extends TimerTask {
        @Override
        public void run() {
            Platform.runLater(() -> {
                String raw = overwriteStringPairs.poll();
                if (raw == null) return;
                Overwrite.fromString(raw).assignOverwriteBox(overwrites, false);
                overwrites.layout();
                overwrites.updateVisibility();
            });
        }
    }

    public static final Queue<String> overwriteStringPairs = new LinkedList<>();
    public void openOverwriteFile() {
        if (selectOverwriteFile() == null)
            return;

        setOpenType(FileType.OVERWRITE);
        overwrites.clear();

        try (FileReader fileReader = new FileReader(files.get(FileType.OVERWRITE))) {
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null)
                overwriteStringPairs.add(line);
            bufferedReader.close();
        } catch (Exception e) {
            JJWUtils.printException(e, "An error occurred while opening overwrite file.");
        }

        romArea.requestFocus();
        refreshOverwrites();
        overwrites.updateVisibility();
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
        assemblyArea.clear();

        try
        {
            FileReader reader = new FileReader( files.get(FileType.ASSEMBLY) );
            BufferedReader br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
                assemblyArea.appendText(line);
                assemblyArea.appendText("\n");
            }
            br.close();

            if (assemblyArea.getText().isEmpty()) {
                assemblyArea.appendText("//Example comment & instruction\n");
                assemblyArea.appendText("06280000:NOP");
            }

            assemblyArea.requestFocus();
            assemblyArea.update();
            assemblyArea.updateVisualsOnly(true);
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
        createOverwrite("", "", "");
    }

    public void createOverwrite(String addressText, String overwriteText, String commentText) {
        overwriteScrollPane.setVvalue(0.0);
        Overwrite overwrite = new Overwrite(overwrites);
        overwrite.bufferText(addressText, overwriteText, commentText);
        overwrites.updateVisibility();
        refreshOverwrites();
        overwrite.focus();
    }

    public void showOverwritesAsLUA() {
        output.clear();
        throw new UnsupportedOperationException("Not implemented!");
    }

    public void showInROM(int address, int length) {
        if (romArea.getText().isEmpty()) return;
        try {
            if (address > romRAF.length()) return;
            romScrollBar.setValue(address); // Causes displayROMAt(address) via ChangeListener
            romArea.selectRange(0, length);
        } catch (IOException e) {
            JJWUtils.printException(e, "Something went wrong while reading the ROM files length!");
        }
    }

    // ADDRESS INCREMENTS PER BYTE (OR TWO HEX DIGITS)
    private static final int MAX_ROM_DISPLAY_LENGTH = 1024; //4096
    // Self-explanatory.
    private void displayROMAt(int address) {
        if (romRAF == null) return;

        romArea.setAddress(address);
        romArea.clear();

        try {
            byte[] bytes = new byte[MAX_ROM_DISPLAY_LENGTH];
            if (address > romRAF.length()) {
                throw new IOException("Attempted to read outside file bounds!");
            }

            romRAF.seek(address);
            int bytesRead = romRAF.read(bytes);
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

    public void showOverwriteHelp() {
        var dialog = createStyledAlert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Overwrite Info");
        dialog.setHeaderText(
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
                        [ADDRESS];[DATA];[COMMENT]"""
        );
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.getDialogPane().getStyleClass().add("help-dialog");
        dialog.showAndWait();
    }

    public void showHotkeyHelp() {

    }

    private Alert createStyledAlert(Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.getDialogPane().getStylesheets().add(JoJoWriteApplication.DIALOG_STYLESHEET);
        return alert;
    }

    public void clearOutput() {
        output.clear();
    }

    public void appendToOutput(String s) {
        output.append(s, BASIC_TEXT);
    }
}