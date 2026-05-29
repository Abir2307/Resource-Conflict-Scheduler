package com.resolver.resource_conflict_system.domain;

public enum ConflictType {
	SKILL_MISMATCH,
	TIME_OVERLAP,
	RESOURCE_OVERLOAD,
	DEPENDENCY_BLOCKED,
	NO_FEASIBLE_RESOURCE
}