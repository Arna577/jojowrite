package net.arna.jojowrite.node;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import net.arna.jojowrite.JJWUtils;
import net.arna.jojowrite.JoJoWriteController;
import net.arna.jojowrite.asm.Compiler;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.Collections;

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

                    // getParagraphs() causes an IllegalAccessError due to some insane fucking module linking issue
                    String[] paragraphs = getText().split("\n");
                    int startIndex = 0;
                    for (int i = 0; i < paragraphs.length; i++) {
                        String paragraph = paragraphs[i];
                        int paraLength = paragraph.length();
                        if (!paragraph.isEmpty()) {
                            if (paragraph.startsWith("//")) { // Comments
                                setStyleClass(startIndex, startIndex + paraLength, COMMENT_TEXT);
                            } else { // Assembly
                                String[] tokens = paragraph.split(" ");
                                if (paragraph.length() < 9) {
                                    Compiler.raiseError("Invalid address prefix: " + tokens[0]);
                                } else {
                                    String addressStr = tokens[0].substring(0, 8);
                                    if (!JJWUtils.isHexadecimal(addressStr)) {
                                        Compiler.raiseError("Invalid character in Hex literal");
                                    } else {
                                        if (Integer.valueOf(addressStr, 16) % 2 != 0) {
                                            Compiler.raiseError("Unaligned address: " + addressStr);
                                        } else {
                                            setStyleClass(startIndex, startIndex + 8, ADDRESS_TEXT);
                                            if (paragraph.charAt(8) == ':') {
                                                String instructionStr = paragraph.substring(9);
                                                var possible = Compiler.getPossibleInstructions(instructionStr).toList();
                                                if (possible.size() == 1) {
                                                    JoJoWriteController.getInstance().appendToOutput(
                                                            Compiler.compileToHexString(possible.get(0), instructionStr) + '\n');
                                                }
                                                setStyleClass(startIndex + 9, startIndex + 9 + instructionStr.length(), KEYWORD_TEXT);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        startIndex += paraLength + 1; // Account for omitted \n
                    }
                }
        );
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
