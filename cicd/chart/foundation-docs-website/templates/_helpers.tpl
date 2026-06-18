{{/*
Expand the name of the chart.
*/}}
{{- define "foundation-docs-website.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "foundation-docs-website.fullname" -}}
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
{{- define "foundation-docs-website.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "foundation-docs-website.labels" -}}
helm.sh/chart: {{ include "foundation-docs-website.chart" . }}
{{ include "foundation-docs-website.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: docs
app.kubernetes.io/part-of: iqkv-platform
{{- end }}

{{/*
Selector labels
*/}}
{{- define "foundation-docs-website.selectorLabels" -}}
app.kubernetes.io/name: {{ include "foundation-docs-website.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "foundation-docs-website.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "foundation-docs-website.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Generate Nginx site configuration (only if not empty)
*/}}
{{- define "foundation-docs-website.siteConfig" -}}
{{- .Values.app.nginx.customConfig.siteConf }}
{{- end }}

{{/*
Generate Content Security Policy configuration (only if not empty)
*/}}
{{- define "foundation-docs-website.cspConfig" -}}
{{- .Values.app.nginx.customConfig.contentSecurityPolicyConf }}
{{- end }}

{{/*
Generate Security headers configuration (only if not empty)
*/}}
{{- define "foundation-docs-website.securityConfig" -}}
{{- .Values.app.nginx.customConfig.securityConf }}
{{- end }}

{{/*
Check if any nginx config overrides are needed
*/}}
{{- define "foundation-docs-website.hasNginxOverrides" -}}
{{- if or .Values.app.nginx.customConfig.siteConf .Values.app.nginx.customConfig.contentSecurityPolicyConf .Values.app.nginx.customConfig.securityConf }}
true
{{- else }}
false
{{- end }}
{{- end }}
