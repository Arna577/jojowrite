package net.arna.jojowrite.node;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import net.arna.jojowrite.JJWUtils;
import net.arna.jojowrite.JoJoWriteController;
import net.arna.jojowrite.asm.Compiler;
import net.arna.jojowrite.asm.instruction.Instruction;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static net.arna.jojowrite.TextStyles.*;

public class AssemblyArea extends CodeArea {
    public AssemblyArea() {
        setTextInsertionStyle(Collections.singleton(BASIC_TEXT));

        setParagraphGraphicFactory(LineNumberFactory.get(this));
        setContextMenu(new DefaultContextMenu());

        setOnKeyTyped(
                event -> {
                    JoJoWriteController.getInstance().clearOutput();
                    Compiler.clearErrors();

                    if (event.getCharacter().equals("\r") || event.getCharacter().equals("\n")) {
                        int paragraphID = getCurrentParagraph();
                        if (--paragraphID >= 0) {
                            String lastParagraph = getText(paragraphID);
                            if (lastParagraph.length() >= 8) {
                                String address = lastParagraph.substring(0, 8);
                                if (JJWUtils.isHexadecimal(address)) {
                                    int lastAddress = Integer.valueOf(address, 16);
                                    String nextAddress = Integer.toString(lastAddress + 2, 16);
                                    nextAddress = ("00000000" + nextAddress).substring(nextAddress.length());
                                    insert(getCaretPosition(), nextAddress + ':', BASIC_TEXT);
                                }
                            }
                        }
                    }

                    update();
                }
        );
    }

    private final Map<Character, String> styleMap = Map.of(
            '(', BASIC_TEXT,
            ')', BASIC_TEXT,
            ',', BASIC_TEXT,
            '@', AT_SYMBOL,
            '#', TEMP_OVERWRITE_TEXT
    );

    List<Long> perfAvg = new ArrayList<>();
    public void update() {
        long ms = System.currentTimeMillis();

        // getParagraphs() causes an IllegalAccessError due to some insane fucking module linking issue
        String[] paragraphs = getText().split("\n");
        int startIndex = 0;
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];
            if (paragraph.isEmpty()) {
                startIndex++;
            } else {
                int paraLength = paragraph.length();
                Compiler.openErrorLog(i);

                if (paragraph.startsWith("//")) { // Comments
                    setStyleClass(startIndex, startIndex + paraLength, COMMENT_TEXT);
                } else { // Assembly
                    processAssemblyParagraph(i, paragraph);
                }

                startIndex += paraLength + 1; // Account for omitted \n
            }
        }

        Compiler.displayErrors();

        long delta = (System.currentTimeMillis() - ms);
        perfAvg.add(delta);
        System.out.println("Finished AssemblyArea#update() at: " + delta + "ms, average: " + perfAvg.stream().mapToLong(i -> i).sum() / perfAvg.size() + ".");
    }

    private void processAssemblyParagraph(int lineIndex, String paragraph) {
        String[] tokens = paragraph.split(":");
        final String addressStr = tokens[0]; // 06123456:TEST

        if (addressStr.length() < 8) {
            Compiler.raiseError("Invalid address length: " + addressStr);
            return;
        }

        if (!JJWUtils.isHexadecimal(addressStr)) {
            Compiler.raiseError("Invalid character in Hex literal");
            return;
        }

        if (Integer.valueOf(addressStr, 16) % 2 != 0) {
            Compiler.raiseError("Unaligned address: " + addressStr);
            return;
        }

        if (tokens.length < 2) return;

        final String instructionStr = tokens[1];
        if (instructionStr.isEmpty()) return;

        List<Instruction> possible = Compiler.getPossibleInstructions(addressStr, instructionStr).toList();
        if (possible.size() == 1) {
            Compiler.clearErrors(lineIndex);
            JoJoWriteController.getInstance().appendToOutput(
                    Compiler.compileToHexString(possible.get(0), addressStr, instructionStr) + '\n');
        }
    }

    private static class DefaultContextMenu extends ContextMenu
    {
        private final MenuItem fold, unfold, print;

        public DefaultContextMenu()
        {
            fold = new MenuItem( "Fold selected text" );
            fold.setOnAction( AE -> { hide(); fold(); } );

            unfold = new MenuItem( "Unfold from cursor" );
            unfold.setOnAction( AE -> { hide(); unfold(); } );

            print = new MenuItem( "Print" );
            print.setOnAction( AE -> { hide(); print(); } );

            getItems().addAll( fold, unfold, print );
        }

        /**
         * Folds multiple lines of selected text, only showing the first line and hiding the rest.
         */
        private void fold() {
            ((CodeArea) getOwnerNode()).foldSelectedParagraphs();
        }

        /**
         * Unfold the CURRENT line/paragraph if it has a fold.
         */
        private void unfold() {
            CodeArea area = (CodeArea) getOwnerNode();
            area.unfoldParagraphs( area.getCurrentParagraph() );
        }

        private void print() {
            System.out.println( ((CodeArea) getOwnerNode()).getText() );
        }
    }
}
