package guessmarket.console.ui;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.engine.dto.EventDetailsDTO;
import guessmarket.engine.dto.EventSummaryDTO;
import guessmarket.engine.dto.OptionDTO;
import guessmarket.engine.dto.ReceiptDTO;
import guessmarket.engine.dto.TransactionDTO;
import java.util.List;
import java.util.Scanner;

public class ConsoleUIEX1 {
    
    private final MarketEngine engine;
    private final Scanner scanner;

    public ConsoleUIEX1(MarketEngine engine) {
        this.engine = engine;
        this.scanner = new Scanner(System.in);
    }
    
    public void run() {
        boolean exit = false;
        while (!exit) {
            System.out.println("\n--- Guess Market Console ---");
            System.out.println("1. Load XML File");
            System.out.println("2. Display Events");
            System.out.println("3. Event Trading Status");
            System.out.println("4. Participate in Event");
            System.out.println("5. Close Event");
            System.out.println("6. Save System State");
            System.out.println("7. Load System State");
            System.out.println("8. Exit");
            System.out.print("Select an option: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    handleLoadXmlFile();
                    break;
                case "2":
                    handleDisplayEvents();
                    break;
                case "3":
                    handleEventTradingStatus();
                    break;
                case "4":
                    handleParticipateInEvent();
                    break;
                case "5":
                    handleCloseEvent();
                    break;
                case "6":
                    handleSaveState();
                    break;
                case "7":
                    handleLoadState();
                    break;
                case "8":
                    exit = true;
                    System.out.println("Exiting the system...");
                    break;
                default:
                    System.out.println("Not implemented yet!");
            }
        }
    }

    private void handleLoadXmlFile() {
        System.out.print("Please enter the full path to the XML file: ");
        String filePath = scanner.nextLine().trim();
        try {
            engine.loadData(filePath);
            System.out.println("Success! System data loaded successfully.");
        } catch (Exception e) {
            System.out.println("Failed to load file: " + e.getMessage());
        }
    }

