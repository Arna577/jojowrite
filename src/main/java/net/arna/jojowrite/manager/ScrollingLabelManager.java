package net.arna.jojowrite.manager;

import javafx.scene.control.Label;

import java.util.ArrayList;

public class ScrollingLabelManager {
    private final ArrayList<Label> scrollingLabels = new ArrayList<>();

    public ScrollingLabelManager() {

    }

    public boolean addLabel(Label l) {
        return scrollingLabels.add(l);
    }
}
