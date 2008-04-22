package com.zipeg;

import java.util.*;
import java.awt.*;
import java.awt.event.InvocationEvent;

public final class IdlingEventQueue extends EventQueue {

    public static final int DELAY = 100; // pulser does measure load 10 times per second
    private static final IdlingEventQueue queue = new IdlingEventQueue();
    private static final InvocationEvent strobeEvent = new InvocationEvent(IdlingEventQueue.class, new Runnable() {
        public void run() { strobe(); }
    });
    private static long lastStrobeTime;
    private static long eventCount;
    private static long lastIdleEventCount;
    private static int nested;
    private static boolean isInsideFileChooserUI; // bug in AquaDirectoryModel
    private static final Set idlers = new HashSet();
    private static LinkedList idleQueue = new LinkedList();
    private static final Map threadStats = new HashMap();
    private static boolean first = true; // first event event dispatch thread

    private static class ThreadStats {
        // only one of the following 2 values can be none zero at any given time
        long idle; // time in microseconds when last item of work completed
        long work; // time in microseconds when last item of work started
        long totalIdle; // cumulative counter for the last measuring interval
        long totalWork; // cumulative counter for the last measuring interval
        long nested; // for EDT last level of nesting
        int load; // 0..100%
        public String toString() {
            return "w:" + work + " i:" + idle + " tw:" + totalWork + " ti:" + totalIdle + " nested:" + nested;
        }
    }

    public static void init() {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(queue);
        Thread thread = new Thread(new Strober());
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setName("Strober");
        thread.start();
    }

    public static void setInsideFileChooser(boolean inside) {
        isInsideFileChooserUI = inside;
    }

    public static void reportThreadIsWorking() {
        long now = Clock.microseconds();
        ThreadStats ts = getThreadStats(now, true);
        if (now < 0) {
            Debug.traceln("Warning: Clock.microseconds() <= 0 " + now);
            ts.work = 0;
            ts.idle = 0;
            return;
        }
//      Debug.traceln("work: " +  Thread.currentThread().hashCode() + " " + ts);
        if (isDispatchThread() && nested > ts.nested) {
            assert ts.idle == 0 || !Debug.isDebug();
            if (ts.work > 0) {
                ts.totalWork +=  now - ts.work;
            }
        } else {
            assert ts.work == 0 || !Debug.isDebug();
            if (ts.idle > 0) {
                ts.totalIdle +=  now - ts.idle;
            }
        }
        ts.idle = 0;
        ts.work = now;
        ts.nested = nested;
    }

    public static void reportThreadIsIdling() {
        long now = Clock.microseconds();
        ThreadStats ts = getThreadStats(now, false);
        if (now < 0) {
            Debug.traceln("Warning: Clock.microseconds() <= 0 " + now);
            ts.work = 0;
            ts.idle = 0;
            return;
        }
//      Debug.traceln("idle: " +  Thread.currentThread().hashCode() + " " + ts);
        if (isDispatchThread() && nested < ts.nested) {
            assert ts.work == 0 || !Debug.isDebug();
            if (ts.idle > 0) {
                ts.totalIdle +=  now - ts.idle;
            }
        } else {
            assert ts.idle == 0 || !Debug.isDebug();
            if (ts.work > 0) {
                ts.totalWork +=  now - ts.work;
            }
        }
        ts.work = 0;
        ts.idle = now;
        ts.nested = nested;
    }

    public static void reportThreadExit() {
        synchronized (threadStats) {
            threadStats.remove(Thread.currentThread());
        }
    }

    private static ThreadStats getThreadStats(long now, boolean working) {
        synchronized (threadStats) {
            ThreadStats ts = (ThreadStats)threadStats.get(Thread.currentThread());
            if (ts == null) {
                ts = new ThreadStats();
                threadStats.put(Thread.currentThread(), ts);
                if (working) {
                    ts.idle = now;
                } else {
                    ts.work = now;
                }
            }
            return ts;
        }
    }

    /**
     * Idlers are called on each idle but only once. Idle cycle restarts after
     * arrival of next non idle event.
     * Caveat: cursor blinking in text editor causes timer messages
     * repeatedly going thru the queue which causes all idlers to be called.
     * No special effort to optimize this situation is made.
     * @param r runnable
     */
    public static void addIdler(Runnable r) {
        assert !idlers.contains(r);
        idlers.add(r);
    }

    public static void removeIdler(Runnable r) {
        assert idlers.contains(r);
        idlers.remove(r);
    }

    /**
     * Schedules runnable to be executed on _next_ idle.
     * Idle happens after DELAY milliseconds of event dispatch queue inactivity.
     * All idle actions accumulated by that time are posted to the event queue
     * will be executed.
     * If action want to be executed at every idle it must use addIdler instead
     * of rescheduling itself repeatedly.
     * Note that rescheduling itself will lead to calling action each DELAY
     * interval even if no messages arrived in between.
     * @param r action to execute on idle.
     */
    public static void invokeOnIdle(Runnable r) {
        assert IdlingEventQueue.isDispatchThread();
        assert r != null;
        idleQueue.addLast(r);
        eventCount++; // to kick in idle dispatching
    }

