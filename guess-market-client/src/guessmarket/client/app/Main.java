package guessmarket.client.app;

import guessmarket.client.ui.LoginController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final URL fxmlLocation = getClass().getResource("/guessmarket/client/ui/login.fxml");
        if (fxmlLocation == null) {
            throw new IllegalStateException("Cannot find login.fxml");
        }

        final FXMLLoader loader = new FXMLLoader(fxmlLocation);
        final Parent root = loader.load();

        final LoginController controller = loader.getController();
        controller.setStage(stage);

        stage.setTitle("Guess Market - Login");
        stage.setScene(new Scene(root, 400, 320));
        // Unlike the main window, this screen has nothing useful to show if stretched bigger or
        // squeezed smaller - it's a small fixed form, not a resizable window. setResizable(false)
        // also removes the maximize button entirely, not just the drag handles.
        stage.setResizable(false);
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(args);
    }
}
