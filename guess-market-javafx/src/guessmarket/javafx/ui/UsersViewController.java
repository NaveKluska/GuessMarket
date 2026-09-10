package guessmarket.javafx.ui;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.dto.UserDTO;
import guessmarket.dto.PortfolioItemDTO;
import guessmarket.dto.EventSummaryDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.Optional;

/**
 * Users screen: pick a user, see their balance/portfolio, and pick an event
 * (either one they already hold shares in, or any event in the system - e.g.
 * one they are the Market Maker for but haven't opened yet) to manage/trade
 * in as that user, via the shared {@link EventDetailController}.
 */
public class UsersViewController {

    @FXML private TableView<UserDTO> usersTable;
    @FXML private TableColumn<UserDTO, String> userNameColumn;
    @FXML private TableColumn<UserDTO, Double> userBalanceColumn;

    @FXML private Label selectedUserNameLabel;
    @FXML private Label selectedUserBalanceLabel;
    @FXML private Label selectedUserMarketMakerLabel;

    @FXML private TableView<PortfolioItemDTO> portfolioTable;
    @FXML private TableColumn<PortfolioItemDTO, Integer> portfolioEventColumn;
    @FXML private TableColumn<PortfolioItemDTO, String> portfolioOptionColumn;
    @FXML private TableColumn<PortfolioItemDTO, Integer> portfolioQuantityColumn;

    @FXML private TableView<EventSummaryDTO> allEventsTable;
    @FXML private TableColumn<EventSummaryDTO, Integer> aeIdColumn;
    @FXML private TableColumn<EventSummaryDTO, String> aeNameColumn;
    @FXML private TableColumn<EventSummaryDTO, String> aeStatusColumn;
    @FXML private TableColumn<EventSummaryDTO, String> aeCommissionColumn;

    @FXML private javafx.scene.chart.LineChart<Number, Number> balanceChart;

    @FXML private EventDetailController eventDetailController;

    private MarketEngine engine;
    private UserDTO selectedUser;
    private java.util.List<EventSummaryDTO> allEvents = new java.util.ArrayList<>();
    private boolean suppressUserSelectionHandling = false;

