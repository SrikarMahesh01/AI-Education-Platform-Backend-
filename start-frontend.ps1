Set-Location "$PSScriptRoot\AI-Education-Platform-Frontend-"

# MYSQL_USER and MYSQL_PASSWORD already have defaults in application.properties.
# Override them here only if you need different credentials.
# $env:MYSQL_USER = "root"
# $env:MYSQL_PASSWORD = "system"

.\mvnw.cmd spring-boot:run
