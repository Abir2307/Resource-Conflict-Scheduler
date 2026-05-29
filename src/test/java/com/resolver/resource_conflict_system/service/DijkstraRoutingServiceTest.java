package com.resolver.resource_conflict_system.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DijkstraRoutingServiceTest {

    @Test
    public void shortestPathSimple() {
        DijkstraRoutingService svc = new DijkstraRoutingService();
        Map<String, List<DijkstraRoutingService.Edge>> graph = Map.of(
                "A", List.of(new DijkstraRoutingService.Edge("B", 5), new DijkstraRoutingService.Edge("C", 10)),
                "B", List.of(new DijkstraRoutingService.Edge("C", 3)),
                "C", List.of()
        );
        var res = svc.shortestPath(graph, "A", "C");
        assertEquals(3, res.path.size());
        assertEquals(List.of("A", "B", "C"), res.path);
        assertEquals(8.0, res.cost);
    }
}
