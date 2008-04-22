export cpu=`uname -p`
if [ "$cpu" == "powerpc" ]; then
  export cpu="ppc"
fi
echo "cpu=$cpu"
make jni
mv bin/lib7za.jnilib ../../Zipeg.app/Contents/Resources/Java/lib7za-osx-$cpu.jnilib
