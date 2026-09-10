package guessmarket.javafx.ui;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import guessmarket.dto.UserDTO;
import guessmarket.engine.core.api.MarketEngine;
import guessmarket.engine.core.impl.MarketEngineImpl;
import guessmarket.engine.parsing.impl.ex2.EX2_JAXB_XMLFileParser;
import guessmarket.engine.billing.impl.StandardCommissionCalculator;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.util.Duration;

public class MainController {

    @FXML private Button loadFileButton;
    @FXML private Label filePathLabel;
    @FXML private ProgressBar progressBar;
    @FXML private ComboBox<String> userComboBox;
    @FXML private ComboBox<String> themeComboBox;
    
    @FXML private ToggleButton eventsNavButton;
    @FXML private ToggleButton usersNavButton;
    @FXML private ToggleGroup navGroup;
    
    @FXML private StackPane centerContentArea;

    private MarketEngine engine;

    @FXML
    public void initialize() {
        System.out.println("Main Controller initialized.");
        
        engine = new MarketEngineImpl(new EX2_JAXB_XMLFileParser(), new StandardCommissionCalculator());
        
        // Setup themes
        themeComboBox.getItems().addAll("Light", "Dark", "Blue");
        themeComboBox.setValue("Light");
        themeComboBox.setOnAction(this::handleThemeChange);
        
        // Ensure one button is always selected
        navGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            }
        });
    }

    @FXML
    private void handleLoadFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XML Market File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        
        // We get the window from the button's scene
        Stage stage = (Stage) loadFileButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            filePathLabel.setText(selectedFile.getAbsolutePath());
            loadXmlFileAsync(selectedFile);
        }
    }
    
    private void loadXmlFileAsync(File file) {
        // Show progress bar
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        loadFileButton.setDisable(true);
        
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Simulate delay as required by assignment (1-2 seconds)
                Thread.sleep(1500); 
                
                engine.loadData(file.getAbsolutePath());
                
                return null;
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            progressBar.setVisible(false);
            loadFileButton.setDisable(false);
            
            userComboBox.getItems().clear();
            List<String> userNames = engine.getAllUsers().stream()
                .map(UserDTO::getName)
                .collect(Collectors.toList());
            userComboBox.getItems().addAll(userNames);
            if (!userComboBox.getItems().isEmpty()) {
                userComboBox.getSelectionModel().selectFirst();
            }
            
            System.out.println("File loaded successfully!");
            
            // Reload the view with new data
            if (eventsNavButton.isSelected()) {
                Platform.runLater(this::loadEventsView);
            } else if (usersNavButton.isSelected()) {
                Platform.runLater(this::loadUsersView);
            }
        });
        
        loadTask.setOnFailed(e -> {
            progressBar.setVisible(false);
            loadFileButton.setDisable(false);
            
            Throwable exception = loadTask.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Loading File");
            alert.setHeaderText("Failed to parse the XML file.");
            alert.setContentText(exception != null ? exception.getMessage() : "Unknown error occurred.");
            alert.showAndWait();
        });
        
        new Thread(loadTask).start();
    }

    @FXML
    private void handleNavEvents(ActionEvent event) {
        if (!eventsNavButton.isSelected()) return;
        System.out.println("Switching to Events View...");
        loadEventsView();
    }
    
    private void loadEventsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/guessmarket/javafx/ui/events_view.fxml"));
            Parent root = loader.load();
            EventsViewController controller = loader.getController();
            controller.setEngine(engine);
            controller.setActiveUserSupplier(() -> userComboBox.getValue());
            
            centerContentArea.getChildren().clear();
            centerContentArea.getChildren().add(root);
            
            // Bonus 2: Animation
            FadeTransition ft = new FadeTransition(Duration.seconds(1), root);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNavUsers(ActionEvent event) {
        if (!usersNavButton.isSelected()) return;
        System.out.println("Switching to Users View...");
        loadUsersView();
    }

    private void loadUsersView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/guessmarket/javafx/ui/users_view.fxml"));
            Parent root = loader.load();
            UsersViewController controller = loader.getController();
            controller.setEngine(engine);
            
            centerContentArea.getChildren().clear();
            centerContentArea.getChildren().add(root);
            
            // Bonus 2: Animation
            FadeTransition ft = new FadeTransition(Duration.seconds(1), root);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleThemeChange(ActionEvent event) {
        String theme = themeComboBox.getValue();
        System.out.println("Switching theme to: " + theme);
        
        if (themeComboBox.getScene() == null) return;
        
        themeComboBox.getScene().getStylesheets().clear();
        if ("Dark".equals(theme)) {
            String css = getClass().getResource("/guessmarket/javafx/ui/dark.css").toExternalForm();
            themeComboBox.getScene().getStylesheets().add(css);
        }
        // Light and Blue themes will just use default for now unless added
    }

    @FXML
    private void handleSaveState(ActionEvent event) {
        if (engine == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Save State");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("State Files", "*.dat"));
        Stage stage = (Stage) loadFileButton.getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try {
                // Remove .dat extension if user typed it — engine adds it
                String path = file.getAbsolutePath();
                if (path.endsWith(".dat")) path = path.substring(0, path.length() - 4);
                engine.saveState(path);
                new Alert(Alert.AlertType.INFORMATION, "State saved successfully!").showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to save: " + ex.getMessage()).showAndWait();
            }
        }
    }

    @FXML
    private void handleLoadState(ActionEvent event) {
        if (engine == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Load State");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("State Files", "*.dat"));
        Stage stage = (Stage) loadFileButton.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            try {
                String path = file.getAbsolutePath();
                if (path.endsWith(".dat")) path = path.substring(0, path.length() - 4);
                engine.loadState(path);
                // Refresh users and current view
                userComboBox.getItems().clear();
                engine.getAllUsers().stream()
                    .map(guessmarket.dto.UserDTO::getName)
                    .forEach(userComboBox.getItems()::add);
                if (!userComboBox.getItems().isEmpty()) userComboBox.getSelectionModel().selectFirst();
                if (eventsNavButton.isSelected()) loadEventsView();
                else if (usersNavButton.isSelected()) loadUsersView();
                new Alert(Alert.AlertType.INFORMATION, "State loaded successfully!").showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to load: " + ex.getMessage()).showAndWait();
            }
        }
    }
}
