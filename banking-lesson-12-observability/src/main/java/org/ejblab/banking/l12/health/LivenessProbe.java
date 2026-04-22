package org.ejblab.banking.l12.health;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * {@link Liveness} is about "is the JVM in a salvageable state?". It should
 * only fail when the *only* fix is a process restart (OOM trajectory,
 * deadlock detected, etc.). A DB outage does NOT fail liveness — that's
 * what readiness is for.
 */
@Liveness
@ApplicationScoped
public class LivenessProbe implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("banking-liveness");
    }
}
