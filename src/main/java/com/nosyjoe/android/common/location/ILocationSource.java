package com.nosyjoe.android.common.location;

import android.location.Location;
import android.location.LocationListener;

/**
 * This interface is an abstraction of the Android location manager and enables support of 3rd-party position providers.
 *
 * @author Philipp Engel <philipp@filzip.com>
 */
public interface ILocationSource {

    /**
     * Starts positioning. Before calling enablePositionUpdates(), the position provider should be paused.
     */
    void enablePositionUpdates();

    /**
     * Stops positioning. In order to save resources, the provider should be stopped.
     */
    void disablePositionUpdates();

    /**
     * Adds a listener for location updates.
     *
     * @param toAdd The LocationListener being added
     */
    void addLocationUpdateListener(LocationListener toAdd);

    /**
     * Removes the specified LocationListener from the set of listeners.
     *
     * @param toRemove The listener to remove
     */
    void removeLocationUpdateListener(LocationListener toRemove);

    /**
     * If there is  the best should be returned immediately.  If there is no location information available,
     * the method returns null and delivers the location as soon as it is available via
     * LocationListener.onLocationChanged(), so register a location listener first before using this method.
     *
     * @param maxAge the maximum age in milliseconds
     * @return The best location information available or null if no fix was acquired yet.
     */
    Location requestBestLocation(long maxAge);

    /**
     * Passive positioning only: Returns the last best known location that is cached in the system,
     * only returns null if there is no last location in any of the location providers that meet the maxAge
     * requirements
     *
     * @param maxAge the maximum age in milliseconds
     * @return the latest best location, null if none is available
     */
    Location getLastBestKnownLocation(long maxAge);


}
