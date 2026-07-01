{{- define "prometheus.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "prometheus.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name (include "prometheus.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{- define "prometheus.labels" -}}
app: {{ include "prometheus.fullname" . }}
{{- end }}
