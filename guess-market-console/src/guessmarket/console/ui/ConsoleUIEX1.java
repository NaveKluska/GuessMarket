package guessmarket.console.ui;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.OptionDTO;
import guessmarket.dto.ReceiptDTO;
import guessmarket.dto.TransactionDTO;
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
                    System.out.println("Invalid choice. Please select an option between 1 and 8.");
            }
        }
    }

    private void handleLoadXmlFile() {
        System.out.print("Please enter the full path to the XML file: ");
        String filePath = scanner.nextLine().trim();
        if (filePath.isEmpty()) {
            System.out.println("Error: File path cannot be empty.");
            return;
        }
        try {
            engine.loadData(filePath);
            System.out.println("Success! System data loaded successfully.");
        } catch (Exception e) {
            System.out.println("Failed to load file: " + e.getMessage());
        }
    }

    private boolean handleDisplayEvents() {
        try {
            List<EventSummaryDTO> events = engine.getAllEvents();
            System.out.println("\n--- All System Events ---");
            for (EventSummaryDTO event : events) {
                System.out.printf("Event ID: %d\n", event.getId());
                System.out.printf("Name: %s\n", event.getName());
                System.out.printf("Description: %s\n", event.getDescription());
                System.out.printf("Commission: %d%%\n", event.getCommission());
                System.out.printf("Commission Type: %s\n", event.getCommissionType());
                System.out.println("Options:");
                int i = 1;
                for (String option : event.getOptions()) {
                    System.out.printf("%d. %s\n", i++, option);
                }
                String status = event.getActiveStatus() ? "Active" : "Closed";
                System.out.printf("Status: %s\n", status);
                System.out.println("-------------------------");
            }
            return true;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    private void handleEventTradingStatus() {
        try {
            if (!handleDisplayEvents()) {
                return;
            }
            System.out.print("Please enter the Event ID from the list above: ");
            int eventId = Integer.parseInt(scanner.nextLine().trim());
            EventDetailsDTO details = engine.getEventDetails(eventId);
            printEventTradingStatus(details);
        } catch (NumberFormatException e) {
            System.out.println("Error: Event ID must be a valid number.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void printEventTradingStatus(EventDetailsDTO details) {
        System.out.println("\n--- Event Trading Status ---");
        
        System.out.println("Current State:");
        int optIndex = 1;
        for (OptionDTO option : details.getOptions()) {
            System.out.printf("  %d. %s - Value: %.2f | Shares Bought: %d\n", 
                optIndex++, option.getName(), option.getCurrentProbability(), option.getSharesBought());
        }
        
        System.out.printf("Event Account Balance: %.2f\n", details.getAccountBalance());
        System.out.printf("Total Commission Collected: %.2f\n", details.getTotalCommissionCollected());
        
        System.out.println("Trade History:");
        List<TransactionDTO> txs = details.getTransactions();
        if (txs.isEmpty()) {
            System.out.println("  No trades have been made yet.");
        } else {
            for (int i = txs.size() - 1; i >= 0; i--) {
                TransactionDTO tx = txs.get(i);
                System.out.printf("  %d. Option: %s | Quantity: %d | Paid: %.2f\n", (i + 1), tx.getOptionName(), tx.getQuantity(), tx.getPricePaid());
            }
        }
        
        if (!details.getActiveStatus()) {
            System.out.println("\nEvent Closed:");
            for (OptionDTO option : details.getOptions()) {
                System.out.printf("  %s - Total Shares: %d\n", option.getName(), option.getSharesBought());
            }
            System.out.printf("Winning Option: %s\n", details.getWinningOptionName());
        }
        System.out.println("----------------------------");
    }

    private void handleParticipateInEvent() {
        try {
            int eventId = displayActiveEventsAndSelectOne("to participate in");
            if (eventId == -1)
                return;

            EventDetailsDTO details = engine.getEventDetails(eventId);
            printEventTradingStatus(details);

            System.out.print("Enter the Option Index (1 to " + details.getOptions().size() + "): ");
            int optionIndex;
            try {
                optionIndex = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                throw new Exception("Option Index must be a valid integer number.");
            }
            if (optionIndex < 1 || optionIndex > details.getOptions().size()) {
                throw new Exception("The selected Option Index is not in the active events list.");
            }

            System.out.print("Enter the quantity of shares to buy: ");
            int quantity;
            try {
                quantity = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                throw new Exception("Quantity must be a valid positive integer number. Value might be too large.");
            }
            if (quantity <= 0) {
                throw new Exception("Quantity must be a positive number.");
            }

            ReceiptDTO receipt = engine.buyShares("ConsoleUser", eventId, optionIndex - 1, quantity);
            displayReceipt(receipt);
            printEventTradingStatus(receipt.getUpdatedEventStatus());

        } catch (Exception e) {
            System.out.println("Error during purchase: " + e.getMessage());
        }
    }

    private int displayActiveEventsAndSelectOne(String actionName) {
        try {
            List<EventSummaryDTO> activeEvents = engine.getActiveEvents();
            if (activeEvents.isEmpty()) {
                System.out.println("There are no active events " + actionName + ".");
                return -1;
            }

            System.out.println("\n--- Active Events ---");
            for (EventSummaryDTO event : activeEvents) {
                System.out.printf("Event ID: %d\n", event.getId());
                System.out.printf("Name: %s\n", event.getName());
                System.out.printf("Description: %s\n", event.getDescription());
                System.out.printf("Commission: %d%%\n", event.getCommission());
                System.out.printf("Commission Type: %s\n", event.getCommissionType());
                System.out.println("Options:");
                int i = 1;
                for (String option : event.getOptions()) {
                    System.out.printf("%d. %s\n", i++, option);
                }
                System.out.println("Status: Active");
                System.out.println("-------------------------");
            }

            System.out.print("\nEnter the Event ID from the list above: ");
            int selectedId = Integer.parseInt(scanner.nextLine().trim());
            if (selectedId <= 0) {
                System.out.println("Error: Event ID must be a positive number.");
                return -1;
            }

            boolean found = false;
            for (EventSummaryDTO event : activeEvents) {
                if (event.getId() == selectedId) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println("Error: The selected Event ID is not in the active events list.");
                return -1;
            }

            return selectedId;
        } catch (NumberFormatException e) {
            System.out.println("Error: Event ID must be a valid number.");
            return -1;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return -1;
        }
    }

    private void displayReceipt(ReceiptDTO receipt) {
        System.out.println("\n--- Receipt ---");
        System.out.printf("Total Paid: %.2f\n", receipt.getTotalPaid());
        System.out.printf("Cost of shares: %.2f\n", receipt.getCostOfShares());
        if (receipt.isCommissionApplied()) {
            System.out.printf("Commission Paid: %.2f\n", receipt.getCommissionPaid());
        }
        System.out.println("-----------------");
    }

    private void handleCloseEvent() {
        try {
            int eventId = displayActiveEventsAndSelectOne("to close");
            if (eventId == -1) return;

            EventDetailsDTO details = engine.getEventDetails(eventId);
            printEventTradingStatus(details);

            System.out.print("Enter the Winning Option Index (1 to " + details.getOptions().size() + "): ");
            int winningOptionIndex;
            try {
                winningOptionIndex = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                throw new Exception("Winning Option Index must be a valid integer number.");
            }
            if (winningOptionIndex < 1 || winningOptionIndex > details.getOptions().size()) {
                throw new Exception("Invalid winning option selection.");
            }

            engine.closeEvent(eventId, winningOptionIndex - 1);
            
            System.out.println("Success! Event " + eventId + " has been closed and payouts have been calculated.");
            
            EventDetailsDTO finalDetails = engine.getEventDetails(eventId);
            printEventTradingStatus(finalDetails);
            
        } catch (Exception e) {
            System.out.println("Error closing event: " + e.getMessage());
        }
    }

    private void handleSaveState() {
        System.out.print("Enter the full path and filename (without extension) to save to: ");
        String filePath = scanner.nextLine().trim();
        if (filePath.isEmpty()) {
            System.out.println("Error: File path cannot be empty.");
            return;
        }
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
        if (filePath.isEmpty()) {
            System.out.println("Error: File path cannot be empty.");
            return;
        }
        try {
            engine.loadState(filePath);
            System.out.println("Success! System state loaded from " + filePath + ".dat");
        } catch (Exception e) {
            System.out.println("Error loading state: " + e.getMessage());
        }
    }
}