    private void handleDisplayEvents() {
        try {
            List<EventSummaryDTO> events = engine.getAllEvents();
            System.out.println("\n--- All System Events ---");
            for (EventSummaryDTO event : events) {
                String status = event.getActiveStatus() ? "Active" : "Closed";
                System.out.printf("ID: %d | Name: %s | Status: %s\n", event.getId(), event.getName(), status);
                System.out.printf("Description: %s\n", event.getDescription());
                System.out.printf("Commission: %d%% (%s)\n", event.getCommission(), event.getCommissionType());
                System.out.println("Options: " + String.join(" vs ", event.getOptions()));
                System.out.println("-------------------------");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleEventTradingStatus() {
        handleDisplayEvents();
        System.out.print("\nPlease enter the Event ID from the list above: ");
        try {
            int eventId = Integer.parseInt(scanner.nextLine().trim());
            EventDetailsDTO details = engine.getEventDetails(eventId);
            
            System.out.println("\n--- Event Trading Status ---");
            System.out.printf("Event: %s (ID: %d)\n", details.getName(), details.getId());
            String status = details.getActiveStatus() ? "Active" : "Closed";
            System.out.printf("Status: %s\n", status);
            
            System.out.printf("Total Account Balance: %.2f\n", details.getAccountBalance());
            System.out.printf("Total Commission Collected: %.2f\n", details.getTotalCommissionCollected());
            
            System.out.println("\nOptions:");
            for (OptionDTO option : details.getOptions()) {
                System.out.printf("- %s (Shares bought: %d)\n", option.getName(), option.getSharesBought());
            }
            
            System.out.println("\nTrade History:");
            if (details.getTransactions().isEmpty()) {
                System.out.println("No transactions yet.");
            } else {
                for (TransactionDTO tx : details.getTransactions()) {
                    System.out.printf("[%s] %s bought %d shares of '%s' for %.2f total\n", 
                        tx.getTimestamp().toString(), tx.getUserName(), tx.getQuantity(), tx.getOptionName(), tx.getPricePaid());
                }
            }
            System.out.println("----------------------------");
            
        } catch (NumberFormatException e) {
            System.out.println("Error: Event ID must be a valid number.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleParticipateInEvent() {
        try {
            int eventId = displayActiveEventsAndSelectOne();
            if (eventId == -1) return;

            EventDetailsDTO details = engine.getEventDetails(eventId);
            displayEventOptions(details);

            System.out.print("\nEnter your member name: ");
            String memberName = scanner.nextLine().trim();

            System.out.print("Enter the Option Index (1 for first option, 2 for second): ");
            int optionIndex = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter the quantity of shares to buy: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());

            ReceiptDTO receipt = engine.buyShares(memberName, eventId, optionIndex - 1, quantity);
            displayReceipt(receipt);

        } catch (NumberFormatException e) {
            System.out.println("Error: Please enter valid numbers for ID, Option Index, and Quantity.");
        } catch (Exception e) {
            System.out.println("Error during purchase: " + e.getMessage());
        }
    }

    private int displayActiveEventsAndSelectOne() {
        List<EventSummaryDTO> activeEvents = engine.getActiveEvents();
        if (activeEvents.isEmpty()) {
            System.out.println("There are no active events to participate in.");
            return -1;
        }

        System.out.println("\n--- Active Events ---");
        for (EventSummaryDTO event : activeEvents) {
            System.out.printf("ID: %d | Name: %s\n", event.getId(), event.getName());
        }

        System.out.print("\nEnter the Event ID from the list above: ");
        return Integer.parseInt(scanner.nextLine().trim());
    }

    private void displayEventOptions(EventDetailsDTO details) {
        System.out.println("\n--- Current Event Status ---");
        int i = 1;
        for (OptionDTO option : details.getOptions()) {
            System.out.printf("%d. %s (Shares bought: %d)\n", i++, option.getName(), option.getSharesBought());
        }
    }

    private void displayReceipt(ReceiptDTO receipt) {
        System.out.println("\n--- Purchase Receipt ---");
        System.out.printf("Cost of Shares: %.2f\n", receipt.getCostOfShares());
        System.out.printf("Commission Paid: %.2f\n", receipt.getCommissionPaid());
        System.out.printf("Total Paid: %.2f\n", receipt.getTotalPaid());
        System.out.println("------------------------");
    }

    private void handleCloseEvent() {
        try {
            System.out.print("Enter the Event ID to close: ");
            int eventId = Integer.parseInt(scanner.nextLine().trim());
            
            System.out.print("Enter the Winning Option Index (1 for first, 2 for second): ");
            int winningOptionIndex = Integer.parseInt(scanner.nextLine().trim());
            
            engine.closeEvent(eventId, winningOptionIndex - 1);
            
            System.out.println("Success! Event " + eventId + " has been closed and payouts have been calculated.");
            
        } catch (NumberFormatException e) {
            System.out.println("Error: Please enter valid numbers for ID and Winning Option.");
        } catch (Exception e) {
            System.out.println("Error closing event: " + e.getMessage());
        }
    }

    private void handleSaveState() {
        System.out.print("Enter the full path and filename (without extension) to save to: ");
        String filePath = scanner.nextLine().trim();
        try {
            engine.saveState(filePath);
            System.out.println("Success! System state saved to " + filePath + ".dat");
        } catch (Exception e) {
            System.out.println("Error saving state: " + e.getMessage());
        }
    }

    private void handleLoadState() {
        System.out.print("Enter the full path and filename (without extension) to load from: ");
        String filePath = scanner.nextLine().trim();
        try {
            engine.loadState(filePath);
            System.out.println("Success! System state loaded from " + filePath + ".dat");
        } catch (Exception e) {
            System.out.println("Error loading state: " + e.getMessage());
        }
    }
}
