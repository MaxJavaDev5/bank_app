{{- define "elk.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "elk.elasticsearch.fullname" -}}
{{- default "elasticsearch" .Values.elasticsearch.name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "elk.logstash.fullname" -}}
{{- default "logstash" .Values.logstash.name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "elk.kibana.fullname" -}}
{{- default "kibana" .Values.kibana.name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "elk.elasticsearch.labels" -}}
app: {{ include "elk.elasticsearch.fullname" . }}
component: elasticsearch
{{- end }}

{{- define "elk.logstash.labels" -}}
app: {{ include "elk.logstash.fullname" . }}
component: logstash
{{- end }}

{{- define "elk.kibana.labels" -}}
app: {{ include "elk.kibana.fullname" . }}
component: kibana
{{- end }}
