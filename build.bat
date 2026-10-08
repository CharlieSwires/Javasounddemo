@echo off
cd /d "%~dp0"
if not exist build mkdir build
javac -d build src\*.java
if errorlevel 1 exit /b 1
jar cfe JavaSoundDemo.jar JavaSound -C build .
