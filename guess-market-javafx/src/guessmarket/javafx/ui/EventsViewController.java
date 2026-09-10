package guessmarket.javafx.ui;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.OptionDTO;
import guessmarket.dto.TransactionDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Alert;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;

public class EventsViewController {

    @FXML private TableView<EventSummaryDTO> eventsTable;
    @FXML private TableColumn<EventSummaryDTO, Integer> idColumn;
    @FXML private TableColumn<EventSummaryDTO, String> nameColumn;
    @FXML private TableColumn<EventSummaryDTO, String> statusColumn;
    @FXML private TableColumn<EventSummaryDTO, String> commissionColumn;
    
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

    @FXML private TextField tradeQuantityField;
    @FXML private TextField tradePriceField;
    @FXML private javafx.scene.control.ComboBox<String> tradeTypeComboBox;

    @FXML private TableView<HoldingViewModel> transactionsTable;
    @FXML private TableColumn<HoldingViewModel, String> txUserColumn;
    @FXML private TableColumn<HoldingViewModel, String> txOptionColumn;
    @FXML private TableColumn<HoldingViewModel, Integer> txQuantityColumn;
    @FXML private TableColumn<HoldingViewModel, Double> txPriceColumn;

    public static class HoldingViewModel {
        private final String userName;
        private final String optionName;
        private final int quantity;
        private final double totalValue;
        public HoldingViewModel(String userName, String optionName, int quantity, double totalValue) {
            this.userName = userName; this.optionName = optionName; this.quantity = quantity; this.totalValue = totalValue;
        }
        public String getUserName() { return userName; }
        public String getOptionName() { return optionName; }
        public int getQuantity() { return quantity; }
        public double getTotalValue() { return totalValue; }
    }

    @FXML private javafx.scene.layout.HBox orderBookControls;
    @FXML private javafx.scene.layout.VBox pendingOrdersBox;
    @FXML private javafx.scene.control.TableView<guessmarket.dto.PendingOrderDTO> pendingOrdersTable;
    @FXML private javafx.scene.control.TableColumn<guessmarket.dto.PendingOrderDTO, String> poTypeColumn;
    @FXML private javafx.scene.control.TableColumn<guessmarket.dto.PendingOrderDTO, String> poUserColumn;
    @FXML private javafx.scene.control.TableColumn<guessmarket.dto.PendingOrderDTO, String> poOptionColumn;
    @FXML private javafx.scene.control.TableColumn<guessmarket.dto.PendingOrderDTO, Double> poPriceColumn;
    @FXML private javafx.scene.control.TableColumn<guessmarket.dto.PendingOrderDTO, Integer> poQtyColumn;
    @FXML private javafx.scene.chart.LineChart<Number, Number> priceChart;
    @FXML private ToggleButton filterAllTypeBtn;
    @FXML private ToggleButton filterLMSRBtn;
    @FXML private ToggleButton filterOBBtn;

    @FXML private ToggleButton filterAllStatusBtn;
    @FXML private ToggleButton filterNotStartedBtn;
    @FXML private ToggleButton filterActiveBtn;
    @FXML private ToggleButton filterEndedBtn;

    @FXML private ToggleButton filterAllCommBtn;
    @FXML private ToggleButton filterOnPurchaseBtn;
    @FXML private ToggleButton filterOnCloseBtn;

    @FXML private javafx.scene.control.Button startEventButton;
    @FXML private javafx.scene.control.Button closeEventButton;

    private MarketEngine engine;
    private java.util.function.Supplier<String> activeUserSupplier;
    private EventSummaryDTO currentEvent;
    private String activeUser;
    private java.util.List<EventSummaryDTO> allEvents = new java.util.ArrayList<>();

    public void setActiveUserSupplier(java.util.function.Supplier<String> supplier) {
        this.activeUserSupplier = supplier;
    }

