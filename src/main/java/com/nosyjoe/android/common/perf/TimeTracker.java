package com.nosyjoe.android.common.perf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class can be used to record named time intervals.
 * @author Philipp Engel <philipp@filzip.com>
 */
public enum TimeTracker {

    INSTANCE;

    private final Map<String, Long> startMap = Collections.synchronizedMap(new HashMap<String, Long>());
    private final Map<String, Long> endMap = Collections.synchronizedMap(new HashMap<String, Long>());

    public synchronized long markStartOf(String markerName) {
        long start = System.currentTimeMillis();
        startMap.put(markerName, start);
        return start;
    }

    public synchronized long getStartOf(String markerName) {
        if (startMap.containsKey(markerName)) {
            return startMap.get(markerName);
        } else {
            return -1;
        }
    }

    public synchronized long markEndOf(String markerName) {
        long end = System.currentTimeMillis();
        endMap.put(markerName, end);
        return end;
    }

    public synchronized long getEndOf(String markerName) {
        if (endMap.containsKey(markerName)) {
            return endMap.get(markerName);
        } else {
            return -1;
        }
    }

    public synchronized long getTimeDelta(String markerName) {
        if (startMap.containsKey(markerName) && endMap.containsKey(markerName)) {
            return endMap.get(markerName) - startMap.get(markerName);
        } else {
            return -1;
        }
    }

    public synchronized long timeElapsedSince(String markerName) {
        if (startMap.containsKey(markerName) ) {
            return System.currentTimeMillis() - startMap.get(markerName);
        } else {
            return -1;
        }
    }

    public String getTimeSinceString(String markerName, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("Time elapsed since '").append(markerName).append("': ");
        sb.append(timeElapsedSince(markerName)).append(". ").append(message);
        return sb.toString();
    }

}
