package subsystem_c_loyalty;

public class LoyaltyRecord {
    private final String id;
    private final String ticketId;
    private final String mealChoice;
    private final int pointsAwarded;
    private LoyaltyStatus status;

    public LoyaltyRecord(String id, String ticketId, String mealChoice, int pointsAwarded) {
        this(id, ticketId, mealChoice, pointsAwarded, LoyaltyStatus.INITIAL);
    }

    public LoyaltyRecord(String id, String ticketId, String mealChoice, int pointsAwarded, LoyaltyStatus status) {
        this.id = id;
        this.ticketId = ticketId;
        this.mealChoice = mealChoice;
        this.pointsAwarded = pointsAwarded;
        this.status = status;
    }

    public void confirm() {
        if (this.status != LoyaltyStatus.INITIAL) {
            throw new IllegalStateException("Only INITIAL record can be confirmed");
        }
        this.status = LoyaltyStatus.CONFIRMED;
    }

    public void revoke() {
        if (this.status != LoyaltyStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED record can be revoked");
        }
        this.status = LoyaltyStatus.REVOKED;
    }

    public String getId() { return id; }
    public String getTicketId() { return ticketId; }
    public String getMealChoice() { return mealChoice; }
    public int getPointsAwarded() { return pointsAwarded; }
    public LoyaltyStatus getStatus() { return status; }
}