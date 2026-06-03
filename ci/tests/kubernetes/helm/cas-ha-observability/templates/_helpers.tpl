{{- define "cas-ha-observability.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "cas-ha-observability.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "cas-ha-observability.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "cas-ha-observability.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
app.kubernetes.io/name: {{ include "cas-ha-observability.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "cas-ha-observability.selectorLabels" -}}
app.kubernetes.io/name: {{ include "cas-ha-observability.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "cas-ha-observability.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "cas-ha-observability.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{- define "cas-ha-observability.hazelcastMembers" -}}
{{- $fullName := include "cas-ha-observability.fullname" . -}}
{{- $replicas := int .Values.replicaCount -}}
{{- range $index, $_ := until $replicas -}}
{{- if gt $index 0 }},{{ end -}}{{ printf "%s-%d.%s-hazelcast-headless:%v" $fullName $index $fullName $.Values.service.hazelcastPort }}
{{- end -}}
{{- end -}}
