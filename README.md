# Fair-Share Ingestion Scheduler

A simulation of a **fair-share ingestion scheduler** for a multi-tenant data platform.

The project demonstrates how multiple tenants can fairly share a limited worker pool while maintaining service differentiation between tenant tiers (Gold, Silver, Bronze). Instead of simple FIFO scheduling, workers dynamically choose which tenant to serve next using a pluggable scoring strategy.

---

## Features

- Multi-tenant ingestion simulation
- Tier-aware scheduling (Gold / Silver / Bronze)
- Per-tenant queues
- Fair-share worker dispatching
- Pluggable scheduling strategies
- Thread-safe architecture
- Queue capacity management
- Runtime metrics collection
- CSV metric export
- Charts generated from simulation results
- Java 21
- Maven build

---

## Architecture

```text
                  Incoming Messages
                          │
                          ▼
                 +------------------+
                 |  Tenant Router   |
                 +------------------+
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
      Gold Queues     Silver Queues   Bronze Queues
          │               │               │
          └───────────────┼───────────────┘
                          ▼
              Worker Dispatcher
                          │
                Scoring Engine
                          │
             Scheduling Strategy
          (LRU / Future Strategies)
                          │
                          ▼
                 Message Processor
                          │
                          ▼
                 Metrics Registry
                          │
                    CSV + Charts
```

---

## Project Structure

```
src/main/java/com/example/scheduler
│
├── app
│   └── SchedulerSimulationApp
│
├── config
│   └── TenantConfig
│
├── dispatcher
│   └── WorkerDispatcher
│
├── domain
│   ├── Message
│   └── Tier
│
├── metrics
│   └── MetricsChartGenerator
│
├── processor
│   └── MessageProcessor
│
├── queue
│   ├── TenantQueue
│   ├── TenantQueueKey
│   └── TenantQueueRegistry
│
├── registry
│   ├── TenantRegistry
│   ├── TenantStats
│   ├── TenantStatsRegistry
│   └── TenantStatsSnapshot
│
├── router
│   └── TenantRouter
│
└── strategy
    ├── ScoringStrategy
    ├── ScoringEngine
    └── LRUScoringStrategy
```

---

## Scheduling Model

The scheduler separates tenants into service tiers.

| Tier | Priority |
|------|----------|
| Gold | Highest |
| Silver | Medium |
| Bronze | Best Effort |

Each tenant owns an independent queue.

Workers continuously select the next tenant to process instead of simply processing messages in arrival order.

This avoids:

- noisy-neighbor problems
- starvation
- unfair resource allocation

---

## Scoring Strategy

Scheduling decisions are delegated to a `ScoringStrategy`.

Current implementation:

### Least Recently Used (LRU)

```
score = currentTime - lastProcessedTime
```

The tenant that has waited the longest receives the highest score.

Advantages:

- naturally fair
- prevents monopolization
- simple to extend

Future strategies could include:

- Weighted Fair Queueing
- Deficit Round Robin
- Aging
- Priority Boosting
- Credit-based scheduling
- Token bucket scheduling

No dispatcher changes are required when adding a new strategy.

---

## Simulation Configuration

`SchedulerSimulationApp` defines the simulation tenants.

Example:

```java
new TenantConfig("G1", Tier.GOLD, 1, 1000)
```

Parameters include:

- tenant id
- service tier
- allocated capacity
- number of generated messages

---

## Running the Project

### Prerequisites

- Java 21
- Maven 3.9+

### Build

```bash
mvn clean package
```

### Run

```bash
mvn exec:java
```

or

```bash
java -jar target/multi-tenant-scheduler-1.0.0.jar
```

---

## Metrics

The simulation exports CSV files including:

- `metrics-GOLD.csv`
- `metrics-SILVER.csv`
- `metrics-BRONZE.csv`

Collected metrics include tenant processing statistics and throughput information.

Charts are automatically generated using **XChart**.

---

## Design Principles

### Strategy Pattern

Scheduling algorithms are interchangeable.

```text
Worker Dispatcher
        │
        ▼
 Scoring Engine
        │
        ▼
Scoring Strategy
```

---

### Single Responsibility

Each package owns a single concern.

- routing
- scheduling
- queues
- processing
- metrics
- registry

---

### Open/Closed Principle

New scheduling algorithms can be introduced without modifying the dispatcher.

---

## Threading Model

The simulation uses a worker pool backed by Java's `ExecutorService`.

Workers:

- continuously request work
- evaluate tenant scores
- process messages
- update scheduling statistics

---

## Technologies

- Java 21
- Maven
- XChart
- Java Concurrency API

---

## Example Use Cases

This project can serve as a reference implementation for:

- Multi-tenant SaaS platforms
- Event ingestion pipelines
- Stream processing
- Data lake ingestion
- Background job scheduling
- Fair resource allocation
- Queue management research

---

## Future Enhancements

- Dynamic tenant registration
- Runtime tier changes
- Weighted scheduling
- Deficit Round Robin
- Priority aging
- Backpressure support
- REST API
- Spring Boot integration
- Prometheus metrics
- Grafana dashboards
- Kubernetes deployment
- Configuration via YAML

---

## License

This project is intended for educational and experimentation purposes.

Choose an open-source license such as MIT or Apache-2.0 if publishing publicly.

---

## Author

Developed as a demonstration of a fair-share scheduling architecture for multi-tenant ingestion systems.