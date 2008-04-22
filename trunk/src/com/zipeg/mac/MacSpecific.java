package com.zipeg.mac;

import com.zipeg.Zipeg;
import com.zipeg.Util;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.net.URL;

public final class MacSpecific {

    public static final int
    /*
        /System/Library/Frameworks/CoreServices.framework/Versions/A/Frameworks/CarbonCore.framework/Versions/A/Headers/Folders.h
    */
    // folderDomain:
    kOnSystemDisk                 = -32768, // previously was 0x8000 but that is an unsigned value whereas vRefNum is signed
    kOnAppropriateDisk            = -32767, // Generally, the same as kOnSystemDisk, but it's clearer that this isn't always the 'boot' disk.
                                            // Folder Domains - Carbon only.  The constants above can continue to be used, but the folder/volume returned will
                                            // be from one of the domains below.
    kSystemDomain                 = -32766, // Read-only system hierarchy.
    kLocalDomain                  = -32765, // All users of a single machine have access to these resources.
    kNetworkDomain                = -32764, // All users configured to use a common network server has access to these resources.
    kUserDomain                   = -32763, // Read/write. Resources that are private to the user.
    kClassicDomain                = -32762, // Domain referring to the currently configured Classic System Folder

    // folderType:

    kSystemFolderType             = fourCC("macs"), // the system folder
    kDesktopFolderType            = fourCC("desk"), // the desktop folder; objects in this folder show on the desk top.
    kSystemDesktopFolderType      = fourCC("sdsk"), // the desktop folder at the root of the hard drive), never the redirected user desktop folder
    kTrashFolderType              = fourCC("trsh"), // the trash folder; objects in this folder show up in the trash
    kSystemTrashFolderType        = fourCC("strs"), // the trash folder at the root of the drive), never the redirected user trash folder
    kWhereToEmptyTrashFolderType  = fourCC("empt"), // the "empty trash" folder; Finder starts empty from here down
    kPrintMonitorDocsFolderType   = fourCC("prnt"), // Print Monitor documents
    kStartupFolderType            = fourCC("strt"), // Finder objects (applications), documents), DAs), aliases), to...) to open at startup go here
    kShutdownFolderType           = fourCC("shdf"), // Finder objects (applications), documents), DAs), aliases), to...) to open at shutdown go here
    kAppleMenuFolderType          = fourCC("amnu"), // Finder objects to put into the Apple menu go here
    kControlPanelFolderType       = fourCC("ctrl"), // Control Panels go here (may contain INITs)
    kSystemControlPanelFolderType = fourCC("sctl"), // System control panels folder - never the redirected one), always "Control Panels" inside the System Folder
    kExtensionFolderType          = fourCC("extn"), // System extensions go here
    kFontsFolderType              = fourCC("font"), // Fonts go here
    kPreferencesFolderType        = fourCC("pref"), // preferences for applications go here
    kSystemPreferencesFolderType  = fourCC("sprf"), // System-type Preferences go here - this is always the system's preferences folder), never a logged in user's
                                                    //   On Mac OS X), items in the temporary items folder on the boot volume will be deleted a certain amount of time after their
                                                    //    last access.  On non-boot volumes), items in the temporary items folder may never get deleted.  Thus), the use of the
                                                    //    temporary items folder on Mac OS X is discouraged), especially for long lived data.  Using this folder temporarily ( like
                                                    //    to write a temporary copy of a document to during a save), after which you FSpExchangeFiles() to swap the new contents with
                                                    //    the old version ) is certainly ok), but using the temporary items folder to cache data is not a good idea.  Instead), look
                                                    //    at tmpfile() and its cousins for a better way to do this kind of thing.  On Mac OS X 10.4 and later), this folder is inside a
                                                    //    folder named ".TemporaryItems" and in earlier versions of Mac OS X this folder is inside a folder named "Temporary Items".
                                                    //    On Mac OS 9.x), items in the the Temporary Items folder are never automatically deleted.  Instead), when a 9.x machine boots
                                                    //    up the temporary items folder on a volume ( if one still exists), and is not empty ) is moved into the trash folder on the
                                                    //    same volume and renamed "Rescued Items from <diskname>".
    kTemporaryFolderType          = fourCC("temp")  // temporary files go here (deleted periodically), but don't rely on it.)
    ;
    private static Object sharedWorkspace; // com.apple.cocoa.application.NSWorkspace.sharedWorkspace

