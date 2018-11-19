#!/bin/bash
#
# A script to install the Door Gui
# Written by Rich Rademacher, IQC/UWaterloo

PROGRAM=`basename $0`
FULLPATH=`realpath $0`
THISPATH=`dirname ${FULLPATH}`

# check args
if [ "$#" -eq 0 ] ; then
	echo "Usage:  ${PROGRAM} master | listener"
	exit 1
fi

if [ "$1" = "master" ] ; then
	SHORTCUT=DoorGUI_Master.desktop	
elif [ "$1" = "listener" ] ; then
	SHORTCUT=DoorGUI.desktop	
else
	echo "Must be either master or listener.  Exit."
	exit 1
fi	

echo "Create autostart directory..."
mkdir ~/.config/autostart || echo "  Autostart directory exists. Skipping"

echo "Create link to autostart script..."
ln -f -s ${THISPATH}/${SHORTCUT} ~/.config/autostart/DoorGUI_Autostart.desktop || echo "  Link to autostart program exists. Skipping"
echo "Done!...Reboot now"
