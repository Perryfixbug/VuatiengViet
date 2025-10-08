module vuatiengvietpj {
    requires javafx.controls;
    requires javafx.fxml;

    opens vuatiengvietpj to javafx.fxml;
    exports vuatiengvietpj;
}
