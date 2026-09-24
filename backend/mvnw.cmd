@echo off
set "MAVEN_CMD=C:\Users\shrip\.maven\apache-maven-3.9.6\bin\mvn.cmd"
if exist "%MAVEN_CMD%" (
    "%MAVEN_CMD%" %*
) else (
    mvn %*
)
