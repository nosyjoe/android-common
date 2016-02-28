package com.nosyjoe.android.common.location;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import com.nosyjoe.android.common.NjLog;

import java.util.List;
import java.util.WeakHashMap;

/**
 * @author Philipp Engel <philipp@filzip.com>
 */
public class LocationHelper implements ILocationSource {

    private static final int TWO_MINUTES = 2 * 60 * 1000;
    private static final Object dummy = new Object();
    protected static String INTENT_ACTION_SINGLE_LOCATION_UPDATE =
            "com.nosyjoe.android.common.location.SINGLE_LOCATION_UPDATE_ACTION";
    protected static String INTENT_ACTION_LOCATION_UPDATES =
            "com.nosyjoe.android.common.location.SINGLE_LOCATION_UPDATE_ACTION";

    private final LocationManager locationManager;
    private final Context context;
    private final WeakHashMap<LocationListener, Object> listeners = new WeakHashMap<LocationListener, Object>();
    private final BroadcastReceiver locationUpdateReceiver = new LocationBroadcastReceiver();
    private PendingIntent singleUpatePI;
    private final Criteria criteriaFine;
    private PendingIntent updatePI;
    private long minDistance = 10;
    private long minTime = 10*1000;
    private final Criteria criteriaCoarse;

    public LocationHelper(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.context = context;

        criteriaFine = new Criteria();
        criteriaFine.setAccuracy(Criteria.ACCURACY_FINE);
        criteriaCoarse = new Criteria();
        criteriaCoarse.setAccuracy(Criteria.ACCURACY_COARSE);

        Intent intent = new Intent(INTENT_ACTION_SINGLE_LOCATION_UPDATE);
        singleUpatePI = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent updateIntent = new Intent(INTENT_ACTION_LOCATION_UPDATES);
        updatePI = PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void enablePositionUpdates() {
        // make sure it's disabled first


        IntentFilter intentFilter = new IntentFilter(INTENT_ACTION_LOCATION_UPDATES);

        context.registerReceiver(this.locationUpdateReceiver, intentFilter);

        boolean gotProvider = false;
        try {
            this.locationManager.requestLocationUpdates(minTime,
                    minDistance, criteriaFine, updatePI);
            gotProvider = true;
        } catch (IllegalArgumentException e) {
            // not that bad yet, we could still get a coarse provider
            NjLog.d(this, "No location provider is matching criteriaFine: " + criteriaFine);
        }

        try {
            this.locationManager.requestLocationUpdates(minTime,
                    minDistance, criteriaCoarse, updatePI);
            gotProvider = true;
        } catch (IllegalArgumentException e) {
            NjLog.d(this, "No location provider is matching criteriaCoarse: " + criteriaCoarse);
        }

        if (!gotProvider) {
            NjLog.w(this, "No location provider available");
        }
    }

    @Override
    public void disablePositionUpdates() {
        if (this.locationManager != null && updatePI != null) {
            this.locationManager.removeUpdates(updatePI);
        }
        try {
            context.unregisterReceiver(locationUpdateReceiver);
        } catch (IllegalArgumentException e) {
            NjLog.w(this, "Error unregistering receiver: " + e.getMessage());
        }
    }

    @Override
    public void addLocationUpdateListener(LocationListener toAdd) {
        synchronized (listeners) {
            if (!listeners.containsKey(toAdd)) {
                listeners.put(toAdd, dummy);
            }
        }
    }

    @Override
    public void removeLocationUpdateListener(LocationListener toRemove) {
        synchronized (listeners) {
            if (listeners.containsKey(toRemove)) {
                listeners.remove(toRemove);
            }
        }
    }

    @Override
    public Location requestBestLocation(long maxAge) {
        long currentTime = System.currentTimeMillis();
        long minTime = currentTime - maxAge;

        Location result = getLastBestKnownLocation(maxAge);

        if (listeners.size() > 0 && result.getTime() < minTime) {
            IntentFilter locIntentFilter = new IntentFilter(INTENT_ACTION_SINGLE_LOCATION_UPDATE);
            context.registerReceiver(singleUpdateReceiver, locIntentFilter);
            locationManager.requestSingleUpdate(criteriaFine, singleUpatePI);
            NjLog.d(this, "Requesting single location update: latest fix age: " +
                    ((currentTime - result.getTime()) / 1000 ) + "s, required max age: " + (maxAge / 1000) + "s");
        }

        return result;
    }

    @Override
    public Location getLastBestKnownLocation(long maxAge) {
        Location bestResult = null;
        float bestAccuracy = Float.MAX_VALUE;
        long bestTime = 0;

        long minTime = System.currentTimeMillis() - maxAge;
        minTime = minTime > 0 ? minTime : 0;

        // Iterate through all the providers on the system, keeping
        // note of the most accurate result within the acceptable time limit.
        // If no result is found within maxTime, return the newest Location.
        List<String> matchingProviders = locationManager.getAllProviders();
        for (String provider : matchingProviders) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
//                AqLog.i(this, "getBestLocation: candidate (timestamp: " + dateFormat.format(location.getTime()) + "): " + location);
                float accuracy = location.getAccuracy();
                long time = location.getTime();

                if ((time > minTime && accuracy < bestAccuracy)) {
                    bestResult = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                } else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
                    bestResult = location;
                    bestTime = time;
                }
            }
        }

        return bestResult;
    }

    private void updateListener(Location location) {
        if (location != null) {
            synchronized (listeners) {
                for (LocationListener aListener : listeners.keySet()) {
                    aListener.onLocationChanged(location);
                }
            }
        }
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private class LocationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
                Location location = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
                updateListener(location);
            }
        }
    }

    /**
     * This {@link BroadcastReceiver} listens for a single location
     * update before unregistering itself.
     */
    protected BroadcastReceiver singleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.unregisterReceiver(singleUpdateReceiver);
            locationManager.removeUpdates(singleUpatePI);

            if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
                Location location = (Location)intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
                NjLog.d(LocationHelper.this, "Received single location update: " + location);
                updateListener(location);
            }
        }
    };
}
