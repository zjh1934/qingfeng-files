#!/bin/sh
# Gradle start up script for POSIX (骨架版：首次运行会自动下载 gradle 8.7)
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then PRG="$link"; else PRG=`dirname "$PRG"`"/$link"; fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
if [ ! -f "$CLASSPATH" ] ; then
  echo "gradle-wrapper.jar 缺失，正在下载 gradle 8.7 ..."
  curl -sSL -o /tmp/gradle-8.7-bin.zip https://services.gradle.org/distributions/gradle-8.7-bin.zip
  unzip -q -o /tmp/gradle-8.7-bin.zip -d /tmp
  exec /tmp/gradle-8.7/bin/gradle "$@"
fi
exec java $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
