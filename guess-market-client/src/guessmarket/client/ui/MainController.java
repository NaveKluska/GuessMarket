package guessmarket.client.ui;

import guessmarket.client.net.ApiClient;
import guessmarket.client.net.ApiException;
import guessmarket.client.net.CommissionType;
import guessmarket.client.net.OrderSide;
import guessmarket.client.net.Session;
import guessmarket.dto.AccountEntryDTO;
import guessmarket.dto.ChatMessageDTO;
import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.OptionDTO;
import guessmarket.dto.TransactionDTO;
import guessmarket.dto.UserSummaryDTO;
import guessmarket.dto.lmsr.LmsrEventDetailsDTO;
import guessmarket.dto.orderbook.OptionBookDTO;
import guessmarket.dto.orderbook.OrderBookEventDetailsDTO;
import guessmarket.dto.orderbook.OrderDTO;
import guessmarket.dto.orderbook.ParticipantHoldingDTO;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
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
import javafx.scene.control.ScrollPane;
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
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MainController
{
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    // Full date+time for a chat message's accessible text (screen readers still need the date even
    // though the visible row only shows the time, since the date divider above it is a separate
    // element they might not associate with this particular message).
    private static final DateTimeFormatter CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    // The date-divider row's own format - day.month.year, dot-separated, as requested.
    private static final DateTimeFormatter CHAT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    // Ex2's trade-history timestamp format, ported as-is. Locale.ENGLISH is mandatory, not cosmetic:
    // without it, month names silently follow the JVM's default locale.
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss", java.util.Locale.ENGLISH);
    private static final String NOT_YOURSELF_MESSAGE = "You can only manage your own account.";

    @FXML
    private Label welcomeLabel;

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
    private Button loadFileButton;

    @FXML
    private Label filePathLabel;

    @FXML
    private ListView<UserSummaryDTO> userListView;

    @FXML
    private VBox userAccountPane;

    @FXML
    private VBox userDetailPane;

    @FXML
    // Object, not ChatMessageDTO: holds a mix of real messages and plain LocalDate date-divider
    // rows (see withDateDividers/ChatCell).
    private ListView<Object> chatListView;

    @FXML
    private TextField chatMessageField;

    @FXML
    private Button sendChatButton;

    private final ChangeListener<EventSummaryDTO> eventSelectionListener =
        (observable, oldSelection, newSelection) -> showEventDetails(newSelection);

    private final ChangeListener<UserSummaryDTO> userSelectionListener =
        (observable, oldSelection, newSelection) -> showUserDetails(newSelection);

    /** The unfiltered list from the server - what the filter combos above the list are applied to. */
    private List<EventSummaryDTO> allEvents = new ArrayList<>();

    /** Which of your own events is currently expanded inline in the Users tab, if any. */
    private String selectedInlineEventName;
    // -1 so the very first (possibly empty) chat fetch is never mistaken for "nothing changed".
    private int lastChatMessageCount = -1;

    // ---------------------------------------------------------------- live detail views
    // eventDetailPane (Events tab) and the Account tab's inline "Single event details and trade"
    // box are each backed by one persistent LmsrView/OrderBookView (see those classes below) that
    // is built once and patched in place afterward - so these two just track which event/box that
    // live view currently belongs to, to know when a genuinely different one needs a fresh view
    // instead of a patch. Reset to null wherever the pane goes back to its placeholder.

    private String currentEventsDetailEventName;
    /** An LmsrView or an OrderBookView - the two don't share a supertype (their update() methods
     * take different DTO types), so callers branch on instanceof rather than a common interface. */
    private Object currentEventsDetailView;

    private VBox currentInlineBox;
    private String currentInlineEventName;
    private Object currentInlineView;

    /** userAccountPane's live view (Account tab, left column) - built once, patched in place, same
     * as the event detail views above. */
    private AccountPaneView accountPaneView;

    /** userDetailPane (Account tab, right column) - still rebuilt wholesale, but only when the
     * things it actually renders change (see renderUserDetail): which user is shown, and the
     * participation-relevant fields of the event list. A pure balance change no longer touches it. */
    private UserSummaryDTO lastRenderedUserDetailUser;
    /** buildUserDetail also iterates allEvents (participation cards) - allEvents itself is a fresh
     * List instance every tick regardless of content, so this needs eventSummaryEquals, not
     * reference equality. */
    private List<EventSummaryDTO> lastRenderedUserDetailEvents = new ArrayList<>();

    @FXML
    private void initialize()
    {
        welcomeLabel.setText("Logged in as " + Session.getUserName());

        // The combo's ITEMS stay the same raw values filterEvents() already compares against -
        // only how each one is displayed changes, via the cell factories below. That keeps the
        // filtering logic untouched and avoids ever showing a raw wire value like "NOT_ACTIVE" or
        // "ORDER_BOOK" in the UI (the underscore in those reads like a stray underline at a glance).
        methodFilterCombo.getItems().addAll("All", "LMSR", "ORDER_BOOK");
        methodFilterCombo.getSelectionModel().selectFirst();
        applyFilterLabels(methodFilterCombo, value -> "All".equals(value) ? "All Methods" : readableType(value));
        methodFilterCombo.setOnAction(event -> refreshEventListDisplay());

        statusFilterCombo.getItems().addAll("All", "NOT_ACTIVE", "ACTIVE", "CLOSED");
        statusFilterCombo.getSelectionModel().selectFirst();
        applyFilterLabels(statusFilterCombo, value -> "All".equals(value) ? "All Statuses" : readableStatus(value));
        statusFilterCombo.setOnAction(event -> refreshEventListDisplay());

        commissionFilterCombo.getItems().addAll("All", "ON_PURCHASE", "ON_CLOSE");
        commissionFilterCombo.getSelectionModel().selectFirst();
        applyFilterLabels(commissionFilterCombo, value -> "All".equals(value) ? "All Commission Types" : readableCommission(value));
        commissionFilterCombo.setOnAction(event -> refreshEventListDisplay());

        eventListView.setCellFactory(list -> new EventCell());
        eventListView.getSelectionModel().selectedItemProperty().addListener(eventSelectionListener);
        refreshEvents();

        loadFileButton.setOnAction(event -> handleChooseFile());

        userListView.setCellFactory(list -> new UserCell());
        userListView.getSelectionModel().selectedItemProperty().addListener(userSelectionListener);
        refreshUsers();

        chatListView.setCellFactory(list -> new ChatCell());
        // Chat is a passive log, not a pickable list like Events/Users - nothing reads its
        // selection, so the only effect of leaving it selectable was a row lighting up on click
        // for no reason. A capturing-phase filter on the ListView itself intercepts the press
        // before the cell's own default selection handler sees it, so clicking never selects
        // anything while scrolling (which isn't a MouseEvent at all) is untouched.
        chatListView.addEventFilter(MouseEvent.MOUSE_PRESSED, Event::consume);
        sendChatButton.setOnAction(event -> handleSendChat());
        chatMessageField.setOnAction(event -> handleSendChat());
        refreshChat();

        startPolling();
    }

    // ---------------------------------------------------------------- polling

    /**
     * Every open screen is a snapshot from the last request, not a live view - another user's
     * trade, deposit, or chat message only shows up here once we ask again. This is what makes
     * that asking happen on its own instead of only on a manual action.
     */
    private void startPolling()
    {
        final Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            refreshEvents(true);
            refreshUsers(true);
            refreshChat(true);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // ---------------------------------------------------------------- events: list + selection

    private void refreshEvents()
    {
        refreshEvents(false);
    }

    // quiet=true is for the polling loop: a poll running unattended in the background should never
    // interrupt the user with a dialog over a transient network blip - it just tries again next
    // second. Every explicit action still refreshes loudly, since there a failure is worth showing.
    private void refreshEvents(final boolean quiet)
    {
        if (quiet) {
            runAsyncQuiet(ApiClient::getAllEvents, events -> applyEvents(events, true));
        } else {
            runAsync(ApiClient::getAllEvents, events -> applyEvents(events, false));
        }
    }

    private void applyEvents(final List<EventSummaryDTO> events, final boolean quiet)
    {
        allEvents = events;
        refreshEventListDisplay(quiet);
    }

    private void refreshEventListDisplay()
    {
        refreshEventListDisplay(false);
    }

    /**
     * A poll tick fetches brand-new DTO instances every time, even when nothing actually changed -
     * so naively replacing the list's items every second would redraw every row and flicker
     * whichever one is selected, once a second, forever. Only touching the ListView when the
     * content actually differs (by value, not by object identity) is what avoids that; on a tick
     * where nothing changed this does nothing at all. When it does need to change, the old
     * selection's object reference is never one of the new instances, so re-selecting by name
     * still means selection briefly drops to nothing in between - left alone, that transient null
     * would reach the selection listener and wipe the detail pane down to its placeholder before
     * the real reselect happens a moment later. Detaching the listener for that one mutation skips
     * the pointless wipe-then-rebuild, and showEventDetails is then called once, directly, for
     * whatever ends up selected - every tick, regardless of whether the list itself changed, since
     * a trade can change an event's detail without changing anything in its summary row. The quiet
     * flag carries through to showEventDetails, which is what actually skips the detail rebuild
     * while you're mid-interaction with it - see the comment there.
     */
    private void refreshEventListDisplay(final boolean quiet)
    {
        final List<EventSummaryDTO> filtered = filterEvents(allEvents);

        if (!listsEqual(eventListView.getItems(), filtered, this::eventSummaryEquals)) {
            final EventSummaryDTO currentlySelected = eventListView.getSelectionModel().getSelectedItem();
            final String selectedName = currentlySelected == null ? null : currentlySelected.getName();

            eventListView.getSelectionModel().selectedItemProperty().removeListener(eventSelectionListener);
            eventListView.getItems().setAll(filtered);
            if (selectedName != null) {
                for (final EventSummaryDTO event : filtered) {
                    if (event.getName().equals(selectedName)) {
                        eventListView.getSelectionModel().select(event);
                        break;
                    }
                }
            }
            eventListView.getSelectionModel().selectedItemProperty().addListener(eventSelectionListener);
        }

        showEventDetails(eventListView.getSelectionModel().getSelectedItem(), quiet);
    }

    private boolean eventSummaryEquals(final EventSummaryDTO x, final EventSummaryDTO y)
    {
        return x.getName().equals(y.getName())
            && x.getDescription().equals(y.getDescription())
            && x.getCommission() == y.getCommission()
            && x.getCommissionType().equals(y.getCommissionType())
            && x.getOptions().equals(y.getOptions())
            && x.getStatus().equals(y.getStatus())
            && x.getType().equals(y.getType())
            && x.getMarketMakerName().equals(y.getMarketMakerName())
            && Double.compare(x.getAccountBalance(), y.getAccountBalance()) == 0;
    }

    // Narrower than eventSummaryEquals on purpose: buildUserDetail's participation cards (see
    // participationCard) only ever show an event's name/type/status/MM-tag, never its
    // accountBalance/commission/description/options - and accountBalance in particular changes on
    // every single trade against that event, which would otherwise force renderUserDetail's gate to
    // treat any trade by anyone, anywhere, as a reason to rebuild this pane.
    private boolean participationRelevantEquals(final EventSummaryDTO x, final EventSummaryDTO y)
    {
        return x.getName().equals(y.getName())
            && x.getType().equals(y.getType())
            && x.getStatus().equals(y.getStatus())
            && x.getMarketMakerName().equals(y.getMarketMakerName());
    }

    private List<EventSummaryDTO> filterEvents(final List<EventSummaryDTO> events)
    {
        final String method = methodFilterCombo.getValue();
        final String status = statusFilterCombo.getValue();
        final String commission = commissionFilterCombo.getValue();

        final List<EventSummaryDTO> filtered = new ArrayList<>();
        for (final EventSummaryDTO event : events) {
            if (!"All".equals(method) && !event.getType().equals(method)) {
                continue;
            }
            if (!"All".equals(status) && !event.getStatus().equals(status)) {
                continue;
            }
            if (!"All".equals(commission) && !event.getCommissionType().equals(commission)) {
                continue;
            }
            filtered.add(event);
        }
        return filtered;
    }

    private void showEventDetails(final EventSummaryDTO selected)
    {
        showEventDetails(selected, false);
    }

    /**
     * quiet=true is the background poll calling in. renderEventDetail patches values in place for
     * an unchanged event, so a focused control is only ever at risk during a genuine structural
     * rebuild (the event's status or winning option actually changed) - rare enough, and real
     * enough when it happens, that skipping the whole refresh while mid-interaction is still the
     * safer default. An explicit action (Buy, Open, a filter change) always refreshes for real.
     */
    private void showEventDetails(final EventSummaryDTO selected, final boolean quiet)
    {
        if (selected == null) {
            eventDetailPane.getChildren().setAll(new Label("Select an event to see its details."));
            currentEventsDetailEventName = null;
            currentEventsDetailView = null;
            return;
        }
        if (quiet && paneHasFocus(eventDetailPane)) {
            return;
        }
        runAsync(() -> ApiClient.getEventDetails(selected.getName()), this::renderEventDetail);
    }

    private class EventCell extends ListCell<EventSummaryDTO>
    {
        @Override
        protected void updateItem(final EventSummaryDTO event, final boolean empty)
        {
            super.updateItem(event, empty);
            if (empty || event == null) {
                setGraphic(null);
                return;
            }

            final Label name = new Label(event.getName());
            name.getStyleClass().add("event-cell-name");

            final HBox typeStatus = new HBox(6,
                pill(readableType(event.getType()), typePillClass(event.getType())),
                pill(readableStatus(event.getStatus()), statusPillClass(event.getStatus())));

            final FlowPane metaPills = new FlowPane(6, 4,
                pill(readableCommission(event.getCommissionType()) + " · " + event.getCommission() + "%", "pill-commission"),
                pill("Account: " + money(event.getAccountBalance()), "pill-account"),
                pill("MM: " + event.getMarketMakerName(), "pill-mm"));

            final VBox box = new VBox(4, name, typeStatus, metaPills);
            box.setPadding(new Insets(4, 2, 4, 2));
            // Forces wrapping instead of a horizontal scrollbar clipping the pills when the
            // SplitPane divider leaves less room than the box's natural width.
            box.maxWidthProperty().bind(getListView().widthProperty().subtract(24));
            // A custom graphic has no text of its own for a screen reader (or UI Automation) to
            // read out, unlike the plain setText() this replaced - spelling it out here keeps the
            // row genuinely accessible instead of just visually informative.
            box.setAccessibleText(event.getName() + ", " + readableType(event.getType()) + ", " + readableStatus(event.getStatus()));
            setGraphic(box);
        }
    }

    // ---------------------------------------------------------------- events: detail rendering

    /**
     * Read-only, exactly like Ex2's Events tab - trading happens on the Account tab instead. null
     * viewedUserName (passed down to the view below) is what makes it read-only. A genuinely
     * different event gets a fresh LmsrView/OrderBookView (see those classes, further down); the
     * same event just gets patched in place - no rebuild, so no flicker, no lost scroll position.
     */
    private void renderEventDetail(final EventDetailsDTO details)
    {
        final Runnable onChange = () -> reloadAndShowEvent(details.getName());
        if (!details.getName().equals(currentEventsDetailEventName)) {
            currentEventsDetailEventName = details.getName();
            if (details instanceof LmsrEventDetailsDTO) {
                final LmsrView view = new LmsrView(null);
                view.update((LmsrEventDetailsDTO) details, onChange);
                currentEventsDetailView = view;
                eventDetailPane.getChildren().setAll(view.getRoot());
            } else {
                final OrderBookView view = new OrderBookView(null);
                view.update((OrderBookEventDetailsDTO) details, onChange);
                currentEventsDetailView = view;
                eventDetailPane.getChildren().setAll(view.getRoot());
            }
            return;
        }
        if (currentEventsDetailView instanceof LmsrView) {
            ((LmsrView) currentEventsDetailView).update((LmsrEventDetailsDTO) details, onChange);
        } else {
            ((OrderBookView) currentEventsDetailView).update((OrderBookEventDetailsDTO) details, onChange);
        }
    }

    // A trade/open/close can change both this event's own detail and any user's balance or
    // relevant-events list - refreshing only one side would leave the Users tab showing stale
    // data until the next poll tick, which is a needless few seconds of wrong information.
    private void reloadAndShowEvent(final String eventName)
    {
        refreshEvents();
        refreshUsers();
        runAsync(() -> ApiClient.getEventDetails(eventName), this::renderEventDetail);
    }

    /**
     * Ex2's own event-detail view, ported as-is (header, price cards / order books, trade history).
     * viewedUserName is Ex2's own viewingUserName: null on the Events tab (read-only overview, no
     * trade controls, no open/close, ever); on the Account tab it's whoever's page is showing this,
     * so the price cards become clickable (interactiveLmsrCards) and, if that page belongs to this
     * event's Market Maker, the open/close controls (actionBar) appear - grayed out with a tooltip
     * if the logged-in user isn't actually that MM, same as Deposit/Create Event elsewhere.
     */
    private List<Node> buildLmsrDetail(final LmsrEventDetailsDTO dto, final Runnable onChange, final String viewedUserName)
    {
        final boolean interactive = viewedUserName != null;
        final List<Node> nodes = new ArrayList<>();
        // Viewed interactively (the Account tab), this figure should be your own commission, not
        // the event-wide total - otherwise it reads as "your commission" but silently shows
        // everyone's. The read-only Events tab has no single viewer, so it keeps the event-wide
        // total there.
        final String commissionLabel = interactive ? "Your commission" : "Commission collected";
        final double commissionValue = interactive ? dto.getCommissionPaidBy(Session.getUserName()) : dto.getTotalCommissionCollected();
        nodes.add(detailHeader(dto.getName(), dto.getDescription(), dto.getStatus(), dto.getMarketMakerName(),
            readableCommission(dto.getCommissionType()) + " · " + dto.getCommission() + "%",
            "Account balance", money(dto.getAccountBalance()),
            commissionLabel, money(commissionValue)));

        if (dto.getWinningOptionName() != null) {
            nodes.add(winnerBanner(dto.getWinningOptionName()));
        }

        final List<String> optionNames = new ArrayList<>();
        for (final OptionDTO option : dto.getOptions()) {
            optionNames.add(option.getName());
        }

        // Shown whenever the page we're on belongs to this event's MM - grayed out below (inside
        // actionBar) if the logged-in user isn't actually them.
        final boolean mmMatchesViewedPage = interactive && viewedUserName.equals(dto.getMarketMakerName());
        if (mmMatchesViewedPage && !"CLOSED".equals(dto.getStatus())) {
            final boolean youAreMm = Session.getUserName().equals(dto.getMarketMakerName());
            nodes.add(actionBar(dto.getName(), dto.getStatus(), optionNames, dto.getMarketMakerName(), youAreMm, onChange));
        }

        if (interactive && "ACTIVE".equals(dto.getStatus())) {
            final boolean isSelf = Session.getUserName().equals(viewedUserName);
            nodes.add(interactiveLmsrCards(dto.getName(), dto.getOptions(), onChange, isSelf));
        } else {
            final FlowPane priceCards = new FlowPane(12, 10);
            priceCards.setAlignment(Pos.CENTER);
            for (final OptionDTO option : dto.getOptions()) {
                priceCards.getChildren().add(priceCard(option, option.getName().equals(dto.getWinningOptionName())));
            }
            priceCards.setPadding(new Insets(0, 0, 10, 0));
            nodes.add(priceCards);
        }

        final List<TransactionDTO> history = filteredLmsrHistory(dto, viewedUserName);

        final TableView<TransactionDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        if (!interactive) {
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
        // Account tab it's already filtered down to just the viewer, so the title says so.
        nodes.add(centerLabel(sectionLabel(interactive ? "Your trade history" : "Trade history")));
        nodes.add(table);

        return nodes;
    }

    /** Most recent first - matching Ex2 exactly (getTransactions() is oldest-first, so newest ends
     * up on top here, same as everywhere else trade history is shown). Interactive (Account tab)
     * filters down to just the viewer's own trades; the read-only Events tab keeps everyone's. */
    private List<TransactionDTO> filteredLmsrHistory(final LmsrEventDetailsDTO dto, final String viewedUserName)
    {
        final List<TransactionDTO> history = new ArrayList<>(dto.getTransactions());
        Collections.reverse(history);
        if (viewedUserName != null) {
            history.removeIf(tx -> !tx.getUserName().equals(Session.getUserName()));
        }
        return history;
    }

    /** The two LMSR option cards, made clickable: pick a card to trade it, then a shared quantity
     * spinner and Buy button below act on whichever card is currently selected. Nothing is
     * pre-selected - the spinner and button stay disabled until the user picks an option. When
     * isSelf is false (viewing someone else's page), the whole controls row is grayed out with a
     * tooltip instead - the card-click selection still visually works, it just never enables
     * anything, since a disabled parent keeps its children effectively disabled regardless of
     * their own state.
     * <p>
     * This VBox is now only ever built once per structural epoch (see LmsrView) - a click here
     * survives every poll tick on its own simply because nothing rebuilds it out from under the
     * user, so there is no need to separately remember and restore which card was selected. */
    private VBox interactiveLmsrCards(final String eventId, final List<OptionDTO> options, final Runnable onChange, final boolean isSelf)
    {
        final List<VBox> cardNodes = new ArrayList<>();
        final int[] selectedIndex = { -1 };

        final Spinner<Integer> qtySpinner = new Spinner<>(1, 1_000_000, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(100);
        qtySpinner.setDisable(true);

        final Button buyBtn = new Button("Buy Shares");
        buyBtn.getStyleClass().add("primary-button");
        buyBtn.setDisable(true);

        final Label pickHint = new Label("Click an option below to trade it ↓");
        pickHint.getStyleClass().add("hint-chip");
        final HBox pickHintWrap = new HBox(pickHint);
        pickHintWrap.setAlignment(Pos.CENTER);

        final FlowPane cardsRow = new FlowPane(12, 10);
        cardsRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < options.size(); i++) {
            final VBox card = priceCard(options.get(i), false);
            card.setCursor(Cursor.HAND);
            final int idx = i;
            card.setOnMouseClicked(e -> {
                selectedIndex[0] = idx;
                for (int j = 0; j < cardNodes.size(); j++) {
                    final VBox c = cardNodes.get(j);
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
            final int quantity = qtySpinner.getValue();
            runAsync(() -> ApiClient.buyShares(Session.getUserName(), eventId, selectedIndex[0], quantity), receipt -> onChange.run());
        });

        final Label qtyCaption = new Label("QUANTITY");
        qtyCaption.getStyleClass().add("meta-label");
        final VBox controls = new VBox(8, centerLabel(qtyCaption), qtySpinner, buyBtn);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(4, 0, 10, 0));

        final VBox box = new VBox(10, pickHintWrap, cardsRow, withDisabledTooltip(controls, isSelf, NOT_YOURSELF_MESSAGE));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private List<Node> buildOrderBookDetail(final OrderBookEventDetailsDTO dto, final Runnable onChange, final String viewedUserName)
    {
        final boolean interactive = viewedUserName != null;
        final List<Node> nodes = new ArrayList<>();
        nodes.add(detailHeader(dto.getName(), dto.getDescription(), dto.getStatus(), dto.getMarketMakerName(),
            readableCommission(dto.getCommissionType()) + " · " + dto.getCommission() + "%",
            "Base value (d)", money(dto.getBaseValue()),
            "Account balance", money(dto.getAccountBalance())));

        if (dto.getWinningOptionName() != null) {
            nodes.add(winnerBanner(dto.getWinningOptionName()));
        }

        final List<String> optionNames = new ArrayList<>();
        for (final OptionBookDTO book : dto.getOptionBooks()) {
            optionNames.add(book.getOptionName());
        }

        final boolean mmMatchesViewedPage = interactive && viewedUserName.equals(dto.getMarketMakerName());
        if (mmMatchesViewedPage && !"CLOSED".equals(dto.getStatus())) {
            final boolean youAreMm = Session.getUserName().equals(dto.getMarketMakerName());
            nodes.add(actionBar(dto.getName(), dto.getStatus(), optionNames, dto.getMarketMakerName(), youAreMm, onChange));
        }

        final boolean active = interactive && "ACTIVE".equals(dto.getStatus());
        final boolean isSelf = interactive && Session.getUserName().equals(viewedUserName);
        final FlowPane books = new FlowPane(14, 14);
        books.setAlignment(Pos.CENTER);
        for (int i = 0; i < dto.getOptionBooks().size(); i++) {
            final boolean isWinner = dto.getOptionBooks().get(i).getOptionName().equals(dto.getWinningOptionName());
            books.getChildren().add(bookPanel(dto.getOptionBooks().get(i), dto.getName(), i, active, isWinner, isSelf, dto.getBaseValue(), onChange));
        }
        nodes.add(books);

        if (interactive) {
            // Account tab: this user's own position only - never expose other participants'
            // holdings here. Ported from Ex2's buildOrderBookDetail, which branched the exact same
            // way on viewingUserName - the port here had dropped that branch and always showed the
            // full participants table regardless of which tab this was.
            final VBox positionCard = new VBox(10);
            positionCard.getStyleClass().add("book-panel");
            positionCard.setPadding(new Insets(14));
            positionCard.getChildren().add(centerLabel(sectionLabel("Your position")));

            ParticipantHoldingDTO mine = null;
            for (final ParticipantHoldingDTO p : dto.getParticipants()) {
                if (p.getUserName().equals(viewedUserName)) {
                    mine = p;
                    break;
                }
            }
            if (mine == null) {
                positionCard.getChildren().add(centerLabel(placeholder("No holdings by this user yet.")));
            } else {
                final FlowPane optionCards = new FlowPane(12, 10);
                optionCards.setAlignment(Pos.CENTER);
                for (int i = 0; i < dto.getOptionBooks().size(); i++) {
                    optionCards.getChildren().add(optionPositionCard(dto.getOptionBooks().get(i).getOptionName(), mine.getHoldingsByOption().get(i), mine.getPaidByOption().get(i)));
                }
                positionCard.getChildren().add(optionCards);

                final List<Node> summaryTiles = new ArrayList<>();
                // Same sleek, non-obvious phrasing as the LMSR detail's per-viewer commission tile -
                // this box only ever renders for the one user looking at their own position.
                summaryTiles.add(statTile("Your commission", money(mine.getCommissionPaid()), "meta-value"));
                if ("CLOSED".equals(dto.getStatus()) && mine.getProfitOrLoss() != null) {
                    final double pl = mine.getProfitOrLoss();
                    final boolean profit = pl >= 0;
                    summaryTiles.add(statTile(profit ? "Profit" : "Loss", money(Math.abs(pl)), profit ? "pl-positive" : "pl-negative"));
                }
                final FlowPane summaryStrip = new FlowPane(10, 10, summaryTiles.toArray(new Node[0]));
                summaryStrip.setAlignment(Pos.CENTER);
                positionCard.getChildren().add(summaryStrip);
            }
            nodes.add(positionCard);
        } else {
            // Events tab: a read-only overview of every participant's position - appropriate here,
            // since this view is about the whole market, not any one person.
            final TableView<ParticipantHoldingDTO> participantsTable = new TableView<>();
            participantsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            final TableColumn<ParticipantHoldingDTO, String> userCol = new TableColumn<>("Participant");
            userCol.setCellValueFactory(new PropertyValueFactory<>("userName"));
            userCol.setPrefWidth(140);
            participantsTable.getColumns().add(userCol);
            for (int i = 0; i < dto.getOptionBooks().size(); i++) {
                final int optionIndex = i;
                final TableColumn<ParticipantHoldingDTO, String> holdingCol = new TableColumn<>(dto.getOptionBooks().get(i).getOptionName() + " held");
                holdingCol.setCellValueFactory(row -> new SimpleStringProperty(String.valueOf(row.getValue().getHoldingsByOption().get(optionIndex))));
                holdingCol.setPrefWidth(110);
                participantsTable.getColumns().add(holdingCol);
            }
            final TableColumn<ParticipantHoldingDTO, String> valueCol = new TableColumn<>("Est. value");
            valueCol.setCellValueFactory(row -> new SimpleStringProperty(money(row.getValue().getEstimatedValue())));
            valueCol.setPrefWidth(100);
            participantsTable.getColumns().add(valueCol);
            participantsTable.getItems().addAll(dto.getParticipants());
            participantsTable.setPlaceholder(new Label("No participants yet."));

            nodes.add(centerLabel(sectionLabel("Participations")));
            nodes.add(participantsTable);
        }

        return nodes;
    }

    private VBox bookPanel(final OptionBookDTO book, final String eventId, final int optionIndex, final boolean active, final boolean isWinner, final boolean isSelf, final double baseValue, final Runnable onChange)
    {
        final Label headerLabel = new Label(book.getOptionName());
        headerLabel.getStyleClass().add("book-panel-title");
        final HBox header = isWinner ? new HBox(6, headerLabel, pill("WINNER", "pill-winner")) : new HBox(headerLabel);
        header.setAlignment(Pos.CENTER);

        final HBox stats = new HBox();
        stats.getStyleClass().add("stats-row");
        for (final VBox box : List.of(statBox("Last", book.getLast()), statBox("Bid", book.getBid()), statBox("Ask", book.getAsk()),
                                       statBox("Mid", book.getMid()), statBox("Spread", book.getSpread()))) {
            HBox.setHgrow(box, Priority.ALWAYS);
            box.setMaxWidth(Double.MAX_VALUE);
            stats.getChildren().add(box);
        }

        final TableView<OrderDTO> bids = orderTable(book.getBids());
        final TableView<OrderDTO> asks = orderTable(book.getAsks());

        final VBox panel = new VBox(6, header, stats, centerLabel(sectionLabel("Bids")), bids, centerLabel(sectionLabel("Asks")), asks);
        if (active) {
            panel.getChildren().add(withDisabledTooltip(obTradeForm(eventId, optionIndex, baseValue, onChange), isSelf, NOT_YOURSELF_MESSAGE));
        }
        panel.getStyleClass().add(isWinner ? "book-panel-winner" : "book-panel");
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(340);
        panel.setMinWidth(300);
        return panel;
    }

    private TableView<OrderDTO> orderTable(final List<OrderDTO> orders)
    {
        final TableView<OrderDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("User", "userName", 90));
        table.getColumns().add(column("Qty", "quantity", 60));
        table.getColumns().add(moneyColumn("Price", "price", 80));
        table.getItems().addAll(orders);
        table.setPlaceholder(new Label("—"));
        table.setPrefHeight(120);
        return table;
    }

    // ---------------------------------------------------------------- live detail views
    //
    // The actual flicker/selection-loss fix. A poll tick fetches a brand-new DTO tree every
    // second even when nothing on the server changed, so naively rebuilding a detail pane from
    // that DTO every time is what caused every symptom this project chased separately: the
    // selected Buy card resetting, the ScrollPane jumping to the top, a stale equality check
    // occasionally skipping a render that was actually needed. None of that is a series of
    // separate bugs to patch - it's one wrong shape (rebuild-or-skip) for the whole render path.
    //
    // Real fix: build the Node tree exactly once per "structural epoch" (still via the existing
    // buildLmsrDetail/buildOrderBookDetail - nothing about how this looks changes at all), keep
    // live references to the handful of Nodes that can change value on their own, and on every
    // later tick push new values into those same Nodes directly. Nothing is ever torn down unless
    // the event's status or winning option actually changes - a real, rare content transition,
    // not something to optimize around - in which case a full rebuild of that epoch is correct
    // and expected (a fresh coat of controls for a genuinely new phase of the event), same as it
    // always was. Because nothing else is ever destroyed, a selected card, a typed quantity, and
    // a scroll position all simply survive on their own - there is nothing left patching them.

    /**
     * Walks a just-built Node tree looking for a FlowPane whose direct children are all
     * "one option's price card" (priceCard's own shape: a VBox with >= 4 children, the second of
     * which is a Label styled price-card-value) - true for both the read-only priceCards FlowPane
     * and interactiveLmsrCards' cardsRow, without needing to know which of the two produced it.
     * Collects into cardsOut and stops at the first match, since a detail view only ever has one
     * such row.
     */
    private void collectPriceCards(final Node node, final List<VBox> cardsOut)
    {
        if (node instanceof FlowPane) {
            final List<Node> children = ((FlowPane) node).getChildren();
            boolean allPriceCards = !children.isEmpty();
            for (final Node child : children) {
                if (!(child instanceof VBox) || ((VBox) child).getChildren().size() < 4
                    || !(((VBox) child).getChildren().get(1) instanceof Label)
                    || !((Label) ((VBox) child).getChildren().get(1)).getStyleClass().contains("price-card-value")) {
                    allPriceCards = false;
                    break;
                }
            }
            if (allPriceCards) {
                for (final Node child : children) {
                    cardsOut.add((VBox) child);
                }
                return;
            }
        }
        if (node instanceof Parent) {
            for (final Node child : ((Parent) node).getChildrenUnmodifiable()) {
                collectPriceCards(child, cardsOut);
                if (!cardsOut.isEmpty()) {
                    return;
                }
            }
        }
    }

    /** Scoped exception to "never rebuild a table": OrderDTO carries no stable per-row id (just
     * userName/quantity/price), so a resting order that partially fills is indistinguishable from
     * one that was cancelled and a new one rested at the same price - there is no way to diff this
     * table row-by-row without guessing. Comparing the whole list by value and replacing it only
     * when it actually changed is still far better than the old approach (which replaced it every
     * tick regardless) - this table has no per-row selection that matters, so the only real cost
     * is losing its own scroll position on a tick where it genuinely changed, not every tick. */
    private void patchOrderTable(final TableView<OrderDTO> table, final List<OrderDTO> newOrders)
    {
        if (!listsEqual(table.getItems(), newOrders, this::orderEqual)) {
            table.getItems().setAll(newOrders);
        }
    }

    /** Unlike the order tables above, a participant has a stable identity (userName), so this
     * diffs properly: an existing participant whose holdings changed gets their one row replaced
     * (table.getItems().set - refreshes just that row's cells, everyone else's row, the table's
     * scroll position and its selection are all left completely alone); a brand new participant
     * gets appended. Participants never stop being one once they've traded, so there is nothing to
     * remove. */
    private void patchParticipants(final TableView<ParticipantHoldingDTO> table, final List<ParticipantHoldingDTO> newParticipants)
    {
        final ObservableList<ParticipantHoldingDTO> items = table.getItems();
        final Map<String, Integer> indexByUser = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            indexByUser.put(items.get(i).getUserName(), i);
        }
        for (final ParticipantHoldingDTO p : newParticipants) {
            final Integer idx = indexByUser.get(p.getUserName());
            if (idx == null) {
                items.add(p);
            } else if (!participantEqual(items.get(idx), p)) {
                items.set(idx, p);
            }
        }
    }

    /** One LMSR event's detail view: built once via buildLmsrDetail, then patched in place. See the
     * section comment above for why. viewedUserName is threaded straight through to buildLmsrDetail
     * exactly as before (null = read-only Events tab; a real name = the Account tab, interactive). */
    private final class LmsrView
    {
        private final VBox root = new VBox();
        private final String viewedUserName;
        private String structuralKey;
        private Label balanceValueLabel;
        private Label commissionValueLabel;
        private List<Label> priceLabels;
        private List<Label> chanceLabels;
        private List<Label> sharesLabels;
        private TableView<TransactionDTO> historyTable;
        private int lastHistoryCount = -1;

        LmsrView(final String viewedUserName)
        {
            this.viewedUserName = viewedUserName;
        }

        Node getRoot()
        {
            return root;
        }

        void update(final LmsrEventDetailsDTO dto, final Runnable onChange)
        {
            // Status/winner are the only things that change which controls exist at all (the
            // action bar, the winner banner, interactive-vs-plain price cards) - everything else
            // that can change (balance, commission, prices, shares, new trades) is a value update
            // to a Node that's already there, handled by patchValues below.
            final String key = dto.getStatus() + "|" + dto.getWinningOptionName();
            if (!key.equals(structuralKey)) {
                rebuild(dto, onChange);
                structuralKey = key;
            } else {
                patchValues(dto);
            }
        }

        private void rebuild(final LmsrEventDetailsDTO dto, final Runnable onChange)
        {
            final List<Node> nodes = buildLmsrDetail(dto, onChange, viewedUserName);
            root.getChildren().setAll(nodes);
            harvest(nodes);
            lastHistoryCount = filteredLmsrHistory(dto, viewedUserName).size();
        }

        /** detailHeader's own fixed shape - see that method - is what makes the header lookup safe
         * to hardcode by index; the price cards' shape varies (interactive vs plain), so those go
         * through the generic collectPriceCards search instead. */
        @SuppressWarnings("unchecked")
        private void harvest(final List<Node> nodes)
        {
            final VBox header = (VBox) nodes.get(0);
            final FlowPane meta = (FlowPane) header.getChildren().get(2);
            balanceValueLabel = (Label) ((VBox) meta.getChildren().get(3)).getChildren().get(1);
            commissionValueLabel = (Label) ((VBox) meta.getChildren().get(4)).getChildren().get(1);

            final List<VBox> cards = new ArrayList<>();
            for (final Node n : nodes) {
                collectPriceCards(n, cards);
                if (!cards.isEmpty()) {
                    break;
                }
            }
            priceLabels = new ArrayList<>();
            chanceLabels = new ArrayList<>();
            sharesLabels = new ArrayList<>();
            for (final VBox card : cards) {
                priceLabels.add((Label) card.getChildren().get(1));
                chanceLabels.add((Label) card.getChildren().get(2));
                sharesLabels.add((Label) card.getChildren().get(3));
            }

            historyTable = null;
            for (final Node n : nodes) {
                if (n instanceof TableView) {
                    historyTable = (TableView<TransactionDTO>) n;
                    break;
                }
            }
        }

        private void patchValues(final LmsrEventDetailsDTO dto)
        {
            balanceValueLabel.setText(money(dto.getAccountBalance()));
            final double commissionValue = viewedUserName != null
                ? dto.getCommissionPaidBy(Session.getUserName())
                : dto.getTotalCommissionCollected();
            commissionValueLabel.setText(money(commissionValue));

            final List<OptionDTO> options = dto.getOptions();
            for (int i = 0; i < options.size() && i < priceLabels.size(); i++) {
                final OptionDTO option = options.get(i);
                priceLabels.get(i).setText(money(option.getCurrentProbability()));
                chanceLabels.get(i).setText(Math.round(option.getCurrentProbability() * 100) + "% implied chance");
                sharesLabels.get(i).setText(option.getSharesBought() + " shares bought");
            }

            final List<TransactionDTO> history = filteredLmsrHistory(dto, viewedUserName);
            if (history.size() != lastHistoryCount) {
                if (lastHistoryCount >= 0 && history.size() > lastHistoryCount) {
                    // Newest-first, and a transaction is never edited or removed once recorded -
                    // so a growing history always adds exactly this many brand new rows at the
                    // front, never touching the existing rows below them.
                    historyTable.getItems().addAll(0, history.subList(0, history.size() - lastHistoryCount));
                } else {
                    historyTable.getItems().setAll(history);
                }
                lastHistoryCount = history.size();
            }
        }
    }

    /** One Order Book event's detail view - same idea as LmsrView, see the section comment above. */
    private final class OrderBookView
    {
        private final VBox root = new VBox();
        private final String viewedUserName;
        private String structuralKey;
        private Label balanceValueLabel;
        private final List<TableView<OrderDTO>> bidsTables = new ArrayList<>();
        private final List<TableView<OrderDTO>> asksTables = new ArrayList<>();
        // Per book: [Last, Bid, Ask, Mid, Spread] value Labels, in that order - matches statBox's
        // own fixed call order in bookPanel.
        private final List<List<Label>> bookStatLabels = new ArrayList<>();
        /** Events tab only (viewedUserName == null). */
        private TableView<ParticipantHoldingDTO> participantsTable;
        /** Account tab only (viewedUserName != null) - null until the viewer has any holdings at
         * all, since "Your position" has no cards to harvest labels from before that. */
        private List<Label> positionSharesLabels;
        private List<Label> positionPaidLabels;

        OrderBookView(final String viewedUserName)
        {
            this.viewedUserName = viewedUserName;
        }

        Node getRoot()
        {
            return root;
        }

        void update(final OrderBookEventDetailsDTO dto, final Runnable onChange)
        {
            // Same idea as LmsrView's key, plus one Order-Book-only wrinkle: on the Account tab,
            // whether "Your position" has any cards to patch at all depends on whether the viewer
            // has traded yet - the very first trade needs a real rebuild too (to harvest labels
            // that didn't exist a tick ago), not just a value patch into labels that don't exist.
            final boolean hasHoldings = viewedUserName != null && hasHoldings(dto);
            final String key = dto.getStatus() + "|" + dto.getWinningOptionName() + "|" + hasHoldings;
            if (!key.equals(structuralKey)) {
                rebuild(dto, onChange);
                structuralKey = key;
            } else {
                patchValues(dto);
            }
        }

        private boolean hasHoldings(final OrderBookEventDetailsDTO dto)
        {
            for (final ParticipantHoldingDTO p : dto.getParticipants()) {
                if (p.getUserName().equals(viewedUserName)) {
                    return true;
                }
            }
            return false;
        }

        private void rebuild(final OrderBookEventDetailsDTO dto, final Runnable onChange)
        {
            final List<Node> nodes = buildOrderBookDetail(dto, onChange, viewedUserName);
            root.getChildren().setAll(nodes);
            harvest(nodes);
        }

        @SuppressWarnings("unchecked")
        private void harvest(final List<Node> nodes)
        {
            final VBox header = (VBox) nodes.get(0);
            final FlowPane meta = (FlowPane) header.getChildren().get(2);
            balanceValueLabel = (Label) ((VBox) meta.getChildren().get(4)).getChildren().get(1);

            bidsTables.clear();
            asksTables.clear();
            bookStatLabels.clear();
            for (final Node n : nodes) {
                if (n instanceof FlowPane) {
                    for (final Node child : ((FlowPane) n).getChildren()) {
                        if (child instanceof VBox && (((VBox) child).getStyleClass().contains("book-panel")
                            || ((VBox) child).getStyleClass().contains("book-panel-winner"))) {
                            harvestBookPanel((VBox) child);
                        }
                    }
                }
            }

            participantsTable = null;
            positionSharesLabels = null;
            positionPaidLabels = null;
            for (final Node n : nodes) {
                if (n instanceof TableView) {
                    participantsTable = (TableView<ParticipantHoldingDTO>) n;
                }
            }
            if (viewedUserName != null && !nodes.isEmpty()) {
                // "Your position" card: the last node buildOrderBookDetail adds when interactive.
                final Node last = nodes.get(nodes.size() - 1);
                if (last instanceof VBox) {
                    harvestPositionCard((VBox) last);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void harvestBookPanel(final VBox panel)
        {
            final HBox stats = (HBox) panel.getChildren().get(1);
            final List<Label> labels = new ArrayList<>();
            for (final Node statNode : stats.getChildren()) {
                labels.add((Label) ((VBox) statNode).getChildren().get(1));
            }
            bookStatLabels.add(labels);
            bidsTables.add((TableView<OrderDTO>) panel.getChildren().get(3));
            asksTables.add((TableView<OrderDTO>) panel.getChildren().get(5));
        }

        /** positionCard's children are [title] alone before any trade (nothing to harvest - see
         * hasHoldings above, which keeps this method from even being reached that early) or
         * [title, optionCards, summaryStrip] once the viewer holds something. */
        private void harvestPositionCard(final VBox positionCard)
        {
            if (positionCard.getChildren().size() < 3) {
                return;
            }
            final FlowPane optionCards = (FlowPane) positionCard.getChildren().get(1);
            positionSharesLabels = new ArrayList<>();
            positionPaidLabels = new ArrayList<>();
            for (final Node card : optionCards.getChildren()) {
                final List<Node> cardChildren = ((VBox) card).getChildren();
                positionSharesLabels.add((Label) cardChildren.get(1));
                positionPaidLabels.add((Label) cardChildren.get(2));
            }
        }

        private void patchValues(final OrderBookEventDetailsDTO dto)
        {
            balanceValueLabel.setText(money(dto.getAccountBalance()));

            final List<OptionBookDTO> books = dto.getOptionBooks();
            for (int i = 0; i < books.size(); i++) {
                final OptionBookDTO book = books.get(i);
                final List<Label> labels = bookStatLabels.get(i);
                labels.get(0).setText(book.getLast() == null ? "—" : money(book.getLast()));
                labels.get(1).setText(book.getBid() == null ? "—" : money(book.getBid()));
                labels.get(2).setText(book.getAsk() == null ? "—" : money(book.getAsk()));
                labels.get(3).setText(book.getMid() == null ? "—" : money(book.getMid()));
                labels.get(4).setText(book.getSpread() == null ? "—" : money(book.getSpread()));
                patchOrderTable(bidsTables.get(i), book.getBids());
                patchOrderTable(asksTables.get(i), book.getAsks());
            }

            if (viewedUserName != null) {
                if (positionSharesLabels != null) {
                    ParticipantHoldingDTO mine = null;
                    for (final ParticipantHoldingDTO p : dto.getParticipants()) {
                        if (p.getUserName().equals(viewedUserName)) {
                            mine = p;
                            break;
                        }
                    }
                    if (mine != null) {
                        for (int i = 0; i < books.size() && i < positionSharesLabels.size(); i++) {
                            positionSharesLabels.get(i).setText(mine.getHoldingsByOption().get(i) + " shares held");
                            positionPaidLabels.get(i).setText("Paid: " + money(mine.getPaidByOption().get(i)));
                        }
                    }
                }
            } else if (participantsTable != null) {
                patchParticipants(participantsTable, dto.getParticipants());
            }
        }
    }

    // ---------------------------------------------------------------- events: actions

    /** actionBar only renders when there is something to do - callers gate on Session's user being the MM. */
    /**
     * Shown whenever the page we're on belongs to this event's MM - youAreMm decides whether the
     * buttons are actually clickable or just grayed out (still their real green/primary color,
     * per JavaFX's default disabled styling) with a tooltip naming who the real MM is.
     */
    private FlowPane actionBar(final String eventId, final String status, final List<String> optionNames, final String marketMakerName, final boolean youAreMm, final Runnable onChange)
    {
        final FlowPane bar = new FlowPane(8, 6);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(0, 0, 10, 0));
        final String reason = "Only " + marketMakerName + ", this event's Market Maker, can do this.";

        if ("NOT_ACTIVE".equals(status)) {
            final Button openBtn = new Button("Open Event");
            openBtn.getStyleClass().add("primary-button");
            openBtn.setOnAction(e -> runAsync(() -> ApiClient.openEvent(Session.getUserName(), eventId), onChange));
            bar.getChildren().add(withDisabledTooltip(openBtn, youAreMm, reason));
        } else if ("ACTIVE".equals(status)) {
            for (int i = 0; i < optionNames.size(); i++) {
                final int winningIndex = i;
                final Button closeBtn = new Button("Close: " + optionNames.get(i) + " wins");
                closeBtn.getStyleClass().add("close-button");
                closeBtn.setOnAction(e -> runAsync(() -> ApiClient.closeEvent(Session.getUserName(), eventId, winningIndex), onChange));
                bar.getChildren().add(withDisabledTooltip(closeBtn, youAreMm, reason));
            }
        }
        return bar;
    }

    private FlowPane obTradeForm(final String eventId, final int optionIndex, final double baseValue, final Runnable onChange)
    {
        final ComboBox<String> sideCombo = new ComboBox<>();
        sideCombo.getItems().addAll("Buy", "Sell");
        sideCombo.getSelectionModel().selectFirst();

        final Spinner<Integer> qtySpinner = new Spinner<>(1, 1_000_000, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(90);

        final double minPrice = 0.01;
        final double maxPrice = Math.max(minPrice, baseValue - 0.01);
        final double defaultPrice = Math.round((minPrice + maxPrice) / 2 * 100.0) / 100.0;
        final Spinner<Double> priceSpinner = new Spinner<>(minPrice, maxPrice, defaultPrice, 0.01);
        priceSpinner.setEditable(true);
        priceSpinner.setPrefWidth(100);

        final Button submitBtn = new Button("Submit Order");
        submitBtn.getStyleClass().add("primary-button");
        submitBtn.setOnAction(e -> {
            final int quantity = qtySpinner.getValue();
            final double price = priceSpinner.getValue();
            final OrderSide side = "Sell".equals(sideCombo.getValue()) ? OrderSide.SELL : OrderSide.BUY;
            runAsync(() -> ApiClient.submitOrder(Session.getUserName(), eventId, optionIndex, side, price, quantity), onChange);
        });

        final FlowPane form = new FlowPane(6, 6, sideCombo, labeledField("QTY", qtySpinner), labeledField("PRICE", priceSpinner), submitBtn);
        form.getStyleClass().add("tradebox");
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(8, 0, 0, 0));
        return form;
    }

    /** A small uppercase caption stacked above a control - used so a bare Spinner isn't left
     * unlabeled about what quantity it means (order side, in this case; see obTradeForm). */
    private VBox labeledField(final String caption, final Node control)
    {
        final Label label = new Label(caption);
        label.getStyleClass().add("meta-label");
        final VBox box = new VBox(2, centerLabel(label), control);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // ---------------------------------------------------------------- events: small builders

    private VBox detailHeader(final String name, final String description, final String status, final String marketMakerName, final String commissionText, final String metaLabel1, final String metaValue1, final String metaLabel2, final String metaValue2)
    {
        final Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("detail-title");

        final Label descHeading = new Label("DESCRIPTION");
        descHeading.getStyleClass().add("meta-label");
        final Label descLabel = new Label(description);
        descLabel.getStyleClass().add("detail-desc");
        descLabel.setWrapText(true);
        final VBox descBox = new VBox(4, descHeading, descLabel);
        descBox.getStyleClass().add("description-box");
        descBox.setPadding(new Insets(10, 14, 10, 14));

        final FlowPane meta = new FlowPane(10, 10, metaBox("Status", readableStatus(status)), metaBox("Market Maker", marketMakerName),
            metaBox("Commission", commissionText), metaBox(metaLabel1, metaValue1), metaBox(metaLabel2, metaValue2));
        meta.setAlignment(Pos.CENTER);

        final VBox header = new VBox(8, centerLabel(nameLabel), descBox, meta);
        header.setPadding(new Insets(0, 0, 8, 0));
        return header;
    }

    private VBox metaBox(final String label, final String value)
    {
        final Label l = new Label(label.toUpperCase());
        l.getStyleClass().add("meta-label");
        final Label v = new Label(value);
        final boolean isBalance = label.equalsIgnoreCase("Account balance");
        v.getStyleClass().add(isBalance ? "meta-value-money" : "meta-value");
        final VBox box = new VBox(2, centerLabel(l), centerLabel(v));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("stat-tile");
        return box;
    }

    /** A prominent, hard-to-miss banner announcing the winning option once an event is closed. */
    private HBox winnerBanner(final String winningOptionName)
    {
        final Label label = new Label("🏆  Winner: " + winningOptionName);
        label.getStyleClass().add("winner-banner-label");
        final HBox banner = new HBox(label);
        banner.setAlignment(Pos.CENTER);
        banner.getStyleClass().add("winner-banner");
        banner.setPadding(new Insets(10, 14, 10, 14));
        return banner;
    }

    private <S, T> TableColumn<S, T> column(final String title, final String property, final double width)
    {
        final TableColumn<S, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }

    private <S> TableColumn<S, String> moneyColumn(final String title, final String property, final double width)
    {
        final TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(row -> {
            try {
                final Object raw = row.getValue().getClass().getMethod("get" + Character.toUpperCase(property.charAt(0)) + property.substring(1)).invoke(row.getValue());
                return new SimpleStringProperty(money(((Number) raw).doubleValue()));
            } catch (final Exception e) {
                return new SimpleStringProperty("");
            }
        });
        col.setPrefWidth(width);
        return col;
    }

    /** Like moneyColumn, but shows an em dash instead of $0.00 - for values that are usually zero (e.g. no commission charged). */
    private <S> TableColumn<S, String> optionalMoneyColumn(final String title, final String property, final double width)
    {
        final TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(row -> {
            try {
                final Object raw = row.getValue().getClass().getMethod("get" + Character.toUpperCase(property.charAt(0)) + property.substring(1)).invoke(row.getValue());
                final double value = ((Number) raw).doubleValue();
                return new SimpleStringProperty(value > 0 ? money(value) : "—");
            } catch (final Exception e) {
                return new SimpleStringProperty("");
            }
        });
        col.setPrefWidth(width);
        return col;
    }

    private <S> TableColumn<S, String> timestampColumn(final String title, final String property, final double width)
    {
        final TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(row -> {
            try {
                final Object raw = row.getValue().getClass().getMethod("get" + Character.toUpperCase(property.charAt(0)) + property.substring(1)).invoke(row.getValue());
                return new SimpleStringProperty(((LocalDateTime) raw).format(TIMESTAMP_FORMAT));
            } catch (final Exception e) {
                return new SimpleStringProperty("");
            }
        });
        col.setPrefWidth(width);
        return col;
    }

    /**
     * Wraps a control that only makes sense for your own account. When interactive is false (viewing
     * someone else's page in the Users tab), the control is disabled - but a disabled node never
     * fires the mouse-enter event a Tooltip needs, so the tooltip is installed on an enabled wrapper
     * around it instead.
     */
    private Node withDisabledTooltip(final Region control, final boolean interactive, final String reason)
    {
        if (interactive) {
            return control;
        }
        control.setDisable(true);
        // A disabled node still intercepts mouse picking by default (disable only blocks clicks/
        // focus, not hit-testing), so without this the hover never reaches the wrapper below and
        // its tooltip never shows.
        control.setMouseTransparent(true);
        final HBox wrapper = new HBox(control);
        // Matches whatever alignment the caller gave the control - a plain HBox wrapper otherwise
        // defaults to left, which is why a centered row would drift left only once it's disabled.
        wrapper.setAlignment(Pos.CENTER);
        final Tooltip tooltip = new Tooltip(reason);
        // Default show delay is ~1s, which reads as "nothing happened" on a quick hover - this is
        // pure hover-timer UI, not extra computation, so there's no cost to showing it fast.
        tooltip.setShowDelay(Duration.millis(150));
        Tooltip.install(wrapper, tooltip);
        return wrapper;
    }

    // ---------------------------------------------------------------- events: actions

    private void handleBuy(final EventDetailsDTO details, final ComboBox<String> optionCombo, final TextField quantityField)
    {
        final int optionIndex = optionCombo.getSelectionModel().getSelectedIndex();
        if (optionIndex < 0) {
            showError("Choose an option first.");
            return;
        }
        final int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
        } catch (final NumberFormatException e) {
            showError("Quantity must be a whole number.");
            return;
        }
        runAsync(() -> ApiClient.buyShares(Session.getUserName(), details.getName(), optionIndex, quantity), receipt -> refreshEventsAndUsers());
    }

    private void handleSubmitOrder(final EventDetailsDTO details, final ComboBox<String> optionCombo, final ComboBox<OrderSide> sideCombo, final TextField priceField, final TextField quantityField)
    {
        final int optionIndex = optionCombo.getSelectionModel().getSelectedIndex();
        final OrderSide side = sideCombo.getSelectionModel().getSelectedItem();
        if (optionIndex < 0 || side == null) {
            showError("Choose an option and a side first.");
            return;
        }
        final double price;
        final int quantity;
        try {
            price = Double.parseDouble(priceField.getText().trim());
            quantity = Integer.parseInt(quantityField.getText().trim());
        } catch (final NumberFormatException e) {
            showError("Price and quantity must be numbers.");
            return;
        }
        runAsync(() -> ApiClient.submitOrder(Session.getUserName(), details.getName(), optionIndex, side, price, quantity), this::refreshEventsAndUsers);
    }

    private void handleOpen(final EventDetailsDTO details)
    {
        runAsync(() -> ApiClient.openEvent(Session.getUserName(), details.getName()), this::refreshEventsAndUsers);
    }

    private void handleClose(final EventDetailsDTO details, final ComboBox<String> winnerCombo)
    {
        final int winningIndex = winnerCombo.getSelectionModel().getSelectedIndex();
        if (winningIndex < 0) {
            showError("Choose the winning option first.");
            return;
        }
        runAsync(() -> ApiClient.closeEvent(Session.getUserName(), details.getName(), winningIndex), this::refreshEventsAndUsers);
    }

    // A trade/open/close can change both an event's own detail and any user's balance or
    // relevant-events list - refreshing only one side would leave the other tab showing stale
    // data until the next poll tick, which is a needless few seconds of wrong information.
    private void refreshEventsAndUsers()
    {
        refreshEvents();
        refreshUsers();
    }

    // ---------------------------------------------------------------- events: upload

    private void handleChooseFile()
    {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Select an events file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));
        final File file = chooser.showOpenDialog(loadFileButton.getScene().getWindow());
        if (file == null) {
            return;
        }

        filePathLabel.setText(file.getAbsolutePath());
        runAsync(() -> uploadFile(file), () -> {
            refreshEvents();
            refreshUsers();
        });
    }

    // Reading the file and uploading it both belong in the background work of the same runAsync
    // call, not two separate ones - a failure reading the file should surface the exact same way
    // a failure uploading it would (an error dialog), rather than needing its own handling.
    private void uploadFile(final File file)
    {
        final byte[] content;
        try {
            content = Files.readAllBytes(file.toPath());
        } catch (final IOException e) {
            throw new ApiException("Could not read the file: " + e.getMessage());
        }
        ApiClient.addEventsFromUpload(Session.getUserName(), content);
    }

    // ---------------------------------------------------------------- users: list + selection

    private void refreshUsers()
    {
        refreshUsers(false);
    }

    private void refreshUsers(final boolean quiet)
    {
        if (quiet) {
            runAsyncQuiet(ApiClient::getAllUsers, users -> applyUsers(users, true));
        } else {
            runAsync(ApiClient::getAllUsers, users -> applyUsers(users, false));
        }
    }

    // Same reasoning as refreshEventListDisplay: only touch the ListView when the content actually
    // changed (by value), so a poll tick where nothing changed does not flicker the selected row.
    // The quiet flag carries through to showUserDetails the same way it does for events.
    private void applyUsers(final List<UserSummaryDTO> users, final boolean quiet)
    {
        if (!listsEqual(userListView.getItems(), users, this::userSummaryEquals)) {
            final UserSummaryDTO currentlySelected = userListView.getSelectionModel().getSelectedItem();
            final String selectedName = currentlySelected == null ? null : currentlySelected.getName();

            userListView.getSelectionModel().selectedItemProperty().removeListener(userSelectionListener);
            userListView.getItems().setAll(users);
            if (selectedName != null) {
                for (final UserSummaryDTO user : users) {
                    if (user.getName().equals(selectedName)) {
                        userListView.getSelectionModel().select(user);
                        break;
                    }
                }
            }
            userListView.getSelectionModel().selectedItemProperty().addListener(userSelectionListener);
        }

        showUserDetails(userListView.getSelectionModel().getSelectedItem(), quiet);
    }

    // Deliberately skips balanceHistory/accountHistory - neither is shown in the list row, and
    // both are lists of DTOs with no equals() of their own, so comparing them would only ever
    // report "changed" and defeat the whole point of this check.
    private boolean userSummaryEquals(final UserSummaryDTO x, final UserSummaryDTO y)
    {
        return x.getName().equals(y.getName())
            && Double.compare(x.getBalance(), y.getBalance()) == 0
            && x.isBlocked() == y.isBlocked()
            && x.isMarketMaker() == y.isMarketMaker()
            && x.getRelevantEventNames().equals(y.getRelevantEventNames());
    }

    private void showUserDetails(final UserSummaryDTO selected)
    {
        showUserDetails(selected, false);
    }

    // Same reasoning as showEventDetails(selected, quiet): skip a background poll's rebuild while
    // a field is focused, so it never gets pulled out from under you mid-type. The account pane
    // (balance/deposit) and the detail pane (events/trade) are guarded independently, so typing
    // in one survives a poll tick that only needed to refresh the other.
    private void showUserDetails(final UserSummaryDTO selected, final boolean quiet)
    {
        if (selected == null) {
            userAccountPane.getChildren().setAll(new Label("Select a user to see their account."));
            userDetailPane.getChildren().setAll(new Label("Select a user to see their details."));
            // The placeholder just replaced the live view's root, so the view has to be dropped
            // too - otherwise reselecting would patch a node that is no longer on screen.
            accountPaneView = null;
            lastRenderedUserDetailUser = null;
            return;
        }
        // Unlike events, getAllUsers() already returns everything both panes need - there is no
        // separate "get one user" endpoint to call.
        if (!(quiet && paneHasFocus(userAccountPane))) {
            renderUserAccountPane(selected);
        }
        if (!(quiet && paneHasFocus(userDetailPane))) {
            renderUserDetail(selected);
        }
    }

    private class UserCell extends ListCell<UserSummaryDTO>
    {
        @Override
        protected void updateItem(final UserSummaryDTO user, final boolean empty)
        {
            super.updateItem(user, empty);
            if (empty || user == null) {
                setGraphic(null);
                return;
            }

            final boolean isSelf = user.getName().equals(Session.getUserName());

            final Text name = new Text(user.getName());
            name.getStyleClass().add("event-cell-name");
            styleUserNameText(name, user.getName());

            final FlowPane pills = new FlowPane(6, 4, pill(money(user.getBalance()), "pill-account"));
            if (isSelf) {
                // The one row you need to be able to find instantly in a long list, at a glance,
                // without reading every name - a pill alone is easy to miss, so the whole row also
                // gets a background tint.
                pills.getChildren().add(pill("YOU", "pill-you"));
            }
            if (user.isBlocked()) {
                pills.getChildren().add(pill("BLOCKED", "pill-closed"));
            }
            if (user.isMarketMaker()) {
                pills.getChildren().add(pill("MARKET MAKER", "pill-mm"));
            }

            final VBox box = new VBox(4, name, pills);
            box.setPadding(new Insets(4, 2, 4, 2));
            if (isSelf) {
                box.getStyleClass().add("self-row");
            }
            box.maxWidthProperty().bind(getListView().widthProperty().subtract(24));
            box.setAccessibleText(user.getName() + ", " + money(user.getBalance()) + (user.isBlocked() ? ", blocked" : "") + (isSelf ? ", you" : ""));
            setGraphic(box);
        }
    }

    // ---------------------------------------------------------------- users: detail rendering

    private void renderUserAccountPane(final UserSummaryDTO user)
    {
        if (accountPaneView == null) {
            accountPaneView = new AccountPaneView();
            userAccountPane.getChildren().setAll(accountPaneView.getRoot());
        }
        accountPaneView.update(user);
    }

    /**
     * The compact, always-visible box pinned under the user list - balance, deposit, and account
     * history. Same bordered "titled card" look as the sections on the right (via sectionCard()),
     * per the user's explicit request to match "Single event details and trade"'s styling.
     */
    private List<Node> buildAccountPane(final UserSummaryDTO user, final boolean isSelf)
    {
        final SectionCard card = sectionCard("Account Details");
        card.body().setAlignment(Pos.CENTER);
        // Stretch to the full width of the list above it - BorderPane's bottom slot alone doesn't
        // force this since a VBox's own preferred width is just whatever its content needs.
        card.outer().setMaxWidth(Double.MAX_VALUE);

        final FlowPane stats = new FlowPane(10, 10,
            statTile("Balance", money(user.getBalance()), "meta-value-money"),
            statTile("Blocked", user.isBlocked() ? "Yes" : "No", user.isBlocked() ? "pl-negative" : "pl-positive"));
        stats.setAlignment(Pos.CENTER);
        card.body().getChildren().add(stats);

        final Label dollarSign = new Label("$");
        dollarSign.getStyleClass().add("meta-value");
        final Spinner<Integer> amountSpinner = new Spinner<>(1, 1_000_000, 100);
        amountSpinner.setId("depositAmountSpinner");
        amountSpinner.setEditable(true);
        amountSpinner.setPrefWidth(100);
        final Button depositButton = new Button("Deposit");
        depositButton.getStyleClass().add("primary-button");
        depositButton.setOnAction(event -> handleDeposit(amountSpinner));
        // FlowPane, not HBox: at the window's minimum width this column is narrow enough that an
        // HBox would rather compress the Button below its natural size than overflow - which
        // clips its label to "De..." instead of wrapping. FlowPane wraps the button to its own row
        // instead of ever shrinking it.
        final FlowPane depositRow = new FlowPane(6, 6, dollarSign, amountSpinner, depositButton);
        depositRow.setAlignment(Pos.CENTER);
        // Without this, the VBox body (fillWidth=true by default) stretches this row to the card's
        // full width, which then hugs its *own* content to the left inside that width - capping it
        // to its natural width lets the body's own CENTER alignment actually center it.
        depositRow.setMaxWidth(Region.USE_PREF_SIZE);
        depositRow.getStyleClass().add("tradebox");
        card.body().getChildren().add(withDisabledTooltip(depositRow, isSelf, NOT_YOURSELF_MESSAGE));

        final SectionCard historyCard = sectionCard("Account History");
        if (user.getAccountHistory().isEmpty()) {
            historyCard.body().getChildren().add(placeholder("No account activity yet."));
        } else {
            for (final AccountEntryDTO entry : user.getAccountHistory()) {
                historyCard.body().getChildren().add(accountHistoryRow(entry));
            }
        }
        card.body().getChildren().add(historyCard.outer());

        return List.of(card.outer());
    }

    /** One line of the Account History log. Extracted so the first build and every later append
     * (see AccountPaneView) format a row exactly the same way. */
    private Label accountHistoryRow(final AccountEntryDTO entry)
    {
        final String sign = entry.getAmount() >= 0 ? "+" : "";
        final Label row = new Label(String.format("[%s] %s: %s%s (balance after: %s)",
            entry.getAt().format(TIME_FORMAT), entry.getDescription(), sign, money(entry.getAmount()), money(entry.getBalanceAfter())));
        row.getStyleClass().add(entry.getAmount() >= 0 ? "pl-positive" : "pl-negative");
        // Without this, a narrow window (see the resize requirement) hard-truncates this with
        // Label's default ellipsis instead of wrapping to a second line.
        row.setWrapText(true);
        return row;
    }

    /**
     * The Account tab's left-hand Account Details pane, built once and patched in place - the same
     * treatment LmsrView/OrderBookView give the detail views, and for the same reason. This pane
     * reacts to the viewer's own balance, which changes on every single trade they make, so the
     * old rebuild-on-change approach threw this pane's scroll position to the top every time the
     * user traded. Balance and Blocked are single Labels to retext; Account History only ever
     * grows at the end, so new entries are appended rather than the whole log being rebuilt.
     */
    private final class AccountPaneView
    {
        private final VBox root = new VBox();
        private String structuralKey;
        private Label balanceLabel;
        private Label blockedLabel;
        private VBox historyBody;
        private int lastHistoryCount = -1;

        Node getRoot()
        {
            return root;
        }

        void update(final UserSummaryDTO user)
        {
            final boolean isSelf = user.getName().equals(Session.getUserName());
            // Whose page this is decides whether Deposit is live, and an empty history renders a
            // placeholder instead of rows - both change which nodes exist, so both belong here.
            final String key = user.getName() + "|" + isSelf + "|" + user.getAccountHistory().isEmpty();
            if (!key.equals(structuralKey)) {
                rebuild(user, isSelf);
                structuralKey = key;
            } else {
                patchValues(user);
            }
        }

        private void rebuild(final UserSummaryDTO user, final boolean isSelf)
        {
            root.getChildren().setAll(buildAccountPane(user, isSelf));
            harvest();
            lastHistoryCount = user.getAccountHistory().size();
        }

        private void harvest()
        {
            final VBox cardOuter = (VBox) root.getChildren().get(0);
            final VBox cardBody = (VBox) cardOuter.getChildren().get(1);
            final FlowPane stats = (FlowPane) cardBody.getChildren().get(0);
            balanceLabel = (Label) ((VBox) stats.getChildren().get(0)).getChildren().get(1);
            blockedLabel = (Label) ((VBox) stats.getChildren().get(1)).getChildren().get(1);
            final VBox historyOuter = (VBox) cardBody.getChildren().get(2);
            historyBody = (VBox) historyOuter.getChildren().get(1);
        }

        private void patchValues(final UserSummaryDTO user)
        {
            balanceLabel.setText(money(user.getBalance()));
            blockedLabel.setText(user.isBlocked() ? "Yes" : "No");
            blockedLabel.getStyleClass().removeAll("pl-negative", "pl-positive");
            blockedLabel.getStyleClass().add(user.isBlocked() ? "pl-negative" : "pl-positive");

            final List<AccountEntryDTO> history = user.getAccountHistory();
            if (history.size() != lastHistoryCount) {
                if (lastHistoryCount >= 0 && history.size() > lastHistoryCount) {
                    // Oldest-first here (unlike the reversed trade history), so new entries land
                    // at the end - append only those, leaving every existing row untouched.
                    for (final AccountEntryDTO entry : history.subList(lastHistoryCount, history.size())) {
                        historyBody.getChildren().add(accountHistoryRow(entry));
                    }
                } else {
                    historyBody.getChildren().clear();
                    for (final AccountEntryDTO entry : history) {
                        historyBody.getChildren().add(accountHistoryRow(entry));
                    }
                }
                lastHistoryCount = history.size();
            }
        }
    }

    private void renderUserDetail(final UserSummaryDTO user)
    {
        // buildUserDetail's own output only ever depends on user.getName() (for the title/isSelf)
        // and, for the participation cards, each event's name/type/status/marketMakerName - never
        // balance (yours or the event's own account/pot), commission, description, or option names.
        // Gating on reference equality of the whole UserSummaryDTO was wrong (a Buy/Deposit always
        // changes your balance), and reusing eventSummaryEquals for the events-list half was just as
        // wrong the other way: it also compares accountBalance, and a Buy always changes the traded
        // event's own account balance too - so either check alone made this pane, singleEventBox and
        // all, rebuild from scratch a second after every trade, undoing the selected-card/scroll
        // restoration the immediate post-trade refresh had just put back. Comparing only what this
        // pane actually renders means a pure balance change - on either side - no longer touches it.
        final boolean sameUser = lastRenderedUserDetailUser != null && lastRenderedUserDetailUser.getName().equals(user.getName());
        if (sameUser && listsEqual(lastRenderedUserDetailEvents, allEvents, this::participationRelevantEquals)) {
            return;
        }
        lastRenderedUserDetailUser = user;
        lastRenderedUserDetailEvents = new ArrayList<>(allEvents);

        final Map<String, String> typedValues = captureFieldValues(userDetailPane);
        final Double scroll = captureScroll(userDetailPane);
        final boolean isSelf = user.getName().equals(Session.getUserName());
        userDetailPane.getChildren().setAll(buildUserDetail(user, isSelf));
        restoreFieldValues(userDetailPane, typedValues);
        restoreScroll(userDetailPane, scroll);
    }

    /**
     * Events participation/owner + single-event trade, same as Ex2 showed for every user with no
     * distinction between them - the only Ex3 addition is that the trade/MM controls inside the
     * single-event box are disabled (via populateSingleEventBox's isSelf flag) when this isn't you.
     * The whole thing sits inside one outer "Account Details" card, mirroring the left-side box.
     */
    private List<Node> buildUserDetail(final UserSummaryDTO user, final boolean isSelf)
    {
        final SectionCard outer = sectionCard("Account Details");
        final List<Node> nodes = outer.body().getChildren();
        final Label title = new Label(user.getName());
        title.getStyleClass().add("detail-title");
        nodes.add(centerLabel(title));

        // Creating an event makes you its Market Maker, so this only makes sense on your own page.
        final Button createEventButton = new Button("+ Create Event");
        createEventButton.getStyleClass().add("primary-button");
        createEventButton.setOnAction(event -> openCreateEventDialog());
        final HBox createRow = new HBox(withDisabledTooltip(createEventButton, isSelf, NOT_YOURSELF_MESSAGE));
        createRow.setAlignment(Pos.CENTER);
        nodes.add(createRow);

        final SectionCard eventsCard = sectionCard("Events participation / owner");
        final VBox singleEventBox = new VBox(10);
        boolean hasEvents = false;
        boolean selectionStillListed = false;
        if (allEvents.isEmpty()) {
            eventsCard.body().getChildren().add(placeholder("No events loaded yet."));
        } else {
            hasEvents = true;
            // Every event in the system, same as Ex2 - not just the ones this user already has a
            // position in, since the point of this section is to show at a glance which ones they
            // don't (yet).
            for (final EventSummaryDTO event : allEvents) {
                eventsCard.body().getChildren().add(participationCard(user.getName(), event.getName(), singleEventBox));
                if (event.getName().equals(selectedInlineEventName)) {
                    selectionStillListed = true;
                }
            }
        }
        nodes.add(eventsCard.outer());

        if (hasEvents) {
            final SectionCard singleEventCard = sectionCard("Single event details and trade");
            if (selectionStillListed) {
                populateSingleEventBox(selectedInlineEventName, singleEventBox, user.getName());
            } else {
                selectedInlineEventName = null;
                currentInlineEventName = null;
                currentInlineBox = null;
                currentInlineView = null;
                singleEventBox.getChildren().add(placeholder("Click an event above to see its full details and trade here."));
            }
            singleEventCard.body().getChildren().add(singleEventBox);
            nodes.add(singleEventCard.outer());
        }

        return List.of(outer.outer());
    }

    /**
     * One event in the system, showing this user's participation in it - ported from Ex2's
     * participationCard. The one necessary Ex3 change: Ex2 fetched event details synchronously,
     * in-process, to fill in the share/holdings tiles below the title; here that's an HTTP call,
     * so those tiles fill in asynchronously (via runAsyncQuiet - a failed fetch just leaves the
     * card at title/pills/hint rather than popping an error dialog, since this can run as part of
     * a silent poll-tick refresh same as the rest of the pane).
     */
    private VBox participationCard(final String userName, final String eventName, final VBox singleEventBox)
    {
        EventSummaryDTO summary = null;
        for (final EventSummaryDTO event : allEvents) {
            if (event.getName().equals(eventName)) {
                summary = event;
                break;
            }
        }

        final VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("book-panel");
        card.setPadding(new Insets(10));
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(event -> {
            selectedInlineEventName = eventName;
            populateSingleEventBox(eventName, singleEventBox, userName);
        });

        if (summary == null) {
            card.getChildren().add(placeholder(eventName));
            return card;
        }

        final Label title = new Label(summary.getName());
        title.getStyleClass().add("book-panel-title");
        final HBox pills = new HBox(6,
            pill(readableType(summary.getType()), typePillClass(summary.getType())),
            pill(readableStatus(summary.getStatus()), statusPillClass(summary.getStatus())));
        pills.setAlignment(Pos.CENTER);
        if (userName.equals(summary.getMarketMakerName())) {
            pills.getChildren().add(pill("MM", "pill-mm-tag"));
        }
        final Label hint = new Label("Click to view details & trade below");
        hint.getStyleClass().add("hint-chip");
        card.getChildren().addAll(centerLabel(title), pills, centerLabel(hint));

        runAsyncQuiet(() -> ApiClient.getEventDetails(eventName), details -> {
            if (details instanceof LmsrEventDetailsDTO) {
                final LmsrEventDetailsDTO lmsr = (LmsrEventDetailsDTO) details;
                final Map<String, Integer> sharesByOption = new LinkedHashMap<>();
                for (final OptionDTO option : lmsr.getOptions()) {
                    sharesByOption.put(option.getName(), 0);
                }
                boolean hasTraded = false;
                for (final TransactionDTO tx : lmsr.getTransactions()) {
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
                    final FlowPane tiles = new FlowPane(8, 8);
                    tiles.setAlignment(Pos.CENTER);
                    for (final OptionDTO option : lmsr.getOptions()) {
                        final int qty = sharesByOption.get(option.getName());
                        tiles.getChildren().add(statTile(option.getName(), qty + " shares", "meta-value"));
                    }
                    card.getChildren().add(tiles);
                }
            } else if (details instanceof OrderBookEventDetailsDTO) {
                final OrderBookEventDetailsDTO ob = (OrderBookEventDetailsDTO) details;
                ParticipantHoldingDTO match = null;
                for (final ParticipantHoldingDTO p : ob.getParticipants()) {
                    if (p.getUserName().equals(userName)) {
                        match = p;
                        break;
                    }
                }
                if (match == null) {
                    card.getChildren().add(centerLabel(placeholder("No holdings by this user yet.")));
                } else {
                    final FlowPane tiles = new FlowPane(8, 8);
                    tiles.setAlignment(Pos.CENTER);
                    for (int i = 0; i < ob.getOptionBooks().size(); i++) {
                        final int qty = match.getHoldingsByOption().get(i);
                        tiles.getChildren().add(statTile(ob.getOptionBooks().get(i).getOptionName(), qty + " shares", "meta-value"));
                    }
                    tiles.getChildren().add(statTile("Est. value", money(match.getEstimatedValue()), "meta-value-money"));
                    card.getChildren().add(tiles);

                    if ("CLOSED".equals(ob.getStatus()) && match.getProfitOrLoss() != null) {
                        final double pl = match.getProfitOrLoss();
                        final Label plLabel = new Label((pl >= 0 ? "Profit: " : "Loss: ") + money(Math.abs(pl)));
                        plLabel.getStyleClass().add(pl >= 0 ? "pl-positive" : "pl-negative");
                        card.getChildren().add(centerLabel(plLabel));
                    }
                }
            }
        });

        return card;
    }

    /**
     * Fills in the "Selected Event's Details & Trade" box for whichever event was last clicked in
     * the Account tab's event list - backed by the same LmsrView/OrderBookView the Events tab
     * itself uses, just constructed with a real viewedUserName instead of null. Buying/submitting
     * an order still always acts as whoever is logged in regardless of viewedUserName (that's
     * still an open question); only the Market-Maker open/close controls are gated by it so far.
     * <p>
     * box is a brand new VBox instance every time buildUserDetail rebuilds (switching which user's
     * page you're on) - unlike eventDetailPane, which is one fixed FXML pane reused forever - so
     * the view has to be keyed to *this specific box*, not just the event name: a different box
     * always gets a fresh view, even for the exact same event, so it's never left unpopulated.
     */
    private void populateSingleEventBox(final String eventName, final VBox box, final String viewedUserName)
    {
        final Runnable onChange = () -> populateSingleEventBox(eventName, box, viewedUserName);
        runAsync(() -> ApiClient.getEventDetails(eventName), details -> {
            final boolean freshContext = box != currentInlineBox || !eventName.equals(currentInlineEventName);
            if (freshContext) {
                currentInlineBox = box;
                currentInlineEventName = eventName;
                if (details instanceof LmsrEventDetailsDTO) {
                    final LmsrView view = new LmsrView(viewedUserName);
                    view.update((LmsrEventDetailsDTO) details, onChange);
                    currentInlineView = view;
                    box.getChildren().setAll(view.getRoot());
                } else {
                    final OrderBookView view = new OrderBookView(viewedUserName);
                    view.update((OrderBookEventDetailsDTO) details, onChange);
                    currentInlineView = view;
                    box.getChildren().setAll(view.getRoot());
                }
                return;
            }
            if (currentInlineView instanceof LmsrView) {
                ((LmsrView) currentInlineView).update((LmsrEventDetailsDTO) details, onChange);
            } else {
                ((OrderBookView) currentInlineView).update((OrderBookEventDetailsDTO) details, onChange);
            }
        });
    }

    // ---------------------------------------------------------------- users: actions

    private void handleDeposit(final Spinner<Integer> amountSpinner)
    {
        final double amount = amountSpinner.getValue();
        runAsync(() -> ApiClient.depositCash(Session.getUserName(), amount), this::refreshUsers);
    }

    /**
     * Runs the HTTP call synchronously, right inside the OK button's own click handling, instead
     * of through runAsync. That briefly freezes the dialog while it talks to the server, but it's
     * what lets a rejection (e.consume()) keep the dialog open with everything the user typed -
     * doing that across an async round trip would need real machinery to hold the dialog open
     * while a background Task is still running, for a call that is local and near-instant anyway.
     */
    private void openCreateEventDialog()
    {
        final TextField nameField = new TextField();
        final TextField descriptionField = new TextField();
        final TextField optionOneField = new TextField();
        final TextField optionTwoField = new TextField();

        final ComboBox<CommissionType> commissionTypeCombo = new ComboBox<>();
        commissionTypeCombo.getItems().addAll(CommissionType.ON_PURCHASE, CommissionType.ON_CLOSE);
        commissionTypeCombo.getSelectionModel().selectFirst();

        final Spinner<Integer> commissionSpinner = new Spinner<>(0, 90, 5);
        commissionSpinner.setEditable(true);

        final ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll("LMSR", "Order Book");
        methodCombo.getSelectionModel().selectFirst();

        final Spinner<Integer> bSpinner = new Spinner<>(1, 1_000_000, 100);
        bSpinner.setEditable(true);
        final VBox lmsrFields = new VBox(6, fieldRow("Liquidity (b)", bSpinner));

        final Spinner<Integer> dSpinner = new Spinner<>(1, 1_000_000, 1);
        dSpinner.setEditable(true);
        final Spinner<Integer> initialSpinner = new Spinner<>(0, 1_000_000, 100);
        initialSpinner.setEditable(true);
        final CheckBox allowMintBox = new CheckBox("Allow mint");
        allowMintBox.setSelected(true);
        final VBox orderBookFields = new VBox(6, fieldRow("Base value (d)", dSpinner), fieldRow("Initial shares", initialSpinner), allowMintBox);

        final VBox methodFields = new VBox(8, lmsrFields);

        final VBox form = new VBox(10,
            fieldRow("Event name", nameField),
            fieldRow("Description", descriptionField),
            fieldRow("Option 1", optionOneField),
            fieldRow("Option 2", optionTwoField),
            fieldRow("Commission type", commissionTypeCombo),
            fieldRow("Commission %", commissionSpinner),
            fieldRow("Trading method", methodCombo),
            methodFields);
        form.setPadding(new Insets(10));

        final Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Event");
        dialog.setHeaderText("New event, with " + Session.getUserName() + " as its Market Maker");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getStylesheets().addAll(userDetailPane.getScene().getStylesheets());
        // Not user-resizable: dragging the edge only ever stretched the window, never the fields
        // inside it, which looked broken rather than useful. The window still resizes itself
        // programmatically below, when switching between LMSR and Order Book - setResizable(false)
        // only removes the user's own drag handles, not that automatic resize.
        dialog.setResizable(false);

        // The Order Book fields are taller than the LMSR ones, so the window needs to grow to fit
        // them - otherwise OK/Cancel get pushed below the visible area with no way to reach them.
        // Releasing the min size first is what lets sizeToScene() shrink the window back down too,
        // switching from Order Book back to LMSR.
        final Runnable fitToContent = () -> {
            final Object window = dialog.getDialogPane().getScene().getWindow();
            if (window instanceof Stage) {
                final Stage stage = (Stage) window;
                stage.setMinWidth(0);
                stage.setMinHeight(0);
                stage.sizeToScene();
                stage.setMinWidth(stage.getWidth());
                stage.setMinHeight(stage.getHeight());
            }
        };
        dialog.setOnShown(event -> Platform.runLater(fitToContent));
        methodCombo.setOnAction(event -> {
            methodFields.getChildren().setAll("LMSR".equals(methodCombo.getValue()) ? lmsrFields : orderBookFields);
            fitToContent.run();
        });

        final Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            final List<String> optionNames = List.of(optionOneField.getText().trim(), optionTwoField.getText().trim());
            try {
                if ("LMSR".equals(methodCombo.getValue())) {
                    ApiClient.createLmsrEvent(Session.getUserName(), nameField.getText().trim(), descriptionField.getText().trim(),
                        commissionSpinner.getValue(), commissionTypeCombo.getValue(), optionNames, bSpinner.getValue());
                } else {
                    ApiClient.createOrderBookEvent(Session.getUserName(), nameField.getText().trim(), descriptionField.getText().trim(),
                        commissionSpinner.getValue(), commissionTypeCombo.getValue(), optionNames,
                        allowMintBox.isSelected(), initialSpinner.getValue(), dSpinner.getValue());
                }
            } catch (final ApiException e) {
                showError(e.getMessage());
                event.consume();
            }
        });

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            refreshEvents();
            refreshUsers();
        }
    }

    private HBox fieldRow(final String caption, final Node control)
    {
        final Label label = new Label(caption.toUpperCase());
        label.getStyleClass().add("meta-label");
        label.setMinWidth(120);
        final HBox row = new HBox(10, label, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ---------------------------------------------------------------- chat

    private void refreshChat()
    {
        refreshChat(false);
    }

    private void refreshChat(final boolean quiet)
    {
        if (quiet) {
            runAsyncQuiet(ApiClient::getChatMessages, this::applyChat);
        } else {
            runAsync(ApiClient::getChatMessages, this::applyChat);
        }
    }

    // Same flicker concern as the Events/Users lists, but simpler to guard against: chat messages
    // are only ever appended, never edited or removed, so the count alone says whether anything
    // new arrived - no need to compare every message's content like the other two lists do.
    private void applyChat(final List<ChatMessageDTO> messages)
    {
        if (messages.size() == lastChatMessageCount) {
            return;
        }
        lastChatMessageCount = messages.size();
        chatListView.getItems().setAll(withDateDividers(messages));
        if (!chatListView.getItems().isEmpty()) {
            chatListView.scrollTo(chatListView.getItems().size() - 1);
        }
    }

    /**
     * Interleaves a date-divider row (a plain LocalDate - ChatCell tells it apart from a real
     * message by type) whenever the calendar day changes, WhatsApp-style - so each message row
     * only needs its time, not a full date repeated on every single line.
     */
    private List<Object> withDateDividers(final List<ChatMessageDTO> messages)
    {
        final List<Object> rows = new ArrayList<>();
        LocalDate lastDate = null;
        for (final ChatMessageDTO message : messages) {
            final LocalDate date = message.getAt().toLocalDate();
            if (!date.equals(lastDate)) {
                rows.add(date);
                lastDate = date;
            }
            rows.add(message);
        }
        return rows;
    }

    private void handleSendChat()
    {
        final String message = chatMessageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        runAsync(() -> ApiClient.postChatMessage(Session.getUserName(), message), () -> {
            chatMessageField.clear();
            refreshChat();
        });
    }

    private class ChatCell extends ListCell<Object>
    {
        @Override
        protected void updateItem(final Object item, final boolean empty)
        {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            if (item instanceof LocalDate) {
                final Label dateLabel = new Label(((LocalDate) item).format(CHAT_DATE_FORMAT));
                dateLabel.getStyleClass().add("chat-date-divider");
                final HBox wrap = new HBox(dateLabel);
                wrap.setAlignment(Pos.CENTER);
                wrap.setPadding(new Insets(10, 0, 6, 0));
                wrap.setAccessibleText("Messages from " + ((LocalDate) item).format(CHAT_DATE_FORMAT));
                setGraphic(wrap);
                return;
            }

            final ChatMessageDTO message = (ChatMessageDTO) item;
            final Label time = new Label(message.getAt().format(TIME_FORMAT));
            time.getStyleClass().add("chat-time");
            final Text user = new Text(message.getUserName() + ":");
            user.getStyleClass().add("chat-user");
            // A per-user color, not a per-message one - same username always hashes to the same
            // hue, so a person's messages stay visually consistent as the chat scrolls.
            styleUserNameText(user, message.getUserName());
            final Label text = new Label(message.getMessage());
            text.getStyleClass().add("chat-message-text");
            text.setWrapText(true);

            final HBox box = new HBox(6, time, user, text);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(4, 2, 4, 2));
            HBox.setHgrow(text, Priority.ALWAYS);
            box.maxWidthProperty().bind(getListView().widthProperty().subtract(20));
            box.setAccessibleText(message.getUserName() + " at " + message.getAt().format(CHAT_TIME_FORMAT) + ": " + message.getMessage());
            setGraphic(box);
        }
    }

    // ---------------------------------------------------------------- helpers

    /** True if both lists are the same size and every element matches by value at the same position. */
    private <T> boolean listsEqual(final List<T> a, final List<T> b, final BiPredicate<T, T> equalByValue)
    {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!equalByValue.test(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- table-row equality
    // Used only by the scoped exceptions in patchOrderTable/patchParticipants above - everything
    // else in the live detail views patches Labels directly instead of comparing DTOs.

    private boolean orderEqual(final OrderDTO x, final OrderDTO y)
    {
        return x.getUserName().equals(y.getUserName())
            && x.getQuantity() == y.getQuantity()
            && Double.compare(x.getPrice(), y.getPrice()) == 0;
    }

    private boolean participantEqual(final ParticipantHoldingDTO x, final ParticipantHoldingDTO y)
    {
        return x.getUserName().equals(y.getUserName())
            && x.getHoldingsByOption().equals(y.getHoldingsByOption())
            && x.getPaidByOption().equals(y.getPaidByOption())
            && Double.compare(x.getEstimatedValue(), y.getEstimatedValue()) == 0
            && Double.compare(x.getCommissionPaid(), y.getCommissionPaid()) == 0
            && Objects.equals(x.getProfitOrLoss(), y.getProfitOrLoss());
    }

    private List<String> optionNames(final EventDetailsDTO details)
    {
        final List<String> names = new ArrayList<>();
        if (details instanceof LmsrEventDetailsDTO) {
            for (final OptionDTO option : ((LmsrEventDetailsDTO) details).getOptions()) {
                names.add(option.getName());
            }
        } else {
            for (final OptionBookDTO option : ((OrderBookEventDetailsDTO) details).getOptionBooks()) {
                names.add(option.getOptionName());
            }
        }
        return names;
    }

    /**
     * A poll refresh rebuilds a detail pane's fields from scratch (a fresh TextField every time),
     * which would otherwise wipe out anything not yet submitted. Capturing by id before the
     * rebuild and restoring after keeps whatever you were typing, even though it's a new field
     * instance - the text survives, though you'd need to click back into the field to keep typing.
     */
    private Map<String, String> captureFieldValues(final Parent pane)
    {
        final Map<String, String> values = new HashMap<>();
        collectFieldValues(pane, values);
        return values;
    }

    private void collectFieldValues(final Parent pane, final Map<String, String> values)
    {
        for (final Node node : pane.getChildrenUnmodifiable()) {
            if (node instanceof TextField && node.getId() != null) {
                values.put(node.getId(), ((TextField) node).getText());
            } else if (node instanceof Parent) {
                collectFieldValues((Parent) node, values);
            }
        }
    }

    private void restoreFieldValues(final Parent pane, final Map<String, String> values)
    {
        for (final Node node : pane.getChildrenUnmodifiable()) {
            if (node instanceof TextField && node.getId() != null && values.containsKey(node.getId())) {
                ((TextField) node).setText(values.get(node.getId()));
            } else if (node instanceof Parent) {
                restoreFieldValues((Parent) node, values);
            }
        }
    }

    /**
     * True if the Scene's currently focused control is this pane itself or lives somewhere inside
     * it - the signal a background poll uses to tell "the user is actively using this form" from
     * "nothing is happening here right now." Walking up from the focus owner via getParent() is
     * the only way to ask that, since Node has no built-in isAncestorOf.
     */
    private boolean paneHasFocus(final Parent pane)
    {
        if (pane.getScene() == null) {
            return false;
        }
        Node focused = pane.getScene().getFocusOwner();
        while (focused != null) {
            if (focused == pane) {
                return true;
            }
            focused = focused.getParent();
        }
        return false;
    }

    /**
     * Finds the nearest ancestor ScrollPane (if any) and returns its current vvalue, so a rebuild
     * that swaps out a whole subtree - the only way any of these detail panes ever update - can put
     * the viewport back where it was instead of snapping to the top, which a fresh layout pass
     * otherwise has no memory of.
     */
    private Double captureScroll(final Node node)
    {
        final ScrollPane scrollPane = findAncestorScrollPane(node);
        return scrollPane == null ? null : scrollPane.getVvalue();
    }

    // Layout needs a beat to settle on the new content's height before vvalue means anything -
    // setting it in the same pulse that just replaced the children is what silently no-ops here.
    private void restoreScroll(final Node node, final Double vvalue)
    {
        if (vvalue == null) {
            return;
        }
        final ScrollPane scrollPane = findAncestorScrollPane(node);
        if (scrollPane != null) {
            Platform.runLater(() -> scrollPane.setVvalue(vvalue));
        }
    }

    private ScrollPane findAncestorScrollPane(final Node node)
    {
        Node current = node.getParent();
        while (current != null) {
            if (current instanceof ScrollPane) {
                return (ScrollPane) current;
            }
            current = current.getParent();
        }
        return null;
    }

    private VBox priceCard(final OptionDTO option, final boolean isWinner)
    {
        final HBox nameRow = isWinner
            ? new HBox(6, new Label(option.getName()), pill("WINNER", "pill-winner"))
            : new HBox(new Label(option.getName()));
        nameRow.setAlignment(Pos.CENTER);
        final Label price = new Label(money(option.getCurrentProbability()));
        price.getStyleClass().add("price-card-value");
        final Label chance = new Label(Math.round(option.getCurrentProbability() * 100) + "% implied chance");
        chance.getStyleClass().add("price-card-chance");
        final Label shares = new Label(option.getSharesBought() + " shares bought");
        shares.getStyleClass().add("price-card-shares");
        final VBox card = new VBox(5, nameRow, centerLabel(price), centerLabel(chance), centerLabel(shares));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add(isWinner ? "price-card-winner" : "price-card");
        card.setPadding(new Insets(16, 22, 16, 22));
        card.setPrefWidth(190);
        return card;
    }

    /** One option's worth of an Order Book "Your position" card - ported from Ex2 as-is. */
    private VBox optionPositionCard(final String optionName, final int sharesHeld, final double amountPaid)
    {
        final Label name = new Label(optionName);
        final Label shares = new Label(sharesHeld + " shares held");
        shares.getStyleClass().add("position-card-shares");
        shares.setWrapText(true);
        shares.setTextAlignment(TextAlignment.CENTER);
        final Label paid = new Label("Paid: " + money(amountPaid));
        paid.getStyleClass().add("position-card-paid");
        final VBox card = new VBox(4, centerLabel(name), centerLabel(shares), centerLabel(paid));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("price-card");
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setPrefWidth(210);
        return card;
    }

    private VBox statBox(final String label, final Double value)
    {
        final Label l = new Label(label.toUpperCase());
        l.getStyleClass().add("stat-label");
        final Label v = new Label(value == null ? "—" : money(value));
        v.getStyleClass().add("stat-value");
        final VBox box = new VBox(2, centerLabel(l), centerLabel(v));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("stat-box");
        return box;
    }

    private Label pill(final String text, final String styleClass)
    {
        final Label label = new Label(text);
        label.getStyleClass().addAll("pill", styleClass);
        return label;
    }

    /**
     * A stable per-username color, not stored anywhere - the same name always hashes to the same
     * hue, so it's automatically consistent for that person everywhere this is called, with no
     * server-side state to keep in sync. Saturation/brightness are fixed (not randomized) so every
     * hue stays vivid rather than dull.
     *
     * This needs a real Text node, not a Label: Label has no text-outline property at all, and
     * faking one with a CSS drop-shadow (tried first) comes out as a chunky, cartoonish emboss
     * around bold text rather than a clean border. Text is a Shape, so it has real fill/stroke -
     * StrokeType.OUTSIDE (rather than the default CENTERED, which sinks half the width into the
     * glyph). OUTSIDE at 0.8 (tried first) came out as a thick comic-book outline on bold text at
     * this size - CENTERED sinks half the width into the glyph itself, so the same nominal width
     * reads much lighter.
     */
    private void styleUserNameText(final Text text, final String userName)
    {
        final int hue = Math.floorMod(userName.hashCode(), 360);
        text.setFill(Color.hsb(hue, 0.85, 0.68));
        text.setStroke(Color.rgb(0, 0, 0, 0.6));
        text.setStrokeWidth(0.2);
        text.setStrokeType(StrokeType.CENTERED);
    }

    /** A small standalone stat card - a caption above a bold value, with its own visible border. */
    private VBox statTile(final String label, final String value, final String valueStyleClass)
    {
        final Label l = new Label(label.toUpperCase());
        l.getStyleClass().add("meta-label");
        final Label v = new Label(value);
        v.getStyleClass().add(valueStyleClass);
        final VBox box = new VBox(2, centerLabel(l), centerLabel(v));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("stat-tile");
        return box;
    }

    private record SectionCard(VBox outer, VBox body) {}

    /** A titled, bordered card - the title reads as a heading of its own rather than blending into the content. */
    private SectionCard sectionCard(final String title)
    {
        final Label headerLabel = new Label(title);
        headerLabel.getStyleClass().add("section-card-title");
        final HBox headerBar = new HBox(headerLabel);
        headerBar.setAlignment(Pos.CENTER);
        headerBar.getStyleClass().add("section-card-header");

        final VBox body = new VBox(8);
        body.getStyleClass().add("section-card-body");

        final VBox outer = new VBox(headerBar, body);
        outer.getStyleClass().add("section-card");
        return new SectionCard(outer, body);
    }

    private Label sectionLabel(final String text)
    {
        final Label label = new Label(text);
        label.getStyleClass().add("section-label");
        return label;
    }

    private Label placeholder(final String text)
    {
        final Label label = new Label(text);
        label.getStyleClass().add("placeholder-label");
        return label;
    }

    /** Centers a label's text within whatever width its parent stretches it to. */
    private Label centerLabel(final Label label)
    {
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private String money(final double value)
    {
        return String.format(java.util.Locale.ENGLISH, "$%.2f", value);
    }

    /**
     * Makes a filter ComboBox display a translated label for each raw item instead of the item
     * itself - both the closed box (button cell) and the open dropdown (cell factory) need their
     * own cell for this, since JavaFX treats them as two separate rendering spots.
     */
    private void applyFilterLabels(final ComboBox<String> combo, final java.util.function.Function<String, String> toLabel)
    {
        combo.setButtonCell(filterCell(toLabel));
        combo.setCellFactory(list -> filterCell(toLabel));
    }

    private ListCell<String> filterCell(final java.util.function.Function<String, String> toLabel)
    {
        return new ListCell<String>()
        {
            @Override
            protected void updateItem(final String item, final boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : toLabel.apply(item));
            }
        };
    }

    private String readableType(final String type)
    {
        return "ORDER_BOOK".equals(type) ? "Order Book" : "LMSR";
    }

    private String readableStatus(final String status)
    {
        if ("NOT_ACTIVE".equals(status)) return "Not active";
        if ("ACTIVE".equals(status)) return "Active";
        if ("CLOSED".equals(status)) return "Closed";
        return status;
    }

    private String readableCommission(final String type)
    {
        return "ON_CLOSE".equals(type) ? "On close" : "On purchase";
    }

    private String typePillClass(final String type)
    {
        return "ORDER_BOOK".equals(type) ? "pill-ob" : "pill-lmsr";
    }

    private String statusPillClass(final String status)
    {
        if ("ACTIVE".equals(status)) return "pill-active";
        if ("CLOSED".equals(status)) return "pill-closed";
        return "pill-notactive";
    }

    private void showError(final String message)
    {
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Guess Market");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Runs backgroundWork off the FX thread, then hands its result to onSuccess back on the FX
     * thread - the same Task shape Ex2 already used for its file-load flow, just pulled into one
     * place instead of repeating the same six lines of Task/thread boilerplate at every call site.
     * Any exception from backgroundWork (an ApiException from the server, or a network failure)
     * ends up as an error dialog instead.
     */
    private <T> void runAsync(final Supplier<T> backgroundWork, final Consumer<T> onSuccess)
    {
        final Task<T> task = new Task<T>()
        {
            @Override
            protected T call()
            {
                return backgroundWork.get();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> showError(task.getException().getMessage()));
        new Thread(task).start();
    }

    private void runAsync(final Runnable backgroundWork, final Runnable onSuccess)
    {
        runAsync(() -> {
            backgroundWork.run();
            return null;
        }, ignored -> onSuccess.run());
    }

    /**
     * Same shape as runAsync, but for the polling loop specifically: a poll runs unattended in the
     * background, once a second, so a failed tick (the server was briefly unreachable) has nowhere
     * useful to report an error to - showing a dialog for it would mean a new one popping up every
     * second for as long as the server stays down. It fails silently instead and just tries again
     * on the next tick; once the server answers again, everything resumes on its own.
     */
    private <T> void runAsyncQuiet(final Supplier<T> backgroundWork, final Consumer<T> onSuccess)
    {
        final Task<T> task = new Task<T>()
        {
            @Override
            protected T call()
            {
                return backgroundWork.get();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        new Thread(task).start();
    }
}