    @FXML
    public void initialize() {
        // Set up the table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        // Custom formatting for status
        statusColumn.setCellValueFactory(cellData -> {
            boolean isActive = cellData.getValue().getActiveStatus();
            boolean isStarted = cellData.getValue().isStarted();
            String status = "Not Started";
            if (isStarted) {
                status = isActive ? "Active" : "Closed";
            }
            return new SimpleStringProperty(status);
        });
        
        // Custom formatting for commission
        commissionColumn.setCellValueFactory(cellData -> {
            EventSummaryDTO dto = cellData.getValue();
            return new SimpleStringProperty(dto.getCommission() + "% (" + dto.getCommissionType() + ")");
        });

        // Set up the transaction table columns
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

        // Colour BUY rows green and SELL rows red
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

        // Listen for row selection
        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                displayEventDetails(newSelection);
            }
        });
    }

    private void displayEventDetails(EventSummaryDTO eventSummary) {
        if (engine == null) return;
        this.currentEvent = eventSummary;
        
        // Show/hide Order Book controls depending on event type
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
        java.util.List<HoldingViewModel> holdingsList = new java.util.ArrayList<>();
        for (guessmarket.dto.UserDTO u : engine.getAllUsers()) {
            for (guessmarket.dto.PortfolioItemDTO pItem : u.getPortfolio()) {
                if (pItem.getEventId() == currentEvent.getId() && pItem.getQuantity() > 0) {
                    // For value, we can just show Quantity * Current Price
                    double currentPrice = 0;
                    for (guessmarket.dto.OptionDTO opt : details.getOptions()) {
                        if (opt.getName().equals(pItem.getOptionName())) {
                            currentPrice = opt.getCurrentProbability();
                            break;
                        }
                    }
                    holdingsList.add(new HoldingViewModel(u.getName(), pItem.getOptionName(), pItem.getQuantity(), pItem.getQuantity() * currentPrice));
                }
            }
        }
        transactionsTable.setItems(FXCollections.observableArrayList(holdingsList));

        if (isOrderBook) {
            pendingOrdersTable.setItems(FXCollections.observableArrayList(
                engine.getPendingOrders(eventSummary.getId())));
        }
        
        // Update price chart
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

    public void setEngine(MarketEngine engine) {
        this.engine = engine;
        refreshEvents();
    }

    public void refreshEvents() {
        if (engine == null) return;
        
        allEvents = engine.getAllEvents();
        applyFilter();
    }

    private void applyFilter() {
        // With ToggleGroups, a user can deselect by clicking the active button, leaving nothing selected.
        // Treat "nothing selected" the same as "All".
        boolean noTypeSelected  = !filterAllTypeBtn.isSelected()   && !filterLMSRBtn.isSelected()       && !filterOBBtn.isSelected();
        boolean noStatusSelected = !filterAllStatusBtn.isSelected() && !filterNotStartedBtn.isSelected() && !filterActiveBtn.isSelected() && !filterEndedBtn.isSelected();
        boolean noCommSelected  = !filterAllCommBtn.isSelected()    && !filterOnPurchaseBtn.isSelected() && !filterOnCloseBtn.isSelected();

        java.util.List<EventSummaryDTO> filtered = new java.util.ArrayList<>();
        for (EventSummaryDTO e : allEvents) {
            boolean matchType = noTypeSelected || filterAllTypeBtn.isSelected() ||
                                (filterLMSRBtn.isSelected() && "LMSR".equals(e.getMethod())) ||
                                (filterOBBtn.isSelected() && "OrderBook".equals(e.getMethod()));

            boolean isStarted = e.isStarted();
            boolean isActive = e.getActiveStatus();

            boolean matchStatus = noStatusSelected || filterAllStatusBtn.isSelected() ||
                                  (filterNotStartedBtn.isSelected() && !isStarted) ||
                                  (filterActiveBtn.isSelected() && isStarted && isActive) ||
                                  (filterEndedBtn.isSelected() && isStarted && !isActive);

            boolean matchComm = noCommSelected || filterAllCommBtn.isSelected() ||
                                (filterOnPurchaseBtn.isSelected() && "ON_PURCHASE".equals(e.getCommissionType())) ||
                                (filterOnCloseBtn.isSelected() && "ON_CLOSE".equals(e.getCommissionType()));

            if (matchType && matchStatus && matchComm) {
                filtered.add(e);
            }
        }
        eventsTable.setItems(FXCollections.observableArrayList(filtered));

        // Re-select current event if still visible
        if (currentEvent != null) {
            int currentId = currentEvent.getId();
            for (EventSummaryDTO ev : eventsTable.getItems()) {
                if (ev.getId() == currentId) {
                    eventsTable.getSelectionModel().select(ev);
                    displayEventDetails(ev);
                    break;
                }
            }
        }
    }

    @FXML private void handleFilterChange() {
        applyFilter();
    }

    @FXML
    private void handleCreateEvent() {
        if (engine == null) return;
        
        // Build a dialog with a form
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Create New Event");
        dialog.setHeaderText("Configure New Event");
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10));

        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField descField = new javafx.scene.control.TextField();
        javafx.scene.control.TextField opt1Field = new javafx.scene.control.TextField("YES");
        javafx.scene.control.TextField opt2Field = new javafx.scene.control.TextField("NO");
        javafx.scene.control.TextField commField = new javafx.scene.control.TextField("5");
        javafx.scene.control.ComboBox<String> typeBox = new javafx.scene.control.ComboBox<>();
        typeBox.getItems().addAll("LMSR", "OrderBook");
        typeBox.getSelectionModel().selectFirst();
        javafx.scene.control.TextField bField = new javafx.scene.control.TextField("100");

        grid.add(new javafx.scene.control.Label("Name:"), 0, 0);        grid.add(nameField, 1, 0);
        grid.add(new javafx.scene.control.Label("Description:"), 0, 1); grid.add(descField, 1, 1);
        grid.add(new javafx.scene.control.Label("Option 1:"), 0, 2);    grid.add(opt1Field, 1, 2);
        grid.add(new javafx.scene.control.Label("Option 2:"), 0, 3);    grid.add(opt2Field, 1, 3);
        grid.add(new javafx.scene.control.Label("Commission %:"), 0, 4); grid.add(commField, 1, 4);
        grid.add(new javafx.scene.control.Label("Type:"), 0, 5);        grid.add(typeBox, 1, 5);
        grid.add(new javafx.scene.control.Label("B (LMSR param):"), 0, 6); grid.add(bField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        java.util.Optional<javafx.scene.control.ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                String name = nameField.getText().trim();
                String desc = descField.getText().trim();
                int comm = Integer.parseInt(commField.getText().trim());
                java.util.List<String> opts = java.util.Arrays.asList(opt1Field.getText().trim(), opt2Field.getText().trim());

                if (name.isEmpty() || opts.get(0).isEmpty() || opts.get(1).isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "Name and option names are required.").showAndWait();
                    return;
                }

                if ("LMSR".equals(typeBox.getValue())) {
                    int b = Integer.parseInt(bField.getText().trim());
                    engine.createLmsrEvent(name, desc, comm, guessmarket.engine.models.CommissionType.ON_PURCHASE, opts, b);
                } else {
                    engine.createOrderBookEvent(name, desc, comm, guessmarket.engine.models.CommissionType.ON_PURCHASE, opts, true, 0, 1);
                }
                refreshEvents();
                new Alert(Alert.AlertType.INFORMATION, "Event '" + name + "' created successfully!").showAndWait();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Commission and B must be valid numbers.").showAndWait();
            }
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
        // Always get the latest active user from the supplier
        this.activeUser = activeUserSupplier != null ? activeUserSupplier.get() : null;
        
        if (engine == null || currentEvent == null) {
            new Alert(Alert.AlertType.WARNING, "Please load a file and select an event first.").showAndWait();
            return;
        }
        if (activeUser == null || activeUser.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select a user from the top bar.").showAndWait();
            return;
        }
        
        try {
            int quantity = Integer.parseInt(tradeQuantityField.getText());
            
            if (currentEvent.getMethod().equals("LMSR")) {
                // LMSR Trading
                engine.buyShares(activeUser, currentEvent.getId(), optionIndex, quantity);
                System.out.println("LMSR: Bought " + quantity + " shares of option " + optionIndex + " for user " + activeUser);
            } else {
                // Order Book Trading
                double price = Double.parseDouble(tradePriceField.getText());
                String typeStr = tradeTypeComboBox.getValue();
                guessmarket.engine.models.Order.Type type = typeStr.equals("Buy") ? 
                        guessmarket.engine.models.Order.Type.BUY : guessmarket.engine.models.Order.Type.SELL;
                        
                engine.placeOrder(activeUser, currentEvent.getId(), optionIndex, quantity, price, type);
                System.out.println("OrderBook: Placed " + typeStr + " order for " + quantity + " shares at " + price);
            }
            
            // Refresh details so the new transaction shows up
            displayEventDetails(currentEvent);
            refreshEvents(); // Also refresh the left table in case status or something changes
            
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid quantity or price. Please enter valid numbers.");
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error executing trade: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleStartEvent() {
        this.activeUser = activeUserSupplier != null ? activeUserSupplier.get() : null;
        if (engine == null || currentEvent == null) return;
        if (activeUser == null || activeUser.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select a user from the top bar.").showAndWait();
            return;
        }
        try {
            engine.startEvent(currentEvent.getId(), activeUser);
            System.out.println("Started event " + currentEvent.getId() + " by " + activeUser);
            displayEventDetails(currentEvent);
            refreshEvents();
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
            new Alert(Alert.AlertType.WARNING, "Please select a user from the top bar.").showAndWait();
            return;
        }
        
        guessmarket.dto.EventDetailsDTO details = engine.getEventDetails(currentEvent.getId());
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

        // Show dialog to pick winning option
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
                System.out.println("Closed event " + currentEvent.getId() + " with winner " + winningName);
                
                // Refresh
                displayEventDetails(currentEvent);
                refreshEvents();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Event Closed Successfully");
                alert.setContentText("Winner: " + winningName + ".\nPayouts have been distributed to the users!");
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
