# ADR-0001: Modular Monolith as Initial Architecture

Status: Accepted
Date: 2026-11-02

## Context
The platform requires strong consistency for catalog, pricing, stock, payment, shipping, and refund operations with a small initial team and cost-sensitive launch.

## Decision
Use a modular monolith with explicit internal module boundaries and architecture tests.

## Consequences
- Lower operational overhead at launch.
- Strong transaction consistency and simpler debugging.
- Future extraction remains possible if boundaries are preserved.

## Constraints
- Do not introduce microservices, Kafka, Kubernetes, or Redis without demonstrated operational need.
