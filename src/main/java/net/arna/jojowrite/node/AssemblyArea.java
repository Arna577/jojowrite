package net.arna.jojowrite.node;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import net.arna.jojowrite.DialogHelper;
import net.arna.jojowrite.JoJoWriteController;
import net.arna.jojowrite.asm.Compiler;
import net.arna.jojowrite.asm.instruction.Instruction;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;

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

    private final TextInputDialog findDialog = DialogHelper.createStyledTextInputDialog();
    private final TextInputDialog goToDialog = DialogHelper.createStyledTextInputDialog();

    public static final String commentPrefix = "//";

    public AssemblyArea() {
        setTextInsertionStyle(Collections.singleton(PARAMETER_TEXT));

        setParagraphGraphicFactory(LineNumberFactory.get(this));

        setContextMenu(new DefaultContextMenu());

        initDialogs();

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.G) { // Ctrl + G - Go to Line
                    event.consume();
                    goToDialog.showAndWait().ifPresent(lineNum -> {
                        if (lineNum.isEmpty()) return;
                        try {
                            int paragraph = Integer.parseUnsignedInt(lineNum, 10) - 1; // Not zero-indexed
                            if (paragraph > getText().split("\n").length) return;
                            selectRange(paragraph, 0, paragraph, 0);
                        } catch (Exception ignored) {}
                    });
                }
                //todo: actually good find menu (find all occurrences then let user go through them)
                if (event.getCode() == KeyCode.F) { // Ctrl + F - Find Text
                    event.consume();
                    findDialog.showAndWait().ifPresent(text -> {
                        if (text.isEmpty()) return;
                        int index = getText().indexOf(text);
                        if (index == -1) return;
                        selectRange(index, index + text.length());
                    });
                }
            }
        });

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

    private void initDialogs() {
        // Styling for Find Hex String Dialog
        findDialog.setTitle("Find");
        findDialog.setHeaderText("Find: ");
        findDialog.getDialogPane().getStyleClass().add("help-dialog");
        findDialog.getEditor().getStyleClass().add("main");

        // Styling for Go To Dialog
        goToDialog.setTitle("Go To");
        goToDialog.setHeaderText("Line: ");
        goToDialog.getDialogPane().getStyleClass().add("help-dialog");
        goToDialog.getEditor().getStyleClass().add("main");
    }

    /**
     * Styles paragraphs within the document.
     * @param forceAll Whether to style the entire document or not.
     */
    public void updateVisualsOnly(boolean forceAll) {
        long ms = System.currentTimeMillis();

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
                    if (paragraph.startsWith(commentPrefix)) { // Comments
                        setStyleClass(startIndex, startIndex + paraLength, COMMENT_TEXT);
                    } else { // Assembly
                        styleAssemblyParagraph(startIndex, i, paragraph);
                    }
                }

                startIndex += paraLength + 1; // Account for omitted \n
            }
        }

        System.out.println("Finished AssemblyArea#updateVisualsOnly() in: " + (System.currentTimeMillis() - ms) + "ms.");
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
        final int firstModifiedCharacter = getSelection().getStart();
        final int lastModifiedCharacter = getSelection().getEnd();

        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];
            if (paragraph.isEmpty()) {
                startIndex++;
            } else {
                int paraLength = paragraph.length();

                boolean visible = firstVisibleParagraphIndex <= i && i <= lastVisibleParagraphIndex;
                // Also catches the next few paragraphs, but that's fine
                // +/- paraLength serves as a buffer to ensure the paragraph was caught in the range.
                boolean modified = lastModifiedCharacter >= startIndex - paraLength && firstModifiedCharacter <= startIndex + paraLength;

                if (paragraph.startsWith(commentPrefix)) { // Comments
                    if (visible) setStyleClass(startIndex, startIndex + paraLength, COMMENT_TEXT);
                } else { // Assembly
                    Compiler.openErrorLog(i);
                    boolean success = processAssemblyParagraph(i, paragraph);
                    if ( (visible || modified) && success )
                        styleAssemblyParagraph(startIndex, i, paragraph);
                }

                startIndex += paraLength + 1; // Account for omitted \n
            }
        }

        Compiler.displayErrors();
        JoJoWriteController.getInstance().appendToOutput(outputBuilder.toString());

        long delta = (System.currentTimeMillis() - ms);
        perfAvg.add(delta);
        System.out.println("Finished AssemblyArea#update() in: " + delta + "ms, average: " + perfAvg.stream().mapToLong(i -> i).sum() / perfAvg.size() + ".");
    }

    private static final StyleSpans<? extends Collection<String>> addressPrefixStyle =
            StyleSpans.singleton(Collections.singleton(ADDRESS_TEXT), 8).append(
                    new StyleSpan<>(Collections.singleton(BASIC_TEXT), 1));
    /**
     * Styles a paragraph assuming it contains an address pointer and Assembly instruction.
     * @param startIndex The character index, within the context of {@link AssemblyArea#getText()}
     */
    private void styleAssemblyParagraph(int startIndex, int paraIndex, String paragraph) {
        //long ms = System.currentTimeMillis();
        int paraLength = paragraph.length();
        if (paraLength < 9) return;

        setStyleSpans(paraIndex, 0, addressPrefixStyle);

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
            /*
            outputBuilder.append( // Testing whether bytecode matches direct hex string compilation
                    JJWUtils.bytesToHex(Compiler.compileToBytes(possible.get(0), addressStr, instructionStr))
            ).append('\n');
             */
        }

        return true;
    }

    private static class DefaultContextMenu extends ContextMenu
    {
        private final MenuItem showInOutput, fold, unfold, print;

        public DefaultContextMenu()
        {
            showInOutput = new MenuItem( "Show in output" );
            showInOutput.setOnAction( AE -> { hide(); showInOutput(); } );

            fold = new MenuItem( "Fold selected text" );
            fold.setOnAction( AE -> { hide(); fold(); } );

            unfold = new MenuItem( "Unfold from cursor" );
            unfold.setOnAction( AE -> { hide(); unfold(); } );

            print = new MenuItem( "Print" );
            print.setOnAction( AE -> { hide(); print(); } );

            getItems().addAll( showInOutput, fold, unfold, print );
        }

        // Shows the currently selected instruction in the output
        private void showInOutput() {
            CodeArea area = ((CodeArea) getOwnerNode());
            String[] paragraphs = area.getText().split("\n");
            int paragraphIndex = -1;
            int currentParagraphIndex = area.getCurrentParagraph();
            if (notAnInstruction(paragraphs[currentParagraphIndex])) return;

            for (int i = 0; i < paragraphs.length; i++) {
                String paragraph = paragraphs[i];
                if (notAnInstruction(paragraph)) continue;
                paragraphIndex++;
                if (i == currentParagraphIndex) break;
            }
            if (paragraphIndex == -1) return;
            JoJoWriteController.getInstance().output.selectRange(paragraphIndex, 4, paragraphIndex, 0);
        }

        private static boolean notAnInstruction(String s) {
            return s.isEmpty() || s.startsWith(commentPrefix);
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