    @FXML
    public void initialize() {
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        userBalanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));

        portfolioEventColumn.setCellValueFactory(new PropertyValueFactory<>("eventId"));
        portfolioOptionColumn.setCellValueFactory(new PropertyValueFactory<>("optionName"));
        portfolioQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        aeIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        aeNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        aeStatusColumn.setCellValueFactory(cellData -> {
            boolean isActive = cellData.getValue().getActiveStatus();
            boolean isStarted = cellData.getValue().isStarted();
            String status = "Not Started";
            if (isStarted) {
                status = isActive ? "Active" : "Closed";
            }
            return new SimpleStringProperty(status);
        });
        aeCommissionColumn.setCellValueFactory(cellData -> {
            EventSummaryDTO dto = cellData.getValue();
            return new SimpleStringProperty(dto.getCommission() + "% (" + dto.getCommissionType() + ")");
        });

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null && !suppressUserSelectionHandling) {
                displayUserDetails(newSel);
            }
        });

        portfolioTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                showEventById(newSel.getEventId());
            }
        });

        allEventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                eventDetailController.showEvent(newSel);
            }
        });

        // Per the assignment, event management (start/close/trade) happens from
        // the Users area, acting as whichever user is currently selected here.
        eventDetailController.setActionsVisible(true);
        eventDetailController.setActiveUserSupplier(() -> selectedUser != null ? selectedUser.getName() : null);
        eventDetailController.setOnChange(() -> {
            refreshUsers();
            refreshAllEvents();
            // Refresh the selected user's balance/portfolio labels in place, without
            // going through the "switch to a different user" reset - that would wipe
            // out the event panel this very action just updated.
            if (selectedUser != null) {
                for (UserDTO u : usersTable.getItems()) {
                    if (u.getName().equals(selectedUser.getName())) {
                        selectedUser = u;
                        refreshSelectedUserInfo(u);
                        suppressUserSelectionHandling = true;
                        usersTable.getSelectionModel().select(u);
                        suppressUserSelectionHandling = false;
                        break;
                    }
                }
            }
        });
    }

    public void setEngine(MarketEngine engine) {
        this.engine = engine;
        eventDetailController.setEngine(engine);
        refreshUsers();
        refreshAllEvents();
    }

    private void refreshUsers() {
        if (engine == null) return;
        usersTable.setItems(FXCollections.observableArrayList(engine.getAllUsers()));
    }

    private void refreshAllEvents() {
        if (engine == null) return;
        allEvents = engine.getAllEvents();
        allEventsTable.setItems(FXCollections.observableArrayList(allEvents));
    }

    private void showEventById(int eventId) {
        for (EventSummaryDTO e : allEvents) {
            if (e.getId() == eventId) {
                eventDetailController.showEvent(e);
                return;
            }
        }
    }

    /** Refreshes the balance/portfolio/chart display for the given user, without touching
     *  the currently-selected event - safe to call after a trade/start/close. */
    private void refreshSelectedUserInfo(UserDTO user) {
        selectedUserNameLabel.setText("User: " + user.getName());
        String blockedSuffix = user.isBlocked() ? "   ⛔ BLOCKED (negative balance - cannot trade)" : "";
        selectedUserBalanceLabel.setText(String.format("Balance: $%.2f%s", user.getBalance(), blockedSuffix));
        selectedUserBalanceLabel.setStyle(user.isBlocked() ? "-fx-text-fill: red; -fx-font-weight: bold;" : "");

        java.util.List<Integer> mmEvents = user.getMarketMakerForEvents();
        if (mmEvents != null && !mmEvents.isEmpty()) {
            selectedUserMarketMakerLabel.setText("⭐ Market Maker for event(s): " + mmEvents.toString());
        } else {
            selectedUserMarketMakerLabel.setText("");
        }

        portfolioTable.setItems(FXCollections.observableArrayList(user.getPortfolio()));

        // Populate balance history chart
        balanceChart.getData().clear();
        javafx.scene.chart.XYChart.Series<Number, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName(user.getName());
        java.util.List<Double> history = user.getBalanceHistory();
        for (int i = 0; i < history.size(); i++) {
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(i, history.get(i)));
        }
        balanceChart.getData().add(series);
    }

    /** Called when the user clicks a different row in the Users table: switches the whole
     *  right-hand side over to that user, including clearing whatever event was shown. */
    private void displayUserDetails(UserDTO user) {
        this.selectedUser = user;
        refreshSelectedUserInfo(user);

        portfolioTable.getSelectionModel().clearSelection();
        allEventsTable.getSelectionModel().clearSelection();
        eventDetailController.clear();
    }

    @FXML
    private void handleAddFunds() {
        if (selectedUser == null || engine == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Funds");
        dialog.setHeaderText("Add Funds to " + selectedUser.getName());
        dialog.setContentText("Enter amount to add:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                engine.addFunds(selectedUser.getName(), amount);

                refreshUsers();

                for (UserDTO u : usersTable.getItems()) {
                    if (u.getName().equals(selectedUser.getName())) {
                        selectedUser = u;
                        refreshSelectedUserInfo(u);
                        suppressUserSelectionHandling = true;
                        usersTable.getSelectionModel().select(u);
                        suppressUserSelectionHandling = false;
                        break;
                    }
                }

            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Invalid Amount");
                alert.setContentText("Please enter a valid number.");
                alert.showAndWait();
            }
        });
    }

    @FXML
    private void handleCreateEvent() {
        if (engine == null) return;
        if (selectedUser == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a user first - they will become the new event's Market Maker.").showAndWait();
            return;
        }

        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Create New Event");
        dialog.setHeaderText("Configure New Event (you, " + selectedUser.getName() + ", will be its Market Maker)");
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
        javafx.scene.control.TextField initialField = new javafx.scene.control.TextField("100");
        javafx.scene.control.TextField dField = new javafx.scene.control.TextField("1");
        javafx.scene.control.CheckBox allowMintBox = new javafx.scene.control.CheckBox("Allow Mint");
        allowMintBox.setSelected(true);

        javafx.scene.control.Label bLabel = new javafx.scene.control.Label("B (LMSR param):");
        javafx.scene.control.Label initialLabel = new javafx.scene.control.Label("Initial shares (OB):");
        javafx.scene.control.Label dLabel = new javafx.scene.control.Label("d - base value (OB):");

        grid.add(new javafx.scene.control.Label("Name:"), 0, 0);        grid.add(nameField, 1, 0);
        grid.add(new javafx.scene.control.Label("Description:"), 0, 1); grid.add(descField, 1, 1);
        grid.add(new javafx.scene.control.Label("Option 1:"), 0, 2);    grid.add(opt1Field, 1, 2);
        grid.add(new javafx.scene.control.Label("Option 2:"), 0, 3);    grid.add(opt2Field, 1, 3);
        grid.add(new javafx.scene.control.Label("Commission %:"), 0, 4); grid.add(commField, 1, 4);
        grid.add(new javafx.scene.control.Label("Type:"), 0, 5);        grid.add(typeBox, 1, 5);
        grid.add(bLabel, 0, 6);       grid.add(bField, 1, 6);
        grid.add(initialLabel, 0, 7); grid.add(initialField, 1, 7);
        grid.add(dLabel, 0, 8);       grid.add(dField, 1, 8);
        grid.add(allowMintBox, 1, 9);

        Runnable updateFieldVisibility = () -> {
            boolean isLmsr = "LMSR".equals(typeBox.getValue());
            bLabel.setVisible(isLmsr); bLabel.setManaged(isLmsr);
            bField.setVisible(isLmsr); bField.setManaged(isLmsr);
            initialLabel.setVisible(!isLmsr); initialLabel.setManaged(!isLmsr);
            initialField.setVisible(!isLmsr); initialField.setManaged(!isLmsr);
            dLabel.setVisible(!isLmsr); dLabel.setManaged(!isLmsr);
            dField.setVisible(!isLmsr); dField.setManaged(!isLmsr);
            allowMintBox.setVisible(!isLmsr); allowMintBox.setManaged(!isLmsr);
        };
        typeBox.setOnAction(e -> updateFieldVisibility.run());
        updateFieldVisibility.run();

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

                int newId;
                if ("LMSR".equals(typeBox.getValue())) {
                    int b = Integer.parseInt(bField.getText().trim());
                    newId = engine.createLmsrEvent(name, desc, comm, guessmarket.engine.models.CommissionType.ON_PURCHASE, opts, b);
                } else {
                    int initial = Integer.parseInt(initialField.getText().trim());
                    int d = Integer.parseInt(dField.getText().trim());
                    newId = engine.createOrderBookEvent(name, desc, comm, guessmarket.engine.models.CommissionType.ON_PURCHASE, opts, allowMintBox.isSelected(), initial, d);
                }
                engine.assignMarketMaker(newId, selectedUser.getName());

                refreshUsers();
                refreshAllEvents();
                new Alert(Alert.AlertType.INFORMATION, "Event '" + name + "' created! You are its Market Maker - open it from the All Events table to begin trading.").showAndWait();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Commission, B, initial and d must be valid numbers.").showAndWait();
            }
        }
    }
}
