# Milestone 100 Monitoring

## Design

The booking service uses Spring Boot Actuator and Micrometer Prometheus because the module is a JVM web service that needs low-friction runtime visibility without adding a custom monitoring protocol. Actuator exposes application health and runtime metrics from the same process, while the Prometheus registry keeps the output compatible with common dashboards and alerting systems.

The monitored surface is intentionally small:

- `/actuator/health` checks whether the service and its dependencies can start correctly.
- `/actuator/metrics` exposes metric names for manual inspection.
- `/actuator/prometheus` exposes scrape-ready metrics for Prometheus or Grafana Cloud.

The metrics include JVM memory, process CPU, HTTP request counts/timing, Tomcat threads, datasource state, and application-level tags using `application=bidmart-booking`.

## Usage

Run the service and inspect:

```bash
curl http://localhost:8085/actuator/health
curl http://localhost:8085/actuator/prometheus | grep http_server_requests
curl http://localhost:8085/actuator/prometheus | grep jvm_memory_used_bytes
```

Staging usage follows the same pattern:

```bash
curl https://bidmart-booking.onrender.com/actuator/health
curl https://bidmart-booking.onrender.com/actuator/prometheus
```

## Justification

This design is enough for the milestone because the most important operational risks in the booking module are service availability, database connectivity, request latency, and JVM resource pressure during realtime notification delivery and event processing. Prometheus-format metrics can later be scraped by a centralized monitoring stack without changing application code.

## Commit Evidence

Use the commit that adds Actuator, Prometheus, and the monitoring integration test as the personal monitoring implementation evidence.
