#!/bin/sh
MYSELF=`which "$0" 2>/dev/null`
[ $? -gt 0 -a -f "$0" ] && MYSELF="./$0"
java=java
if test -n "$JAVA_HOME"; then
    java="$JAVA_HOME/bin/java"
fi
# Suppress sun.misc.Unsafe deprecation warnings from Scala on Java 23+
if "$java" --sun-misc-unsafe-memory-access=allow -version >/dev/null 2>&1; then
    JAVA_ARGS="--sun-misc-unsafe-memory-access=allow $JAVA_ARGS"
fi
exec "$java" $JAVA_ARGS -jar $MYSELF "$@"
exit 1 
