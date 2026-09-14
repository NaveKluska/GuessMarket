@echo off
java -Dprism.order=sw --module-path "javafx-sdk-25.0.4\lib" --add-modules javafx.controls,javafx.fxml -jar guess-market-javafx.jar
pause
