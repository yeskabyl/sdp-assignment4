package main.subsystem_b_ticket;

public class Ticket {
    private final String id;
    private final String reservationId;
    private final double baggageWeight;
    private TicketStatus status;

    public Ticket(String id, String reservationId, double baggageWeight) {
        this(id, reservationId, baggageWeight, TicketStatus.PENDING);
    }

    public Ticket(String id, String reservationId, double baggageWeight, TicketStatus status) {
        this.id = id;
        this.reservationId = reservationId;
        this.baggageWeight = baggageWeight;
        this.status = status;
    }

    public void issue() {
        if (this.status != TicketStatus.PENDING) {
            throw new IllegalStateException("Ticket can only be issued from PENDING state");
        }
        this.status = TicketStatus.ISSUED;
    }

    public void voidTicket() {
        if (this.status != TicketStatus.ISSUED) {
            throw new IllegalStateException("Only ISSUED ticket can be voided");
        }
        this.status = TicketStatus.VOIDED;
    }

    public String getId() { return id; }
    public String getReservationId() { return reservationId; }
    public double getBaggageWeight() { return baggageWeight; }
    public TicketStatus getStatus() { return status; }
}