module net.arna.jojowrite {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens net.arna.jojowrite to javafx.fxml;
    exports net.arna.jojowrite;
}