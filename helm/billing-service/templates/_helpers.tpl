{{/*
Expand the name of the chart.
*/}}
{{- define "billing-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "billing-service.fullname" -}}
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
{{- define "billing-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "billing-service.labels" -}}
helm.sh/chart: {{ include "billing-service.chart" . }}
{{ include "billing-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "billing-service.selectorLabels" -}}
app.kubernetes.io/name: iqscaffold-billing-service
app.kubernetes.io/component: billing
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "billing-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "billing-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
PostgreSQL labels
*/}}
{{- define "billing-service.postgresql.labels" -}}
helm.sh/chart: {{ include "billing-service.chart" . }}
app.kubernetes.io/name: billing-postgres
app.kubernetes.io/component: database
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
PostgreSQL selector labels
*/}}
{{- define "billing-service.postgresql.selectorLabels" -}}
app.kubernetes.io/name: billing-postgres
app.kubernetes.io/component: database
{{- end }}

{{/*
Redis labels
*/}}
{{- define "billing-service.redis.labels" -}}
helm.sh/chart: {{ include "billing-service.chart" . }}
app.kubernetes.io/name: billing-redis
app.kubernetes.io/component: cache
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
Redis selector labels
*/}}
{{- define "billing-service.redis.selectorLabels" -}}
app.kubernetes.io/name: billing-redis
app.kubernetes.io/component: cache
{{- end }}

{{/*
RabbitMQ labels
*/}}
{{- define "billing-service.rabbitmq.labels" -}}
helm.sh/chart: {{ include "billing-service.chart" . }}
app.kubernetes.io/name: billing-rabbitmq
app.kubernetes.io/component: message-queue
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
RabbitMQ selector labels
*/}}
{{- define "billing-service.rabbitmq.selectorLabels" -}}
app.kubernetes.io/name: billing-rabbitmq
app.kubernetes.io/component: message-queue
{{- end }}
