package net.arna.jojowrite.node;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.HBox;
import net.arna.jojowrite.JJWUtils;
import net.arna.jojowrite.JoJoWriteController;

import java.io.IOException;
import java.io.RandomAccessFile;

import static net.arna.jojowrite.TextStyles.BASIC_TEXT;
import static net.arna.jojowrite.TextStyles.OVERWRITTEN_TEXT;

public class ROMBox extends HBox {
    private final ScrollBar scrollBar;
    private final ROMArea area;
    private RandomAccessFile romRAF;

    public ROMBox() {
        /*
                        <ROMArea fx:id="romArea" wrapText="true" styleClass="code-area"/>
                <ScrollBar fx:id="romScrollBar" blockIncrement="20.0" orientation="VERTICAL" styleClass="scroll-pane"/>
         */
        area = new ROMArea();
        area.getStyleClass().add("code-area");

        scrollBar = new ScrollBar();
        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.getStyleClass().add("scroll-pane");

        getChildren().addAll(area, scrollBar);

        area.setScrollBar(scrollBar);
        area.widthProperty().addListener(
                (observable, oldValue, newValue) -> {
                    scrollBar.setUnitIncrement(Math.round(newValue.doubleValue() / ROMArea.BYTE_WIDTH));
                    scrollBar.setBlockIncrement(scrollBar.getUnitIncrement());
                }
        );

        scrollBar.valueProperty().addListener(
                (observable, oldValue, newValue) -> {
                    long romLength = JoJoWriteController.getInstance().files.get(JJWUtils.FileType.ROM).length() - area.getByteCapacity();
                    if (scrollBar.getMax() != romLength) {
                        //System.out.println("Incorrect romScrollBar maximum detected!");
                        scrollBar.setMax(romLength);
                    }

                    try {
                        displayROMAt(newValue.intValue());
                        displayOverwrites(JoJoWriteController.getInstance().overwrites);
                    } catch (IOException e) {
                        JJWUtils.printException(e, "Something went wrong while displaying ROM file!");
                    }
                }
        );
    }

    public void goToZero() {
        scrollBar.setValue(0.0);
    }

    public ROMArea getArea() {
        return area;
    }

    public ScrollBar getScrollBar() {
        return scrollBar;
    }

    public RandomAccessFile getRomRAF() {
        return romRAF;
    }

    public void setRomRAF(RandomAccessFile romRAF) {
        this.romRAF = romRAF;
        try {
            scrollBar.setMax(romRAF.length() - area.getByteCapacity());
        } catch (IOException e) {
            JJWUtils.printException(e, "Failed to read romRAF length!");
        }
    }

    public void displayROMAt(long address) throws IOException {
        if (romRAF == null) {
            System.out.println("Tried to ROMBox#displayROMAt() with null romRAF!");
            return;
        }

        area.setAddress(address);
        area.clear();

        byte[] bytes = new byte[area.getByteCapacity()];
        if (address > romRAF.length()) {
            throw new IOException("Attempted to read outside file bounds!");
        }

        romRAF.seek(address);
        int bytesRead = romRAF.read(bytes);
        if (bytesRead != -1) {
            byte[] newBytes = new byte[bytesRead];
            System.arraycopy(bytes, 0, newBytes, 0, bytesRead);
            //System.out.println("Read " + bytesRead + " bytes.");

            area.setWritingOriginal(true);
            area.append(JJWUtils.bytesToHex(newBytes), BASIC_TEXT);
            area.setWritingOriginal(false);
        }
    }

    public void displayOverwrites(final OverwriteBox overwrites) {
        long address = area.getAddress();

        for (Node node : overwrites.getChildren()) {
            if (node instanceof Overwrite overwrite) {
                var byteMap = overwrite.getByteStrings();
                int overwriteAddress = overwrite.getAddress();
                for (int i = 0; i < byteMap.size(); i++) {
                    // Bytes
                    // If we ever wish to support larger file sizes, all that is required is that overwrite addresses are also longs, then we downcast the subtraction.
                    int byteTextAddress = overwriteAddress - (int) address + i; // Relativize
                    if (byteTextAddress < 0) continue;
                    if (byteTextAddress >= area.getLength() / 2) continue;

                    // Characters (2/Byte)
                    int characterIndex = byteTextAddress * 2;
                    area.replace(characterIndex, characterIndex + 2, byteMap.get(i), OVERWRITTEN_TEXT);
                }
            }
        }

        area.resetUndoManager();
    }
}
