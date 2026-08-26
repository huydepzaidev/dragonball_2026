@ECHO OFF
cd /d "%~dp0"
java -server -Dfile.encoding=UTF-8 -jar dist\NgocRongOnline.jar
PAUSE
