#!/bin/bash

# 
# starter script to launch trappytown door sign

# build stuff

THISPATH=`dirname $0`

SRCFILE=${THISPATH}/TrappyTownDoorSign.java
OBJFILE=TrappyTownDoorSign.class
CLASSPATH="${THISPATH}:.:/opt/pi4j/lib/pi4j-core.jar"

# set display variable to show on local screen
export DISPLAY=:0.0

# check for auto-rebuild
if [ ! -f "$OBJFILE" ] || [ "$SRCFILE" -nt "$OBJFILE" ] ; then
	echo "Rebuilding..."
	javac -cp ${CLASSPATH} $SRCFILE
fi

# run
echo "Running"
java -cp ${CLASSPATH} TrappyTownDoorSign
