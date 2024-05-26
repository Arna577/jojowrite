module net.arna.jojowrite {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;

    opens net.arna.jojowrite to javafx.fxml;
    exports net.arna.jojowrite;
    exports net.arna.jojowrite.node;
    exports net.arna.jojowrite.manager;
}