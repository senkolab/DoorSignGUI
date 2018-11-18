#!/bin/bash
export DISPLAY=:0.0

# create white background image
convert -size 320x240 xc:white base.jpg

# create IP image
IP=`ip -4 a s eth0 | grep -Eo 'inet [0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' | awk '{print $2}'`

convert base.jpg -pointsize 50 -fill black -draw "text 0,150 '${IP}'" ip.jpg
