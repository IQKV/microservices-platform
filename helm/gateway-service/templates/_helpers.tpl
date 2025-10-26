{{/*
Expand the name of the chart.
*/}}
{{- define "gateway-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "gateway-service.fullname" -}}
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
{{- define "gateway-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "gateway-service.labels" -}}
helm.sh/chart: {{ include "gateway-service.chart" . }}
{{ include "gateway-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "gateway-service.selectorLabels" -}}
app.kubernetes.io/name: gripday-gateway-service
app.kubernetes.io/component: gateway
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "gateway-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "gateway-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Redis labels
*/}}
{{- define "gateway-service.redis.labels" -}}
helm.sh/chart: {{ include "gateway-service.chart" . }}
app.kubernetes.io/name: gateway-redis
app.kubernetes.io/component: cache
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: {{ .Values.global.platform }}
{{- end }}

{{/*
Redis selector labels
*/}}
{{- define "gateway-service.redis.selectorLabels" -}}
app.kubernetes.io/name: gateway-redis
app.kubernetes.io/component: cache
{{- end }}
