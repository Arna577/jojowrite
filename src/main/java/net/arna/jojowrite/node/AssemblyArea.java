package net.arna.jojowrite.node;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
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
     * Used in {@link #styleAssemblyParagraph(int, String)}.
     */
    private final Map<Character, Set<String>> styleMap = new HashMap<>(
            Map.of(
                    '(', Collections.singleton(BASIC_TEXT),
                    ')', Collections.singleton(BASIC_TEXT),
                    ',', Collections.singleton(BASIC_TEXT),
                    '@', Collections.singleton(AT_SYMBOL),
                    '#', Collections.singleton(TEMP_OVERWRITE_TEXT)
            )
    );

    private static final StringBuilder outputBuilder = new StringBuilder();

    private final TextInputDialog findDialog = DialogHelper.createFindDialog();
    private final TextInputDialog goToDialog = DialogHelper.createStyledTextInputDialog();

    private static final Set<String> keywordTextStyle = Collections.singleton(KEYWORD_TEXT);
    private static final Set<String> commentTextStyle = Collections.singleton(COMMENT_TEXT);

    public static final String commentPrefix = "/";

    public AssemblyArea() {
        setTextInsertionStyle(Collections.singleton(PARAMETER_TEXT));

        setParagraphGraphicFactory(LineNumberFactory.get(this));

        setContextMenu(new DefaultContextMenu());

        initDialogs();

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.G) { // Ctrl + G - Go to Line
                    event.consume();
                    goToDialog.getEditor().requestFocus();
                    goToDialog.showAndWait().ifPresent(this::goToLine);
                }
                //todo: actually good find menu (find all occurrences then let user go through them)
                if (event.getCode() == KeyCode.F) { // Ctrl + F - Find Text
                    event.consume();
                    findDialog.getEditor().requestFocus();
                    findDialog.show();
                }
            }
        });

        setOnKeyTyped(
                event -> {
                    JoJoWriteController.getInstance().clearOutput();
                    // Autocompletes the next address on the new line, provided the line on which the user pressed enter starts with an address.
                    if (event.getCharacter().equals("\r") || event.getCharacter().equals("\n")) {
                        if (event.isShiftDown()) {
                            insertText(getCaretPosition(), "\r");
                        } else {
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
                    }

                    queueUpdate();
                }
        );
    }

    private void goToLine(String lineNum) {
        if (lineNum.isEmpty()) return;
        try {
            final int paragraph = Integer.parseUnsignedInt(lineNum, 10) - 1; // Not zero-indexed
            final double numParagraphs = getText().split("\n").length;
            if (numParagraphs == 0 || paragraph > numParagraphs) return;
            final double paragraphHeight = getTotalHeightEstimate() / numParagraphs; // Assuming all one-liners (must be, it's an ASM editor)
            final double baseNewScrollY = paragraph * paragraphHeight;
            // Same offsetting concept as OverwriteBox#assignParentPane()
            final double newScrollY = baseNewScrollY - paragraphHeight * (baseNewScrollY / getTotalHeightEstimate());
            Platform.runLater(
                    () -> {
                        selectRange(paragraph, 0, paragraph, 0);
                        scrollToPixel(
                                getEstimatedScrollX(),
                                newScrollY
                        );
                    }
            );
        } catch (Exception ignored) {}
    }

    private class UpdateTask extends TimerTask {
        @Override public void run() { Platform.runLater(AssemblyArea.this::update); }
    }
    private Timer updateTimer = new Timer();
    private void queueUpdate() {
        updateTimer.cancel();
        updateTimer = new Timer();
        updateTimer.schedule(new UpdateTask(), 350);
    }

    private void initDialogs() {
        // Styling for Find Hex String Dialog
        findDialog.setTitle("Find");
        findDialog.setHeaderText("Find: ");
        final DialogPane findDialogPane = findDialog.getDialogPane();
        findDialogPane.getStyleClass().add("help-dialog");
        final TextField findDialogEditor = findDialog.getEditor();
        findDialogEditor.getStyleClass().add("main");
        final Button nextButton = (Button) findDialogPane.lookupButton(ButtonType.NEXT);
        nextButton.addEventFilter(ActionEvent.ACTION,
                event -> {
                    event.consume();

                    String text = findDialogEditor.getText();
                    if (text.isEmpty()) return;
                    int index = getText().indexOf(text, getCaretPosition());
                    if (index == -1) return;
                    Platform.runLater(() ->
                            selectRange(index, index + text.length())
                    );
                }
        );


        // Styling for Go To Dialog
        goToDialog.setTitle("Go To");
        goToDialog.setHeaderText("Line: ");
        goToDialog.getDialogPane().getStyleClass().add("help-dialog");
        goToDialog.getEditor().getStyleClass().add("main");
        goToDialog.setOnShown(event -> Platform.runLater(() ->
                        goToDialog.getEditor().requestFocus()
                )
        );
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
                        setStyle(startIndex, startIndex + paraLength, commentTextStyle);
                    } else { // Assembly
                        styleAssemblyParagraph(startIndex, paragraph);
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

        Compiler.clearErrors();

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
                    if (visible) setStyle(startIndex, startIndex + paraLength, commentTextStyle);
                } else { // Assembly
                    Compiler.openErrorLog(i);
                    boolean success = processAssemblyParagraph(i, paragraph);
                    if ( (visible || modified) && success )
                        styleAssemblyParagraph(startIndex, paragraph);
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
    private void styleAssemblyParagraph(int startIndex, String paragraph) {
        //long ms = System.currentTimeMillis();
        final int paraLength = paragraph.length();
        if (paraLength < 9) return;

        setStyleSpans(startIndex, addressPrefixStyle);

        // Style keyword
        final int keywordEndIndex = paragraph.indexOf(" ", 10); // 8 address digits, ':', minimum 2 character instruction keyword, zero-based indexing
        if (keywordEndIndex == -1) {
            setStyle( startIndex + 9, startIndex + paraLength, keywordTextStyle);
            return;
        } else {
            setStyle( startIndex + 9, startIndex + keywordEndIndex, keywordTextStyle);
        }

        // Style specific characters
        for (int i = keywordEndIndex; i < paraLength; i++) {
            Collection<String> style = styleMap.get(paragraph.charAt(i));
            if (style == null) continue;
            int charIndex = startIndex + i;
            setStyle(charIndex, charIndex + 1, style);
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
        if (possible.isEmpty()) {
            if (Compiler.noLoggedErrors()) Compiler.raiseError("Unrecognized instruction: " + instructionStr);
        } else if (possible.size() == 1) {
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
            if (paragraphIndex == -1 || paragraphIndex >= paragraphs.length) return;
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
