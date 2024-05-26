package net.arna.jojowrite.manager;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ScrollingLabelManager {
    private static final List<Label> scrollingLabels = new ArrayList<>();
    private static final Timer scrollTimer = new Timer();
    private static final ScrollingLabelManager instance;

    private ScrollingLabelManager() { }

    // todo: restore original label text if it shouldnt be scrolled but was
    static {
        instance = new ScrollingLabelManager();
        scrollTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                scrollingLabels.stream().filter(ScrollingLabelManager::shouldScrollLabel).<Runnable>map(label -> () -> {
                    char initialLetter = label.getText().charAt(0);
                    label.setText(label.getText().substring(1) + initialLetter);
                }).forEach(Platform::runLater);
            }
        }, 0, 300);
    }

    public static ScrollingLabelManager getInstance() {
        return instance;
    }

    public boolean addLabel(Label l) {
        return scrollingLabels.add(l);
    }

    public boolean removeLabel(Label l) {
        return scrollingLabels.remove(l);
    }

    public static boolean shouldScrollLabel(Labeled labeled) {
        String originalString = labeled.getText();
        if (labeled.getChildrenUnmodifiable().isEmpty()) return false;
        Text textNode = (Text) labeled.lookup(".text"); // "text" is the style class of Text
        if (textNode == null) return false;
        String actualString = textNode.getText();

        return !actualString.isEmpty() && !originalString.equals(actualString);
    }
}
