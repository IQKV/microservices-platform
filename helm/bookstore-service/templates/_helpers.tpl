{{/*
Expand the name of the chart.
*/}}
{{- define "bookstore-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "bookstore-service.fullname" -}}
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
{{- define "bookstore-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "bookstore-service.labels" -}}
helm.sh/chart: {{ include "bookstore-service.chart" . }}
{{ include "bookstore-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "bookstore-service.selectorLabels" -}}
app.kubernetes.io/name: iqscaffold-bookstore-service
app.kubernetes.io/component: bookstore
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "bookstore-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "bookstore-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
PostgreSQL labels
*/}}
{{- define "bookstore-service.postgresql.labels" -}}
helm.sh/chart: {{ include "bookstore-service.chart" . }}
app.kubernetes.io/name: bookstore-postgres
app.kubernetes.io/component: database
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
PostgreSQL selector labels
*/}}
{{- define "bookstore-service.postgresql.selectorLabels" -}}
app.kubernetes.io/name: bookstore-postgres
app.kubernetes.io/component: database
{{- end }}

{{/*
Redis labels
*/}}
{{- define "bookstore-service.redis.labels" -}}
helm.sh/chart: {{ include "bookstore-service.chart" . }}
app.kubernetes.io/name: bookstore-redis
app.kubernetes.io/component: cache
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
Redis selector labels
*/}}
{{- define "bookstore-service.redis.selectorLabels" -}}
app.kubernetes.io/name: bookstore-redis
app.kubernetes.io/component: cache
{{- end }}
