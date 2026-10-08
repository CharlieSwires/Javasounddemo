@echo off
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
javac -cp build -d build tests\SampleAudioTest.java
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp build SampleAudioTest
