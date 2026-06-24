{{- define "gateway-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "gateway-service.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name (include "gateway-service.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{- define "gateway-service.labels" -}}
app: {{ include "gateway-service.fullname" . }}
{{- end }}
