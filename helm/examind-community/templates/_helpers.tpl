{{/*
All in one deployment name
*/}}
{{- define "examind.all-in-one.fullname" }}
    {{- include "common.names.fullname" . }}-all-in-one
{{- end }}

{{/*
Kubernetes component name to set in labels
*/}}
{{- define "examind.component" -}}
{{ printf "%s-%s" "examind" (default "toto" .Values.edition) }}
{{- end }}

{{/*
Full JDBC URL to use to connect Examind to its administration database
*/}}
{{- define "examind.backend.jdbcUrl" -}}
    {{ printf "jdbc:postgresql://%s:%s/%s"
              (include "examind.backend.databaseHost" $)
              (include "examind.backend.databasePort" $)
              (include "examind.backend.databaseName" $)
    }}
{{- end }}

{{/*
Examind administration database host
*/}}
{{- define "examind.backend.databaseHost" -}}
    {{- if .Values.postgresql.enabled }}
        {{- if eq .Values.postgresql.architecture "replication" }}
            {{- include "examind.postgresql.fullname" . -}}-primary
        {{- else }}
            {{- include "examind.postgresql.fullname" . -}}
        {{- end -}}
    {{- else }}
        {{- required "External database host is missing" (tpl .Values.externalDatabase.host $) }}
    {{- end }}
{{- end }}

{{- define "examind.backend.databasePort" -}}
    {{- .Values.postgresql.enabled | ternary "5432" .Values.externalDatabase.port -}}
{{- end }}

{{/*
Return the Database database name
*/}}
{{- define "examind.backend.databaseName" -}}
    {{- if .Values.postgresql.enabled }}
        {{- coalesce
           .Values.postgresql.auth.database
           (((.Values.global).postgresql).auth).database
           | required "Examind database name is missing" }}
    {{- else -}}
        {{- required "Examind external database name is missing" .Values.externalDatabase.database -}}
    {{- end -}}
{{- end -}}

{{/*
Return the name of the secret that holds database credentials
*/}}
{{- define "examind.backend.databaseSecretName" -}}
{{- if .Values.postgresql.enabled -}}
    {{- if (((.Values.global).postgresql).auth).existingSecret -}}
        {{- tpl .Values.global.postgresql.auth.existingSecret $ -}}
    {{- else if (.Values.postgresql.auth).existingSecret }}
        {{- tpl .Values.postgresql.auth.existingSecret $ -}}
    {{- else }}
        {{- (include "examind.postgresql.fullname" .) }}
    {{- end }}
{{- else -}}
    {{- (tpl .Values.externalDatabase.existingSecret $) | default (printf "%s-externaldb" (include "common.names.fullname" .)) -}}
{{- end -}}
{{- end -}}

{{/*
Return the name of the secret that holds database credentials
*/}}
{{- define "examind.backend.databaseSecretUsernameKey" -}}
{{/*
IMPORTANT: Bitnami Postgres chart does NOT store username in a secret.
Therefore, we return an empty key, meaning that target template must use the inline username value instead.
*/}}
{{- if .Values.postgresql.enabled -}}
    {{- "" -}}
{{- else -}}
    {{- if and .Values.externalDatabase.existingSecret .Values.externalDatabase.existingSecretUserKey -}}
        {{- .Values.externalDatabase.existingSecretUserKey -}}
    {{- else -}}
        {{- print "DATABASE_USERNAME" -}}
    {{- end -}}
{{- end -}}
{{- end -}}

{{/*
Return the key of the variable holding password value in database credentials secret
*/}}
{{- define "examind.backend.databaseSecretPasswordKey" -}}
{{- if .Values.postgresql.enabled -}}
    {{- .Values.postgresql.auth.secretKeys.userPasswordKey | default "password" -}}
{{- else -}}
    {{- if and .Values.externalDatabase.existingSecret .Values.externalDatabase.existingSecretUserKey -}}
        {{- .Values.externalDatabase.existingSecretUserKey -}}
    {{- else -}}
        {{- print "DATABASE_USERNAME" -}}
    {{- end -}}
{{- end -}}
{{- end -}}

{{/*
Craft a public URL for exaind by looking at its ingress configuration
*/}}
{{- define "examind.all-in-one.publicUrl" -}}
{{- $host := first (coalesce (.Values.allInOne.ingress).hosts ((.Values.global).ingress).hosts (list "")) }}
{{- if $host }}
  {{- $tls := coalesce (.Values.allInOne.ingress).tls ((.Values.global).ingress).tls }}
  {{- printf "%s://%s/%s" (empty $tls | ternary "http" "https") (trimAll "/" $host) (trimPrefix "/" (.Values.allInOne.ingress.path | default "/examind")) }}
{{- else }}
  {{- "" }}
{{- end }}
{{- end }}

{{/*
Get deployed postgresql full name. Useful to find PostgreSQL internal host name.
*/}}
{{- define "examind.postgresql.fullname" -}}
{{- include "common.names.dependency.fullname" (dict "chartName" "postgresql" "chartValues" .Values.postgresql "context" $) -}}
{{- end -}}
