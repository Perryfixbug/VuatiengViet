// client/src/main/java/module-info.java
module vuatiengvietclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires jbcrypt;
    requires com.google.gson; 

    opens vuatiengvietpj.model to com.google.gson;

    // Allow JavaFX FXMLLoader to access controller classes reflectively for @FXML injection and handlers
    opens vuatiengvietpj.controller to javafx.fxml;

    exports vuatiengvietpj;
    exports vuatiengvietpj.controller;
    exports vuatiengvietpj.model;
    exports vuatiengvietpj.util;

}