
DEST_BIN=/usr/local/bin
DEST_SHARE=/usr/local/lib/p7zip
DEST_MAN=/usr/local/man

.PHONY: default all all2 jni common clean depend

default:jni

all:jni

common:
	mkdir -p  bin
	cd Common       ; $(MAKE) all
	cd myWindows    ; $(MAKE) all

jni: common
	mkdir -p  bin/Codecs bin/Formats
	cd 7zip/Archive/7z        ; $(MAKE) all
	cd 7zip/Archive/Arj       ; $(MAKE) all
	cd 7zip/Archive/BZip2     ; $(MAKE) all
	cd 7zip/Archive/Cab       ; $(MAKE) all
	cd 7zip/Archive/Chm       ; $(MAKE) all
	cd 7zip/Archive/Cpio      ; $(MAKE) all
	cd 7zip/Archive/Deb       ; $(MAKE) all
	cd 7zip/Archive/GZip      ; $(MAKE) all
	cd 7zip/Archive/Iso       ; $(MAKE) all
	cd 7zip/Archive/Lzh       ; $(MAKE) all
	cd 7zip/Archive/Nsis      ; $(MAKE) all
	cd 7zip/Archive/Rar       ; $(MAKE) all
	cd 7zip/Archive/RPM       ; $(MAKE) all
	cd 7zip/Archive/Split     ; $(MAKE) all
	cd 7zip/Archive/Tar       ; $(MAKE) all
	cd 7zip/Archive/Z         ; $(MAKE) all
	cd 7zip/Archive/Zip       ; $(MAKE) all
	cd 7zip/Compress/Branch   ; $(MAKE) all
	cd 7zip/Compress/ByteSwap ; $(MAKE) all
	cd 7zip/Compress/BZip2    ; $(MAKE) all
	cd 7zip/Compress/Copy     ; $(MAKE) all
	cd 7zip/Compress/Deflate  ; $(MAKE) all
	cd 7zip/Compress/Implode  ; $(MAKE) all
	cd 7zip/Compress/LZMA     ; $(MAKE) all
	cd 7zip/Compress/PPMD     ; $(MAKE) all
	cd 7zip/Compress/Rar29    ; $(MAKE) all
	cd 7zip/Crypto/7zAES      ; $(MAKE) all
	cd 7zip/Crypto/AES        ; $(MAKE) all
	cd 7zip/Bundles/Jni   ; $(MAKE) all

depend:
	cd Common                 ; $(MAKE) depend
	cd myWindows              ; $(MAKE) depend
	cd 7zip/Bundles/Jni       ; $(MAKE) depend
	cd 7zip/Archive/7z        ; $(MAKE) depend
	cd 7zip/Archive/Arj       ; $(MAKE) depend
	cd 7zip/Archive/BZip2     ; $(MAKE) depend
	cd 7zip/Archive/Cab       ; $(MAKE) depend
	cd 7zip/Archive/Chm       ; $(MAKE) depend
	cd 7zip/Archive/Cpio      ; $(MAKE) depend
	cd 7zip/Archive/Deb       ; $(MAKE) depend
	cd 7zip/Archive/GZip      ; $(MAKE) depend
	cd 7zip/Archive/Iso       ; $(MAKE) depend
	cd 7zip/Archive/Lzh       ; $(MAKE) depend
	cd 7zip/Archive/Nsis      ; $(MAKE) depend
	cd 7zip/Archive/Rar       ; $(MAKE) depend
	cd 7zip/Archive/RPM       ; $(MAKE) depend
	cd 7zip/Archive/Split     ; $(MAKE) depend
	cd 7zip/Archive/Tar       ; $(MAKE) depend
	cd 7zip/Archive/Z         ; $(MAKE) depend
	cd 7zip/Archive/Zip       ; $(MAKE) depend
	cd 7zip/Compress/Branch   ; $(MAKE) depend
	cd 7zip/Compress/ByteSwap ; $(MAKE) depend
	cd 7zip/Compress/BZip2    ; $(MAKE) depend
	cd 7zip/Compress/Copy     ; $(MAKE) depend
	cd 7zip/Compress/Deflate  ; $(MAKE) depend
	cd 7zip/Compress/Implode  ; $(MAKE) depend
	cd 7zip/Compress/LZMA     ; $(MAKE) depend
	cd 7zip/Compress/PPMD     ; $(MAKE) depend
	cd 7zip/Compress/Rar29    ; $(MAKE) depend
	cd 7zip/Crypto/7zAES      ; $(MAKE) depend
	cd 7zip/Crypto/AES        ; $(MAKE) depend

clean:
	cd Common                 ; $(MAKE) clean
	cd myWindows              ; $(MAKE) clean
	cd 7zip/Bundles/Jni	  ; $(MAKE) clean
	cd 7zip/Archive/7z        ; $(MAKE) clean
	cd 7zip/Archive/Arj       ; $(MAKE) clean
	cd 7zip/Archive/BZip2     ; $(MAKE) clean
	cd 7zip/Archive/Cab       ; $(MAKE) clean
	cd 7zip/Archive/Chm       ; $(MAKE) clean
	cd 7zip/Archive/Cpio      ; $(MAKE) clean
	cd 7zip/Archive/Deb       ; $(MAKE) clean
	cd 7zip/Archive/GZip      ; $(MAKE) clean
	cd 7zip/Archive/Iso       ; $(MAKE) clean
	cd 7zip/Archive/Lzh       ; $(MAKE) clean
	cd 7zip/Archive/Nsis      ; $(MAKE) clean
	cd 7zip/Archive/Rar       ; $(MAKE) clean
	cd 7zip/Archive/RPM       ; $(MAKE) clean
	cd 7zip/Archive/Split     ; $(MAKE) clean
	cd 7zip/Archive/Tar       ; $(MAKE) clean
	cd 7zip/Archive/Z         ; $(MAKE) clean
	cd 7zip/Archive/Zip       ; $(MAKE) clean
	cd 7zip/Compress/Branch   ; $(MAKE) clean
	cd 7zip/Compress/ByteSwap ; $(MAKE) clean
	cd 7zip/Compress/BZip2    ; $(MAKE) clean
	cd 7zip/Compress/Copy     ; $(MAKE) clean
	cd 7zip/Compress/Deflate  ; $(MAKE) clean
	cd 7zip/Compress/Implode  ; $(MAKE) clean
	cd 7zip/Compress/LZMA     ; $(MAKE) clean
	cd 7zip/Compress/PPMD     ; $(MAKE) clean
	cd 7zip/Compress/Rar29    ; $(MAKE) clean
	cd 7zip/Crypto/7zAES      ; $(MAKE) clean
	cd 7zip/Crypto/AES        ; $(MAKE) clean
	find . -name "*~" -exec rm -f {} \;
	find . -name "*.orig" -exec rm -f {} \;
	find . -name ".*.swp" -exec rm -f {} \;
	find . -name "*.[ch]" -exec chmod -x {} \;
	find . -name "*.cpp" -exec chmod -x {} \;
	find . -name "makefile*" -exec chmod -x {} \;
	chmod +x contrib/VirtualFileSystemForMidnightCommander/u7z
	chmod +x contrib/gzip-like_CLI_wrapper_for_7z/p7zip
	chmod +x install.sh check/check.sh check/clean_all.sh check/check_7zr.sh 
	rm -fr bin
	cd check                  ; ./clean_all.sh

