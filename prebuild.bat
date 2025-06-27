@echo off
echo [1/2] Generating BuildInfo.java...
javac src\main\java\version\GenerateBuildInfo.java
java -cp src\main\java version.GenerateBuildInfo

echo [2/2] Building with Maven...
mvn clean install

