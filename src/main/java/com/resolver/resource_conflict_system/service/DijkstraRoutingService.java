package com.resolver.resource_conflict_system.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class DijkstraRoutingService {

    public static final class Edge {
        public final String to;
        public final double cost;

        public Edge(String to, double cost) {
            this.to = to;
            this.cost = cost;
        }
    }

    public static final class PathResult {
        public final List<String> path;
        public final double cost;

        public PathResult(List<String> path, double cost) {
            this.path = path;
            this.cost = cost;
        }
    }

    /**
     * Graph representation: map of node -> list of outgoing edges.
     */
    public PathResult shortestPath(Map<String, List<Edge>> graph, String source, String target) {
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<Map.Entry<String, Double>> pq = new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

        for (String node : graph.keySet()) {
            dist.put(node, Double.POSITIVE_INFINITY);
        }
        if (!graph.containsKey(source) || !graph.containsKey(target)) {
            return new PathResult(List.of(), Double.POSITIVE_INFINITY);
        }

        dist.put(source, 0.0);
        pq.add(Map.entry(source, 0.0));

        while (!pq.isEmpty()) {
            var e = pq.poll();
            String u = e.getKey();
            double d = e.getValue();
            if (d > dist.get(u)) continue;
            if (u.equals(target)) break;
            List<Edge> neighbours = graph.getOrDefault(u, List.of());
            for (Edge edge : neighbours) {
                double nd = d + edge.cost;
                if (nd < dist.getOrDefault(edge.to, Double.POSITIVE_INFINITY)) {
                    dist.put(edge.to, nd);
                    prev.put(edge.to, u);
                    pq.add(Map.entry(edge.to, nd));
                }
            }
        }

        double finalCost = dist.getOrDefault(target, Double.POSITIVE_INFINITY);
        if (Double.isInfinite(finalCost)) {
            return new PathResult(List.of(), Double.POSITIVE_INFINITY);
        }

        List<String> path = new ArrayList<>();
        String cur = target;
        while (cur != null) {
            path.add(0, cur);
            cur = prev.get(cur);
        }
        return new PathResult(path, finalCost);
    }
}
