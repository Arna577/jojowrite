package net.arna.jojowrite.node;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import net.arna.jojowrite.JoJoWriteController;
import net.arna.jojowrite.asm.Compiler;
import net.arna.jojowrite.asm.instruction.Instruction;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.*;

import static net.arna.jojowrite.TextStyles.*;

/**
 * A {@link CodeArea} specialized for x16 RISC Assembly.
 */
public class AssemblyArea extends CodeArea {
    /**
     * Maps key characters with a style from {@link net.arna.jojowrite.TextStyles}.
     * Used in {@link AssemblyArea#styleAssemblyParagraph(int, String)}.
     */
    private final Map<Character, String> styleMap = new HashMap<>(
            Map.of(
                    '(', BASIC_TEXT,
                    ')', BASIC_TEXT,
                    ',', BASIC_TEXT,
                    '@', AT_SYMBOL,
                    '#', TEMP_OVERWRITE_TEXT
            )
    );

    private static final StringBuilder outputBuilder = new StringBuilder();

    public AssemblyArea() {
        setTextInsertionStyle(Collections.singleton(PARAMETER_TEXT));

        setParagraphGraphicFactory(LineNumberFactory.get(this));
        setContextMenu(new DefaultContextMenu());

        setOnKeyTyped(
                event -> {
                    JoJoWriteController.getInstance().clearOutput();
                    Compiler.clearErrors();

                    // Autocompletes the next address on the new line, provided the line on which the user pressed enter starts with an address.
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

    /**
     * Styles paragraphs within the document.
     * @param forceAll Whether to style the entire document or not.
     */
    public void updateVisualsOnly(boolean forceAll) {
        String[] paragraphs = getText().split("\n");
        int startIndex = 0;
        final int firstVisibleParagraphIndex = forceAll ? 0 : firstVisibleParToAllParIndex();
        final int lastVisibleParagraphIndex = forceAll ? paragraphs.length : lastVisibleParToAllParIndex();

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

    @Deprecated
    final List<Long> perfAvg = new ArrayList<>();

    /**
     * Processes all Assembly instructions and attempts compilation on them.
     * Styles all visible paragraphs.
     */
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

    /**
     * Styles a paragraph assuming it contains an address pointer and Assembly instruction.
     * @param startIndex The character index, within the context of {@link AssemblyArea#getText()}
     */
    private void styleAssemblyParagraph(int startIndex, String paragraph) {
        //long ms = System.currentTimeMillis();

        int paraLength = paragraph.length();
        if (paraLength < 9) return;

        // Style address pointer
        setStyleClass(startIndex, startIndex + 8, ADDRESS_TEXT);
        // Style colon separator
        setStyleClass(startIndex + 8, startIndex + 9, BASIC_TEXT);

        // Style keyword
        final int keywordEndIndex = paragraph.indexOf(' ');
        if (keywordEndIndex == -1) {
            setStyleClass( startIndex + 9, startIndex + paraLength, KEYWORD_TEXT);
            return;
        } else {
            setStyleClass( startIndex + 9, startIndex + keywordEndIndex, KEYWORD_TEXT);
        }

        // Style specific characters
        for (int i = keywordEndIndex; i < paraLength; i++) {
            String style = styleMap.get(paragraph.charAt(i));
            if (style == null) continue;
            int charIndex = startIndex + i;
            setStyleClass(charIndex, charIndex + 1, style);
        }

        //System.out.println("Completed AssemblyArea#styleAssemblyParagraph() in: " + (System.currentTimeMillis() - ms) + "ms.");
    }

    /**
     * Gets all possible instructions for this paragraph.
     * If there is only one, it is compiled and put in the {@link JoJoWriteController#output}
     * @return Whether the paragraph contains enough data to try to compile.
     */
    private boolean processAssemblyParagraph(int lineIndex, String paragraph) {
        String[] tokens = paragraph.split(":"); // [06123456:FOO BAR] -> [06123456], [FOO BAR]
        final String addressStr = tokens[0];
        if (tokens.length < 2) return false; // Must have address and instruction to begin styling

        // Ensure valid address format
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
