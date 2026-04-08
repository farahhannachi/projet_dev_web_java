module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.google.gson;

    opens org.example to javafx.fxml;
    opens org.example.controller.Backoffice to javafx.fxml;
    opens org.example.controller.Frontoffice to javafx.fxml;
    opens org.example.entities to javafx.base, com.google.gson;

    exports org.example;
}
