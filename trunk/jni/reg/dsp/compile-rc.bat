echo on
svn up ../../..
rm -rf %3win32reg.rc.rev
subwcrev.exe ../../.. %3win32reg.rc %3win32reg.rc.rev 
rc -i "../src" -l 0x409 /fo"%1" /d "%2" %3win32reg.rc.rev
