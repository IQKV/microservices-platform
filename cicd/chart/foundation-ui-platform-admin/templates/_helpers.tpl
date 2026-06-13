{{/*
Expand the name of the chart.
*/}}
{{- define "foundation-ui-platform-admin.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "foundation-ui-platform-admin.fullname" -}}
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
{{- define "foundation-ui-platform-admin.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "foundation-ui-platform-admin.labels" -}}
helm.sh/chart: {{ include "foundation-ui-platform-admin.chart" . }}
{{ include "foundation-ui-platform-admin.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/component: admin-dashboard-ui
app.kubernetes.io/part-of: iqkv-platform
{{- end }}

{{/*
Selector labels
*/}}
{{- define "foundation-ui-platform-admin.selectorLabels" -}}
app.kubernetes.io/name: {{ include "foundation-ui-platform-admin.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "foundation-ui-platform-admin.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "foundation-ui-platform-admin.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Generate runtime configuration JavaScript for the application
*/}}
{{- define "foundation-ui-platform-admin.runtimeConfig" -}}
// Runtime configuration injected via ConfigMap
// This file overrides build-time VITE_* environment variables

window.VITE_API_SERVER_URL = {{ .Values.app.env.apiServerUrl | quote }};

// Development configuration
window.VITE_ENABLE_MSW = {{ .Values.app.env.enableMsw | quote }};
window.VITE_LOG_LEVEL = {{ .Values.app.env.logLevel | quote }};
window.VITE_DEMO_MODE = {{ .Values.app.env.demoMode | quote }};
{{- end }}

{{/*
Generate Nginx site configuration (only if not empty)
*/}}
{{- define "foundation-ui-platform-admin.siteConfig" -}}
{{- .Values.app.nginx.customConfig.siteConf }}
{{- end }}

{{/*
Generate Content Security Policy configuration (only if not empty)
*/}}
{{- define "foundation-ui-platform-admin.cspConfig" -}}
{{- .Values.app.nginx.customConfig.contentSecurityPolicyConf }}
{{- end }}

{{/*
Generate Security headers configuration (only if not empty)
*/}}
{{- define "foundation-ui-platform-admin.securityConfig" -}}
{{- .Values.app.nginx.customConfig.securityConf }}
{{- end }}

{{/*
Check if any nginx config overrides are needed
*/}}
{{- define "foundation-ui-platform-admin.hasNginxOverrides" -}}
{{- if or .Values.app.nginx.customConfig.siteConf .Values.app.nginx.customConfig.contentSecurityPolicyConf .Values.app.nginx.customConfig.securityConf }}
true
{{- else }}
false
{{- end }}
{{- end }}

{{/*
Common environment variables for init container
*/}}
{{- define "foundation-ui-platform-admin.initEnv" -}}
- name: CONFIG_OUTPUT_PATH
  value: "/usr/share/nginx/html/config.js"
- name: CONFIG_TEMPLATE_PATH
  value: "/tmp/config-template/config.js"
{{- end }}
