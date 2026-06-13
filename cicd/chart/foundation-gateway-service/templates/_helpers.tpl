{{/*
Expand the name of the chart.
*/}}
{{- define "foundation-gateway-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "foundation-gateway-service.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "foundation-gateway-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "foundation-gateway-service.labels" -}}
helm.sh/chart: {{ include "foundation-gateway-service.chart" . }}
{{ include "foundation-gateway-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: gateway-service
app.kubernetes.io/part-of: iqkv-platform
{{- end }}

{{/*
Selector labels
*/}}
{{- define "foundation-gateway-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "foundation-gateway-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "foundation-gateway-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "foundation-gateway-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Environment variables injected into the container.
Only variables actually consumed by application.yml are included.
*/}}
{{- define "foundation-gateway-service.env" -}}
- name: SPRING_PROFILES_ACTIVE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-gateway-service.fullname" . }}-config
      key: SPRING_PROFILES_ACTIVE
- name: MANAGEMENT_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-gateway-service.fullname" . }}-config
      key: MANAGEMENT_PORT
- name: IAM_SERVICE_URI
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-gateway-service.fullname" . }}-config
      key: IAM_SERVICE_URI
- name: IAM_JWKS_URI
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-gateway-service.fullname" . }}-config
      key: IAM_JWKS_URI
- name: BILLING_SERVICE_URI
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-gateway-service.fullname" . }}-config
      key: BILLING_SERVICE_URI
- name: AUDIT_SERVICE_URI
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-gateway-service.fullname" . }}-config
      key: AUDIT_SERVICE_URI
- name: CORS_ALLOWED_ORIGINS
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-gateway-service.fullname" . }}-config
      key: CORS_ALLOWED_ORIGINS
# Platform Configuration
- name: ROLLOUT_MODE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-gateway-service.fullname" . }}-config
      key: ROLLOUT_MODE
{{- if .Values.platform.defaultTenantKey }}
- name: DEFAULT_TENANT_KEY
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-gateway-service.fullname" . }}-config
      key: DEFAULT_TENANT_KEY
{{- end }}
- name: IAM_SERVICE_URL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-gateway-service.fullname" . }}-config
      key: IAM_SERVICE_URL
- name: IQKV_IAM_SERVICE_URL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-gateway-service.fullname" . }}-config
      key: IQKV_IAM_SERVICE_URL
# Pod Information
- name: POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: POD_NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
- name: POD_IP
  valueFrom:
    fieldRef:
      fieldPath: status.podIP
- name: NODE_NAME
  valueFrom:
    fieldRef:
      fieldPath: spec.nodeName
{{- end }}

{{/*
Active Spring profiles
*/}}
{{- define "foundation-gateway-service.springProfiles" -}}
{{- .Values.app.springBoot.profiles.active | default "default" }}
{{- end }}

{{/*
JVM Options for Spring Boot Gateway (Reactive / WebFlux)
*/}}
{{- define "foundation-gateway-service.jvmOpts" -}}
{{- $jvm := .Values.jvm | default dict -}}
{{- $maxRamPercentage := $jvm.maxRAMPercentage | default "70.0" -}}
{{- $initialRAMPercentage := $jvm.initialRAMPercentage | default "50.0" -}}
{{- $minRAMPercentage := $jvm.minRAMPercentage | default "50.0" -}}
{{- $gcAlgorithm := $jvm.gcAlgorithm | default "G1GC" -}}
{{- $maxGCPauseMillis := $jvm.maxGCPauseMillis | default "200" -}}
{{- $reactorNettyIoWorkerCount := $jvm.reactorNettyIoWorkerCount | default "4" -}}
{{- $reactorNettyMaxConnections := $jvm.reactorNettyMaxConnections | default "500" -}}
{{- $additionalOpts := $jvm.additionalOpts | default "" -}}
-XX:+UseContainerSupport -XX:InitialRAMPercentage={{ $initialRAMPercentage }} -XX:MinRAMPercentage={{ $minRAMPercentage }} -XX:MaxRAMPercentage={{ $maxRamPercentage }} -XX:+Use{{ $gcAlgorithm }} -XX:MaxGCPauseMillis={{ $maxGCPauseMillis }} -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -Dreactor.netty.ioWorkerCount={{ $reactorNettyIoWorkerCount }} -Dreactor.netty.pool.maxConnections={{ $reactorNettyMaxConnections }} -Dspring.reactor.netty.shutdown-quiet-period=2s -Djava.security.egd=file:/dev/./urandom -Duser.timezone=UTC -Dfile.encoding=UTF-8 -Djava.awt.headless=true -Dspring.backgroundpreinitializer.ignore=true{{ if $additionalOpts }} {{ $additionalOpts }}{{ end }}
{{- end }}
