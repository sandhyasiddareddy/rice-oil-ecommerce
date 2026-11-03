# ADR-0005: Email-Only Notifications at Launch

Status: Accepted
Date: 2026-11-02

## Context
A reliable launch baseline is required with minimal integration complexity.

## Decision
- Use email as the only customer notification channel at launch.
- Defer SMS and WhatsApp integration to post-launch phases.

## Consequences
- Simpler operations and reduced integration surface area.
- Fewer delivery channels to troubleshoot in early production.
- Requires roadmap planning if multi-channel engagement becomes necessary.

## Constraints
- Notification module should remain channel-extensible without speculative overengineering.
