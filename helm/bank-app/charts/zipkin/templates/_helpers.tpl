{{- define "zipkin.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "zipkin.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name (include "zipkin.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{- define "zipkin.labels" -}}
app: {{ include "zipkin.fullname" . }}
{{- end }}
