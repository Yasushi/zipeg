Zipeg - open source freeware decompression UI front end for p7zip engine for Macintosh and Windows. Implemented in java.

&lt;wiki:gadget url="http://hosting.gmodules.com/ig/gadgets/file/110509162544058635853/steegle-google-sites-facebook-like-button.xml" height="200" height="50" border="0" /&gt;



Stable versions are on: http://www.zipeg.com

Zipeg - 2.9.4 release available for download.

![http://www.zipeg.com/img/zipeg.oth.w400px.jpg](http://www.zipeg.com/img/zipeg.oth.w400px.jpg)

Fixed issues found in 2.9.3 release:

  * frame is decorated exception in Java 7u5
  * fix for putenv PATH for JRE 1.7.0\_05
  * removed obsolete workaround for IBM encoding
  * fix for StackOverflowError while rendering MessageBox text
  * corrected some spelling typos
  * fixed NullPointerException DirectoryTree.isLastFocused
  * made sure license is accepted before opening first archive
  * fixed AssertionError at ArchiveProcessor.doWork
  * fixed NullPointerException at AutoCompleteDropDown.setFilter
  * fixed NullPointerException in InputBlocker.getContentPane
  * improved protection against OutOfMemory errors
  * fixed NullPointerException at Zipeg.archiveOpened
  * fixed NullPointerException Zipeg.extractList
  * fixed NullPointerException in Registry.createShellCommands
  * fixed Error: Unable to instantiate CImage with null native image reference.
  * fixed AssertionError in MultipartHeuristics.checkMultipart
  * improved Win32ShellFolderManager6372808$MyInvoker.invoke robustness

Fixed issues found in 2.9.2 release:

This update provides a number of important bug fixes, including:

  * NoClassDefFoundError for ShellFolder Invoker
  * java 1.5 compatible DnD filters.
  * workaround for Oracle [bug 4673161](https://code.google.com/p/zipeg/issues/detail?id=673161) JFileChooser throws IOException
  * command Window Minimize and Zoom for Mac
  * fixed null pointer exception in InputBlocker getGlassPane()
  * crash logs improvements
  * protected against accidental overwrite of system folders
  * added warning for folder overwrite. fixed extracting empty folders
  * fixed resource forks processing
  * implemented protection agains clicking multiple times to open the same archive.
  * fixed flickering of toolbar window dragging is in action
  * workaround for file open blues
  * fix for exceptions due to late invocations, focus changes and progress reporting
  * increased memory limit to 1GB
  * fixed extract to cache when destination and temp files are the same
  * fixed some issues with message boxes

Please report bugs to [support@zipeg.com](mailto:support@zipeg.com)

SVN contains abridged stable snapshots.

&lt;wiki:gadget url="http://www.ohloh.net/p/13455/widgets/project\_cocomo.xml" width="400" height="240" border="0"/&gt;