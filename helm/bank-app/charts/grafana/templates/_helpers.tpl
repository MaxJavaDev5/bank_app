{{- define "grafana.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "grafana.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name (include "grafana.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{- define "grafana.labels" -}}
app: {{ include "grafana.fullname" . }}
{{- end }}
