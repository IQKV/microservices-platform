{{/*
Expand the name of the chart.
*/}}
{{- define "standard-ui-blank-astro-daisyui-ssr.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "standard-ui-blank-astro-daisyui-ssr.fullname" -}}
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
{{- define "standard-ui-blank-astro-daisyui-ssr.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "standard-ui-blank-astro-daisyui-ssr.labels" -}}
helm.sh/chart: {{ include "standard-ui-blank-astro-daisyui-ssr.chart" . }}
{{ include "standard-ui-blank-astro-daisyui-ssr.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: ui
app.kubernetes.io/part-of: iqkv-platform
{{- end }}

{{/*
Selector labels
*/}}
{{- define "standard-ui-blank-astro-daisyui-ssr.selectorLabels" -}}
app.kubernetes.io/name: {{ include "standard-ui-blank-astro-daisyui-ssr.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "standard-ui-blank-astro-daisyui-ssr.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "standard-ui-blank-astro-daisyui-ssr.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Check if any runtime environment variables are defined
*/}}
{{- define "standard-ui-blank-astro-daisyui-ssr.hasEnv" -}}
{{- if .Values.app.env }}
{{- if gt (len .Values.app.env) 0 }}
true
{{- end }}
{{- end }}
{{- end }}