    public void dispatchEvent(AWTEvent event) {
/*
        Debug.traceln(eventCount + " : dispatchEvent " + event.getClass() + " source " +
                      event.getSource() + " " + event.getSource().getClass());
*/
        if (first) { // first event
            first = false;
            long now = Clock.microseconds();
            // Clock.microseconds returns negative values after
            // system goes on stand by and wakes up. Looks like
            // bug somewhere in OS X. Just ignore negatives
            if (now > 0) {
                lastStrobeTime = now;
            } else {
                Debug.traceln("Warning: Clock.microseconds() <= 0 " + now);
            }
        }
        if (event != strobeEvent) {
            eventCount++;
            reportThreadIsWorking();
        }
        nested++;
        try {
            super.dispatchEvent(event);
        } catch (java.awt.dnd.InvalidDnDOperationException x){
            // known but rare issue on Mac "Drag and drop in progress"
            Debug.traceln("warning: " + x.getMessage());
        } catch (ArrayIndexOutOfBoundsException aioob) {
            if (Util.isMac() && isInsideFileChooserUI) {
                // repro steps: After FileChooser dialog window shows up
                // on the folder with several chosable entries
                // crefully move it with the mouse (holding on title bar)
                // and hit Meta+A immediately after that w/o putting focus
                // inside the file table. AquaFileChooserUI.MacListSelectionModel.
                // isSelectableInListIndex  calls getValueAt(-1). Nice.
                // Everything is package private or private in 1.4.2, 1.5.* 1.6.*
                // on the Mac around there. This is the best workaround I can
                // think of:
                Debug.traceln("warning: ArrayIndexOutOfBoundsException(" + aioob.getMessage() +
                              ") in AquaDirectoryModel ignored");
            } else {
                throw aioob;
            }
        } finally {
            nested--;
            if (event != strobeEvent) {
                reportThreadIsIdling();
            }
        }
    }

    private synchronized static void strobe() {
//      Debug.traceln("strobe()");
        assert IdlingEventQueue.isDispatchThread();
        long now = Clock.microseconds();
        if (now <= 0 || lastStrobeTime <= 0) {
            Debug.traceln("Warning: Clock.microseconds() <= 0 " + now);
            return;
        }
        int maxLoad = 0;
        long maxTime = 0;
        long delta = now - lastStrobeTime;
        if (delta <= 0) {
            return; // ignore strobes if they come too often
        }
        synchronized (threadStats) {
            for (Iterator i = threadStats.values().iterator(); i.hasNext(); ) {
                ThreadStats ts = (ThreadStats)i.next();
                if (ts.work > 0) {
                    ts.totalWork += now - ts.work;
                    ts.work = now;
                } else if (ts.idle > 0) {
                    ts.totalIdle += now - ts.idle;
                    ts.idle = now;
                }
//              Debug.traceln("ts=" + ts);
                maxTime = Math.max(maxTime, (ts.totalIdle + ts.totalWork));
            }
            for (Iterator i = threadStats.values().iterator(); i.hasNext(); ) {
                ThreadStats ts = (ThreadStats)i.next();
//???           assert maxTime <= delta;
//???           assert ts.totalIdle + ts.totalWork <= maxTime;
                ts.load = (int)(ts.totalWork * 100 / delta);
                maxLoad = Math.max(maxLoad, ts.load);
//              Debug.traceln("thread work time " + Clock.milliseconds(ts.totalWork) + " milliseconds (" + ts.load + "%)");
                ts.totalIdle = 0;
                ts.totalWork = 0;
            }
        }
        if (now > 0) {
            lastStrobeTime = now;
        } else {
            Debug.traceln("Warning: Clock.microseconds() <= 0 " + now);
        }
        if (eventCount > lastIdleEventCount) {
            // idle throtling can be implemented here if needed.
            // my measurements say that at the moment it is unnecessary
/*
            Debug.traceln("eventCount " + eventCount + " max time " + Clock.milliseconds(maxTime) + "; " +
                    "since last strobe " + Clock.milliseconds(delta) + " milliseconds (" + maxLoad + "%) " +
                    "cpu=" + Runtime.getRuntime().availableProcessors());
*/
            lastIdleEventCount = eventCount;
//          now = Clock.microseconds();
            for (Iterator i = idlers.iterator(); i.hasNext(); ) {
                Runnable r = (Runnable)i.next();
                r.run();
            }
//          Debug.traceln("idlers time " + Clock.milliseconds(Clock.microseconds() - now));
            if (!idleQueue.isEmpty()) {
                LinkedList q = idleQueue;
                idleQueue = new LinkedList();
//              now = Clock.microseconds();
                while (!q.isEmpty() && queue.isEmpty()) {
                    Runnable r = (Runnable)q.removeFirst();
                    r.run();
                }
//              Debug.traceln("idle time: " + Clock.milliseconds(Clock.microseconds() - now));
                if (!q.isEmpty()) {
                    q.addAll(idleQueue);
                    idleQueue = q;
                }
            }
        }
    }

    private boolean isEmpty() {
        return peekEvent() == null;
    }

    private static class Strober implements Runnable {

        public void run() {
            try {
                // noinspection InfiniteLoopStatement
                for (;;) {
                    Thread.sleep(DELAY);
                    queue.postEvent(strobeEvent);
                }
            } catch (InterruptedException e) {
                Actions.reportError(e.getMessage());
                throw new Error(e);
            }
        }
    }

}