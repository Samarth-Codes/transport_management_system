# CargoPro Transport Management System

A robust Spring Boot backend application for managing shipping loads, transporters, bids, and bookings.

## Repository Structure

```
.
├── src/
│   ├── main/
│   │   ├── java/com/cargopro/
│   │   │   ├── controller/      # REST API Controllers (Load, Bid, Booking, Transporter)
│   │   │   ├── service/         # Business Logic Layer
│   │   │   ├── repository/      # JPA Repositories
│   │   │   ├── entity/          # Database Entities (UUID based)
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── enums/           # Enumerations (LoadStatus, TruckType, etc.)
│   │   │   └── exception/       # Global Exception Handling
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/cargopro/integration/ # Integration Tests
├── pom.xml                      # Maven Dependencies
├── README.md                    # Documentation
└── TEST_SUMMARY.md              # Test execution report
```

## 1. Database Schema Diagram

```mermaid
erDiagram
    LOAD ||--o{ BID : "receives"
    LOAD ||--|{ BOOKING : "results_in"
    TRANSPORTER ||--o{ BID : "places"
    TRANSPORTER ||--o{ BOOKING : "fulfills"
    BID ||--|| BOOKING : "becomes"

    LOAD {
        UUID loadId PK
        string shipperId
        string loadingCity
        string unloadingCity
        string productType
        string truckType
        int noOfTrucks
        int remainingTrucks
        double weight
        enum status "POSTED, BOOKED, CANCELLED"
        timestamp loadingDate
    }

    TRANSPORTER {
        UUID transporterId PK
        string companyName
        double rating
        json availableTrucks "List of {type, count}"
    }

    BID {
        UUID bidId PK
        UUID loadId FK
        UUID transporterId FK
        double proposedRate
        int trucksOffered
        enum status "PENDING, ACCEPTED, REJECTED"
        timestamp submittedAt
    }

    BOOKING {
        UUID bookingId PK
        UUID loadId FK
        UUID bidId FK
        UUID transporterId FK
        double finalRate
        int allocatedTrucks
        enum status "CONFIRMED, CANCELLED"
        timestamp bookedAt
    }
```

## 2. API Documentation

The application comes with built-in Swagger/OpenAPI documentation.

*   **Swagger UI URL**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
*   **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

You can use the Swagger UI to interactively test all endpoints.

## 3. Test Coverage Summary

> **Status**: ✅ **PASSING** (100% Success Rate)

| Test Suite | Tests Run | Status | Description |
| :--- | :---: | :---: | :--- |
| **LoadIntegrationTest** | 3 | ✅ PASS | Validation of Load creation, retrieval, and cancellation. |
| **BidIntegrationTest** | 3 | ✅ PASS | Verification of bidding rules (capacity checks, rate validation). |
| **BookingIntegrationTest** | 2 | ✅ PASS | End-to-end booking flow, capacity deduction, and status updates. |
| **TransporterIntegrationTest** | 1 | ✅ PASS | Transporter registration and management. |

![Test Coverage Placeholder](https://via.placeholder.com/800x200?text=Place+Test+Coverage+Screenshot+Here)

*To generate a fresh report run:* `mvn test`

## Setup Instructions

### Prerequisites
*   Java 17+
*   Maven 3.6+
*   PostgreSQL (Optional for local dev, H2 used for tests)

### Build and Run
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The server will start on port `8080`.

## Key Business Rules

1.  **UUID Identity**: All major entities use UUIDs for secure and scalable identification.
2.  **Capacity Management**:
    *   `Load.remainingTrucks` tracks how many trucks are still needed.
    *   `Transporter.availableTrucks` tracks fleet availability by type.
    *   Bids are rejected if the transporter lacks capacity or offers more than the load needs.
3.  **Booking Logic**:
    *   Accepting a bid automatically creates a booking.
    *   Decrements `remainingTrucks` from the Load.
    *   Decrements `availableTrucks` from the Transporter.
    *   If `remainingTrucks` reaches 0, the Load status updates to `BOOKED`.
4.  **Cancellation**:
    *   Cancelling a Booking restores trucks to both the Load and the Transporter.
