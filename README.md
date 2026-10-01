## Approach

- `TicketServiceImpl` uses constructor injection so the service can be tested with small recording fakes without changing the public contract.
- The no-arg constructor still wires the supplied third-party payment and seat reservation implementations.
- Validation happens before any side effects, so invalid purchases fail fast before payment or seat allocation is attempted.
- `TicketTypeRequest` remains immutable.

## Rules Applied

- `ADULT` tickets cost `£25`
- `CHILD` tickets cost `£15`
- `INFANT` tickets cost `£0`
- payment is calculated from adult and child tickets only
- seats are reserved for adult and child tickets only
- the purchase limit is 25 tickets in total
- child and infant tickets require at least one adult ticket

## Assumptions

- The phrase "Infants ... will be sitting on an Adult's lap" is interpreted to mean a purchase cannot include more infants than adults.
- That keeps the seat count correct and prevents a single adult from being responsible for more infants than they can reasonably accommodate.

## Tests

The test suite covers the happy path and the invalid paths:

- mixed purchases
- adult-only purchases
- adult + child purchases
- adult + infant purchases
- exactly 25 tickets
- over 25 tickets
- child-only and infant-only purchases
- null and empty request input
- null request elements
- null ticket types
- zero and negative quantities
- invalid account IDs
- infant count greater than adult count

I kept the tests lightweight by using simple recording fakes instead of adding a mocking library.

## Running Tests

Requires JDK 21.

```bash
mvn test
```
