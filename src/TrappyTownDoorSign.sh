#!/bin/bash

# 
# starter script to launch trappytown door sign

# build stuff

SRCFILE=TrappyTownDoorSign.java
OBJFILE=TrappyTownDoorSign.class
CLASSPATH=".:/opt/pi4j/lib/pi4j-core.jar"

if [ ! -f "$OBJFILE" ] || [ "$SRCFILE" -nt "$OBJFILE" ] ; then
	echo "Rebuilding..."
	javac -cp ${CLASSPATH} $SRCFILE
fi

echo "Running"
java -cp ${CLASSPATH} TrappyTownDoorSign
