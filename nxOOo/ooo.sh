#!/bin/sh
Xvfb :77 -auth Xperm -screen 0 1024x768x24 &
export DISPLAY=":77.0"
/opt/openoffice.org2.2/program/soffice -headless -nofirststartwizard
