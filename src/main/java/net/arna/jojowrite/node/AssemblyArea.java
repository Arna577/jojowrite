package net.arna.jojowrite.node;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import net.arna.jojowrite.JJWUtils;
import net.arna.jojowrite.JoJoWriteController;
import net.arna.jojowrite.asm.Compiler;
import net.arna.jojowrite.asm.instruction.Instruction;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.arna.jojowrite.TextStyles.*;

public class AssemblyArea extends CodeArea {
    public AssemblyArea() {
        setTextInsertionStyle(Collections.singleton(PARAMETER_TEXT));

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
                                try {
                                    int lastAddress = Integer.parseUnsignedInt(address, 16);
                                    String nextAddress = Integer.toString(lastAddress + 2, 16);
                                    nextAddress = ("00000000" + nextAddress).substring(nextAddress.length());
                                    insertText(getCaretPosition(), nextAddress + ':');
                                } catch (NumberFormatException ignored) {}
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

    public void updateVisualsOnly() {
        String[] paragraphs = getText().split("\n");
        int startIndex = 0;
        final int firstVisibleParagraphIndex = firstVisibleParToAllParIndex();
        final int lastVisibleParagraphIndex = lastVisibleParToAllParIndex();

        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];
            if (paragraph.isEmpty()) {
                startIndex++;
            } else {
                int paraLength = paragraph.length();

                if (firstVisibleParagraphIndex <= i && i <= lastVisibleParagraphIndex) { // Visible
                    if (paragraph.startsWith("//")) { // Comments
                        setStyleClass(startIndex, startIndex + paraLength, COMMENT_TEXT);
                    } else { // Assembly
                        styleAssemblyParagraph(startIndex, paragraph);
                    }
                }

                startIndex += paraLength + 1; // Account for omitted \n
            }
        }
    }

    final List<Long> perfAvg = new ArrayList<>();
    private static final StringBuilder outputBuilder = new StringBuilder();
    public void update() {
        long ms = System.currentTimeMillis();

        outputBuilder.setLength(0);

        // getParagraphs() causes an IllegalAccessError due to some insane fucking module linking issue
        String[] paragraphs = getText().split("\n");
        int startIndex = 0;
        final int firstVisibleParagraphIndex = firstVisibleParToAllParIndex();
        final int lastVisibleParagraphIndex = lastVisibleParToAllParIndex();

        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];
            if (paragraph.isEmpty()) {
                startIndex++;
            } else {
                int paraLength = paragraph.length();

                boolean visible = firstVisibleParagraphIndex <= i && i <= lastVisibleParagraphIndex;

                if (paragraph.startsWith("//")) { // Comments
                    if (visible)
                        setStyleClass(startIndex, startIndex + paraLength, COMMENT_TEXT);
                } else { // Assembly
                    Compiler.openErrorLog(i);
                    boolean success = processAssemblyParagraph(i, paragraph);
                    if (visible && success)
                        styleAssemblyParagraph(startIndex, paragraph);
                }

                startIndex += paraLength + 1; // Account for omitted \n
            }
        }

        Compiler.displayErrors();
        JoJoWriteController.getInstance().appendToOutput(outputBuilder.toString());

        long delta = (System.currentTimeMillis() - ms);
        perfAvg.add(delta);
        System.out.println("Finished AssemblyArea#update() at: " + delta + "ms, average: " + perfAvg.stream().mapToLong(i -> i).sum() / perfAvg.size() + ".");
    }

    private void styleAssemblyParagraph(int startIndex, String paragraph) {
        if (paragraph.length() < 9) return;
        setStyleClass(startIndex, startIndex + 8, ADDRESS_TEXT);
        final String instructionStr = paragraph.substring(9);

        setStyleClass(startIndex + 8, startIndex + 9, BASIC_TEXT);

        final int keywordEndIndex = instructionStr.indexOf(' ');
        if (keywordEndIndex == -1) {
            setStyleClass( startIndex + 9, startIndex + paragraph.length(), KEYWORD_TEXT);
            return;
        } else {
            setStyleClass( startIndex + 9, startIndex + 9 + keywordEndIndex, KEYWORD_TEXT);
        }

        for (int i = keywordEndIndex; i < instructionStr.length(); i++) {
            String style = styleMap.get(instructionStr.charAt(i));
            if (style == null) continue;
            int charIndex = startIndex + 9 + i;
            setStyleClass(charIndex, charIndex + 1, style);
        }
    }

    /**
     * Gets all possible instructions for this paragraph.
     * If there is only one, it is compiled and put in the {@link JoJoWriteController#output}
     * @return Whether the paragraph contains enough data to try to compile.
     */
    private boolean processAssemblyParagraph(int lineIndex, String paragraph) {
        String[] tokens = paragraph.split(":");
        final String addressStr = tokens[0]; // 06123456:TEST

        if (addressStr.length() < 8) {
            Compiler.raiseError("Invalid address length: " + addressStr);
            return false;
        }

        try {
            if (Integer.parseUnsignedInt(addressStr, 16) % 2 != 0) {
                Compiler.raiseError("Unaligned address: " + addressStr);
                return false;
            }
        } catch (NumberFormatException e) {
            Compiler.raiseError("Invalid character in Hex literal");
            return false;
        }

        if (tokens.length < 2) return false;

        final String instructionStr = tokens[1];
        if (instructionStr.isEmpty()) return false;

        final List<Instruction> possible = Compiler.getPossibleInstructions(addressStr, instructionStr).toList();
        if (possible.size() == 1) {
            Compiler.clearErrors(lineIndex);
            outputBuilder.append(Compiler.compileToHexString(possible.get(0), addressStr, instructionStr)).append('\n');
        }

        return true;
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
