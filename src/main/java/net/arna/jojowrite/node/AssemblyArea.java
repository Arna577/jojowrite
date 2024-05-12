package net.arna.jojowrite.node;

import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import net.arna.jojowrite.asm.Compiler;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//import org.reactfx.collection.ListModification;

import static net.arna.jojowrite.TextStyles.BASIC_TEXT;

public class AssemblyArea extends CodeArea {

    public static final String[] KEYWORDS = new String[] {
            "noop"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
    );

    public AssemblyArea() {
        setTextInsertionStyle(Collections.singleton(BASIC_TEXT));

        //setParagraphGraphicFactory(LineNumberFactory.get(this));
        setContextMenu(new DefaultContextMenu());

        setOnKeyTyped(
                event -> {
                    String firstLine = getText(0);

                    var possible = Compiler.getPossibleInstructions(firstLine).toList();
                    if (possible.size() == 1) {
                        System.out.println(
                                Compiler.compileToHexString(possible.get(0), firstLine)
                        );
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
