{{/*
Expand the name of the chart.
*/}}
{{- define "foundation-audit-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "foundation-audit-service.fullname" -}}
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
{{- define "foundation-audit-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "foundation-audit-service.labels" -}}
helm.sh/chart: {{ include "foundation-audit-service.chart" . }}
{{ include "foundation-audit-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: audit-service
app.kubernetes.io/part-of: iqkv-platform
{{- end }}

{{/*
Selector labels
*/}}
{{- define "foundation-audit-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "foundation-audit-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "foundation-audit-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "foundation-audit-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Common environment variables for the application.
*/}}
{{- define "foundation-audit-service.env" -}}
# Spring Profile Configuration
- name: SPRING_PROFILES_ACTIVE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-config
      key: SPRING_PROFILES_ACTIVE

# Database Configuration
- name: DB_HOST
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-config
      key: DB_HOST
- name: DB_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-config
      key: DB_PORT
- name: DB_NAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-config
      key: DB_NAME
- name: DB_USERNAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-config
      key: DB_USERNAME
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-secrets
      key: database-password

# RabbitMQ Configuration
- name: RABBITMQ_HOST
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-config
      key: RABBITMQ_HOST
- name: RABBITMQ_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-config
      key: RABBITMQ_PORT
- name: RABBITMQ_USERNAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-config
      key: RABBITMQ_USERNAME
- name: RABBITMQ_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-secrets
      key: rabbitmq-password

# JWT RSA Key Configuration
- name: JWT_PUBLIC_KEY_PATH
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-config
      key: JWT_PUBLIC_KEY_PATH

# Messaging Configuration
- name: MESSAGING_ENABLED
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-config
      key: MESSAGING_ENABLED

# Platform rollout mode
- name: ROLLOUT_MODE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-audit-service.fullname" . }}-config
      key: ROLLOUT_MODE
{{- end }}

{{/*
JVM Options
*/}}
{{- define "foundation-audit-service.jvmOpts" -}}
-XX:MaxRAMPercentage={{ .Values.jvm.maxRAMPercentage }}
-XX:InitialRAMPercentage={{ .Values.jvm.initialRAMPercentage }}
-XX:MinRAMPercentage={{ .Values.jvm.minRAMPercentage }}
-XX:MaxGCPauseMillis={{ .Values.jvm.maxGCPauseMillis }}
-XX:+Use{{ .Values.jvm.gcAlgorithm }}
{{ .Values.jvm.additionalOpts }}
{{- end }}
