package guessmarket.javafx.ui;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.OptionDTO;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Alert;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;

/**
 * Displays the details of a single event (both LMSR and Order Book) and, when
 * actions are enabled, lets the caller trade in it / start it / close it as a
 * given "active user". This is shared between the browse-only Events screen
 * (actions hidden) and the Users screen (actions shown - per the assignment,
 * event management happens from the Users area after picking a user then an
 * event).
 */
public class EventDetailController {

    @FXML private Label option1NameLabel;
    @FXML private Label option1PriceLabel;
    @FXML private javafx.scene.layout.VBox option1StatsBox;
    @FXML private Label opt1BidLabel;
    @FXML private Label opt1AskLabel;
    @FXML private Label opt1MidLabel;
    @FXML private Label opt1SpreadLabel;
    @FXML private Label opt1LastLabel;

    @FXML private Label option2NameLabel;
    @FXML private Label option2PriceLabel;
    @FXML private javafx.scene.layout.VBox option2StatsBox;
    @FXML private Label opt2BidLabel;
    @FXML private Label opt2AskLabel;
    @FXML private Label opt2MidLabel;
    @FXML private Label opt2SpreadLabel;
    @FXML private Label opt2LastLabel;

    @FXML private javafx.scene.layout.VBox actionsBox;
    @FXML private TextField tradeQuantityField;
    @FXML private TextField tradePriceField;
    @FXML private javafx.scene.control.ComboBox<String> tradeTypeComboBox;
    @FXML private javafx.scene.layout.HBox orderBookControls;
    @FXML private javafx.scene.control.Button startEventButton;
    @FXML private javafx.scene.control.Button closeEventButton;

    @FXML private TableView<EventsViewController.HoldingViewModel> transactionsTable;
    @FXML private TableColumn<EventsViewController.HoldingViewModel, String> txUserColumn;
    @FXML private TableColumn<EventsViewController.HoldingViewModel, String> txOptionColumn;
    @FXML private TableColumn<EventsViewController.HoldingViewModel, Integer> txQuantityColumn;
    @FXML private TableColumn<EventsViewController.HoldingViewModel, Double> txPriceColumn;

    @FXML private javafx.scene.layout.VBox pendingOrdersBox;
    @FXML private javafx.scene.control.TableView<guessmarket.dto.PendingOrderDTO> pendingOrdersTable;
    @FXML private javafx.scene.control.TableColumn<guessmarket.dto.PendingOrderDTO, String> poTypeColumn;
    @FXML private javafx.scene.control.TableColumn<guessmarket.dto.PendingOrderDTO, String> poUserColumn;
    @FXML private javafx.scene.control.TableColumn<guessmarket.dto.PendingOrderDTO, String> poOptionColumn;
    @FXML private javafx.scene.control.TableColumn<guessmarket.dto.PendingOrderDTO, Double> poPriceColumn;
    @FXML private javafx.scene.control.TableColumn<guessmarket.dto.PendingOrderDTO, Integer> poQtyColumn;
    @FXML private javafx.scene.chart.LineChart<Number, Number> priceChart;

    private MarketEngine engine;
    private java.util.function.Supplier<String> activeUserSupplier;
    private EventSummaryDTO currentEvent;
    private String activeUser;
    private Runnable onChange;

    public void setEngine(MarketEngine engine) {
        this.engine = engine;
    }

    public void setActiveUserSupplier(java.util.function.Supplier<String> supplier) {
        this.activeUserSupplier = supplier;
    }

    /** Called after a trade/start/close succeeds, so the caller can refresh its own list/table. */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    /** Show/hide the trade & start/close controls. The Events screen is browse-only; the Users screen manages events. */
    public void setActionsVisible(boolean visible) {
        actionsBox.setVisible(visible);
        actionsBox.setManaged(visible);
    }

    public void clear() {
        this.currentEvent = null;
        transactionsTable.setItems(FXCollections.observableArrayList());
        pendingOrdersTable.setItems(FXCollections.observableArrayList());
        priceChart.getData().clear();
        option1NameLabel.setText("Option 1");
        option1PriceLabel.setText("Price: N/A");
        option2NameLabel.setText("Option 2");
        option2PriceLabel.setText("Price: N/A");
    }

