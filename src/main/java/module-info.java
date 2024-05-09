module net.arna.jojowrite {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;

    //requires org.reactfx;

    opens net.arna.jojowrite to javafx.fxml;
    exports net.arna.jojowrite;
    exports net.arna.jojowrite.node;
}