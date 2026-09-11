package guessmarket.javafx.ui;

import guessmarket.dto.EventSummaryDTO;
import guessmarket.engine.core.api.MarketEngine;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class MainController {

    private static final int SIMULATED_LOAD_DELAY_MS = 1200;

    private MarketEngine engine;

    @FXML
    private Label filePathLabel;

    @FXML
    private ProgressBar loadProgressBar;

    @FXML
    private Button loadFileButton;

    @FXML
    private ComboBox<String> methodFilterCombo;

    @FXML
    private ComboBox<String> statusFilterCombo;

    @FXML
    private ComboBox<String> commissionFilterCombo;

    @FXML
    private ListView<String> eventListView;

    @FXML
    private VBox eventDetailPane;

    @FXML
    private ListView<String> userListView;

    @FXML
    private VBox userDetailPane;

    public void setEngine(MarketEngine engine) {
        this.engine = engine;
    }

    @FXML
    public void initialize() {
        methodFilterCombo.getItems().addAll("All methods", "LMSR", "Order Book");
        methodFilterCombo.getSelectionModel().selectFirst();

        statusFilterCombo.getItems().addAll("All statuses", "Not active", "Active", "Closed");
        statusFilterCombo.getSelectionModel().selectFirst();

        commissionFilterCombo.getItems().addAll("All commission types", "On purchase", "On close");
        commissionFilterCombo.getSelectionModel().selectFirst();

        loadFileButton.setOnAction(event -> onLoadFileClicked());
    }

    private void onLoadFileClicked() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Guess Market events file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));
        File selectedFile = chooser.showOpenDialog(loadFileButton.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        Task<List<EventSummaryDTO>> loadTask = new Task<>() {
            @Override
            protected List<EventSummaryDTO> call() throws Exception {
                updateProgress(0.2, 1);
                engine.loadData(selectedFile.getAbsolutePath());
                updateProgress(0.6, 1);
                Thread.sleep(SIMULATED_LOAD_DELAY_MS);
                updateProgress(1, 1);
                return engine.getAllEvents();
            }
        };

        loadProgressBar.setVisible(true);
        loadProgressBar.setManaged(true);
        loadProgressBar.progressProperty().unbind();
        loadProgressBar.progressProperty().bind(loadTask.progressProperty());
        loadFileButton.setDisable(true);

        loadTask.setOnSucceeded(event -> {
            loadProgressBar.setVisible(false);
            loadProgressBar.setManaged(false);
            loadFileButton.setDisable(false);
            filePathLabel.setText(selectedFile.getAbsolutePath());
            populateEventList(loadTask.getValue());
        });

        loadTask.setOnFailed(event -> {
            loadProgressBar.setVisible(false);
            loadProgressBar.setManaged(false);
            loadFileButton.setDisable(false);
            showError(describeFailure(loadTask.getException()));
        });

        Thread thread = new Thread(loadTask, "load-events-file");
        thread.setDaemon(true);
        thread.start();
    }

    private void populateEventList(List<EventSummaryDTO> events) {
        eventListView.getItems().clear();
        for (EventSummaryDTO event : events) {
            eventListView.getItems().add(event.getId() + ". " + event.getName() + "  [" + event.getType() + " / " + event.getStatus() + "]");
        }
    }

    private String describeFailure(Throwable exception) {
        return (exception != null && exception.getMessage() != null) ? exception.getMessage() : "An unknown error occurred while loading the file.";
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Guess Market");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
