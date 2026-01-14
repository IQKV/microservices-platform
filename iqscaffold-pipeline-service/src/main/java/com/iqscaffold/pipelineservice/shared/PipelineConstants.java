package com.iqscaffold.pipelineservice.shared;

public final class PipelineConstants {
  private PipelineConstants() {
  }

  public static final class MDC {
    private MDC() {
    }

    public static final String USER_ID = "user_id";
    public static final String TENANT_ID = "tenant_id";
    public static final String PIPELINE_ITEM_ID = "pipeline_item_id";
    public static final String FOLLOW_UP_ID = "follow_up_id";
  }

  /**
   * Default values for various operations.
   */
  public static final class Defaults {
    private Defaults() {
    }

    public static final String DEFAULT_TENANT_ID = "default";
  }
}
