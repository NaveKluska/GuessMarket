package guessmarket.javafx.app;

import guessmarket.engine.billing.impl.StandardCommissionCalculator;
import guessmarket.engine.core.api.MarketEngine;
import guessmarket.engine.core.impl.MarketEngineImpl;
import guessmarket.engine.parsing.impl.ex2.EX2_JAXB_XMLFileParser;
import guessmarket.javafx.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL fxmlLocation = getClass().getResource("/guessmarket/javafx/ui/main.fxml");
        if (fxmlLocation == null) {
            throw new IllegalStateException("Cannot find main.fxml");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
        Parent root = fxmlLoader.load();

        MarketEngine engine = new MarketEngineImpl(new EX2_JAXB_XMLFileParser(), new StandardCommissionCalculator());
        MainController controller = fxmlLoader.getController();
        controller.setEngine(engine);

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/guessmarket/javafx/ui/main.css").toExternalForm());

        stage.setTitle("Guess Market");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
