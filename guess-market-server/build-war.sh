#!/bin/bash
set -e

cd "/c/Users/navek/GitHub projects/GuessMarket"
ROOT_WIN="C:/Users/navek/GitHub projects/GuessMarket"
OUT="guess-market-server/build"
OUT_WIN="$ROOT_WIN/guess-market-server/build"
WAR_NAME="guessmarket"

rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/lib"

CP="$ROOT_WIN/apache-tomcat-10.1.26/lib/servlet-api.jar"
CP="$CP;$ROOT_WIN/jaxb-ri-4.0.5/mod/jakarta.xml.bind-api.jar"
CP="$CP;$ROOT_WIN/jaxb-ri-4.0.5/mod/jaxb-core.jar"
CP="$CP;$ROOT_WIN/jaxb-ri-4.0.5/mod/jaxb-impl.jar"
CP="$CP;$ROOT_WIN/jaxb-ri-4.0.5/mod/angus-activation.jar"
CP="$CP;$ROOT_WIN/jaxb-ri-4.0.5/mod/jakarta.activation-api.jar"
CP="$CP;$ROOT_WIN/gson-2.11.0/gson-2.11.0.jar"

find "guess-market-dto/src" -name "*.java" > "$OUT/sources.txt"
find "guess-market-engine/src" -name "*.java" >> "$OUT/sources.txt"
find "guess-market-server/src" -name "*.java" >> "$OUT/sources.txt"

javac -encoding UTF-8 -d "$OUT_WIN/classes" -cp "$CP" @"$OUT/sources.txt"

cp "jaxb-ri-4.0.5/mod/jakarta.xml.bind-api.jar" "$OUT/lib/"
cp "jaxb-ri-4.0.5/mod/jaxb-core.jar" "$OUT/lib/"
cp "jaxb-ri-4.0.5/mod/jaxb-impl.jar" "$OUT/lib/"
cp "jaxb-ri-4.0.5/mod/angus-activation.jar" "$OUT/lib/"
cp "jaxb-ri-4.0.5/mod/jakarta.activation-api.jar" "$OUT/lib/"
cp "gson-2.11.0/gson-2.11.0.jar" "$OUT/lib/"

WAR_ROOT="$OUT/war-root"
rm -rf "$WAR_ROOT"
mkdir -p "$WAR_ROOT/WEB-INF/classes" "$WAR_ROOT/WEB-INF/lib"
cp -r "$OUT/classes/." "$WAR_ROOT/WEB-INF/classes/"
cp "$OUT/lib/"*.jar "$WAR_ROOT/WEB-INF/lib/"

(cd "$WAR_ROOT" && jar -cf "../$WAR_NAME.war" .)

echo "Built: $OUT/$WAR_NAME.war"
