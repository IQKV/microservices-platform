{{/*
Expand the name of the chart.
*/}}
{{- define "foundation-iam-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "foundation-iam-service.fullname" -}}
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
{{- define "foundation-iam-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "foundation-iam-service.labels" -}}
helm.sh/chart: {{ include "foundation-iam-service.chart" . }}
{{ include "foundation-iam-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: iam-service
app.kubernetes.io/part-of: iqkv-platform
{{- end }}

{{/*
Selector labels
*/}}
{{- define "foundation-iam-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "foundation-iam-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "foundation-iam-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "foundation-iam-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Common environment variables for the application.
IAM service uses nested iqkv.* properties mapped via DB_*, RABBITMQ_*, REDIS_*,
MAIL_*, JWT_*, APP_BASE_URL, BILLING_*, and OAUTH2_* / OIDC_* env vars.
*/}}
{{- define "foundation-iam-service.env" -}}
# Spring Profile Configuration
- name: SPRING_PROFILES_ACTIVE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: SPRING_PROFILES_ACTIVE

# Database Configuration
- name: DB_HOST
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: DB_HOST
- name: DB_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: DB_PORT
- name: DB_NAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: DB_NAME
- name: DB_USERNAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: DB_USERNAME
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-secrets
      key: database-password

# RabbitMQ Configuration
- name: RABBITMQ_HOST
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: RABBITMQ_HOST
- name: RABBITMQ_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: RABBITMQ_PORT
- name: RABBITMQ_USERNAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: RABBITMQ_USERNAME
- name: RABBITMQ_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-secrets
      key: rabbitmq-password

# Redis Configuration
- name: REDIS_HOST
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: REDIS_HOST
- name: REDIS_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: REDIS_PORT
- name: REDIS_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-secrets
      key: redis-password
- name: REDIS_DATABASE
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: REDIS_DATABASE
- name: REDIS_TIMEOUT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: REDIS_TIMEOUT

# Object Storage / MinIO Configuration
- name: OBJECTSTORAGE_ENDPOINT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OBJECTSTORAGE_ENDPOINT
- name: OBJECTSTORAGE_PUBLIC_ENDPOINT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OBJECTSTORAGE_PUBLIC_ENDPOINT
- name: OBJECTSTORAGE_ACCESS_KEY
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-secrets
      key: s3-access-key
- name: OBJECTSTORAGE_SECRET_KEY
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-secrets
      key: s3-secret-key
- name: OBJECTSTORAGE_BUCKET_NAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OBJECTSTORAGE_BUCKET_NAME
- name: OBJECTSTORAGE_REGION
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OBJECTSTORAGE_REGION
- name: OBJECTSTORAGE_SSL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OBJECTSTORAGE_SSL
- name: OBJECTSTORAGE_UPLOAD_MAX_FILE_SIZE_BYTES
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OBJECTSTORAGE_UPLOAD_MAX_FILE_SIZE_BYTES
      optional: true
- name: OBJECTSTORAGE_UPLOAD_PRESIGNED_URL_EXPIRATION_MINUTES
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OBJECTSTORAGE_UPLOAD_PRESIGNED_URL_EXPIRATION_MINUTES
      optional: true
- name: OBJECTSTORAGE_UPLOAD_ALLOWED_MIME_TYPES
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OBJECTSTORAGE_UPLOAD_ALLOWED_MIME_TYPES
      optional: true

# Mail Configuration
- name: MAIL_HOST
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: MAIL_HOST
- name: MAIL_PORT
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: MAIL_PORT
- name: MAIL_FROM
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: MAIL_FROM
- name: MAIL_FROM_NAME
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: MAIL_FROM_NAME
- name: MAIL_REPLY_TO
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: MAIL_REPLY_TO
- name: MAIL_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-secrets
      key: mail-username
- name: MAIL_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-secrets
      key: mail-password

# JWT RSA Key Configuration
- name: JWT_PRIVATE_KEY_PATH
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: JWT_PRIVATE_KEY_PATH
- name: JWT_PUBLIC_KEY_PATH
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: JWT_PUBLIC_KEY_PATH
- name: JWT_ISSUER
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: JWT_ISSUER

# Application Base URL
- name: APP_BASE_URL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: APP_BASE_URL

# OAuth2 / OIDC Configuration
- name: OAUTH2_ENABLED_PROVIDERS
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OAUTH2_ENABLED_PROVIDERS
- name: OAUTH2_BASE_URL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OAUTH2_BASE_URL
- name: OAUTH2_POST_LOGIN_REDIRECT_URI
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OAUTH2_POST_LOGIN_REDIRECT_URI
- name: OAUTH2_AUTO_PROVISION_USERS
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OAUTH2_AUTO_PROVISION_USERS
- name: OAUTH2_STATE_TTL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OAUTH2_STATE_TTL
- name: OAUTH2_STATE_TTL_MAX
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OAUTH2_STATE_TTL_MAX
- name: OAUTH2_AUTO_LINK_ENABLED
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OAUTH2_AUTO_LINK_ENABLED
- name: OAUTH2_GOOGLE_CLIENT_ID
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OAUTH2_GOOGLE_CLIENT_ID
- name: OAUTH2_GOOGLE_CLIENT_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-secrets
      key: oauth2-google-client-secret
- name: OAUTH2_GITHUB_CLIENT_ID
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OAUTH2_GITHUB_CLIENT_ID
- name: OAUTH2_GITHUB_CLIENT_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-secrets
      key: oauth2-github-client-secret
- name: OAUTH2_MICROSOFT_CLIENT_ID
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OAUTH2_MICROSOFT_CLIENT_ID
- name: OAUTH2_MICROSOFT_CLIENT_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-secrets
      key: oauth2-microsoft-client-secret
- name: OAUTH2_MICROSOFT_TENANT_ID
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: OAUTH2_MICROSOFT_TENANT_ID
- name: OIDC_ENCRYPTION_KEY
  valueFrom:
    secretKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-secrets
      key: oidc-encryption-key

# Billing Service Configuration
- name: BILLING_SERVICE_URI
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: BILLING_SERVICE_URI
- name: BILLING_PLAN_REFRESH_INTERVAL
  valueFrom:
    configMapKeyRef:
      name: {{ include "foundation-iam-service.fullname" . }}-config
      key: BILLING_PLAN_REFRESH_INTERVAL
      optional: true

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
{{- define "foundation-iam-service.jvmOpts" -}}
{{- $jvm := .Values.jvm | default dict -}}
{{- $maxRamPercentage := $jvm.maxRAMPercentage | default "70.0" -}}
{{- $initialRAMPercentage := $jvm.initialRAMPercentage | default "50.0" -}}
{{- $minRAMPercentage := $jvm.minRAMPercentage | default "50.0" -}}
{{- $gcAlgorithm := $jvm.gcAlgorithm | default "G1GC" -}}
{{- $maxGCPauseMillis := $jvm.maxGCPauseMillis | default "200" -}}
{{- $additionalOpts := $jvm.additionalOpts | default "" -}}
-XX:+UseContainerSupport -XX:InitialRAMPercentage={{ $initialRAMPercentage }} -XX:MinRAMPercentage={{ $minRAMPercentage }} -XX:MaxRAMPercentage={{ $maxRamPercentage }} -XX:+Use{{ $gcAlgorithm }} -XX:MaxGCPauseMillis={{ $maxGCPauseMillis }} -XX:+UseStringDeduplication -XX:+OptimizeStringConcat -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -Djava.security.egd=file:/dev/./urandom -Duser.timezone=UTC -Dfile.encoding=UTF-8 -Djava.awt.headless=true -Dspring.backgroundpreinitializer.ignore=true{{ if $additionalOpts }} {{ $additionalOpts }}{{ end }}
{{- end }}
