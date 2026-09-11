package guessmarket.javafx.ui;

import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.OptionDTO;
import guessmarket.dto.TransactionDTO;
import guessmarket.dto.lmsr.LmsrEventDetailsDTO;
import guessmarket.dto.orderbook.OptionBookDTO;
import guessmarket.dto.orderbook.OrderBookEventDetailsDTO;
import guessmarket.dto.orderbook.OrderDTO;
import guessmarket.dto.orderbook.ParticipantHoldingDTO;
import guessmarket.engine.core.api.MarketEngine;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    private static final int SIMULATED_LOAD_DELAY_MS = 1200;

    private MarketEngine engine;
    private final List<EventSummaryDTO> allEvents = new ArrayList<>();

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
    private ListView<EventSummaryDTO> eventListView;

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

        methodFilterCombo.setOnAction(e -> refreshEventList());
        statusFilterCombo.setOnAction(e -> refreshEventList());
        commissionFilterCombo.setOnAction(e -> refreshEventList());

        eventListView.setCellFactory(list -> new EventCell());
        eventListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> showEventDetails(newVal));

        loadFileButton.setOnAction(event -> onLoadFileClicked());
    }

    // ---------------------------------------------------------------- loading

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
            allEvents.clear();
            allEvents.addAll(loadTask.getValue());
            refreshEventList();
            eventDetailPane.getChildren().setAll(placeholder("Select an event to see its details."));
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

    // ---------------------------------------------------------------- filtering + list

    private void refreshEventList() {
        EventSummaryDTO previouslySelected = eventListView.getSelectionModel().getSelectedItem();
        eventListView.getItems().clear();
        for (EventSummaryDTO event : allEvents) {
            if (passesFilters(event)) {
                eventListView.getItems().add(event);
            }
        }
        if (previouslySelected != null && eventListView.getItems().contains(previouslySelected)) {
            eventListView.getSelectionModel().select(previouslySelected);
        }
    }

    private boolean passesFilters(EventSummaryDTO event) {
        String method = methodFilterCombo.getValue();
        if ("LMSR".equals(method) && !"LMSR".equals(event.getType())) return false;
        if ("Order Book".equals(method) && !"ORDER_BOOK".equals(event.getType())) return false;

        String status = statusFilterCombo.getValue();
        if ("Not active".equals(status) && !"NOT_ACTIVE".equals(event.getStatus())) return false;
        if ("Active".equals(status) && !"ACTIVE".equals(event.getStatus())) return false;
        if ("Closed".equals(status) && !"CLOSED".equals(event.getStatus())) return false;

        String commission = commissionFilterCombo.getValue();
        if ("On purchase".equals(commission) && !"ON_PURCHASE".equals(event.getCommissionType())) return false;
        if ("On close".equals(commission) && !"ON_CLOSE".equals(event.getCommissionType())) return false;

        return true;
    }

    private class EventCell extends ListCell<EventSummaryDTO> {
        @Override
        protected void updateItem(EventSummaryDTO event, boolean empty) {
            super.updateItem(event, empty);
            if (empty || event == null) {
                setGraphic(null);
                return;
            }
            Label name = new Label(event.getId() + ". " + event.getName());
            name.getStyleClass().add("event-cell-name");

            HBox pills = new HBox(6, pill(readableType(event.getType()), typePillClass(event.getType())), pill(readableStatus(event.getStatus()), statusPillClass(event.getStatus())));
            Label meta = new Label(readableCommission(event.getCommissionType()) + " · " + event.getCommission() + "%");
            meta.getStyleClass().add("event-cell-meta");

            VBox box = new VBox(4, name, pills, meta);
            box.setPadding(new Insets(4, 2, 4, 2));
            setGraphic(box);
        }
    }

    // ---------------------------------------------------------------- event details

    private void showEventDetails(EventSummaryDTO selected) {
        if (selected == null) {
            eventDetailPane.getChildren().setAll(placeholder("Select an event to see its details."));
            return;
        }
        try {
            EventDetailsDTO details = engine.getEventDetails(selected.getId());
            if (details instanceof LmsrEventDetailsDTO) {
                eventDetailPane.getChildren().setAll(buildLmsrDetail((LmsrEventDetailsDTO) details));
            } else if (details instanceof OrderBookEventDetailsDTO) {
                eventDetailPane.getChildren().setAll(buildOrderBookDetail((OrderBookEventDetailsDTO) details));
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private List<javafx.scene.Node> buildLmsrDetail(LmsrEventDetailsDTO dto) {
        List<javafx.scene.Node> nodes = new ArrayList<>();
        nodes.add(detailHeader(dto.getName(), dto.getDescription(), dto.getStatus(),
            "Account balance", money(dto.getAccountBalance()),
            "Commission collected", money(dto.getTotalCommissionCollected())));

        HBox priceCards = new HBox(12);
        for (OptionDTO option : dto.getOptions()) {
            priceCards.getChildren().add(priceCard(option));
        }
        priceCards.setPadding(new Insets(0, 0, 10, 0));
        nodes.add(priceCards);

        TableView<TransactionDTO> table = new TableView<>();
        table.getColumns().add(column("User", "userName", 100));
        table.getColumns().add(column("Option", "optionName", 90));
        table.getColumns().add(column("Qty", "quantity", 70));
        table.getColumns().add(moneyColumn("Paid", "pricePaid", 90));
        table.getColumns().add(column("Time", "timestamp", 150));
        table.getItems().addAll(dto.getTransactions());
        table.setPlaceholder(new Label("No trades yet."));
        nodes.add(sectionLabel("Trade history"));
        nodes.add(table);

        if (dto.getWinningOptionName() != null) {
            nodes.add(sectionLabel("Winning option: " + dto.getWinningOptionName()));
        }
        return nodes;
    }

    private List<javafx.scene.Node> buildOrderBookDetail(OrderBookEventDetailsDTO dto) {
        List<javafx.scene.Node> nodes = new ArrayList<>();
        nodes.add(detailHeader(dto.getName(), dto.getDescription(), dto.getStatus(),
            "Base value (d)", money(dto.getBaseValue()),
            "Account balance", money(dto.getAccountBalance())));

        HBox books = new HBox(14);
        for (OptionBookDTO book : dto.getOptionBooks()) {
            books.getChildren().add(bookPanel(book));
        }
        nodes.add(books);

        TableView<ParticipantHoldingDTO> participantsTable = new TableView<>();
        TableColumn<ParticipantHoldingDTO, String> userCol = new TableColumn<>("Participant");
        userCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
        userCol.setPrefWidth(140);
        participantsTable.getColumns().add(userCol);
        for (int i = 0; i < dto.getOptionBooks().size(); i++) {
            final int optionIndex = i;
            TableColumn<ParticipantHoldingDTO, String> holdingCol = new TableColumn<>(dto.getOptionBooks().get(i).getOptionName() + " held");
            holdingCol.setCellValueFactory(row -> new javafx.beans.property.SimpleStringProperty(String.valueOf(row.getValue().getHoldingsByOption().get(optionIndex))));
            holdingCol.setPrefWidth(110);
            participantsTable.getColumns().add(holdingCol);
        }
        TableColumn<ParticipantHoldingDTO, String> valueCol = new TableColumn<>("Est. value");
        valueCol.setCellValueFactory(row -> new javafx.beans.property.SimpleStringProperty(money(row.getValue().getEstimatedValue())));
        valueCol.setPrefWidth(100);
        participantsTable.getColumns().add(valueCol);
        participantsTable.getItems().addAll(dto.getParticipants());
        participantsTable.setPlaceholder(new Label("No participants yet."));

        nodes.add(sectionLabel("Participations"));
        nodes.add(participantsTable);

        if (dto.getWinningOptionName() != null) {
            nodes.add(sectionLabel("Winning option: " + dto.getWinningOptionName()));
        }
        return nodes;
    }

    private VBox bookPanel(OptionBookDTO book) {
        Label header = new Label(book.getOptionName());
        header.getStyleClass().add("book-panel-title");

        HBox stats = new HBox();
        stats.getStyleClass().add("stats-row");
        stats.getChildren().addAll(
            statBox("Last", book.getLast()), statBox("Bid", book.getBid()), statBox("Ask", book.getAsk()),
            statBox("Mid", book.getMid()), statBox("Spread", book.getSpread())
        );

        TableView<OrderDTO> bids = orderTable(book.getBids());
        TableView<OrderDTO> asks = orderTable(book.getAsks());

        VBox panel = new VBox(6, header, stats, sectionLabel("Bids"), bids, sectionLabel("Asks"), asks);
        panel.getStyleClass().add("book-panel");
        panel.setPadding(new Insets(10));
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private TableView<OrderDTO> orderTable(List<OrderDTO> orders) {
        TableView<OrderDTO> table = new TableView<>();
        table.getColumns().add(column("User", "userName", 90));
        table.getColumns().add(column("Qty", "quantity", 60));
        table.getColumns().add(moneyColumn("Price", "price", 80));
        table.getItems().addAll(orders);
        table.setPlaceholder(new Label("—"));
        table.setPrefHeight(120);
        return table;
    }

    // ---------------------------------------------------------------- small builders

    private VBox detailHeader(String name, String description, String status, String metaLabel1, String metaValue1, String metaLabel2, String metaValue2) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("detail-title");
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("detail-desc");
        descLabel.setWrapText(true);

        HBox meta = new HBox(20, metaBox("Status", readableStatus(status)), metaBox(metaLabel1, metaValue1), metaBox(metaLabel2, metaValue2));
        meta.setPadding(new Insets(6, 0, 12, 0));

        VBox header = new VBox(4, nameLabel, descLabel, meta);
        header.setPadding(new Insets(0, 0, 8, 0));
        return header;
    }

    private VBox metaBox(String label, String value) {
        Label l = new Label(label.toUpperCase());
        l.getStyleClass().add("meta-label");
        Label v = new Label(value);
        v.getStyleClass().add("meta-value");
        return new VBox(2, l, v);
    }

    private VBox statBox(String label, Double value) {
        Label l = new Label(label.toUpperCase());
        l.getStyleClass().add("stat-label");
        Label v = new Label(value == null ? "—" : money(value));
        v.getStyleClass().add("stat-value");
        VBox box = new VBox(2, l, v);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("stat-box");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox priceCard(OptionDTO option) {
        Label name = new Label(option.getName());
        Label price = new Label(money(option.getCurrentProbability()));
        price.getStyleClass().add("price-card-value");
        Label chance = new Label(Math.round(option.getCurrentProbability() * 100) + "% implied chance");
        chance.getStyleClass().add("price-card-chance");
        VBox card = new VBox(3, name, price, chance);
        card.getStyleClass().add("price-card");
        card.setPadding(new Insets(10, 14, 10, 14));
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-label");
        return label;
    }

    private Label placeholder(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("placeholder-label");
        return label;
    }

    private Label pill(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("pill", styleClass);
        return label;
    }

    private <S, T> TableColumn<S, T> column(String title, String property, double width) {
        TableColumn<S, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }

    private <S> TableColumn<S, String> moneyColumn(String title, String property, double width) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(row -> {
            try {
                Object raw = row.getValue().getClass().getMethod("get" + Character.toUpperCase(property.charAt(0)) + property.substring(1)).invoke(row.getValue());
                return new javafx.beans.property.SimpleStringProperty(money(((Number) raw).doubleValue()));
            } catch (Exception e) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
        });
        col.setPrefWidth(width);
        return col;
    }

    private String money(double value) {
        return String.format("$%.2f", value);
    }

    private String readableType(String type) {
        return "ORDER_BOOK".equals(type) ? "Order Book" : "LMSR";
    }

    private String readableStatus(String status) {
        if ("NOT_ACTIVE".equals(status)) return "Not active";
        if ("ACTIVE".equals(status)) return "Active";
        if ("CLOSED".equals(status)) return "Closed";
        return status;
    }

    private String readableCommission(String type) {
        return "ON_CLOSE".equals(type) ? "On close" : "On purchase";
    }

    private String typePillClass(String type) {
        return "ORDER_BOOK".equals(type) ? "pill-ob" : "pill-lmsr";
    }

    private String statusPillClass(String status) {
        if ("ACTIVE".equals(status)) return "pill-active";
        if ("CLOSED".equals(status)) return "pill-closed";
        return "pill-notactive";
    }
}
