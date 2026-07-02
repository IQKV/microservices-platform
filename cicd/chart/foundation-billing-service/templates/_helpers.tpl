{{/*
Expand the name of the chart.
*/}}
{{- define "foundation-billing-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "foundation-billing-service.fullname" -}}
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
{{- define "foundation-billing-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "foundation-billing-service.labels" -}}
helm.sh/chart: {{ include "foundation-billing-service.chart" . }}
{{ include "foundation-billing-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: billing-service
app.kubernetes.io/part-of: iqkv-platform
{{- end }}

{{/*
Selector labels
*/}}
{{- define "foundation-billing-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "foundation-billing-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "foundation-billing-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "foundation-billing-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Common environment variables for the application.
Billing service uses the classic iqkv.db.* / iqkv.rabbitmq.* property style
mapped via DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD, RABBITMQ_*, MAIL_*, JWT_* env vars.
*/}}
{{- define "foundation-billing-service.env" -}}
# Spring Profile Configuration
- name: SPRING_PROFILES_ACTIVE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: SPRING_PROFILES_ACTIVE

# Database Configuration
- name: DB_HOST
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: DB_HOST
- name: DB_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: DB_PORT
- name: DB_NAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: DB_NAME
- name: DB_USERNAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: DB_USERNAME
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-secrets
      key: database-password

# RabbitMQ Configuration
- name: RABBITMQ_HOST
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: RABBITMQ_HOST
- name: RABBITMQ_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: RABBITMQ_PORT
- name: RABBITMQ_USERNAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: RABBITMQ_USERNAME
- name: RABBITMQ_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-secrets
      key: rabbitmq-password

# Mail Configuration
- name: MAIL_HOST
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: MAIL_HOST
- name: MAIL_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: MAIL_PORT
- name: MAIL_FROM
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: MAIL_FROM
- name: MAIL_FROM_NAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: MAIL_FROM_NAME
- name: MAIL_REPLY_TO
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: MAIL_REPLY_TO
- name: MAIL_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-secrets
      key: mail-username
- name: MAIL_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-secrets
      key: mail-password

# JWT RSA Key Configuration
- name: JWT_PUBLIC_KEY_PATH
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: JWT_PUBLIC_KEY_PATH

# Application Base URL
- name: APP_BASE_URL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: APP_BASE_URL

# Messaging Configuration
- name: MESSAGING_ENABLED
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: MESSAGING_ENABLED

# Stripe Configuration
- name: STRIPE_SECRET_KEY
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-secrets
      key: stripe-secret-key
- name: STRIPE_WEBHOOK_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-secrets
      key: stripe-webhook-secret
- name: STRIPE_PORTAL_RETURN_URL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: STRIPE_PORTAL_RETURN_URL

# Payment Gateway Selector
- name: PAYMENT_GATEWAY_TYPE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: PAYMENT_GATEWAY_TYPE

# Lemon Squeezy Configuration
- name: LEMON_SQUEEZY_API_KEY
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-secrets
      key: lemon-squeezy-api-key
- name: LEMON_SQUEEZY_STORE_ID
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: LEMON_SQUEEZY_STORE_ID
- name: LEMON_SQUEEZY_WEBHOOK_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-secrets
      key: lemon-squeezy-webhook-secret
- name: LEMON_SQUEEZY_PORTAL_RETURN_URL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: LEMON_SQUEEZY_PORTAL_RETURN_URL

# Platform Configuration
- name: ROLLOUT_MODE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: ROLLOUT_MODE
{{- if .Values.platform.defaultBillingEmail }}
- name: DEFAULT_BILLING_EMAIL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-billing-service.fullname" . }}-config
      key: DEFAULT_BILLING_EMAIL
{{- end }}

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
JVM Options for Spring Boot
*/}}
{{- define "foundation-billing-service.jvmOpts" -}}
{{- $jvm := .Values.jvm | default dict -}}
{{- $maxRamPercentage := $jvm.maxRAMPercentage | default "70.0" -}}
{{- $initialRAMPercentage := $jvm.initialRAMPercentage | default "50.0" -}}
{{- $minRAMPercentage := $jvm.minRAMPercentage | default "50.0" -}}
{{- $gcAlgorithm := $jvm.gcAlgorithm | default "G1GC" -}}
{{- $maxGCPauseMillis := $jvm.maxGCPauseMillis | default "200" -}}
{{- $additionalOpts := $jvm.additionalOpts | default "" -}}
-XX:+UseContainerSupport -XX:InitialRAMPercentage={{ $initialRAMPercentage }} -XX:MinRAMPercentage={{ $minRAMPercentage }} -XX:MaxRAMPercentage={{ $maxRamPercentage }} -XX:+Use{{ $gcAlgorithm }} -XX:MaxGCPauseMillis={{ $maxGCPauseMillis }} -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -Djava.security.egd=file:/dev/./urandom -Duser.timezone=UTC -Dfile.encoding=UTF-8 -Djava.awt.headless=true -Dspring.backgroundpreinitializer.ignore=true{{ if $additionalOpts }} {{ $additionalOpts }}{{ end }}
{{- end }}
