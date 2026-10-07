{{/*
Expand the name of the chart.
*/}}
{{- define "foundation-ai-chat-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "foundation-ai-chat-service.fullname" -}}
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
{{- define "foundation-ai-chat-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "foundation-ai-chat-service.labels" -}}
helm.sh/chart: {{ include "foundation-ai-chat-service.chart" . }}
{{ include "foundation-ai-chat-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: ai-chat-service
app.kubernetes.io/part-of: iqkv-platform
{{- end }}

{{/*
Selector labels
*/}}
{{- define "foundation-ai-chat-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "foundation-ai-chat-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "foundation-ai-chat-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "foundation-ai-chat-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Common environment variables for the application.
*/}}
{{- define "foundation-ai-chat-service.env" -}}
# Spring Profile Configuration
- name: SPRING_PROFILES_ACTIVE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: SPRING_PROFILES_ACTIVE

# Database Configuration
- name: DB_HOST
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: DB_HOST
- name: DB_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: DB_PORT
- name: DB_NAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: DB_NAME
- name: DB_USERNAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: DB_USERNAME
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-secrets
      key: database-password

# RabbitMQ Configuration
- name: RABBITMQ_HOST
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: RABBITMQ_HOST
- name: RABBITMQ_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: RABBITMQ_PORT
- name: RABBITMQ_USERNAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: RABBITMQ_USERNAME
- name: RABBITMQ_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-secrets
      key: rabbitmq-password

# JWT RS256 (OAuth2 Resource Server — validates tokens from IAM via JWKS)
- name: JWT_JWKS_URI
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: JWT_JWKS_URI

# Messaging Configuration
- name: MESSAGING_ENABLED
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: MESSAGING_ENABLED

# AI / LLM Configuration
- name: SPRING_AI_OLLAMA_BASE_URL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: SPRING_AI_OLLAMA_BASE_URL
- name: SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL
- name: AI_SYSTEM_PROMPT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: AI_SYSTEM_PROMPT
- name: AI_TEMPERATURE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: AI_TEMPERATURE
- name: AI_MAX_INPUT_CHARS
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: AI_MAX_INPUT_CHARS
- name: AI_MAX_OUTPUT_TOKENS
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: AI_MAX_OUTPUT_TOKENS

# Billing service URL (quota / plan enforcement)
- name: BILLING_SERVICE_URI
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: BILLING_SERVICE_URI

# Platform rollout mode
- name: ROLLOUT_MODE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-ai-chat-service.fullname" . }}-config
      key: ROLLOUT_MODE

# Helm release info
- name: HELM_RELEASE_NAME
  value: {{ .Release.Name | quote }}
{{- end }}

{{/*
JVM Options
*/}}
{{- define "foundation-ai-chat-service.jvmOpts" -}}
-XX:MaxRAMPercentage={{ .Values.jvm.maxRAMPercentage }}
-XX:InitialRAMPercentage={{ .Values.jvm.initialRAMPercentage }}
-XX:MinRAMPercentage={{ .Values.jvm.minRAMPercentage }}
-XX:MaxGCPauseMillis={{ .Values.jvm.maxGCPauseMillis }}
-XX:+Use{{ .Values.jvm.gcAlgorithm }}
{{ .Values.jvm.additionalOpts }}
{{- end }}
