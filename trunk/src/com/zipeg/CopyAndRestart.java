package com.zipeg;

import javax.swing.*;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.Enumeration;

public class CopyAndRestart {

    public static void main(String[] args) {
        boolean isMac = System.getProperty("os.name").toLowerCase().indexOf("mac os x") >= 0;
        assert isMac;
        int retry = 4;
        while (retry > 0) {
            retry--;
            try {
                File wd = new File(new File(".").getCanonicalPath());
                File src = new File(args[0]);
                File dst = new File(args[1]);
//              File java = new File(args[2]); // no longer needed
/*
                JOptionPane.showConfirmDialog(null, "CopyAndRestart\n" + src + "\n" + dst +
                                                "\n" + src.exists() +
                                                "\n" + java +
                                                "\n" + wd.getCanonicalPath());
*/
                Thread.sleep(2 * 1000);
                copyFiles(src, dst);
//              JOptionPane.showConfirmDialog(null, "CopyAndRestart: open -a Zipeg.app\n");
                Runtime.getRuntime().exec(new String[]{"open", "-a", "Zipeg.app"});
                src.deleteOnExit();
                new File(wd, "com/zipeg/CopyAndRestart.class").deleteOnExit();
                new File(wd, "com/zipeg").deleteOnExit();
                System.exit(0);
            } catch (Exception e) {
                if (retry == 0) {
                    JOptionPane.showConfirmDialog(null, e.getMessage(), "Zipeg: Update Error",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        }
    }

    private static void copyFiles(File from, File to) throws IOException {
        assert to.isDirectory();
        ZipFile zip = new ZipFile(from);
        for (Enumeration i = zip.entries(); i.hasMoreElements();) {
            ZipEntry e = (ZipEntry)i.nextElement();
            InputStream in = zip.getInputStream(e);
            File dst = new File(to, e.getName());
            dst.delete();
            FileOutputStream out = new FileOutputStream(dst);
/*
            JOptionPane.showConfirmDialog(null, "CopyAndRestart\n" + e.getName() + "\n" + dst +
            "\n" + "e.csize() " + e.getCompressedSize() +
                   " e.size() " + e.getSize() + " in.available() " + in.available());
*/
            assert(in.available() > 0);
            // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4818580
            // this is why there is no loop here...            
            copyStream(in, new FileOutputStream(dst));
            in.close();
            out.close();
            Runtime.getRuntime().exec(new String[]{"chmod", "755", dst.getCanonicalPath()});
        }
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16*1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
    }

}
