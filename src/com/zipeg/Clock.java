package com.zipeg;

import java.lang.reflect.*;

public class Clock {

    private static Object sun_misc_Perf;
    private static long ticksPerSecond;
    private static Method highResCounter;

    /** @return microsecond count (probably system power up)
     */
    public static long microseconds() {
        if (Util.getJavaVersion() >= 1.5)
            return ((Long)Util.callStatic("java.lang.System.nanoTime", Util.NONE)).longValue() / 1000;
        else {
            if (ticksPerSecond == 0) {
                loadPerf();
            }
            if (highResCounter != null) {
                long n = getCounter();
                if (ticksPerSecond > 1000000000) {
                    return n * 1000L / (ticksPerSecond / 1000);
                } else {
                    return n * 1000000L / ticksPerSecond;
                }
            }
            return System.currentTimeMillis() * 1000000;
        }
    }

    /**
     * @param microseconds time to format into string
     * @return string formated milliseconds like 1234 microseconds becomes "1.23" milliseconds
     */
    public static String milliseconds(long microseconds) {
        microseconds /= 10;
        int d = (int)(microseconds % 100);
        return microseconds / 100 + (d < 10 ? ".0" + d : "." + d);
    }


    private static void loadPerf() {
        ticksPerSecond = 1000;
        try {
            Class c = Class.forName("sun.misc.Perf");
            Method m = c.getMethod("getPerf", Util.VOID);
            sun_misc_Perf = m.invoke(c, Util.NONE);
            Method highResFrequency = c.getMethod("highResFrequency", Util.VOID);
            highResCounter = c.getMethod("highResCounter", Util.VOID);
            ticksPerSecond = ((Long)highResFrequency.invoke(sun_misc_Perf, Util.NONE)).longValue();
        }
        catch (Throwable ignore) { // ClassNotFoundException, NoSuchMethodException, IllegalAccessException
                                   // InvocationTargetException, NativeMethodNotFound
            ignore.printStackTrace();
            highResCounter = null;
        }
    }

    private static long getCounter() {
        assert highResCounter != null;
        try {
            return ((Long)highResCounter.invoke(sun_misc_Perf, Util.NONE)).longValue();
        } catch (InvocationTargetException e) { // ignore
        } catch (IllegalAccessException e) { // ignore
        }
        return System.currentTimeMillis();
    }

}
