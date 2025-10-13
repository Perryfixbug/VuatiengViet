// client/src/main/java/module-info.java
module vuatiengvietclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires jbcrypt;
    requires com.google.gson; // ✅ Thêm dòng này

    opens vuatiengvietpj.model to com.google.gson;

    exports vuatiengvietpj;
    exports vuatiengvietpj.controller;
    exports vuatiengvietpj.model;
    exports vuatiengvietpj.util;

}