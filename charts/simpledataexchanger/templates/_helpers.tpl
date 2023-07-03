{{/*
Expand the name of the chart.
*/}}
{{- define "sde.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "sde.fullname" -}}
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
{{- define "sde.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "sde.labels" -}}
helm.sh/chart: {{ include "sde.chart" . }}
{{ include "sde.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
backend Common labels
*/}}
{{- define "sde.backend.labels" -}}
helm.sh/chart: {{ include "sde.chart" . }}
{{ include "sde.backend.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
frontend Common labels
*/}}
{{- define "sde.frontend.labels" -}}
helm.sh/chart: {{ include "sde.chart" . }}
{{ include "sde.frontend.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "sde.selectorLabels" -}}
app.kubernetes.io/name: {{ include "sde.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
backend Selector labels
*/}}
{{- define "sde.backend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "sde.name" . }}-backend
app.kubernetes.io/instance: {{ .Release.Name }}-backend
{{- end }}

{{/*
frontend Selector labels
*/}}
{{- define "sde.frontend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "sde.name" . }}-frontend
app.kubernetes.io/instance: {{ .Release.Name }}-frontend
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "sde.backend.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "sde.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "sde.frontend.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "sde.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "sde.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "sde.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}