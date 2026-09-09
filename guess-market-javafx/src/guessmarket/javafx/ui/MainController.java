package guessmarket.javafx.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainController {

    @FXML
    private Label welcomeText;

    @FXML
    public void initialize() {
        System.out.println("Main Controller initialized successfully!");
    }
}
