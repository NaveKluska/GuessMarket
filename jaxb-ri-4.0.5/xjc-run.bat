@echo off

rem Define the classpath to include all necessary JAR files
set CLASSPATH=mod\jaxb-xjc.jar;mod\jaxb-core.jar;mod\jakarta.xml.bind-api.jar;mod\jaxb-impl.jar

rem Run the xjc tool
java -cp "%CLASSPATH%" com.sun.tools.xjc.Driver %*