package guessmarket.javafx.ui;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.dto.EventSummaryDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ToggleButton;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;

/**
 * Browse-only Events screen: lists every event with filters, and shows its
 * details via the shared {@link EventDetailController}. Per the assignment,
 * trading/start/close actions live on the Users screen instead - here they
 * are hidden (see initialize()).
 */
public class EventsViewController {

    @FXML private TableView<EventSummaryDTO> eventsTable;
    @FXML private TableColumn<EventSummaryDTO, Integer> idColumn;
    @FXML private TableColumn<EventSummaryDTO, String> nameColumn;
    @FXML private TableColumn<EventSummaryDTO, String> statusColumn;
    @FXML private TableColumn<EventSummaryDTO, String> commissionColumn;

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

    @FXML private EventDetailController eventDetailController;

    /** View-model row for the "who holds what" table shown under an event's details. */
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

    private MarketEngine engine;
    private EventSummaryDTO currentEvent;
    private java.util.List<EventSummaryDTO> allEvents = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        statusColumn.setCellValueFactory(cellData -> {
            boolean isActive = cellData.getValue().getActiveStatus();
            boolean isStarted = cellData.getValue().isStarted();
            String status = "Not Started";
            if (isStarted) {
                status = isActive ? "Active" : "Closed";
            }
            return new SimpleStringProperty(status);
        });

        commissionColumn.setCellValueFactory(cellData -> {
            EventSummaryDTO dto = cellData.getValue();
            return new SimpleStringProperty(dto.getCommission() + "% (" + dto.getCommissionType() + ")");
        });

        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                currentEvent = newSelection;
                eventDetailController.showEvent(newSelection);
            }
        });

        // This screen is browse-only: management/trading happens from the Users screen.
        eventDetailController.setActionsVisible(false);
        eventDetailController.setOnChange(this::refreshEvents);
    }

    public void setEngine(MarketEngine engine) {
        this.engine = engine;
        eventDetailController.setEngine(engine);
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
                    eventDetailController.showEvent(ev);
                    break;
                }
            }
        }
    }

    @FXML private void handleFilterChange() {
        applyFilter();
    }

}
