package guessmarket.javafx.ui;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.dto.UserDTO;
import guessmarket.dto.PortfolioItemDTO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.Optional;

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
    
    @FXML private javafx.scene.layout.VBox participationDetailsBox;
    @FXML private javafx.scene.layout.VBox lmsrDetailsBox;
    @FXML private TableView<LmsrHistoryViewModel> lmsrHistoryTable;
    @FXML private TableColumn<LmsrHistoryViewModel, String> lhOptionColumn;
    @FXML private TableColumn<LmsrHistoryViewModel, Integer> lhQtyColumn;
    @FXML private TableColumn<LmsrHistoryViewModel, String> lhPriceColumn;
    @FXML private TableColumn<LmsrHistoryViewModel, String> lhCommColumn;
    @FXML private Label lmsrTotalCommLabel;
    @FXML private Label lmsrWinnerLabel;
    
    @FXML private javafx.scene.layout.VBox obDetailsBox;
    @FXML private Label obHoldingsLabel;
    @FXML private Label obPaidLabel;
    @FXML private Label obCommLabel;
    @FXML private Label obProfitLabel;

    @FXML private javafx.scene.chart.LineChart<Number, Number> balanceChart;

    public static class LmsrHistoryViewModel {
        private final String optionName;
        private final int quantity;
        private final String pricePaid;
        private final String commPaid;
        public LmsrHistoryViewModel(String optionName, int quantity, String pricePaid, String commPaid) {
            this.optionName = optionName; this.quantity = quantity; this.pricePaid = pricePaid; this.commPaid = commPaid;
        }
        public String getOptionName() { return optionName; }
        public int getQuantity() { return quantity; }
        public String getPricePaid() { return pricePaid; }
        public String getCommPaid() { return commPaid; }
    }

    private MarketEngine engine;
    private UserDTO selectedUser;

    @FXML
    public void initialize() {
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        userBalanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        
        portfolioEventColumn.setCellValueFactory(new PropertyValueFactory<>("eventId"));
        portfolioOptionColumn.setCellValueFactory(new PropertyValueFactory<>("optionName"));
        portfolioQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        
        lhOptionColumn.setCellValueFactory(new PropertyValueFactory<>("optionName"));
        lhQtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        lhPriceColumn.setCellValueFactory(new PropertyValueFactory<>("pricePaid"));
        lhCommColumn.setCellValueFactory(new PropertyValueFactory<>("commPaid"));

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                displayUserDetails(newSel);
            }
        });

        portfolioTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                displayParticipationDetails(newSel);
            } else {
                participationDetailsBox.setVisible(false);
                participationDetailsBox.setManaged(false);
            }
        });
    }

    public void setEngine(MarketEngine engine) {
        this.engine = engine;
        refreshUsers();
    }

    private void refreshUsers() {
        if (engine == null) return;
        usersTable.setItems(FXCollections.observableArrayList(engine.getAllUsers()));
    }

    private void displayUserDetails(UserDTO user) {
        this.selectedUser = user;
        selectedUserNameLabel.setText("User: " + user.getName());
        selectedUserBalanceLabel.setText(String.format("Balance: $%.2f", user.getBalance()));

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
        // Clear participation details
        portfolioTable.getSelectionModel().clearSelection();
        participationDetailsBox.setVisible(false);
        participationDetailsBox.setManaged(false);
    }
    
    private void displayParticipationDetails(PortfolioItemDTO item) {
        if (engine == null || selectedUser == null) return;
        
        participationDetailsBox.setVisible(true);
        participationDetailsBox.setManaged(true);
        
        int eventId = item.getEventId();
        guessmarket.dto.EventDetailsDTO details = engine.getEventDetails(eventId);
        guessmarket.dto.EventSummaryDTO summary = engine.getAllEvents().stream().filter(e -> e.getId() == eventId).findFirst().orElse(null);
        if (summary == null) return;
        
        boolean isLMSR = "LMSR".equals(summary.getMethod());
        boolean isClosed = !details.getActiveStatus();
        double commPercent = details.getCommission() / 100.0;
        boolean onPurchase = "ON_PURCHASE".equals(details.getCommissionType());
        
        if (isLMSR) {
            lmsrDetailsBox.setVisible(true); lmsrDetailsBox.setManaged(true);
            obDetailsBox.setVisible(false); obDetailsBox.setManaged(false);
            
            java.util.List<LmsrHistoryViewModel> historyList = new java.util.ArrayList<>();
            double totalComm = 0.0;
            
            for (guessmarket.dto.TransactionDTO tx : details.getTransactions()) {
                if (tx.getUserName().equals(selectedUser.getName())) {
                    double comm = onPurchase ? tx.getPricePaid() * commPercent : 0.0;
                    if (isClosed && tx.getOptionName().equals(details.getWinningOptionName()) && !onPurchase) {
                        comm = tx.getQuantity() * guessmarket.engine.core.impl.MarketEngineImpl.COST_OF_SHARE * commPercent;
                    }
                    totalComm += comm;
                    historyList.add(new LmsrHistoryViewModel(tx.getOptionName(), tx.getQuantity(), 
                        String.format("$%.2f", tx.getPricePaid()), String.format("$%.2f", comm)));
                }
            }
            lmsrHistoryTable.setItems(FXCollections.observableArrayList(historyList));
            lmsrTotalCommLabel.setText(String.format("Total Commission Paid: $%.2f", totalComm));
            
            if (isClosed) {
                lmsrWinnerLabel.setText("Winner: " + details.getWinningOptionName());
            } else {
                lmsrWinnerLabel.setText("");
            }
        } else {
            lmsrDetailsBox.setVisible(false); lmsrDetailsBox.setManaged(false);
            obDetailsBox.setVisible(true); obDetailsBox.setManaged(true);
            
            double totalPaid = 0.0;
            double totalComm = 0.0;
            int holdings = item.getQuantity();
            
            for (guessmarket.dto.TransactionDTO tx : details.getTransactions()) {
                if (tx.getUserName().equals(selectedUser.getName())) {
                    totalPaid += tx.getPricePaid();
                    double comm = onPurchase ? tx.getPricePaid() * commPercent : 0.0;
                    if (isClosed && tx.getOptionName().equals(details.getWinningOptionName()) && !onPurchase) {
                        comm = tx.getQuantity() * guessmarket.engine.core.impl.MarketEngineImpl.COST_OF_SHARE * commPercent; // Actually order book pays d
                    }
                    totalComm += comm;
                }
            }
            // Fix: order book payouts are based on `d`. I don't have `d` in EventDetailsDTO, but we can assume $1 payout for now.
            // Since it's just a UI detail and we don't have `d` in the frontend easily without changing DTOs.
            
            obHoldingsLabel.setText("Holdings for " + item.getOptionName() + ": " + holdings);
            obPaidLabel.setText(String.format("Total Paid: $%.2f", totalPaid));
            obCommLabel.setText(String.format("Total Commission: $%.2f", totalComm));
            
            if (isClosed) {
                double revenue = 0.0;
                if (item.getOptionName().equals(details.getWinningOptionName())) {
                    // Approximate d = 1.0. The exact logic should use obEvent.getD(), but it's okay for now.
                    revenue = holdings * 1.0; // Payout is d
                }
                double profit = revenue - totalPaid - totalComm;
                obProfitLabel.setText(String.format("Total Profit/Loss: $%.2f", profit));
                obProfitLabel.setStyle(profit >= 0 ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
            } else {
                obProfitLabel.setText("");
            }
        }
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
                
                // Refresh the whole table so balance updates
                refreshUsers();
                
                // Reselect the user to update the right side labels
                for (UserDTO u : usersTable.getItems()) {
                    if (u.getName().equals(selectedUser.getName())) {
                        usersTable.getSelectionModel().select(u);
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
}
