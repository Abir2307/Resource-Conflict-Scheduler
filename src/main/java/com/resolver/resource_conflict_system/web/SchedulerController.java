package com.resolver.resource_conflict_system.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolver.resource_conflict_system.domain.AuditRecord;
import com.resolver.resource_conflict_system.domain.AvailabilitySlot;
import com.resolver.resource_conflict_system.domain.ProjectTask;
import com.resolver.resource_conflict_system.domain.ResourceProfile;
import com.resolver.resource_conflict_system.domain.SafetyAdvice;
import com.resolver.resource_conflict_system.domain.ScheduleReport;
import com.resolver.resource_conflict_system.service.BankersSafetyService;
import com.resolver.resource_conflict_system.service.DijkstraRoutingService;
import com.resolver.resource_conflict_system.service.AssetCatalogService;
import com.resolver.resource_conflict_system.service.ResourceCatalogService;
import com.resolver.resource_conflict_system.service.SchedulerEngine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

	private final ResourceCatalogService resourceCatalogService;
	private final AssetCatalogService assetCatalogService;
	private final SchedulerEngine schedulerEngine;
	private final BankersSafetyService bankersSafetyService;
	private final DijkstraRoutingService routingService;
	private final com.resolver.resource_conflict_system.repository.AuditRecordRepository auditRecordRepository;
	private final ObjectMapper objectMapper;

	public SchedulerController(ResourceCatalogService resourceCatalogService, SchedulerEngine schedulerEngine,
			AssetCatalogService assetCatalogService, BankersSafetyService bankersSafetyService, DijkstraRoutingService routingService,
			com.resolver.resource_conflict_system.repository.AuditRecordRepository auditRecordRepository,
			ObjectMapper objectMapper) {
		this.resourceCatalogService = resourceCatalogService;
		this.assetCatalogService = assetCatalogService;
		this.schedulerEngine = schedulerEngine;
		this.bankersSafetyService = bankersSafetyService;
		this.routingService = routingService;
		this.auditRecordRepository = auditRecordRepository;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/resources")
	public List<ResourceProfile> resources() {
		return resourceCatalogService.snapshotResources();
	}

	@GetMapping("/resources/{id}")
	public ResponseEntity<ResourceProfile> resource(@PathVariable String id) {
		return resourceCatalogService.findResource(Objects.requireNonNull(id, "id must not be null"))
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/resources")
	public ResponseEntity<ResourceProfile> createResource(@RequestBody @Valid ResourceProfile request) {
		ResourceProfile saved = resourceCatalogService.addResource(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@PutMapping("/resources/{id}")
	public ResponseEntity<ResourceProfile> updateResource(@PathVariable String id, @RequestBody @Valid ResourceProfile request) {
		id = Objects.requireNonNull(id, "id must not be null");
		java.math.BigDecimal salary = request.salaryPerHour() == null ? java.math.BigDecimal.ZERO : request.salaryPerHour();
		ResourceProfile saved = resourceCatalogService.updateResource(new ResourceProfile(id, request.name(), request.skills(),
				request.availabilitySlots(), request.maxWorkloadHours(), request.assignments(), request.location(), salary, request.availableHoursPerWeek()));
		return ResponseEntity.ok(saved);
	}

	@DeleteMapping("/resources/{id}")
	public ResponseEntity<Void> deleteResource(@PathVariable String id) {
		id = Objects.requireNonNull(id, "id must not be null");
		resourceCatalogService.deleteResource(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/audits")
	public List<AuditRecord> audits() {
		return auditRecordRepository.findAll();
	}

	@GetMapping("/audits/{id}")
	public ResponseEntity<AuditRecord> audit(@PathVariable long id) {
		return auditRecordRepository.findById(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/banker")
	public Map<String, Integer> bankerEstimate() {
		// assume an 8-hour new resource by default
		return bankersSafetyService.estimateExtraResources(resourceCatalogService.snapshotTasks(),
				resourceCatalogService.snapshotResources(), 8);
	}

	@PostMapping("/route")
	public DijkstraRoutingService.PathResult route(@RequestBody RouteRequest request) {
		Map<String, List<DijkstraRoutingService.Edge>> graph = new HashMap<>();
		for (var entry : request.graph().entrySet()) {
			List<DijkstraRoutingService.Edge> edges = entry.getValue().stream()
					.map(e -> new DijkstraRoutingService.Edge(e.to(), e.cost())).toList();
			graph.put(entry.getKey(), edges);
		}
		return routingService.shortestPath(graph, request.source(), request.target());
	}

	@PostMapping("/simulate")
	public ScheduleReport simulateWithPayload(@RequestBody @Valid SimulationRequest request) {
		List<ProjectTask> tasks = request.tasks().stream().map(SimulationRequest.TaskPayload::toTask).toList();
		List<ResourceProfile> resources = request.resources().stream().map(SimulationRequest.ResourcePayload::toResource)
				.toList();
		ScheduleReport report = schedulerEngine.simulate(tasks, resources);
		try {
			String payload = objectMapper.writeValueAsString(report);
			auditRecordRepository.save(new AuditRecord(report.generatedAt(), report.summary(), payload));
		} catch (JsonProcessingException ignored) {
		}
		return report;
	}

	@PostMapping("/check-safety")
	public SafetyAdvice checkSafety(@RequestBody(required = false) SimulationRequest request) {
		List<ProjectTask> tasks = request == null ? resourceCatalogService.snapshotTasks()
				: request.tasks().stream().map(SimulationRequest.TaskPayload::toTask).toList();
		List<ResourceProfile> resources = request == null ? resourceCatalogService.snapshotResources()
				: request.resources().stream().map(SimulationRequest.ResourcePayload::toResource).toList();
		java.math.BigDecimal budget = request == null ? null : request.budget();
		long deliveryEtaHours = request == null ? 0L : request.deliveryEtaHours();
		List<String> sequence = request == null ? List.of() : request.sequence();
		return bankersSafetyService.analyzeSafety(tasks, resources, assetCatalogService.snapshotAssets(), 8, budget, deliveryEtaHours, sequence);
	}

	@PostMapping("/logistics")
	public Map<String, Object> logistics(@RequestBody Map<String, Object> payload) {
		// Expect payload: { "graph": { "A": {"B":5}, "B":{"A":5,"C":3} }, "source":"A","target":"C" }
		@SuppressWarnings("unchecked")
		Map<String, Map<String, Number>> graph = (Map<String, Map<String, Number>>) payload.get("graph");
		String source = (String) payload.get("source");
		String target = (String) payload.get("target");
		Map<String, Integer> dist = new HashMap<>();
		Map<String, String> prev = new HashMap<>();
		java.util.Set<String> nodes = graph.keySet();
		java.util.PriorityQueue<String> pq = new java.util.PriorityQueue<>(java.util.Comparator.comparingInt(dist::get));
		for (String n : nodes) { dist.put(n, Integer.MAX_VALUE); prev.put(n, null); }
		if (!dist.containsKey(source) || !dist.containsKey(target)) throw new IllegalArgumentException("source/target not in graph");
		dist.put(source, 0);
		pq.add(source);
		while (!pq.isEmpty()) {
			String u = pq.poll();
			int du = dist.get(u);
			for (var entry : graph.getOrDefault(u, Map.of()).entrySet()) {
				String v = entry.getKey();
				int w = entry.getValue().intValue();
				int alt = du + w;
				if (alt < dist.getOrDefault(v, Integer.MAX_VALUE)) {
					dist.put(v, alt);
					prev.put(v, u);
					pq.remove(v);
					pq.add(v);
				}
			}
		}
		List<String> path = new java.util.ArrayList<>();
		String cur = target;
		if (prev.get(cur) == null && !cur.equals(source) && dist.get(cur) == Integer.MAX_VALUE) {
			throw new IllegalArgumentException("no path found");
		}
		while (cur != null) {
			path.add(0, cur);
			cur = prev.get(cur);
		}
		Map<String, Object> out = new HashMap<>();
		out.put("path", path);
		out.put("cost", dist.get(target));
		return out;
	}

	@PostMapping("/tasks")
	public ResponseEntity<ProjectTask> addTask(@RequestBody @Valid TaskSubmissionRequest request) {
		ProjectTask task = resourceCatalogService.addTask(request.toTask());
		return ResponseEntity.status(HttpStatus.CREATED).body(task);
	}

	@PostMapping("/reset")
	public ResponseEntity<Void> reset() {
		resourceCatalogService.reset();
		return ResponseEntity.noContent().build();
	}

	public record TaskSubmissionRequest(@NotBlank String projectId, @NotBlank String title, @Min(1) long durationHours,
			@NotEmpty Set<String> requiredSkills, @Min(1) int priority, LocalDateTime preferredStart, LocalDateTime deadline,
		    List<String> dependencyTaskIds, List<String> assigneeUsernames, List<String> requiredAssetIds, String location) {

		public ProjectTask toTask() {
			LocalDateTime safePreferredStart = preferredStart == null ? LocalDateTime.now().plusDays(1) : preferredStart;
			return new ProjectTask("pending", projectId, title, Duration.ofHours(durationHours), requiredSkills, priority,
					safePreferredStart, deadline == null ? safePreferredStart.plusDays(7) : deadline, dependencyTaskIds == null ? List.of() : dependencyTaskIds,
					assigneeUsernames == null ? List.of() : assigneeUsernames,
					requiredAssetIds == null ? List.of() : requiredAssetIds, location, false);
		}
	}

	public record SimulationRequest(@NotEmpty List<TaskPayload> tasks, @NotEmpty List<ResourcePayload> resources,
			java.math.BigDecimal budget, Long deliveryEtaHours, List<String> sequence) {

		public record TaskPayload(String id, String projectId, String title, long durationHours, Set<String> requiredSkills,
				int priority, LocalDateTime preferredStart, LocalDateTime deadline, List<String> dependencyTaskIds, List<String> assigneeUsernames,
				List<String> requiredAssetIds, String location) {
			public ProjectTask toTask() {
				LocalDateTime safePreferredStart = preferredStart == null ? LocalDateTime.now().plusDays(1) : preferredStart;
					return new ProjectTask(id, projectId, title, Duration.ofHours(durationHours), requiredSkills, priority,
							safePreferredStart, deadline == null ? safePreferredStart.plusDays(7) : deadline, dependencyTaskIds == null ? List.of() : dependencyTaskIds,
							assigneeUsernames == null ? List.of() : assigneeUsernames,
							requiredAssetIds == null ? List.of() : requiredAssetIds, location, false);
			}
		}

		public record ResourcePayload(String id, String name, Set<String> skills,
				List<com.resolver.resource_conflict_system.domain.AvailabilitySlot> availabilitySlots,
				int maxWorkloadHours, String location, java.math.BigDecimal salaryPerHour, int availableHoursPerWeek) {
			public ResourceProfile toResource() {
				return new ResourceProfile(id, name, skills, availabilitySlots, maxWorkloadHours, List.of(), location, salaryPerHour == null ? java.math.BigDecimal.ZERO : salaryPerHour, availableHoursPerWeek);
			}
		}
	}

	public record ResourceRequest(String id, @NotBlank String name, @NotEmpty Set<String> skills,
			@jakarta.validation.constraints.NotEmpty List<AvailabilitySlot> availabilitySlots,
			@Min(1) int maxWorkloadHours, List<com.resolver.resource_conflict_system.domain.AssignmentResult> assignments,
			String location) {
	}

	public record RouteRequest(Map<String, List<RouteEdge>> graph, String source, String target) {
		public record RouteEdge(String to, double cost) {
		}
	}
}