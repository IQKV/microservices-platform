package com.iqscaffold.pipelineservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iqscaffold")
public class IqScaffoldProperties {

  private String tenantIdHeader = "X-Tenant-ID";
  private final Observability observability = new Observability();
  private final Pipeline pipeline = new Pipeline();

  public String getTenantIdHeader() {
    return tenantIdHeader;
  }

  public void setTenantIdHeader(String tenantIdHeader) {
    this.tenantIdHeader = tenantIdHeader;
  }

  public Observability getObservability() {
    return observability;
  }

  public Pipeline getPipeline() {
    return pipeline;
  }

  public static class Observability {
    private final Tracing tracing = new Tracing();

    public Tracing getTracing() {
      return tracing;
    }

    public static class Tracing {
      private boolean enabled = true;
      private double samplingRate = 1.0;
      private String endpoint = "http://localhost:4317";

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public double getSamplingRate() {
        return samplingRate;
      }

      public void setSamplingRate(double samplingRate) {
        this.samplingRate = samplingRate;
      }

      public String getEndpoint() {
        return endpoint;
      }

      public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
      }
    }
  }

  public static class Pipeline {
    private boolean enableAutoStageProgression = false;
    private boolean enableFollowUpReminders = true;
    private boolean enableActivityTracking = true;

    public boolean isEnableAutoStageProgression() {
      return enableAutoStageProgression;
    }

    public void setEnableAutoStageProgression(boolean enableAutoStageProgression) {
      this.enableAutoStageProgression = enableAutoStageProgression;
    }

    public boolean isEnableFollowUpReminders() {
      return enableFollowUpReminders;
    }

    public void setEnableFollowUpReminders(boolean enableFollowUpReminders) {
      this.enableFollowUpReminders = enableFollowUpReminders;
    }

    public boolean isEnableActivityTracking() {
      return enableActivityTracking;
    }

    public void setEnableActivityTracking(boolean enableActivityTracking) {
      this.enableActivityTracking = enableActivityTracking;
    }
  }
}
