package guessmarket.client.ui;

import guessmarket.client.net.ApiClient;
import guessmarket.client.net.Session;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class LoginController
{
    @FXML
    private TextField nameField;

    @FXML
    private Button continueButton;

    @FXML
    private Label statusLabel;

    private Stage stage;

    public void setStage(final Stage stage)
    {
        this.stage = stage;
    }

    @FXML
    private void initialize()
    {
        continueButton.setOnAction(event -> attemptLogin());
    }

    private void attemptLogin()
    {
        final String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Enter a name.");
            return;
        }

        continueButton.setDisable(true);
        statusLabel.setText("");

        // Every name must be new - there is no "log back in as an existing user" concept at all
        // (no passwords, no accounts to return to). Registering IS logging in; the server itself
        // rejects an already-taken name, which the failure handler below turns into the retry
        // prompt this screen is supposed to show for that case.
        final Task<Void> loginTask = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                ApiClient.registerUser(name);
                return null;
            }
        };

        loginTask.setOnSucceeded(event -> {
            Session.setUserName(name);
            openMainWindow();
        });

        loginTask.setOnFailed(event -> {
            continueButton.setDisable(false);
            statusLabel.setText(loginTask.getException().getMessage());
        });

        new Thread(loginTask).start();
    }

    private void openMainWindow()
    {
        try {
            final URL fxmlLocation = getClass().getResource("/guessmarket/client/ui/main.fxml");
            if (fxmlLocation == null) {
                throw new IllegalStateException("Cannot find main.fxml");
            }
            final FXMLLoader loader = new FXMLLoader(fxmlLocation);
            final Parent root = loader.load();

            final Stage mainStage = new Stage();
            mainStage.setTitle("Guess Market");
            mainStage.setScene(new Scene(root, 1200, 800));
            mainStage.setMinWidth(760);
            mainStage.setMinHeight(520);
            mainStage.show();

            stage.close();
        } catch (final IOException e) {
            continueButton.setDisable(false);
            statusLabel.setText("Could not open main window: " + e.getMessage());
        }
    }
}