    @FXML
    public void initialize() {
        txUserColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        txOptionColumn.setCellValueFactory(new PropertyValueFactory<>("optionName"));
        txQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        txPriceColumn.setCellValueFactory(new PropertyValueFactory<>("totalValue"));

        tradeTypeComboBox.getItems().addAll("Buy", "Sell");
        tradeTypeComboBox.getSelectionModel().selectFirst();

        poTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        poUserColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        poOptionColumn.setCellValueFactory(new PropertyValueFactory<>("optionName"));
        poPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        poQtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        pendingOrdersTable.setRowFactory(tv -> new javafx.scene.control.TableRow<guessmarket.dto.PendingOrderDTO>() {
            @Override
            protected void updateItem(guessmarket.dto.PendingOrderDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if ("BUY".equals(item.getType())) {
                    setStyle("-fx-background-color: #d4edda;");
                } else {
                    setStyle("-fx-background-color: #f8d7da;");
                }
            }
        });
    }

    public void showEvent(EventSummaryDTO eventSummary) {
        if (engine == null || eventSummary == null) return;
        this.currentEvent = eventSummary;

        boolean isOrderBook = "OrderBook".equals(eventSummary.getMethod());
        orderBookControls.setVisible(isOrderBook);
        orderBookControls.setManaged(isOrderBook);
        pendingOrdersBox.setVisible(isOrderBook);
        pendingOrdersBox.setManaged(isOrderBook);

        EventDetailsDTO details = engine.getEventDetails(eventSummary.getId());

        if (details.getOptions().size() >= 2) {
            OptionDTO opt1 = details.getOptions().get(0);
            option1NameLabel.setText(opt1.getName());
            option1PriceLabel.setText(String.format("Price: %.2f", opt1.getCurrentProbability()));

            OptionDTO opt2 = details.getOptions().get(1);
            option2NameLabel.setText(opt2.getName());
            option2PriceLabel.setText(String.format("Price: %.2f", opt2.getCurrentProbability()));

            if (isOrderBook) {
                option1StatsBox.setVisible(true); option1StatsBox.setManaged(true);
                option2StatsBox.setVisible(true); option2StatsBox.setManaged(true);

                opt1BidLabel.setText(String.format("BID: %.2f", opt1.getBid()));
                opt1AskLabel.setText(String.format("ASK: %.2f", opt1.getAsk()));
                opt1MidLabel.setText(String.format("MID: %.2f", opt1.getMid()));
                opt1SpreadLabel.setText(String.format("SPREAD: %.2f", opt1.getSpread()));
                opt1LastLabel.setText(String.format("LAST: %.2f", opt1.getLastPrice()));

                opt2BidLabel.setText(String.format("BID: %.2f", opt2.getBid()));
                opt2AskLabel.setText(String.format("ASK: %.2f", opt2.getAsk()));
                opt2MidLabel.setText(String.format("MID: %.2f", opt2.getMid()));
                opt2SpreadLabel.setText(String.format("SPREAD: %.2f", opt2.getSpread()));
                opt2LastLabel.setText(String.format("LAST: %.2f", opt2.getLastPrice()));
            } else {
                option1StatsBox.setVisible(false); option1StatsBox.setManaged(false);
                option2StatsBox.setVisible(false); option2StatsBox.setManaged(false);
            }
        }

        // Calculate Participant Holdings from actual User portfolios (source of truth)
        java.util.List<EventsViewController.HoldingViewModel> holdingsList = new java.util.ArrayList<>();
        for (guessmarket.dto.UserDTO u : engine.getAllUsers()) {
            for (guessmarket.dto.PortfolioItemDTO pItem : u.getPortfolio()) {
                if (pItem.getEventId() == currentEvent.getId() && pItem.getQuantity() > 0) {
                    double currentPrice = 0;
                    for (guessmarket.dto.OptionDTO opt : details.getOptions()) {
                        if (opt.getName().equals(pItem.getOptionName())) {
                            currentPrice = opt.getCurrentProbability();
                            break;
                        }
                    }
                    holdingsList.add(new EventsViewController.HoldingViewModel(u.getName(), pItem.getOptionName(), pItem.getQuantity(), pItem.getQuantity() * currentPrice));
                }
            }
        }
        transactionsTable.setItems(FXCollections.observableArrayList(holdingsList));

        if (isOrderBook) {
            pendingOrdersTable.setItems(FXCollections.observableArrayList(
                engine.getPendingOrders(eventSummary.getId())));
        }

        priceChart.getData().clear();
        java.util.List<java.util.List<Double>> history = details.getPriceHistory();
        java.util.List<String> optionNames = eventSummary.getOptions();
        for (int i = 0; i < history.size() && i < optionNames.size(); i++) {
            javafx.scene.chart.XYChart.Series<Number, Number> series = new javafx.scene.chart.XYChart.Series<>();
            series.setName(optionNames.get(i));
            java.util.List<Double> prices = history.get(i);
            for (int j = 0; j < prices.size(); j++) {
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(j + 1, prices.get(j)));
            }
            priceChart.getData().add(series);
        }
    }

    @FXML
    private void handleBuyOption1() {
        handleTrade(0);
    }

    @FXML
    private void handleBuyOption2() {
        handleTrade(1);
    }

    private void handleTrade(int optionIndex) {
        this.activeUser = activeUserSupplier != null ? activeUserSupplier.get() : null;

        if (engine == null || currentEvent == null) {
            new Alert(Alert.AlertType.WARNING, "Please select an event first.").showAndWait();
            return;
        }
        if (activeUser == null || activeUser.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select a user.").showAndWait();
            return;
        }

        try {
            int quantity = Integer.parseInt(tradeQuantityField.getText());

            if (currentEvent.getMethod().equals("LMSR")) {
                guessmarket.dto.ReceiptDTO receipt = engine.buyShares(activeUser, currentEvent.getId(), optionIndex, quantity);
                showReceipt(receipt);
            } else {
                double price = Double.parseDouble(tradePriceField.getText());
                String typeStr = tradeTypeComboBox.getValue();
                guessmarket.engine.models.Order.Type type = typeStr.equals("Buy") ?
                        guessmarket.engine.models.Order.Type.BUY : guessmarket.engine.models.Order.Type.SELL;

                engine.placeOrder(activeUser, currentEvent.getId(), optionIndex, quantity, price, type);
            }

            showEvent(currentEvent);
            if (onChange != null) onChange.run();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid quantity or price. Please enter valid numbers.").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error executing trade: " + e.getMessage()).showAndWait();
        }
    }

    private void showReceipt(guessmarket.dto.ReceiptDTO receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Cost of shares: $%.2f%n", receipt.getCostOfShares()));
        if (receipt.isCommissionApplied()) {
            sb.append(String.format("Commission paid: $%.2f%n", receipt.getCommissionPaid()));
        }
        sb.append(String.format("Total paid: $%.2f", receipt.getTotalPaid()));

        Alert alert = new Alert(Alert.AlertType.INFORMATION, sb.toString());
        alert.setTitle("Trade Receipt");
        alert.setHeaderText("Purchase successful");
        alert.showAndWait();
    }

    @FXML
    private void handleStartEvent() {
        this.activeUser = activeUserSupplier != null ? activeUserSupplier.get() : null;
        if (engine == null || currentEvent == null) return;
        if (activeUser == null || activeUser.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select a user.").showAndWait();
            return;
        }
        try {
            engine.startEvent(currentEvent.getId(), activeUser);
            showEvent(currentEvent);
            if (onChange != null) onChange.run();
            new Alert(Alert.AlertType.INFORMATION, "Event started successfully!").showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error starting event: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleCloseEvent() {
        this.activeUser = activeUserSupplier != null ? activeUserSupplier.get() : null;
        if (engine == null || currentEvent == null) return;
        if (activeUser == null || activeUser.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select a user.").showAndWait();
            return;
        }

        EventDetailsDTO details = engine.getEventDetails(currentEvent.getId());
        if (!details.isStarted()) {
            new Alert(Alert.AlertType.WARNING, "Cannot close an event that has not started.").showAndWait();
            return;
        }
        if (!details.getActiveStatus()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Event Closed");
            alert.setHeaderText("Already Closed");
            alert.setContentText("This event has already been closed.");
            alert.showAndWait();
            return;
        }

        java.util.List<String> options = new java.util.ArrayList<>();
        for (guessmarket.dto.OptionDTO opt : details.getOptions()) {
            options.add(opt.getName());
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Close Event");
        dialog.setHeaderText("Close " + currentEvent.getName());
        dialog.setContentText("Select the winning option:");

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(winningName -> {
            int winningIndex = options.indexOf(winningName);
            try {
                engine.closeEvent(currentEvent.getId(), winningIndex, activeUser);

                showEvent(currentEvent);
                if (onChange != null) onChange.run();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Event Closed Successfully");
                alert.setContentText("Winner: " + winningName + ".\nPayouts have been distributed to the users!");
                alert.showAndWait();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Error closing event: " + e.getMessage()).showAndWait();
            }
        });
    }
}
