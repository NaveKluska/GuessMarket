package guessmarket.engine.dto;

public class ReceiptDTO {
    private final double costOfShares;
    private final double commissionPaid;
    private final double totalPaid;
    private final boolean commissionApplied;
    private final EventDetailsDTO updatedEventStatus;

    public ReceiptDTO(final double costOfShares, final double commissionPaid, final double totalPaid, final boolean commissionApplied, final EventDetailsDTO updatedEventStatus) {
        this.costOfShares = costOfShares;
        this.commissionPaid = commissionPaid;
        this.totalPaid = totalPaid;
        this.commissionApplied = commissionApplied;
        this.updatedEventStatus = updatedEventStatus;
    }

    public double getCostOfShares() {
        return costOfShares;
    }

    public double getCommissionPaid() {
        return commissionPaid;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public boolean isCommissionApplied() {
        return commissionApplied;
    }

    public EventDetailsDTO getUpdatedEventStatus() {
        return updatedEventStatus;
    }
}
