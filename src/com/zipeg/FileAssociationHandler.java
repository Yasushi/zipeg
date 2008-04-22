package com.zipeg;

import java.util.Map;
import java.util.HashMap;

public interface FileAssociationHandler {

    /* Other known mac uti:
        com.sun.java-archive
        com.apple.bom-compressed-cpio
        public.iso-diskimage
        org.debian.deb-archive
        org.7-zip.nsis-archive
        cx.c3.lha-archive

     */

    static final Map aliases = new HashMap() {{
        put("public.zip-archive", new String[]{"com.pkware.zip-archive",
                "com.macitbetter.segmented-archive",
                "com.macitbetter.zip-archive"});
        put("com.public.cbz-archive", new String[]{"com.macitbetter.cbz-archive"});
        put("public.archive.bzip2", new String[]{"org.bzip.bzip2-archive", "org.bzip.bzip2-tar-archive"});
        put("public.arj-archive", new String[]{"org.7-zip.arj-archive"});
        put("public.cpio-archive", new String[]{"com.apple.bom-compressed-cpio"});
        put("public.lzh-archive", new String[]{"public.archive.lha", "org.7-zip.lha-archive"});
    }};

    static final Object[][] ext2uti = new Object[][] {
            new Object[] {
                    "zip", "ZIP Archive", "http://en.wikipedia.org/wiki/Zip_%28file_format%29",
                    "public.zip-archive",
                    "application/zip", "application/x-zip", "application/x-zip-compressed" },
            new Object[] {
                    "7z", "7-zip Archive", "http://en.wikipedia.org/wiki/7z_%28file_format%29",
                    "org.7-zip.7-zip-archive",
                    "application/x-7z-compressed" },
            new Object[] {
                    "rar", "RAR: Roshal ARchive", "http://en.wikipedia.org/wiki/RAR_%28file_format%29",
                    "com.rarlab.rar-archive", "application/x-rar-compressed" },
            new Object[] {
                    "bz2", "BZip2 Archive", "http://en.wikipedia.org/wiki/Bzip2",
                    "public.archive.bzip2", "application/x-bzip" },
            new Object[] {
                    "gz", "GNU Zip Archive", "http://en.wikipedia.org/wiki/Gzip",
                    "org.gnu.gnu-zip-archive", "application/gzip",
                    "application/x-gzip", "application/x-gunzip",
                    "application/gzipped", "application/gzip-compressed" },
            new Object[] {
                    "tgz", "Same as tar.gz", "http://en.wikipedia.org/wiki/Tgz",
                    "org.gnu.gnu-zip-tar-archive", "application/x-compressed-tar"},
            new Object[] {
                    "tar", "Tape ARchive",  "http://en.wikipedia.org/wiki/Tar_file_format",
                    "public.tar-archive", "application/x-tar"},
            new Object[] {
                    "arj", "ARJ Archiver Robert Jung", "http://en.wikipedia.org/wiki/ARJ",
                    "public.arj-archive", "application/x-arj-compressed"},
            new Object[] {
                    "lzh", "LHA/LZH Archive", "http://en.wikipedia.org/wiki/LZH",
                    "public.lzh-archive", "application/x-lzh-compressed" },
            new Object[] {
                    "z", "Unix Compress (LZC)", "http://en.wikipedia.org/wiki/Z_%28file_format%29",
                    "com.public.z-archive", "application/x-compress" },
            new Object[] {
                    "cab", "Windows Cabinet", "http://en.wikipedia.org/wiki/Cabinet_%28file_format%29",
                    "com.microsoft.cab-archive", "application/vnd.ms-cab-compressed"},
            new Object[] {
                    "chm", "Compressed HTML", "http://en.wikipedia.org/wiki/Microsoft_Compressed_HTML_Help",
                    "com.microsoft.chm-archive", null },
            new Object[] {
                    "cpio", "Posix CPIO/PAX Archive", "http://en.wikipedia.org/wiki/Cpio",
                    "public.cpio-archive", "application/x-cpio"},
            new Object[] {
                    "ear", "Enterprise ARchive", "http://en.wikipedia.org/wiki/EAR_%28file_format%29",
                    "com.sun.ear-archive", "application/ear",
                    "application/x-zip", "application/x-zip-compressed" },
            new Object[] {
                    "war", "Web ARchive", "http://en.wikipedia.org/wiki/WAR_%28file_format%29",
                    "com.sun.war-archive", "application/war",
                    "application/x-zip", "application/x-zip-compressed" },
            new Object[] {
                    "cbr", "Comic Book Archive (rar)", "http://en.wikipedia.org/wiki/Comic_Book_Archive_file",
                    "com.public.cbr-archive", "application/cbr",
                    "application/x-rar-compressed" },
            new Object[] {
                    "cbz", "Comic Book Archive (zip)", "http://en.wikipedia.org/wiki/Comic_Book_Archive_file",
                    "com.public.cbz-archive",
                    "application/cbz", "application/x-zip", "application/x-zip-compressed" },
/*
            new Object[] { // future zipeg format
                    "zpg", "Zipeg Archive", "http://en.wikipedia.org/wiki/Zip_%28file_format%29",
                    "com.zipeg.archive.zpg",
                    "application/zpg", "application/x-zip", "application/x-zip-compressed" },
*/
    };

    boolean isAvailable();
    void setHandled(long selected);
    long getHandled();
}
