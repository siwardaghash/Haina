#!/bin/sh
# Builds the runnable file dist/Haina.jar (SRS DC-6) with JDK 19.
# Run from Terminal:  sh build-jar.sh
# The encrypted starting question bank is packed inside the JAR; on the first start the game
# creates a "data" folder next to the JAR (history, questions) when there is none.
set -e
cd "$(dirname "$0")"

if [ -z "$JAVA_HOME" ]; then JAVA_HOME=$(/usr/libexec/java_home -v 19 2>/dev/null || true); fi
if [ -z "$JAVA_HOME" ]; then echo "JDK 19 not found: set JAVA_HOME to your JDK 19 folder"; exit 1; fi
if [ ! -f data/questions.dat ]; then echo "data/questions.dat is missing: start the game once from Eclipse first"; exit 1; fi

rm -rf build
mkdir -p build/classes/defaults dist
"$JAVA_HOME/bin/javac" --release 19 -encoding UTF-8 -d build/classes $(find src -name '*.java')
cp data/questions.dat build/classes/defaults/questions.dat
printf 'Main-Class: controller.Main\n' > build/manifest.txt
"$JAVA_HOME/bin/jar" cfm dist/Haina.jar build/manifest.txt -C build/classes .
rm -rf build
echo "Built dist/Haina.jar"
