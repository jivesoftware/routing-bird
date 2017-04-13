package com.jivesoftware.os.routing.bird.deployable;

import com.jivesoftware.os.routing.bird.health.HealthCheck;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponse;
import com.jivesoftware.os.routing.bird.http.client.TenantAwareHttpClient;

/**
 * Created by jonathan.colt on 4/13/17.
 */
public class TenantAwareHttpClientHealthCheck implements HealthCheck {

    private final String poolName;
    private final TenantAwareHttpClient client;

    public TenantAwareHttpClientHealthCheck(String poolName, TenantAwareHttpClient client) {
        this.poolName = poolName;
        this.client = client;
    }

    @Override
    public HealthCheckResponse checkHealth() throws Exception {

        StringBuilder messages = new StringBuilder();

        double[] health = new double[1];

        client.gatherPoolStats((name, poolStats) -> {
            double poolHealth = (poolStats.max - (poolStats.leased + poolStats.pending)) / (double) poolStats.max;
            health[0] = Math.min(health[0], poolHealth);
            
            messages.append(name)
                .append(" = available:").append(poolStats.available)
                .append(" leased:").append(poolStats.leased)
                .append(" pending:").append(poolStats.pending)
                .append(" max:").append(poolStats.max)
                .append(" health:").append(poolStats.max)
                .append("\n");


            return true;
        });

        return new HealthCheckResponse() {

            @Override
            public String getName() {
                return "httpClient>" + poolName;
            }

            @Override
            public double getHealth() {
                return Math.max(0.15, health[0]);
            }

            @Override
            public String getStatus() {
                return "";
            }

            @Override
            public String getDescription() {
                return messages.toString();
            }

            @Override
            public String getResolution() {
                return "Look into http client configuration.";
            }

            @Override
            public long getTimestamp() {
                return System.currentTimeMillis();
            }
        };

    }
}