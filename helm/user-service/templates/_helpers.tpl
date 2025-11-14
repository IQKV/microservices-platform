{{/*
Expand the name of the chart.
*/}}
{{- define "user-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "user-service.fullname" -}}
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
{{- define "user-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "user-service.labels" -}}
helm.sh/chart: {{ include "user-service.chart" . }}
{{ include "user-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "user-service.selectorLabels" -}}
app.kubernetes.io/name: gripday-user-service
app.kubernetes.io/component: authentication
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "user-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "user-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
PostgreSQL labels
*/}}
{{- define "user-service.postgresql.labels" -}}
helm.sh/chart: {{ include "user-service.chart" . }}
app.kubernetes.io/name: auth-postgres
app.kubernetes.io/component: database
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
PostgreSQL selector labels
*/}}
{{- define "user-service.postgresql.selectorLabels" -}}
app.kubernetes.io/name: auth-postgres
app.kubernetes.io/component: database
{{- end }}

{{/*
Redis labels
*/}}
{{- define "user-service.redis.labels" -}}
helm.sh/chart: {{ include "user-service.chart" . }}
app.kubernetes.io/name: auth-redis
app.kubernetes.io/component: cache
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
Redis selector labels
*/}}
{{- define "user-service.redis.selectorLabels" -}}
app.kubernetes.io/name: auth-redis
app.kubernetes.io/component: cache
{{- end }}
