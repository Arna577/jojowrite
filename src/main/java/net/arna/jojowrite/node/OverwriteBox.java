package net.arna.jojowrite.node;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class OverwriteBox extends VBox {
    private ScrollPane parentPane;

    public OverwriteBox() {
        super();
        //getChildren().addListener((ListChangeListener<? super Node>) c -> updateVisibility());
    }

    public void assignParentPane(ScrollPane pane) {
        this.parentPane = pane;
        parentPane.heightProperty().addListener(observable -> updateVisibility());
        parentPane.vvalueProperty().addListener(observable -> updateVisibility());
    }

    /**
     * Culls all Overwrites which are out of view within the viewport.
     * Should be called after any batch of changes is applied to this OverwriteBoxes children, or when the user scrolls through it.
     */
    public void updateVisibility() {
        Bounds parentBounds = parentPane.getLayoutBounds();
        double verticalOffset = parentPane.getVvalue() * getHeight();
        final double maxHeight = parentBounds.getMaxY();
        // Slides the window of viewing properly according to the v.value (otherwise the visible overwrites would only reach where the ScrollBar handle is)
        verticalOffset += maxHeight * (1 - parentPane.getVvalue());

        //int numVisibleNodes = 0;
        for (Node node : getChildrenUnmodifiable()) {
            if (!(node instanceof Overwrite overwrite)) continue;
            double nodeY = node.getLayoutY();
            double nodeHeight = overwrite.getLayoutHeight();
            // The node is in view if the vertical offset (with a buffer of the node height) is over the node Y position. (+Y goes down)
            // ...and if its relative position to the top of the Viewport is under the Viewport height (maxHeight) + node height as a buffer.
            boolean inView = verticalOffset + nodeHeight >= nodeY && verticalOffset - nodeY <= maxHeight + nodeHeight;
            node.setVisible(inView);

            //if (inView) numVisibleNodes++;
        }
        //System.out.println(numVisibleNodes);
    }

    public void remove(Overwrite overwrite) {
        getChildren().remove(overwrite);
    }

    /**
     * Appends an overwrite to the end of this OverwriteBoxes children.
     */
    public void add(Overwrite overwrite) {
        getChildren().add(overwrite);
    }

    public void add(int index, Overwrite overwrite) {
        getChildren().add(index, overwrite);
    }

    public void clear() {
        getChildren().clear();
    }

    public Node get(int index) {
        return getChildrenUnmodifiable().get(index);
    }

    public int size() {
        return getChildrenUnmodifiable().size();
    }
}
