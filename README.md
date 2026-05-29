# Resource Conflict System

Spring Boot demo for scheduling, resource conflicts, and safety analysis.

## What This Project Does

- Manages authenticated users with seeded admin and user accounts.
- Lets admins manage resources, tasks, assets, and audit history.
- Filters tasks so non-admin users only see work assigned to them.
- Runs a simulation to produce a schedule report and audit trail.
- Runs a Banker's-style safety analysis to suggest fixes when the current state is unsafe.
- Supports asset routing with Dijkstra-based supplier path checks.
- Supports budget-aware recommendations for suggested resources and assets.

## Main Features

- Spring Security with DB-backed users through `PersistentUserDetailsManager`.
- Default seeded accounts:
	- `admin` / `admin`
	- `user` / `user`
- Thymeleaf UI for:
	- dashboard
	- resources
	- tasks
	- assets
	- audits
	- simulation and safety advice
	- account details
- REST endpoints for CRUD, simulation, routing, and safety checks.

## Domain Model

- Resources include:
	- skills
	- availability slots
	- maximum workload hours
	- salary per hour
	- available hours per week
	- location
- Assets include:
	- category
	- quantity
	- unit
	- location
	- notes
	- cost per piece
	- supplier node id
	- computed estimated arrival time
- Tasks include:
	- project id
	- title
	- duration
	- required skills
	- priority
	- preferred start
	- dependencies
	- assignees
	- required asset ids
	- location

## Scheduling And Safety

- `SchedulerEngine` produces a greedy schedule report.
- `BankersSafetyService` now builds allocation/request/need matrices for tasks, resources, and assets.
- The safety analysis:
	- checks whether the current catalog is safe
	- suggests extra resources and assets when unsafe
	- supports an optional budget cap
	- builds a safe route when the augmented state becomes safe
- `DijkstraRoutingService` is used for asset supplier routing and ETA estimation at read time.

## Key UI Routes

- `GET /` - dashboard
- `GET /login` - login page
- `GET /resources` - resources list
- `GET /resources/new` - add resource
- `GET /resources/edit/{id}` - edit resource
- `GET /tasks` - tasks list
- `GET /tasks/new` - add task
- `GET /tasks/edit/{id}` - edit task
- `GET /assets` - assets list
- `GET /assets/new` - add asset
- `GET /assets/edit/{id}` - edit asset
- `GET /audits` - audit history
- `GET /simulate/ui` - simulation and safety dashboard

## REST API

### Scheduler And Catalog

- `GET /api/scheduler/resources`
- `GET /api/scheduler/resources/{id}`
- `POST /api/scheduler/resources`
- `PUT /api/scheduler/resources/{id}`
- `DELETE /api/scheduler/resources/{id}`
- `GET /api/scheduler/tasks`
- `POST /api/scheduler/tasks`
- `PUT /api/scheduler/tasks/{id}`
- `DELETE /api/scheduler/tasks/{id}`
- `POST /api/scheduler/reset`

### Simulation And Safety

- `POST /api/scheduler/simulate`
- `POST /api/scheduler/check-safety`
- `GET /api/scheduler/banker`

### Audits

- `GET /api/scheduler/audits`
- `GET /api/scheduler/audits/{id}`

### Routing

- `POST /api/scheduler/route`
- `POST /api/scheduler/logistics`

## Budget-Aware Safety Flow

When a budget is supplied to the safety check, the service:

1. Builds the matrix snapshot for current tasks, resources, and assets.
2. Computes shortages by skill and asset.
3. Estimates unit costs for suggested fixes.
4. Picks the cheapest suggested changes first until the budget is exhausted.
5. Re-runs the matrix safety check on the augmented state.

## Asset Routing Flow

- Admins can provide a supplier node id when creating or editing an asset.
- The asset service resolves ETA using `DijkstraRoutingService` against a small supply graph.
- The computed ETA is read-time logic, so it updates from the current routing graph rather than only from stored values.