    public static void initMacOSXInterface() {
        com.apple.eawt.Application.getApplication().setEnabledPreferencesMenu(true);
        com.apple.eawt.Application.getApplication().setEnabledAboutMenu(true);
        com.apple.eawt.Application.getApplication().addApplicationListener(
            new com.apple.eawt.ApplicationListener() {

                public void handleAbout(com.apple.eawt.ApplicationEvent e) {
                    Zipeg.commandHelpAbout();
                    e.setHandled(true);
                }

                public void handleQuit(com.apple.eawt.ApplicationEvent e) {
                    e.setHandled(true); // tell Mac OS X that we are OK to quit
                    Zipeg.commandFileExit();
                }

                public void handlePreferences(com.apple.eawt.ApplicationEvent e) {
                    Zipeg.commandToolsOptions();
                    e.setHandled(true);
                }

                public void handleOpenApplication(com.apple.eawt.ApplicationEvent e) {
                    Zipeg.commandOpenAppication();
                }

                public void handleOpenFile(com.apple.eawt.ApplicationEvent e) {
                    Zipeg.commandFileOpen(e.getFilename());
                }

                public void handlePrintFile(com.apple.eawt.ApplicationEvent e) {
                    Zipeg.commandFilePrint(e.getFilename());
                }

                public void handleReOpenApplication(com.apple.eawt.ApplicationEvent e) {
                    Zipeg.commandReOpenApplication();
                }

            }
        );

        try {
            Util.class.getClassLoader().loadClass("com.apple.cocoa.application.NSWorkspace");
            sharedWorkspace = Util.callStatic("com.apple.cocoa.application.NSWorkspace.sharedWorkspace", Util.NONE);
        } catch (Throwable t) {
            try {
                if (new File("/System/Library/Java/com/apple/cocoa/application/NSWorkspace.class").exists()) {
                    ClassLoader classLoader = new URLClassLoader(new URL[]{new File("/System/Library/Java").toURL()});
                    Class c = classLoader.loadClass("com.apple.cocoa.application.NSWorkspace");
                    // do not use Util.getDeclaredMethod because of custom classloader
                    Method m = c.getMethod("sharedWorkspace", Util.VOID);
                    sharedWorkspace = m.invoke(null, Util.NONE);
//                  Debug.traceln("sharedWorkspace=" + sharedWorkspace);
                }
            } catch (Throwable ignore) {
                /* ignore */
            }
        }

    }

    // There is no known way to disable Quit menu item on Mac OS X

    public static void enable() {
        com.apple.eawt.Application.getApplication().setEnabledPreferencesMenu(true);
        com.apple.eawt.Application.getApplication().setEnabledAboutMenu(true);
    }

    public static void disable() {
        com.apple.eawt.Application.getApplication().setEnabledPreferencesMenu(false);
        com.apple.eawt.Application.getApplication().setEnabledAboutMenu(false);
    }

    public static File getTempFolder() throws FileNotFoundException {
        return new File(findFolder(kUserDomain, kTemporaryFolderType, true));
    }

    public static File getDeskFolder() throws FileNotFoundException {
        return new File(findFolder(kUserDomain, kDesktopFolderType, false));
    }

    public static String findFolder(int folderDomain, int folderType, boolean create) throws FileNotFoundException {
        return com.apple.eio.FileManager.findFolder((short)folderDomain, folderType, create);
    }

    public static int fourCC(String s) {
        // should fourCC work in reverse on PPC? Hell if I know!
        assert s.length() == 4;
        long cc4 = 0;
        for (int i = 0; i < s.length(); i++) {
            cc4 = (cc4 << 8) | (s.charAt(i) & 0xFF);
        }
        return (int)cc4;
    }

    public static String getCocoaApplicationForFile(String file) {
        assert Util.isMac();
        try {
            if (sharedWorkspace != null) {
                // do not use Util.getDeclaredMethod because of custom classloader
                Method applicationForFile = sharedWorkspace.getClass().getMethod("applicationForFile", Util.STRING);
                return (String)Util.call(applicationForFile, sharedWorkspace, new Object[]{file});
            }
        } catch (Throwable t) {
            /* ignore */
        }
        return null;
    }
}

