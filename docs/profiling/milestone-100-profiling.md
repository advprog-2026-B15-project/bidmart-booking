# Milestone 100 Profiling

## Profiling Scope

The profiling target is the critical booking and notification flow:

- auction event consumption into booking creation,
- SSE notification stream registration and delivery,
- dispute API flow after delivery,
- database-backed integration paths exercised through Spring Boot tests.

These paths were selected because they represent the highest-risk runtime behavior for this module: event throughput, realtime delivery, and lifecycle transitions.

## Profiling Method

Java Flight Recorder is used through a dedicated Gradle task:

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home \
  ./gradlew profileMilestone100

/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home/bin/jfr summary \
  build/profiling/milestone-100.jfr
```

JFR was chosen because it profiles JVM code with low overhead and captures CPU, allocation, thread, lock, GC, socket, and file activity in a single artifact. This is more appropriate than only timing HTTP calls because the current risk is not just endpoint latency; it is also JVM pressure caused by event processing and realtime fan-out.

## Evidence

The recorded summary is stored in:

- `docs/profiling/artifacts/milestone-100-jfr-summary.txt`
- `docs/profiling/artifacts/milestone-100-hot-methods.txt`

The generated binary recording is local build output:

- `build/profiling/milestone-100.jfr`

The binary `.jfr` file is intentionally not committed because it is generated output and can be regenerated with the command above.

## Analysis

The current implementation is functionally reliable, but profiling motivates the next performance improvement:

- JFR did not show a dominant booking-domain CPU hotspot. The highest sampled methods were framework/runtime work such as regex matching, `ConcurrentHashMap.get`, classpath loading, Hibernate/H2 query planning, and Spring annotation scanning.
- The JFR event summary captured allocation samples, GC events, thread parks, execution samples, and process metadata. This is enough to verify the profiling setup is observing JVM runtime behavior, not only wall-clock test duration.
- The SSE integration flow leaves a long-lived request open by design. During shutdown, Spring logs an async timeout path for `text/event-stream`. This supports improving SSE lifecycle management with heartbeat and explicit cleanup.
- Realtime event delivery currently sends SSE events from the caller path. If many subscribers connect or clients slow down, notification publishing can compete with business request processing.
- The service already uses `ConcurrentHashMap` and `CopyOnWriteArrayList` for subscriber storage, which is appropriate for read-heavy realtime fan-out with relatively infrequent subscribe/unsubscribe changes.
- The next improvement should introduce a bounded `TaskExecutor` for SSE delivery and heartbeat cleanup. This keeps event creation and booking status transitions from being blocked by slow network clients.
- Database indexes added in milestone 75 remain important for processed event lookup, notification listing, audit log lookup, and dead-letter inspection.

## Improvement Plan

1. Add bounded async execution for SSE sends.
2. Add heartbeat events for long-lived SSE connections.
3. Add concurrency tests for multiple subscribers and emitter cleanup.
4. Monitor `http_server_requests`, JVM memory, and Tomcat thread metrics after deployment.
