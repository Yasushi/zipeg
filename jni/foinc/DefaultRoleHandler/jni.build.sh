export cpu=`uname -p`
if [ "$cpu" == "powerpc" ]; then
  export cpu="ppc"
fi 
echo "cpu=$cpu"

c++ -Oz -o libdrh-osx-$cpu.jnilib -framework CoreFoundation -framework ApplicationServices -dynamiclib -single_module -I/System/Library/Frameworks/JavaVM.framework/Headers DefaultRoleHandler.cpp
cp libdrh-osx-$cpu.jnilib ../../../Zipeg.app/Contents/Resources/Java/

