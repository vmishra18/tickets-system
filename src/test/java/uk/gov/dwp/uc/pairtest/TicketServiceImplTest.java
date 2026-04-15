package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketServiceImplTest {

    private RecordingTicketPaymentService ticketPaymentService;
    private RecordingSeatReservationService seatReservationService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketPaymentService = new RecordingTicketPaymentService();
        seatReservationService = new RecordingSeatReservationService();
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Test
    void should_process_a_mixed_purchase_correctly() {
        assertPurchase(
                1L,
                65,
                3,
                request(Type.ADULT, 2),
                request(Type.CHILD, 1),
                request(Type.INFANT, 2)
        );
    }

    @Test
    void should_process_an_adult_only_purchase_correctly() {
        assertPurchase(1L, 25, 1, request(Type.ADULT, 1));
    }

    @Test
    void should_process_an_adult_and_infant_purchase_correctly() {
        assertPurchase(
                1L,
                25,
                1,
                request(Type.ADULT, 1),
                request(Type.INFANT, 1)
        );
    }

    @Test
    void should_process_an_adult_and_child_purchase_correctly() {
        assertPurchase(
                1L,
                55,
                3,
                request(Type.ADULT, 1),
                request(Type.CHILD, 2)
        );
    }

    @Test
    void should_allow_exactly_twenty_five_tickets() {
        assertPurchase(
                1L,
                385,
                25,
                request(Type.ADULT, 1),
                request(Type.CHILD, 24)
        );
    }

    @Test
    void should_reject_purchase_when_more_than_twenty_five_tickets_are_requested() {
        assertInvalidPurchase(1L, request(Type.ADULT, 26));
    }

    @Test
    void should_reject_infant_only_purchase() {
        assertInvalidPurchase(1L, request(Type.INFANT, 1));
    }

    @Test
    void should_reject_child_only_purchase() {
        assertInvalidPurchase(1L, request(Type.CHILD, 1));
    }

    @Test
    void should_reject_child_and_infant_purchase_without_an_adult() {
        assertInvalidPurchase(1L, request(Type.CHILD, 1), request(Type.INFANT, 1));
    }

    @Test
    void should_reject_purchase_when_a_ticket_request_has_zero_quantity() {
        assertInvalidPurchase(1L, request(Type.ADULT, 0));
    }

    @Test
    void should_reject_purchase_when_a_ticket_request_has_negative_quantity() {
        assertInvalidPurchase(1L, request(Type.ADULT, -1));
    }

    @Test
    void should_reject_purchase_when_ticket_requests_are_null() {
        assertInvalidPurchase(1L, (TicketTypeRequest[]) null);
    }

    @Test
    void should_reject_purchase_when_no_ticket_requests_are_provided() {
        assertInvalidPurchase(1L);
    }

    @Test
    void should_reject_purchase_when_a_ticket_request_element_is_null() {
        assertInvalidPurchase(1L, request(Type.ADULT, 1), null);
    }

    @Test
    void should_reject_purchase_when_ticket_type_is_null() {
        assertInvalidPurchase(1L, new TicketTypeRequest(null, 1));
    }

    @Test
    void should_reject_purchase_when_account_id_is_null() {
        assertInvalidPurchase(null, request(Type.ADULT, 1));
    }

    @Test
    void should_reject_purchase_when_account_id_is_not_positive() {
        assertInvalidPurchase(0L, request(Type.ADULT, 1));
    }

    @Test
    void should_reject_purchase_when_infants_exceed_adults() {
        assertInvalidPurchase(
                1L,
                request(Type.ADULT, 1),
                request(Type.INFANT, 2)
        );
    }

    private void assertPurchase(long accountId, int expectedAmount, int expectedSeats, TicketTypeRequest... requests) {
        ticketService.purchaseTickets(accountId, requests);

        assertAll(
                () -> assertEquals(accountId, ticketPaymentService.accountId),
                () -> assertEquals(expectedAmount, ticketPaymentService.amount),
                () -> assertEquals(1, ticketPaymentService.callCount),
                () -> assertEquals(accountId, seatReservationService.accountId),
                () -> assertEquals(expectedSeats, seatReservationService.seats),
                () -> assertEquals(1, seatReservationService.callCount)
        );
    }

    private void assertInvalidPurchase(Long accountId, TicketTypeRequest... requests) {
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(accountId, requests));

        assertAll(
                () -> assertEquals(0, ticketPaymentService.callCount),
                () -> assertEquals(0, seatReservationService.callCount)
        );
    }

    private TicketTypeRequest request(Type type, int quantity) {
        return new TicketTypeRequest(type, quantity);
    }

    private static final class RecordingTicketPaymentService implements TicketPaymentService {
        private long accountId;
        private int amount;
        private int callCount;

        @Override
        public void makePayment(long accountId, int totalAmountToPay) {
            this.accountId = accountId;
            this.amount = totalAmountToPay;
            this.callCount++;
        }
    }

    private static final class RecordingSeatReservationService implements SeatReservationService {
        private long accountId;
        private int seats;
        private int callCount;

        @Override
        public void reserveSeat(long accountId, int totalSeatsToAllocate) {
            this.accountId = accountId;
            this.seats = totalSeatsToAllocate;
            this.callCount++;
        }
    }
}
