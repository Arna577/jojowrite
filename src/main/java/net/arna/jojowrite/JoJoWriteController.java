package net.arna.jojowrite;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.arna.jojowrite.JJWUtils.FileType;
import net.arna.jojowrite.node.Overwrite;
import net.arna.jojowrite.node.ROMTextArea;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.*;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;

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
        public CodeArea input;

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

        romScrollBar.setBlockIncrement(20.0);
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

                case ASSEMBLY -> {

                }

                case PATCH -> outWriter.append(input.getText());

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
     * @return The previous file from {@link JoJoWriteController#files}.
     */
    public File selectFile(FileType type) {
        File pastFile = files.put(type, JoJoWriteApplication.chooseFile(type));
        return pastFile;
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

    public void openPatch() {
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
        if (files.get(FileType.ROM) == null) {
            selectROMFile();
            if (files.get(FileType.ROM) == null) {
                return;
            }
        }

        File ROM = files.get(FileType.ROM);
        if (ROM != null && (openType == FileType.OVERWRITE || openType == FileType.ROM)) {
            romScrollBar.setMax(ROM.length());
            try {
                romFile = new RandomAccessFile(ROM, "r");
                displayROMAt(0);
            } catch (Exception e) {
                JJWUtils.printException(e, "An error occurred while opening ROM file.");
            }
        }
    }

    public void selectROMFile() {
        selectFile(FileType.ROM);
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
            JJWUtils.printException(e, "An error occurred while opening assembly file.");
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

    public void showInRom(int address, int length) {
        romScrollBar.setValue(address); // Causes displayROMAt(address) via ChangeListener
        romArea.selectRange(0, length);
    }


    // ADDRESS INCREMENTS PER BYTE, NOT 4 BITS

    /* (INCLUSIVE)
    FILE: 32 BYTES LONG
    0x00 - 0x20

    SCANNED:
    0x10 - 0x20

    OVW 1: 4 BYTES
    0x0F - 0x13
    DISPLAYS
    0x10 - 0x13

    OVW 2: 4 BYTES
    0x1F - 0x23
    DISPLAYS
    0x1F - 0x20
     */

    private static final int MAX_ROM_DISPLAY_LENGTH = 256; //4096
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
                romArea.append(JJWUtils.bytesToHex(newBytes), "basic-text");
                romArea.setWritingOriginal(false);

                for (Node node : overwrites.getChildren()) {
                    if (node instanceof Overwrite overwrite) {
                        for (var byteText : overwrite.getByteMap().entrySet()) {
                            // Bytes
                            int byteTextAddress = byteText.getKey() - address;
                            if (byteTextAddress < 0) continue;
                            if (byteTextAddress + 2 > romArea.getLength()) continue;

                            // Characters (2/Byte)
                            int characterIndex = byteTextAddress * 2;
                            romArea.replace(characterIndex, characterIndex + 2, byteText.getValue(), "overwritten-text");
                        }
                    }
                }
            }
        } catch (IOException e) {
            JJWUtils.printException(e, "Something went wrong while reading ROM file!");
        }
    }
}