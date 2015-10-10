## How to install Zipeg ##

  * On Windows: just download and run installer.

  * On Macintosh OS X: Safari will usually unpack zipeg.dmg file into Zipeg.app on your desktop (unless you configured it to do otherwise). Firefox will just download Zipeg.dmg and you will have to double click on it to "mount" it. In any case drag Zipeg.app into your Applications folder. If you do not have administrative privileges on your system it is recommended to create /Users/you\_user\_name/Application folder (or alike) and keep your applications there.

  * Extra steps for OS X Mountain Lion and above
    1. In System Preferences Security & Privacy (click on the lock icon if "Anywhere" is dimmed). Choose "Anywhere" before running Zipeg first time.
    1. You may need to download and install 32-bit Java for your Mac OS X 10.8 from here:
> > > http://support.apple.com/kb/DL1572

## How to uninstall Zipeg ##

  * on Windows 2000/2003/XP: use Control Panel / Add Remove Programs / Zipeg.
  * on Windows Vista use: Control Panel / Programs / Installed Programs / Change Remove.
  * on Macintosh - just delete Zipeg.app from your Applications folder

It is good idea to use Zipeg Options /Settings or Zipeg / Preferences to disassociate Zipeg from any archives it handles before uninstalling it. It will restore the file extension bindings that existed on your computer prior to Zipeg installation.


## How to open multi-part archive? or ("How do I open folder with RAR/Zip files?") ##

Multi-part (aka multi-volume) archives are usually spread over sequence of files like:
```
    file.part1.rar
    file.part2.rar
    ....
    file.part9.rar
```

or:
```
    file.zip
    file.001
    ....
    file.009
```
Just open first file in Zipeg - other files will be picked up automatically.

## How to delete files from archive ##

Sorry, no easy way here. Looks like you need 7-zip - it is excellent and much more powerful than Zipeg - also free.

Workaround: extract all the files to a folder, delete unwanted files and right mouse (Windows) or control click (Mac) the folder and chose Send To / Compressed Folder (Windows) or Create Archive (Mac).

## "What is the password" ##

"swordfish"? just kidding. I do not know. The password is assigned to the archive by the person who created it. Contact that person and s/he will tell you. Zipeg is not password breaking/guessing tool and both ZIP and RAR use 256 bits AES encryption nowadays which is rather strong.

## CRC error? ##

There are multiple reasons for possible problems with opening or extracting files from archives. Most common are:
  * Archive was corrupted in transit. Try to download archive again.
  * Archive is password protected and password you supplied is incorrect.
  * The format of the archive is not supported by zipeg.
  * Passwords are case sensitive (check if CAPS LOCK is locked).
  * If you believe that the issue is in Zipeg - please let me know I will try to investigate and fix it.

For password protected archives please contact the person who created the archive and request correct password.

## Why Zipeg does not create new archives? ##

Isn't there enough archives already created?

More seriously:

On Windows right mouse click on file or folder and choose Send To / Compressed Folder.

On Macintosh Ctrl click on file or folder and choose Create Archive.

WARNING: Mac OSX BOMArchiveHelper (produced by Apple) may take very long time to create an archive. Make sure you wait long enough for the archiving to be complete before attempting to open or backup archived file. Otherwise you may endup with half completed/corrupted archive.

If this is not what you want need there are other ways:
**On Windows: I recommend 7zip which is excellent.** On Macintosh: there are several free tools that do create archives.

Zipeg is **deliberately** simple and unsophisticated application to help user who almost never create archives via UI tools in their everyday life. I am one of them, yours truly. All the archives I ever created I created with command line utilities from my software build scripts.

## Does Zipeg support command line? ##

There is a project called p7zip that is portable on many many platforms. P7Zip produces command line application "7za" which is much more suitable for command line execution. To my knowledge it work on Windows, Mac all Linuxes and even more. Google it.