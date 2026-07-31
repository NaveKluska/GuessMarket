# AI Handover Document: GuessMarket Engine Project

## 1. Project Context & Goal
You are continuing work on the **GuessMarket** backend engine. This is a university assignment that involves building an LMSR (Logarithmic Market Scoring Rule) prediction market.
Currently, the project is completing **Exercise 1**, which requires building the core domain engine and a local Console Application. Later exercises will scale this into a Client/Server architecture.

## 2. What Has Been Accomplished So Far
We have completed the entire core domain engine and heavily refactored it for enterprise-grade, clean architecture. All 5 core commands described in the assignment PDF have their backend logic fully implemented.

### Key Architectural Decisions:
- **Strict Package Separation**: The project is strictly divided into domain features (`core`, `pricing`, `billing`, `parsing`, `dto`, `models`). Each domain feature has an `api` package for its interfaces and an `impl` package for its concrete implementations.
- **Data Source Decoupling**: The engine (`MarketEngineImpl`) does not parse files itself. It relies on a `FileParser` interface.
- **LMSR Pricing Decoupling**: The LMSR math is handled by the `LmsrPricingModel` implementation of the `PricingModel` interface. 
- **Billing Decoupling**: Commissions are calculated by the `StandardCommissionCalculator` implementing the `CommissionCalculator` interface.
- **Encapsulation (Law of Demeter)**: The engine does not mutate the `Option` objects directly. It calls `event.executePurchase()` and `event.deactivateEvent()`. The `Event` class handles its own internal state and transaction logging.
- **Data Transfer Objects (DTOs)**: The engine strictly returns immutable DTOs (`EventSummaryDTO`, `EventDetailsDTO`, `ReceiptDTO`) to prevent the UI from modifying the engine's internal state.

### Important Logic Implementations:
- **Index over String**: When buying shares or closing events, the engine takes an `int optionIndex` instead of a String name. This prevents typos and string matching errors.
- **Transactions & Users**: The `buyShares` method requires a `String memberName`. This ensures that every `Transaction` logged in the `Event` is tied to a specific user, perfectly setting up the engine for Exercise 3.
- **LMSR Payout Rule**: When `closeEvent` is called, and the commission rule is `ON_CLOSE` (referred to as ON_PROFIT in the PDF), the engine iterates through all past transactions. It calculates profit using the universal LMSR rule: **1 winning share pays out exactly 1 unit of currency**.

## 3. Current Codebase Structure
```text
guess-market-engine/src/guessmarket/engine/
├── billing/
│   ├── api/CommissionCalculator.java
│   └── impl/StandardCommissionCalculator.java
├── core/
│   ├── api/MarketEngine.java
│   └── impl/MarketEngineImpl.java
├── dto/
│   ├── EventDetailsDTO.java
│   ├── EventSummaryDTO.java
│   ├── OptionDTO.java
│   ├── ReceiptDTO.java
│   └── TransactionDTO.java
├── models/
│   ├── CommissionType.java
│   ├── Event.java
│   ├── Option.java
│   └── Transaction.java
├── parsing/
│   └── api/FileParser.java
└── pricing/
    ├── api/PricingModel.java
    └── impl/LmsrPricingModel.java
```

## 4. What Needs To Be Done Next (Your Task)
When the user resumes the session with you, your primary directive is to read this file to understand the architecture, and then await the user's command.

The immediate next steps for the project are:
1. **Implement the XML Parser**: We have the `FileParser` interface, but we need to create `XmlFileParser` inside `guessmarket.engine.parsing.impl`. This parser will read the provided XML files and instantiate the `Event` and `Option` model objects.
2. **Build the Console UI**: Create the main Java application that acts as the user interface for Exercise 1. It will present the menu, parse user input, interact with the `MarketEngine`, and display the DTO data returned by the engine.

### AI Instructions
- **Do not write code immediately.** Just read this document, internalize the strict `api`/`impl` package rules and the DTO boundaries, and wait for the user to tell you to begin the XML parser.
- Maintain the high architectural standards set in this document. Never bypass the `Event` encapsulation, and always program against the interfaces in the `api` packages.
