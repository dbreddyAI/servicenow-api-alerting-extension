@echo off
..\..\..\jre\bin\java -Dlog4j.configuration=file:conf/log4j.xml -jar servicenow-api-alert.jar %*