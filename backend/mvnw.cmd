@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Apache Maven Wrapper startup script, version 3.3.2 (Windows)

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0

set MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties
set MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar

@REM Read distributionUrl from wrapper properties
for /f "tokens=2 delims==" %%a in ('findstr /i "distributionUrl" "%MAVEN_WRAPPER_PROPERTIES%"') do set DISTRIBUTION_URL=%%a

@REM Resolve cache dir
if "%MAVEN_USER_HOME%"=="" set MAVEN_USER_HOME=%USERPROFILE%
set MAVEN_CACHE_DIR=%MAVEN_USER_HOME%\.m2\wrapper

@REM Extract filename and version
for %%f in ("%DISTRIBUTION_URL%") do set DIST_FILENAME=%%~nxf
set DIST_NAME=%DIST_FILENAME:-bin.zip=%
set MAVEN_HOME=%MAVEN_CACHE_DIR%\dists\%DIST_NAME%

@REM Download and extract if not cached
if not exist "%MAVEN_HOME%" (
    if not exist "%MAVEN_CACHE_DIR%\dists" mkdir "%MAVEN_CACHE_DIR%\dists"
    set DIST_ARCHIVE=%MAVEN_CACHE_DIR%\%DIST_FILENAME%

    if not exist "%DIST_ARCHIVE%" (
        echo Downloading Maven from %DISTRIBUTION_URL%
        powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%DIST_ARCHIVE%'"
    )

    echo Extracting Maven distribution
    powershell -Command "Expand-Archive -Path '%DIST_ARCHIVE%' -DestinationPath '%MAVEN_CACHE_DIR%\dists' -Force"

    for /d %%d in ("%MAVEN_CACHE_DIR%\dists\apache-maven-*") do set MAVEN_HOME=%%d
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
endlocal
