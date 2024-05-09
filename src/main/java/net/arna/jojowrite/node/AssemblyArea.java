package net.arna.jojowrite.node;

import net.arna.jojowrite.TextStyles;
import org.fxmisc.richtext.CodeArea;

import java.util.Collections;

public class AssemblyArea extends CodeArea {
    public AssemblyArea() {
        setTextInsertionStyle(Collections.singleton(TextStyles.BASIC_TEXT));
    }
}
