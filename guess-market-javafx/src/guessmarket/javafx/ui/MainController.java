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
import guessmarket.dto.UserSummaryDTO;
import guessmarket.engine.core.api.MarketEngine;
import guessmarket.engine.core.api.MarketMethodSpec;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.orderbook.OrderSide;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainController {

    private static final int SIMULATED_LOAD_DELAY_MS = 1200;
    // Locale.ENGLISH is mandatory here, not cosmetic: without it, month names silently follow the JVM's
    // default locale (e.g. rendering "ספט" instead of "Sep" on a Hebrew-locale machine), and the spec
    // requires all input/output to be English only.
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss", java.util.Locale.ENGLISH);

    private MarketEngine engine;
    private final List<EventSummaryDTO> allEvents = new ArrayList<>();
    private final List<UserSummaryDTO> allUsers = new ArrayList<>();
    /** Tracks which event's full detail+trade view is currently shown inline in the Users tab, so it survives a refresh. */
    private String selectedInlineUserName;
    private Integer selectedInlineEventId;

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
    private ListView<UserSummaryDTO> userListView;

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

        userListView.setCellFactory(list -> new UserCell());
        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> showUserDetails(newVal));

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
            filePathLabel.setTooltip(new Tooltip(selectedFile.getAbsolutePath()));
            selectedInlineUserName = null;
            selectedInlineEventId = null;
            allEvents.clear();
            allEvents.addAll(loadTask.getValue());
            refreshEventList();
            // refreshEventList()/refreshUsersList() already select (and render) the first row when
            // one exists, so only fall back to the placeholder text for the genuinely empty case -
            // otherwise this would unconditionally overwrite the detail pane right after it was
            // just populated by that selection.
            if (eventListView.getSelectionModel().isEmpty()) {
                eventDetailPane.getChildren().setAll(placeholder("Select an event to see its details."));
            }
            refreshUsersList();
            if (userListView.getSelectionModel().isEmpty()) {
                userDetailPane.getChildren().setAll(placeholder("Select a user to see their details."));
            }
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
        } else if (!eventListView.getItems().isEmpty()) {
            // Same reasoning as refreshUsersList(): without an explicit selection, row 0 can look
            // selected before any real SelectionModel change has happened, so a click on it is a
            // no-op. Select it for real so the visible state and the detail pane agree.
            eventListView.getSelectionModel().selectFirst();
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

            FlowPane metaPills = new FlowPane(6, 4,
                pill(readableCommission(event.getCommissionType()) + " · " + event.getCommission() + "%", "pill-commission"),
                pill("Account: " + money(event.getAccountBalance()), "pill-account"),
                pill("MM: " + event.getMarketMakerName(), "pill-mm")
            );

            VBox box = new VBox(4, name, pills, metaPills);
            box.setPadding(new Insets(4, 2, 4, 2));
            // Without this, the box takes its natural (unwrapped) preferred width, which can exceed
            // whatever room the SplitPane divider leaves it - so the ListView grows a horizontal
            // scrollbar and clips the pills/name instead of wrapping them. Binding the box's max
            // width to the ListView's own width forces it to shrink instead, so the name ellipsizes
            // and the pills wrap onto more lines - nothing is ever hidden, no matter where the
            // divider sits.
            box.maxWidthProperty().bind(getListView().widthProperty().subtract(24));
            setGraphic(box);
        }
    }

    private class UserCell extends ListCell<UserSummaryDTO> {
        @Override
        protected void updateItem(UserSummaryDTO user, boolean empty) {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
                return;
            }
            Label name = new Label(user.getName());
            name.getStyleClass().add("event-cell-name");

            FlowPane pills = new FlowPane(6, 4, pill(money(user.getBalance()), "pill-account"));
            if (user.isBlocked()) {
                pills.getChildren().add(pill("BLOCKED", "pill-closed"));
            }

            VBox box = new VBox(4, name, pills);
            box.setPadding(new Insets(4, 2, 4, 2));
            // Same reasoning as EventCell: force wrapping instead of a horizontal scrollbar clipping content.
            box.maxWidthProperty().bind(getListView().widthProperty().subtract(24));
            setGraphic(box);
        }
    }

    // ---------------------------------------------------------------- user details

    private void showUserDetails(UserSummaryDTO user) {
        if (user == null) {
            userDetailPane.getChildren().setAll(placeholder("Select a user to see their details."));
            return;
        }

        List<Node> nodes = new ArrayList<>();

        Label nameLabel = new Label(user.getName());
        nameLabel.getStyleClass().add("detail-title");
        nodes.add(centerLabel(nameLabel));

        HBox balanceRow = new HBox(14,
            statTile("Account balance", money(user.getBalance()), "meta-value-money"),
            statTile("Blocked", user.isBlocked() ? "Yes" : "No", user.isBlocked() ? "pl-negative" : "pl-positive"));
        balanceRow.setAlignment(Pos.CENTER);
        nodes.add(balanceRow);

        // Bonus: this user can found a brand-new event and become its Market Maker.
        Button createButton = new Button("+ Create Event");
        createButton.getStyleClass().add("primary-button");
        createButton.setDisable(user.isBlocked());
        if (user.isBlocked()) {
            createButton.setTooltip(new Tooltip("A blocked user cannot create events."));
        }
        createButton.setOnAction(e -> openCreateEventDialog(user.getName()));
        HBox createRow = new HBox(createButton);
        createRow.setAlignment(Pos.CENTER);
        createRow.setPadding(new Insets(2, 0, 0, 0));
        nodes.add(createRow);

        SectionCard participationSection = sectionCard("Events participation / owner");
        nodes.add(participationSection.outer());

        if (allEvents.isEmpty()) {
            participationSection.body().getChildren().add(centerLabel(placeholder("No events loaded yet.")));
        } else {
            VBox singleEventBox = new VBox(10);
            boolean selectedEventStillListed = false;
            for (EventSummaryDTO event : allEvents) {
                participationSection.body().getChildren().add(participationCard(user.getName(), event.getId(), singleEventBox));
                if (event.getId() == (selectedInlineEventId != null ? selectedInlineEventId : -1)) {
                    selectedEventStillListed = true;
                }
            }

            SectionCard singleEventSection = sectionCard("Single event details and trade");
            if (user.getName().equals(selectedInlineUserName) && selectedEventStillListed) {
                populateSingleEventBox(selectedInlineEventId, singleEventBox);
            } else {
                singleEventBox.getChildren().add(centerLabel(placeholder("Click an event above to see its full details and trade here.")));
            }
            singleEventSection.body().getChildren().add(singleEventBox);
            nodes.add(singleEventSection.outer());
        }

        userDetailPane.getChildren().setAll(nodes);
    }

    /**
     * The "create a new event" form. Collects everything the engine needs, swapping the
     * method-specific half of the form between LMSR and Order Book as the choice changes.
     * The engine does the real validation - this dialog just keeps the user's input on screen
     * when it rejects something, instead of closing and losing what they typed.
     */
    private void openCreateEventDialog(String creatorName) {
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Will it snow in Tel Aviv?");
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("What exactly is being predicted, and how it resolves");

        ComboBox<String> commissionTypeCombo = new ComboBox<>();
        commissionTypeCombo.getItems().addAll("On purchase", "On close");
        commissionTypeCombo.getSelectionModel().selectFirst();
        Spinner<Integer> commissionSpinner = new Spinner<>(0, 90, 5);
        commissionSpinner.setEditable(true);
        commissionSpinner.setPrefWidth(90);

        TextField optionOneField = new TextField();
        optionOneField.setPromptText("First option");
        TextField optionTwoField = new TextField();
        optionTwoField.setPromptText("Second option");

        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll("LMSR", "Order Book");
        methodCombo.getSelectionModel().selectFirst();

        Spinner<Integer> bSpinner = new Spinner<>(1, 1_000_000, 100);
        bSpinner.setEditable(true);
        bSpinner.setPrefWidth(110);
        CheckBox allowMintBox = new CheckBox("Allow mint");
        allowMintBox.setSelected(true);
        Spinner<Integer> initialSpinner = new Spinner<>(0, 1_000_000, 100);
        initialSpinner.setEditable(true);
        initialSpinner.setPrefWidth(110);
        Spinner<Integer> dSpinner = new Spinner<>(1, 1_000_000, 1);
        dSpinner.setEditable(true);
        dSpinner.setPrefWidth(110);

        VBox lmsrFields = new VBox(6, fieldRow("Liquidity (b)", bSpinner),
            hintLabel("Higher b means steadier prices."),
            hintLabel("Opening costs the Market Maker b x ln(2)."));
        VBox bookFields = new VBox(6, fieldRow("Base value (d)", dSpinner),
            fieldRow("Initial shares", initialSpinner), allowMintBox,
            hintLabel("Opening costs the Market Maker initial x d,"),
            hintLabel("and gives them that many of each option."));

        VBox methodBox = new VBox(8, lmsrFields);

        VBox form = new VBox(10,
            fieldRow("Event name", nameField),
            fieldRow("Description", descriptionField),
            fieldRow("Option 1", optionOneField),
            fieldRow("Option 2", optionTwoField),
            fieldRow("Commission", commissionTypeCombo),
            fieldRow("Commission %", commissionSpinner),
            fieldRow("Trading method", methodCombo),
            methodBox);
        form.setPadding(new Insets(4, 2, 4, 2));
        form.setPrefWidth(440);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Event");
        dialog.setHeaderText("New event, with " + creatorName + " as its Market Maker");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getStylesheets().addAll(userDetailPane.getScene().getStylesheets());
        dialog.setResizable(true);

        // Sizes the window to exactly what the form needs, then makes that the floor: the dialog
        // can still be grown, but never dragged smaller than its own content. The minimums are
        // released first, otherwise they would block sizeToScene() from shrinking the window back
        // down when switching from the taller Order Book fields to the shorter LMSR ones.
        Runnable fitToContent = () -> {
            if (dialog.getDialogPane().getScene().getWindow() instanceof Stage stage) {
                stage.setMinWidth(0);
                stage.setMinHeight(0);
                stage.sizeToScene();
                stage.setMinWidth(stage.getWidth());
                stage.setMinHeight(stage.getHeight());
            }
        };
        // Deferred by one pulse: at the moment onShown fires the dialog's window is not fully
        // realised yet, so reading it back gives nothing to set the floor on.
        dialog.setOnShown(e -> Platform.runLater(fitToContent));

        // Swapping in the Order Book fields makes the form taller than the LMSR one. Without
        // re-sizing to the new content the window keeps its old height, which pushes OK and Cancel
        // off the bottom edge and makes the dialog impossible to submit.
        methodCombo.setOnAction(e -> {
            methodBox.getChildren().setAll("LMSR".equals(methodCombo.getValue()) ? lmsrFields : bookFields);
            fitToContent.run();
        });

        // Do the creation inside an event filter so a rejection can consume the event and leave the
        // dialog open with everything the user typed still in place.
        final int[] createdId = { -1 };
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, e -> {
            MarketMethodSpec method = "LMSR".equals(methodCombo.getValue())
                ? new MarketMethodSpec.Lmsr(bSpinner.getValue())
                : new MarketMethodSpec.OrderBook(allowMintBox.isSelected(), initialSpinner.getValue(), dSpinner.getValue());
            CommissionType commissionType = "On close".equals(commissionTypeCombo.getValue())
                ? CommissionType.ON_CLOSE : CommissionType.ON_PURCHASE;
            try {
                createdId[0] = engine.createEvent(creatorName, nameField.getText(), descriptionField.getText(),
                    commissionSpinner.getValue(), commissionType,
                    List.of(optionOneField.getText(), optionTwoField.getText()), method);
            } catch (Exception ex) {
                showError(ex.getMessage());
                e.consume();
            }
        });

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK && createdId[0] > 0) {
            // Point the inline view at the new event BEFORE refreshing. refreshUsersList() reselects
            // the creator, which re-renders their details through the selection listener - so doing
            // this first means that single render already shows the new event, instead of drawing
            // once with the previous selection and again afterwards.
            selectedInlineUserName = creatorName;
            selectedInlineEventId = createdId[0];
            allEvents.clear();
            allEvents.addAll(engine.getAllEvents());
            refreshEventList();
            refreshUsersList();
        }
    }

    /** One labelled form row: a fixed-width caption beside its control. */
    private HBox fieldRow(String caption, Node control) {
        Label label = new Label(caption);
        label.getStyleClass().add("meta-label");
        label.setMinWidth(120);
        label.setPrefWidth(120);
        HBox row = new HBox(10, label, control);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(control, Priority.ALWAYS);
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return row;
    }

    /** A muted one-line note under a form field. Kept short enough to never need wrapping. */
    private Label hintLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("price-card-chance");
        return label;
    }

    /** A titled, bordered card: a header bar (visually distinct from body content) followed by a body area to add content to. */
    private record SectionCard(VBox outer, VBox body) {}

    private SectionCard sectionCard(String title) {
        Label headerLabel = new Label(title);
        headerLabel.getStyleClass().add("section-card-title");
        HBox headerBar = new HBox(headerLabel);
        headerBar.setAlignment(Pos.CENTER);
        headerBar.getStyleClass().add("section-card-header");

        VBox body = new VBox(10);
        body.getStyleClass().add("section-card-body");

        VBox outer = new VBox(headerBar, body);
        outer.getStyleClass().add("section-card");
        return new SectionCard(outer, body);
    }

    /** Renders the same full single-event detail+trade view used by the Events tab, inline within the Users tab, interactive as this user. */
    private void populateSingleEventBox(int eventId, VBox box) {
        try {
            EventDetailsDTO details = engine.getEventDetails(eventId);
            Runnable onChange = () -> {
                allEvents.clear();
                allEvents.addAll(engine.getAllEvents());
                refreshEventList();
                refreshUsersList();
            };
            if (details instanceof LmsrEventDetailsDTO) {
                box.getChildren().setAll(buildLmsrDetail((LmsrEventDetailsDTO) details, selectedInlineUserName, onChange));
            } else if (details instanceof OrderBookEventDetailsDTO) {
                box.getChildren().setAll(buildOrderBookDetail((OrderBookEventDetailsDTO) details, selectedInlineUserName, onChange));
            }
        } catch (Exception e) {
            box.getChildren().setAll(placeholder(e.getMessage()));
        }
    }

    private VBox participationCard(String userName, int eventId, VBox singleEventBox) {
        EventSummaryDTO summary = null;
        for (EventSummaryDTO e : allEvents) {
            if (e.getId() == eventId) {
                summary = e;
                break;
            }
        }
        VBox card = new VBox(6);
        card.getStyleClass().add("book-panel");
        card.setPadding(new Insets(10));
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> {
            selectedInlineUserName = userName;
            selectedInlineEventId = eventId;
            populateSingleEventBox(eventId, singleEventBox);
        });
        if (summary == null) {
            card.getChildren().add(centerLabel(placeholder("Event " + eventId + " (details unavailable).")));
            return card;
        }

        boolean isMm = userName.equals(summary.getMarketMakerName());
        Label title = new Label(summary.getName());
        title.getStyleClass().add("book-panel-title");
        HBox pills = new HBox(6, pill(readableType(summary.getType()), typePillClass(summary.getType())), pill(readableStatus(summary.getStatus()), statusPillClass(summary.getStatus())));
        if (isMm) {
            pills.getChildren().add(pill("MARKET MAKER", "pill-mm-tag"));
        }
        pills.setAlignment(Pos.CENTER);
        Label hint = new Label("Click to view details & trade below ↓");
        hint.getStyleClass().add("hint-chip");
        HBox hintWrap = new HBox(hint);
        hintWrap.setAlignment(Pos.CENTER);
        card.getChildren().addAll(centerLabel(title), pills, hintWrap);

        try {
            EventDetailsDTO details = engine.getEventDetails(eventId);
            if (details instanceof LmsrEventDetailsDTO) {
                LmsrEventDetailsDTO lmsr = (LmsrEventDetailsDTO) details;
                Map<String, Integer> sharesByOption = new LinkedHashMap<>();
                for (OptionDTO option : lmsr.getOptions()) {
                    sharesByOption.put(option.getName(), 0);
                }
                boolean hasTraded = false;
                for (TransactionDTO tx : lmsr.getTransactions()) {
                    if (tx.getUserName().equals(userName)) {
                        hasTraded = true;
                        sharesByOption.merge(tx.getOptionName(), tx.getQuantity(), Integer::sum);
                    }
                }
                if (!hasTraded) {
                    card.getChildren().add(centerLabel(placeholder("No trades by this user yet.")));
                } else {
                    // A quick glance at this user's position - not the full trade-by-trade ledger,
                    // which belongs only in the detailed view below, not in this summary card.
                    FlowPane tiles = new FlowPane(8, 8);
                    tiles.setAlignment(Pos.CENTER);
                    for (OptionDTO option : lmsr.getOptions()) {
                        int qty = sharesByOption.get(option.getName());
                        tiles.getChildren().add(statTile(option.getName(), qty + " shares", "meta-value"));
                    }
                    card.getChildren().add(tiles);
                }
            } else if (details instanceof OrderBookEventDetailsDTO) {
                OrderBookEventDetailsDTO ob = (OrderBookEventDetailsDTO) details;
                ParticipantHoldingDTO match = null;
                for (ParticipantHoldingDTO p : ob.getParticipants()) {
                    if (p.getUserName().equals(userName)) {
                        match = p;
                        break;
                    }
                }
                if (match == null) {
                    card.getChildren().add(centerLabel(placeholder("No holdings by this user yet.")));
                } else {
                    FlowPane tiles = new FlowPane(8, 8);
                    tiles.setAlignment(Pos.CENTER);
                    for (int i = 0; i < ob.getOptionBooks().size(); i++) {
                        int qty = match.getHoldingsByOption().get(i);
                        tiles.getChildren().add(statTile(ob.getOptionBooks().get(i).getOptionName(), qty + " shares", "meta-value"));
                    }
                    tiles.getChildren().add(statTile("Est. value", money(match.getEstimatedValue()), "meta-value-money"));
                    card.getChildren().add(tiles);

                    if ("CLOSED".equals(ob.getStatus()) && match.getProfitOrLoss() != null) {
                        double pl = match.getProfitOrLoss();
                        Label plLabel = new Label((pl >= 0 ? "Profit: " : "Loss: ") + money(Math.abs(pl)));
                        plLabel.getStyleClass().add(pl >= 0 ? "pl-positive" : "pl-negative");
                        card.getChildren().add(centerLabel(plLabel));
                    }
                }
            }
        } catch (Exception e) {
            card.getChildren().add(centerLabel(placeholder(e.getMessage())));
        }

        return card;
    }

    // ---------------------------------------------------------------- event details (read-only - trading only happens from the Users tab)

    private void showEventDetails(EventSummaryDTO selected) {
        if (selected == null) {
            eventDetailPane.getChildren().setAll(placeholder("Select an event to see its details."));
            return;
        }
        try {
            EventDetailsDTO details = engine.getEventDetails(selected.getId());
            Runnable onChange = () -> reloadAndShowEvent(selected.getId());
            if (details instanceof LmsrEventDetailsDTO) {
                eventDetailPane.getChildren().setAll(buildLmsrDetail((LmsrEventDetailsDTO) details, null, onChange));
            } else if (details instanceof OrderBookEventDetailsDTO) {
                eventDetailPane.getChildren().setAll(buildOrderBookDetail((OrderBookEventDetailsDTO) details, null, onChange));
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    /**
     * Builds the full detail view for an LMSR event. When viewingUserName is null (Events tab) the view is
     * read-only and shows every trade; when non-null (embedded in the Users tab) it becomes interactive as that
     * user - open/close appear only if they're the MM, buying is always available, and trade history is filtered
     * down to just their own trades.
     */
    private List<Node> buildLmsrDetail(LmsrEventDetailsDTO dto, String viewingUserName, Runnable onChange) {
        List<Node> nodes = new ArrayList<>();
        // Viewed from a specific person's page (Users tab), this figure should be their own
        // commission, not the event-wide total - otherwise it reads as "your commission" but
        // silently shows everyone's. The read-only Events tab has no single viewer, so it keeps
        // showing the event-wide total there.
        String commissionLabel = viewingUserName != null ? "Your commission" : "Commission collected";
        double commissionValue = viewingUserName != null ? dto.getCommissionPaidBy(viewingUserName) : dto.getTotalCommissionCollected();
        nodes.add(detailHeader(dto.getName(), dto.getDescription(), dto.getStatus(), dto.getMarketMakerName(),
            readableCommission(dto.getCommissionType()) + " · " + dto.getCommission() + "%",
            "Account balance", money(dto.getAccountBalance()),
            commissionLabel, money(commissionValue)));

        if (dto.getWinningOptionName() != null) {
            nodes.add(winnerBanner(dto.getWinningOptionName()));
        }

        List<String> optionNames = new ArrayList<>();
        for (OptionDTO option : dto.getOptions()) {
            optionNames.add(option.getName());
        }

        boolean isMm = viewingUserName != null && viewingUserName.equals(dto.getMarketMakerName());
        if (isMm && !"CLOSED".equals(dto.getStatus())) {
            nodes.add(actionBar(dto.getId(), dto.getStatus(), optionNames, viewingUserName, onChange));
        }

        if (viewingUserName != null && "ACTIVE".equals(dto.getStatus())) {
            nodes.add(interactiveLmsrCards(dto.getId(), dto.getOptions(), viewingUserName, onChange));
        } else {
            FlowPane priceCards = new FlowPane(12, 10);
            priceCards.setAlignment(Pos.CENTER);
            for (OptionDTO option : dto.getOptions()) {
                priceCards.getChildren().add(priceCard(option, option.getName().equals(dto.getWinningOptionName())));
            }
            priceCards.setPadding(new Insets(0, 0, 10, 0));
            nodes.add(priceCards);
        }

        List<TransactionDTO> history = new ArrayList<>(dto.getTransactions());
        Collections.reverse(history);
        if (viewingUserName != null) {
            history.removeIf(tx -> !tx.getUserName().equals(viewingUserName));
        }

        TableView<TransactionDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        if (viewingUserName == null) {
            table.getColumns().add(column("User", "userName", 100));
        }
        table.getColumns().add(column("Option", "optionName", 90));
        table.getColumns().add(column("Qty", "quantity", 70));
        table.getColumns().add(moneyColumn("Paid", "pricePaid", 90));
        table.getColumns().add(optionalMoneyColumn("Commission", "commissionPaid", 100));
        table.getColumns().add(timestampColumn("Time", "timestamp", 170));
        table.getItems().addAll(history);
        table.setPlaceholder(new Label("No trades yet."));
        // On the Events tab this shows every user's trades (hence the "User" column above); on the
        // Users tab it's already filtered down to just the viewer, so the title should say so.
        nodes.add(centerLabel(sectionLabel(viewingUserName != null ? "Your trade history" : "Trade history")));
        nodes.add(table);

        return nodes;
    }

    /** The two LMSR option cards, made clickable: pick a card to trade it, then a shared quantity
     * spinner and Buy button below act on whichever card is currently selected. Nothing is
     * pre-selected - the spinner and button stay disabled until the user picks an option. */
    private VBox interactiveLmsrCards(int eventId, List<OptionDTO> options, String actingUserName, Runnable onChange) {
        List<VBox> cardNodes = new ArrayList<>();
        int[] selectedIndex = { -1 };

        Spinner<Integer> qtySpinner = new Spinner<>(1, 1_000_000, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(100);
        qtySpinner.setDisable(true);

        Button buyBtn = new Button("Buy Shares");
        buyBtn.getStyleClass().add("primary-button");
        buyBtn.setDisable(true);

        Label pickHint = new Label("Click an option below to trade it ↓");
        pickHint.getStyleClass().add("hint-chip");
        HBox pickHintWrap = new HBox(pickHint);
        pickHintWrap.setAlignment(Pos.CENTER);

        FlowPane cardsRow = new FlowPane(12, 10);
        cardsRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < options.size(); i++) {
            VBox card = priceCard(options.get(i), false);
            card.setCursor(javafx.scene.Cursor.HAND);
            final int idx = i;
            card.setOnMouseClicked(e -> {
                selectedIndex[0] = idx;
                for (int j = 0; j < cardNodes.size(); j++) {
                    VBox c = cardNodes.get(j);
                    c.getStyleClass().removeAll("price-card", "price-card-selected");
                    c.getStyleClass().add(j == idx ? "price-card-selected" : "price-card");
                }
                qtySpinner.setDisable(false);
                buyBtn.setDisable(false);
            });
            cardNodes.add(card);
            cardsRow.getChildren().add(card);
        }

        buyBtn.setOnAction(e -> {
            if (selectedIndex[0] < 0) {
                showError("Choose an option first.");
                return;
            }
            int quantity = qtySpinner.getValue();
            runTradeAction(() -> engine.buyShares(actingUserName, eventId, selectedIndex[0], quantity), onChange, actingUserName);
        });

        Label qtyCaption = new Label("QUANTITY");
        qtyCaption.getStyleClass().add("meta-label");
        VBox controls = new VBox(8, centerLabel(qtyCaption), qtySpinner, buyBtn);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(4, 0, 10, 0));

        VBox box = new VBox(10, pickHintWrap, cardsRow, controls);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private List<Node> buildOrderBookDetail(OrderBookEventDetailsDTO dto, String viewingUserName, Runnable onChange) {
        List<Node> nodes = new ArrayList<>();
        nodes.add(detailHeader(dto.getName(), dto.getDescription(), dto.getStatus(), dto.getMarketMakerName(),
            readableCommission(dto.getCommissionType()) + " · " + dto.getCommission() + "%",
            "Base value (d)", money(dto.getBaseValue()),
            "Account balance", money(dto.getAccountBalance())));

        if (dto.getWinningOptionName() != null) {
            nodes.add(winnerBanner(dto.getWinningOptionName()));
        }

        List<String> optionNames = new ArrayList<>();
        for (OptionBookDTO book : dto.getOptionBooks()) {
            optionNames.add(book.getOptionName());
        }

        boolean isMm = viewingUserName != null && viewingUserName.equals(dto.getMarketMakerName());
        if (isMm && !"CLOSED".equals(dto.getStatus())) {
            nodes.add(actionBar(dto.getId(), dto.getStatus(), optionNames, viewingUserName, onChange));
        }

        boolean active = "ACTIVE".equals(dto.getStatus());
        FlowPane books = new FlowPane(14, 14);
        books.setAlignment(Pos.CENTER);
        for (int i = 0; i < dto.getOptionBooks().size(); i++) {
            boolean isWinner = dto.getOptionBooks().get(i).getOptionName().equals(dto.getWinningOptionName());
            books.getChildren().add(bookPanel(dto.getOptionBooks().get(i), dto.getId(), i, active, isWinner, viewingUserName, dto.getBaseValue(), onChange));
        }
        nodes.add(books);

        if (viewingUserName == null) {
            // Events tab: a read-only overview of every participant's position - appropriate here, since
            // this view is about the whole market, not any one person.
            TableView<ParticipantHoldingDTO> participantsTable = new TableView<>();
            participantsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            TableColumn<ParticipantHoldingDTO, String> userCol = new TableColumn<>("Participant");
            userCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
            userCol.setPrefWidth(140);
            participantsTable.getColumns().add(userCol);
            for (int i = 0; i < dto.getOptionBooks().size(); i++) {
                final int optionIndex = i;
                TableColumn<ParticipantHoldingDTO, String> holdingCol = new TableColumn<>(dto.getOptionBooks().get(i).getOptionName() + " held");
                holdingCol.setCellValueFactory(row -> new SimpleStringProperty(String.valueOf(row.getValue().getHoldingsByOption().get(optionIndex))));
                holdingCol.setPrefWidth(110);
                participantsTable.getColumns().add(holdingCol);
            }
            TableColumn<ParticipantHoldingDTO, String> valueCol = new TableColumn<>("Est. value");
            valueCol.setCellValueFactory(row -> new SimpleStringProperty(money(row.getValue().getEstimatedValue())));
            valueCol.setPrefWidth(100);
            participantsTable.getColumns().add(valueCol);
            participantsTable.getItems().addAll(dto.getParticipants());
            participantsTable.setPlaceholder(new Label("No participants yet."));

            nodes.add(centerLabel(sectionLabel("Participations")));
            nodes.add(participantsTable);
        } else {
            // Users tab: this user's own position only - never expose other participants' holdings here.
            VBox positionCard = new VBox(10);
            positionCard.getStyleClass().add("book-panel");
            positionCard.setPadding(new Insets(14));
            positionCard.getChildren().add(centerLabel(sectionLabel("Your position")));

            ParticipantHoldingDTO mine = null;
            for (ParticipantHoldingDTO p : dto.getParticipants()) {
                if (p.getUserName().equals(viewingUserName)) {
                    mine = p;
                    break;
                }
            }
            if (mine == null) {
                positionCard.getChildren().add(centerLabel(placeholder("No holdings by this user yet.")));
            } else {
                FlowPane optionCards = new FlowPane(12, 10);
                optionCards.setAlignment(Pos.CENTER);
                for (int i = 0; i < dto.getOptionBooks().size(); i++) {
                    optionCards.getChildren().add(optionPositionCard(dto.getOptionBooks().get(i).getOptionName(), mine.getHoldingsByOption().get(i), mine.getPaidByOption().get(i)));
                }
                positionCard.getChildren().add(optionCards);

                List<Node> summaryTiles = new ArrayList<>();
                // Same sleek, non-obvious phrasing as the LMSR detail's per-viewer commission tile -
                // this box only ever renders for the one user looking at their own position.
                summaryTiles.add(coloredMetaBox("Your commission", money(mine.getCommissionPaid()), "meta-value"));
                if ("CLOSED".equals(dto.getStatus()) && mine.getProfitOrLoss() != null) {
                    double pl = mine.getProfitOrLoss();
                    boolean profit = pl >= 0;
                    summaryTiles.add(coloredMetaBox(profit ? "Profit" : "Loss", money(Math.abs(pl)), profit ? "pl-positive" : "pl-negative"));
                }
                FlowPane summaryStrip = new FlowPane(10, 10, summaryTiles.toArray(new Node[0]));
                summaryStrip.setAlignment(Pos.CENTER);
                positionCard.getChildren().add(summaryStrip);
            }
            nodes.add(positionCard);
        }

        return nodes;
    }

    private VBox bookPanel(OptionBookDTO book, int eventId, int optionIndex, boolean active, boolean isWinner, String actingUserName, double baseValue, Runnable onChange) {
        Label headerLabel = new Label(book.getOptionName());
        headerLabel.getStyleClass().add("book-panel-title");
        HBox header = isWinner ? new HBox(6, headerLabel, pill("WINNER", "pill-winner")) : new HBox(headerLabel);
        header.setAlignment(Pos.CENTER);

        HBox stats = new HBox();
        stats.getStyleClass().add("stats-row");
        for (VBox box : List.of(statBox("Last", book.getLast()), statBox("Bid", book.getBid()), statBox("Ask", book.getAsk()),
                                 statBox("Mid", book.getMid()), statBox("Spread", book.getSpread()))) {
            HBox.setHgrow(box, Priority.ALWAYS);
            box.setMaxWidth(Double.MAX_VALUE);
            stats.getChildren().add(box);
        }

        TableView<OrderDTO> bids = orderTable(book.getBids());
        TableView<OrderDTO> asks = orderTable(book.getAsks());

        VBox panel = new VBox(6, header, stats, centerLabel(sectionLabel("Bids")), bids, centerLabel(sectionLabel("Asks")), asks);
        if (active && actingUserName != null) {
            panel.getChildren().add(obTradeForm(eventId, optionIndex, actingUserName, baseValue, onChange));
        }
        panel.getStyleClass().add(isWinner ? "book-panel-winner" : "book-panel");
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(340);
        panel.setMinWidth(300);
        return panel;
    }

    private TableView<OrderDTO> orderTable(List<OrderDTO> orders) {
        TableView<OrderDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("User", "userName", 90));
        table.getColumns().add(column("Qty", "quantity", 60));
        table.getColumns().add(moneyColumn("Price", "price", 80));
        table.getItems().addAll(orders);
        table.setPlaceholder(new Label("—"));
        table.setPrefHeight(120);
        return table;
    }

    // ---------------------------------------------------------------- actions

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private void runAction(ThrowingAction action, Runnable onChange) {
        try {
            action.run();
            onChange.run();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    /** Same as runAction, but also warns the acting user the moment a trade pushes their balance negative and blocks them. */
    private void runTradeAction(ThrowingAction action, Runnable onChange, String actingUserName) {
        try {
            action.run();
            onChange.run();
            warnIfNowBlocked(actingUserName);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void warnIfNowBlocked(String userName) {
        try {
            for (UserSummaryDTO u : engine.getAllUsers()) {
                if (u.getName().equals(userName) && u.isBlocked()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Guess Market");
                    alert.setHeaderText("Account blocked");
                    alert.setContentText("'" + userName + "' now has a negative balance (" + money(u.getBalance()) + ") and is blocked from performing any further actions.");
                    alert.showAndWait();
                    return;
                }
            }
        } catch (Exception ignored) {
            // best-effort notification only
        }
    }

    private void reloadAndShowEvent(int eventId) {
        try {
            allEvents.clear();
            allEvents.addAll(engine.getAllEvents());
            refreshEventList();
            refreshUsersList();
            for (EventSummaryDTO e : eventListView.getItems()) {
                if (e.getId() == eventId) {
                    eventListView.getSelectionModel().select(e);
                    return;
                }
            }
            showEventDetailsById(eventId);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void refreshUsersList() {
        UserSummaryDTO previouslySelected = userListView.getSelectionModel().getSelectedItem();
        String previousName = previouslySelected != null ? previouslySelected.getName() : null;
        allUsers.clear();
        try {
            allUsers.addAll(engine.getAllUsers());
        } catch (Exception ignored) {
            // no users loaded yet
        }
        userListView.getItems().setAll(allUsers);
        boolean restored = false;
        if (previousName != null) {
            for (UserSummaryDTO u : userListView.getItems()) {
                if (u.getName().equals(previousName)) {
                    userListView.getSelectionModel().select(u);
                    restored = true;
                    break;
                }
            }
        }
        // Nothing to restore (first load, or the selected user is gone): select the first row
        // explicitly. Otherwise the ListView's default focus model paints row 0 as if it were
        // selected without the SelectionModel actually holding it, so a click on that same row
        // is a no-op (no change event) and the detail pane never populates.
        if (!restored && !userListView.getItems().isEmpty()) {
            userListView.getSelectionModel().selectFirst();
        }
    }

    private void showEventDetailsById(int eventId) {
        try {
            EventDetailsDTO details = engine.getEventDetails(eventId);
            Runnable onChange = () -> reloadAndShowEvent(eventId);
            if (details instanceof LmsrEventDetailsDTO) {
                eventDetailPane.getChildren().setAll(buildLmsrDetail((LmsrEventDetailsDTO) details, null, onChange));
            } else if (details instanceof OrderBookEventDetailsDTO) {
                eventDetailPane.getChildren().setAll(buildOrderBookDetail((OrderBookEventDetailsDTO) details, null, onChange));
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    /** actionBar only renders when there is something for actingUserName to do - callers gate on them being the MM. */
    private FlowPane actionBar(int eventId, String status, List<String> optionNames, String actingUserName, Runnable onChange) {
        FlowPane bar = new FlowPane(8, 6);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(0, 0, 10, 0));

        if ("NOT_ACTIVE".equals(status)) {
            Button openBtn = new Button("Open Event");
            openBtn.getStyleClass().add("primary-button");
            openBtn.setOnAction(e -> runAction(() -> engine.openEvent(actingUserName, eventId), onChange));
            bar.getChildren().add(openBtn);
        } else if ("ACTIVE".equals(status)) {
            for (int i = 0; i < optionNames.size(); i++) {
                final int winningIndex = i;
                Button closeBtn = new Button("Close: " + optionNames.get(i) + " wins");
                closeBtn.getStyleClass().add("close-button");
                closeBtn.setOnAction(e -> runAction(() -> engine.closeEvent(actingUserName, eventId, winningIndex), onChange));
                bar.getChildren().add(closeBtn);
            }
        }
        return bar;
    }

    private FlowPane obTradeForm(int eventId, int optionIndex, String actingUserName, double baseValue, Runnable onChange) {
        ComboBox<String> sideCombo = new ComboBox<>();
        sideCombo.getItems().addAll("Buy", "Sell");
        sideCombo.getSelectionModel().selectFirst();

        Spinner<Integer> qtySpinner = new Spinner<>(1, 1_000_000, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(90);

        double minPrice = 0.01;
        double maxPrice = Math.max(minPrice, baseValue - 0.01);
        double defaultPrice = Math.round((minPrice + maxPrice) / 2 * 100.0) / 100.0;
        Spinner<Double> priceSpinner = new Spinner<>(minPrice, maxPrice, defaultPrice, 0.01);
        priceSpinner.setEditable(true);
        priceSpinner.setPrefWidth(100);

        Button submitBtn = new Button("Submit Order");
        submitBtn.getStyleClass().add("primary-button");
        submitBtn.setOnAction(e -> {
            int quantity = qtySpinner.getValue();
            double price = priceSpinner.getValue();
            OrderSide side = "Sell".equals(sideCombo.getValue()) ? OrderSide.SELL : OrderSide.BUY;
            runTradeAction(() -> engine.submitOrder(actingUserName, eventId, optionIndex, side, price, quantity), onChange, actingUserName);
        });

        FlowPane form = new FlowPane(6, 6, sideCombo, labeledField("QTY", qtySpinner), labeledField("PRICE", priceSpinner), submitBtn);
        form.getStyleClass().add("tradebox");
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(8, 0, 0, 0));
        return form;
    }

    /** A small uppercase caption stacked above a control - used so a bare Spinner isn't left
     * unlabeled about what quantity it means (order side, in this case; see obTradeForm). */
    private VBox labeledField(String caption, Node control) {
        Label label = new Label(caption);
        label.getStyleClass().add("meta-label");
        VBox box = new VBox(2, centerLabel(label), control);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // ---------------------------------------------------------------- small builders

    private VBox detailHeader(String name, String description, String status, String marketMakerName, String commissionText, String metaLabel1, String metaValue1, String metaLabel2, String metaValue2) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("detail-title");

        Label descHeading = new Label("DESCRIPTION");
        descHeading.getStyleClass().add("meta-label");
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("detail-desc");
        descLabel.setWrapText(true);
        VBox descBox = new VBox(4, descHeading, descLabel);
        descBox.getStyleClass().add("description-box");
        descBox.setPadding(new Insets(10, 14, 10, 14));

        FlowPane meta = new FlowPane(10, 10, metaBox("Status", readableStatus(status)), metaBox("Market Maker", marketMakerName),
            metaBox("Commission", commissionText), metaBox(metaLabel1, metaValue1), metaBox(metaLabel2, metaValue2));
        meta.setAlignment(Pos.CENTER);

        VBox header = new VBox(8, centerLabel(nameLabel), descBox, meta);
        header.setPadding(new Insets(0, 0, 8, 0));
        return header;
    }

    /** A prominent, hard-to-miss banner announcing the winning option once an event is closed. */
    private HBox winnerBanner(String winningOptionName) {
        Label label = new Label("🏆  Winner: " + winningOptionName);
        label.getStyleClass().add("winner-banner-label");
        HBox banner = new HBox(label);
        banner.setAlignment(Pos.CENTER);
        banner.getStyleClass().add("winner-banner");
        banner.setPadding(new Insets(10, 14, 10, 14));
        return banner;
    }

    private VBox metaBox(String label, String value) {
        Label l = new Label(label.toUpperCase());
        l.getStyleClass().add("meta-label");
        Label v = new Label(value);
        boolean isBalance = label.equalsIgnoreCase("Account balance");
        v.getStyleClass().add(isBalance ? "meta-value-money" : "meta-value");
        VBox box = new VBox(2, centerLabel(l), centerLabel(v));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("stat-tile");
        return box;
    }

    private VBox statBox(String label, Double value) {
        Label l = new Label(label.toUpperCase());
        l.getStyleClass().add("stat-label");
        Label v = new Label(value == null ? "—" : money(value));
        v.getStyleClass().add("stat-value");
        VBox box = new VBox(2, centerLabel(l), centerLabel(v));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("stat-box");
        return box;
    }

    private VBox priceCard(OptionDTO option, boolean isWinner) {
        HBox nameRow = isWinner
            ? new HBox(6, new Label(option.getName()), pill("WINNER", "pill-winner"))
            : new HBox(new Label(option.getName()));
        nameRow.setAlignment(Pos.CENTER);
        Label price = new Label(money(option.getCurrentProbability()));
        price.getStyleClass().add("price-card-value");
        Label chance = new Label(Math.round(option.getCurrentProbability() * 100) + "% implied chance");
        chance.getStyleClass().add("price-card-chance");
        Label shares = new Label(option.getSharesBought() + " shares bought");
        shares.getStyleClass().add("price-card-shares");
        VBox card = new VBox(5, nameRow, centerLabel(price), centerLabel(chance), centerLabel(shares));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add(isWinner ? "price-card-winner" : "price-card");
        card.setPadding(new Insets(16, 22, 16, 22));
        card.setPrefWidth(190);
        return card;
    }

    /** One option's slice of a user's Order Book position: shares held and the gross amount paid buying into it. */
    private VBox optionPositionCard(String optionName, int sharesHeld, double amountPaid) {
        Label name = new Label(optionName);
        Label shares = new Label(sharesHeld + " shares held");
        shares.getStyleClass().add("position-card-shares");
        shares.setWrapText(true);
        shares.setTextAlignment(TextAlignment.CENTER);
        Label paid = new Label("Paid: " + money(amountPaid));
        paid.getStyleClass().add("position-card-paid");
        VBox card = new VBox(4, centerLabel(name), centerLabel(shares), centerLabel(paid));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("price-card");
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setPrefWidth(210);
        return card;
    }

    /** Like metaBox, but lets the caller pick the value's style class (e.g. to color it green for a profit, red for a loss). */
    private VBox coloredMetaBox(String label, String value, String valueStyleClass) {
        Label l = new Label(label.toUpperCase());
        l.getStyleClass().add("meta-label");
        Label v = new Label(value);
        v.getStyleClass().add(valueStyleClass);
        VBox box = new VBox(2, centerLabel(l), centerLabel(v));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("stat-tile");
        return box;
    }

    /** A small standalone stat card - a caption above a bold value, with its own visible border, so it
     * reads as a compact fact card rather than a pill/tag or a strip-glued tile. */
    private VBox statTile(String label, String value, String valueStyleClass) {
        Label l = new Label(label.toUpperCase());
        l.getStyleClass().add("meta-label");
        Label v = new Label(value);
        v.getStyleClass().add(valueStyleClass);
        VBox box = new VBox(2, centerLabel(l), centerLabel(v));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("stat-tile");
        return box;
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

    /** Centers a label's text within whatever width it ends up stretched to by its parent. */
    private Label centerLabel(Label label) {
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
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
                return new SimpleStringProperty(money(((Number) raw).doubleValue()));
            } catch (Exception e) {
                return new SimpleStringProperty("");
            }
        });
        col.setPrefWidth(width);
        return col;
    }

    /** Like moneyColumn, but shows an em dash instead of $0.00 - for values that are usually zero (e.g. no commission charged). */
    private <S> TableColumn<S, String> optionalMoneyColumn(String title, String property, double width) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(row -> {
            try {
                Object raw = row.getValue().getClass().getMethod("get" + Character.toUpperCase(property.charAt(0)) + property.substring(1)).invoke(row.getValue());
                double value = ((Number) raw).doubleValue();
                return new SimpleStringProperty(value > 0 ? money(value) : "—");
            } catch (Exception e) {
                return new SimpleStringProperty("");
            }
        });
        col.setPrefWidth(width);
        return col;
    }

    private <S> TableColumn<S, String> timestampColumn(String title, String property, double width) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(row -> {
            try {
                Object raw = row.getValue().getClass().getMethod("get" + Character.toUpperCase(property.charAt(0)) + property.substring(1)).invoke(row.getValue());
                return new SimpleStringProperty(((LocalDateTime) raw).format(TIMESTAMP_FORMAT));
            } catch (Exception e) {
                return new SimpleStringProperty("");
            }
        });
        col.setPrefWidth(width);
        return col;
    }

    private String money(double value) {
        // Collapse amounts too small to show so floating-point dust cannot render as "$-0.00":
        // a closed event's account can land on -0.0 or ~-1e-15, and "%.2f" prints the sign even
        // when the digits are all zero. 0.005 is exactly the point below which "%.2f" would round
        // to zero anyway, so no amount that has anything to display is affected.
        if (Math.abs(value) < 0.005) {
            value = 0.0;
        }
        // Locale.ENGLISH pinned for the same reason as TIMESTAMP_FORMAT above: without it, a machine
        // whose default locale uses a comma decimal separator would silently render "$12,50".
        return String.format(java.util.Locale.ENGLISH, "$%.2f", value);
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
