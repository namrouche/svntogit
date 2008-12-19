#!/bin/bash

echo "Create nxOOo directory"
mkdir nxOOo


echo "Extract OOo archive"
tar -zxvf OOo_2.2.0_LinuxIntel_install_fr_deb.tar.gz 


echo "Remove unneeded packages"
rm DEBS/openoffice.org-debian-menus_*.deb
rm DEBS/openoffice.org-gnome-integration_*.deb
rm DEBS/openoffice.org-kde-integration_*.deb


echo "Expand OOo packages"
cd DEBS
for aFile in *.deb;do dpkg --extract $aFile ./OOo2.2;done


echo "Retreive Expanded OOo" 
cd OOo2.2/opt/openoffice.org2.2/ 
mv * ../../../../nxOOo/
cd ../../../../


echo "Apply Patch : bootstraprc"
chmod -R u+w  nxOOo
cp patch/bootstraprc nxOOo/program/


echo "Apply extension : nxSkipInstallWizard"
./nxOOo/program/unopkg add --shared patch/nxSkipInstallWizard.oxt


echo "Apply extension : nxOOoAutoListen"
./nxOOo/program/unopkg add --shared patch/nxOOoAutoListen.oxt


echo
echo "done !!"
echo "nxOOo/program/soffice to launch OOo"
echo

