package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationService;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Objects;

public class TicketServiceImpl implements TicketService {
    private static final int MAX_TICKETS_PER_PURCHASE = 25;
    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl() {
        this(new TicketPaymentServiceImpl(), new SeatReservationServiceImpl());
    }

    TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = Objects.requireNonNull(ticketPaymentService, "ticketPaymentService");
        this.seatReservationService = Objects.requireNonNull(seatReservationService, "seatReservationService");
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validateAccountId(accountId);
        validateTicketRequests(ticketTypeRequests);

        int adultTicketCount = 0;
        int infantTicketCount = 0;
        int totalTickets = 0;
        int totalAmountToPay = 0;
        int totalSeatsToReserve = 0;

        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            validateTicketRequest(ticketTypeRequest);

            int numberOfTickets = ticketTypeRequest.getNoOfTickets();
            totalTickets += numberOfTickets;

            Type ticketType = ticketTypeRequest.getTicketType();
            switch (ticketType) {
                case ADULT -> {
                    adultTicketCount += numberOfTickets;
                    totalAmountToPay += numberOfTickets * ADULT_TICKET_PRICE;
                    totalSeatsToReserve += numberOfTickets;
                }
                case CHILD -> {
                    totalAmountToPay += numberOfTickets * CHILD_TICKET_PRICE;
                    totalSeatsToReserve += numberOfTickets;
                }
                case INFANT -> {
                    infantTicketCount += numberOfTickets;
                    // Infants do not pay and do not receive seats.
                }
            }
        }

        if (totalTickets > MAX_TICKETS_PER_PURCHASE) {
            throw invalidPurchase();
        }

        if (adultTicketCount == 0) {
            throw invalidPurchase();
        }

        if (infantTicketCount > adultTicketCount) {
            throw invalidPurchase();
        }

        ticketPaymentService.makePayment(accountId, totalAmountToPay);
        seatReservationService.reserveSeat(accountId, totalSeatsToReserve);
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw invalidPurchase();
        }
    }

    private void validateTicketRequests(TicketTypeRequest... ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw invalidPurchase();
        }
    }

    private void validateTicketRequest(TicketTypeRequest ticketTypeRequest) {
        if (ticketTypeRequest == null || ticketTypeRequest.getTicketType() == null || ticketTypeRequest.getNoOfTickets() <= 0) {
            throw invalidPurchase();
        }
    }

    private InvalidPurchaseException invalidPurchase() {
        return new InvalidPurchaseException("Invalid ticket purchase request");
    }

}
