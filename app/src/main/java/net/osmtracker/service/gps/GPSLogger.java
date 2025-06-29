package net.osmtracker.service.gps;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.activity.TrackLogger;
import net.osmtracker.data.db.DataHelper;
import net.osmtracker.data.db.TrackContentProvider;
import net.osmtracker.listener.PressureListener;
import net.osmtracker.listener.SensorListener;

/**
 * GPS logging service with a robust, timer-based heartbeat to guarantee point logging.
 *
 * @author Nicolas Guillaumin (Original)
 * @author Modified based on user feedback
 */
public class GPSLogger extends Service implements LocationListener {

	private static final String TAG = GPSLogger.class.getSimpleName();
	private DataHelper dataHelper;
	private boolean isTracking = false;
	private boolean isGpsEnabled = false;

	// Notification
	private static final int NOTIFICATION_ID = 1;
	private static final String CHANNEL_ID = "GPSLogger_Channel";

	// Location variables
	private Location lastLoggedLocation;
	private Location mostRecentLocation; // Stores the absolute last location received, regardless of quality

	// Location Managers
	private LocationManager lmgr_gps;
	private LocationManager lmgr_network;

	// Track variables
	private long currentTrackId = -1;

	// Preferences
	private long gpsLoggingMinDistance;

	// --- FILTERING CONSTANTS ---
	private static final float WALKING_MAX_ACCEPTABLE_ACCURACY = 30.0f; // Relaxed accuracy
	private static final float DRIVING_MAX_ACCEPTABLE_ACCURACY = 50.0f; // Relaxed accuracy
	private static final float WALKING_SPEED_THRESHOLD_MS = 3.0f;

	// --- UNIFIED TIMER FOR GUARANTEED LOGGING ---
	private final Handler guaranteedLogHandler = new Handler(Looper.getMainLooper());
	private static final long GUARANTEED_LOG_INTERVAL_MS = 10000; // 10 seconds

	private final Runnable guaranteedLogRunnable = new Runnable() {
		@Override
		public void run() {
			if (!isTracking) return;

			// Timer has fired. This means 10s passed with no movement-based logging.
			// We must log a point now.
			if (mostRecentLocation != null) {
				// We have a location, log it.
				// This covers both "force first point" and "proactive heartbeat" cases.
				logPoint(mostRecentLocation, "Timer Heartbeat");
			} else {
				// No location ever received from GPS. We can't do anything.
				// Let's reschedule the check to try again.
				Log.w(TAG, "Timer fired but no location has ever been received from GPS. Trying again in 10s.");
				guaranteedLogHandler.postDelayed(this, GUARANTEED_LOG_INTERVAL_MS);
			}
		}
	};

	// Sensors
	private final SensorListener sensorListener = new SensorListener();
	private final PressureListener pressureListener = new PressureListener();

	// Broadcast receiver
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action == null) return;
			final Bundle extras = intent.getExtras();
			if (extras == null) return;

			if (OSMTracker.INTENT_START_TRACKING.equals(action)) {
				startTracking(extras.getLong(TrackContentProvider.Schema.COL_TRACK_ID));
			} else if (OSMTracker.INTENT_STOP_TRACKING.equals(action)) {
				stopTrackingAndSave();
			}
			// Other cases can be added here using if-else if
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		dataHelper = new DataHelper(this);

		gpsLoggingMinDistance = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString(
				OSMTracker.Preferences.KEY_GPS_LOGGING_MIN_DISTANCE, OSMTracker.Preferences.VAL_GPS_LOGGING_MIN_DISTANCE));

		IntentFilter filter = new IntentFilter();
		filter.addAction(OSMTracker.INTENT_START_TRACKING);
		filter.addAction(OSMTracker.INTENT_STOP_TRACKING);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
		} else {
			registerReceiver(receiver, filter);
		}

		lmgr_gps = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lmgr_network = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			long gpsInterval = 1000; // Request updates frequently
			float minDistance = 1; // Request updates frequently
			lmgr_gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsInterval, minDistance, this);
			lmgr_network.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, gpsInterval, minDistance, this);
		}

		sensorListener.register(this);
		pressureListener.register(this, PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean(OSMTracker.Preferences.KEY_USE_BAROMETER, OSMTracker.Preferences.VAL_USE_BAROMETER));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		createNotificationChannel();
		startForeground(NOTIFICATION_ID, getNotification());
		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (isTracking) {
			stopTrackingAndSave();
		}
		guaranteedLogHandler.removeCallbacks(guaranteedLogRunnable);
		lmgr_gps.removeUpdates(this);
		unregisterReceiver(receiver);
		sensorListener.unregister();
		pressureListener.unregister();
	}

	private void startTracking(long trackId) {
		currentTrackId = trackId;
		isTracking = true;
		lastLoggedLocation = null;
		mostRecentLocation = null;

		Log.v(TAG, "Starting track logging for track #" + trackId);
		getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, getNotification());

		// Start the guaranteed logging timer. It will fire in 10 seconds.
		guaranteedLogHandler.postDelayed(guaranteedLogRunnable, GUARANTEED_LOG_INTERVAL_MS);
	}

	private void stopTrackingAndSave() {
		isTracking = false;
		guaranteedLogHandler.removeCallbacks(guaranteedLogRunnable); // Stop the timer
		if (currentTrackId != -1) {
			dataHelper.stopTracking(currentTrackId);
			currentTrackId = -1;
		}
		stopSelf();
	}

	@Override
	public void onLocationChanged(Location location) {
		if (location == null) return;
		isGpsEnabled = true;
		mostRecentLocation = location;

		if (!isTracking) return;

		// If this is the first point, the timer will handle it.
		// We only process movement-based logging after the first point is set.
		if (lastLoggedLocation == null) {
			// A simple quality check before we consider it for movement-based logging
			if (isLocationAcceptable(location)) {
				logPoint(location, "First Acceptable");
			}
			return;
		}

		// For subsequent points, log if moved enough. The timer is our backup.
		if (isLocationAcceptable(location)) {
			float distanceSinceLastLog = location.distanceTo(lastLoggedLocation);
			if (distanceSinceLastLog >= gpsLoggingMinDistance) {
				logPoint(location, "Movement");
			}
		}
	}

	private void logPoint(Location location, String reason) {
		if (!isTracking || location == null) return;

		// Cancel any pending timer before logging
		guaranteedLogHandler.removeCallbacks(guaranteedLogRunnable);

		dataHelper.track(currentTrackId, location, sensorListener.getAzimuth(), sensorListener.getAccuracy(), pressureListener.getPressure());
		lastLoggedLocation = location;

		Log.d(TAG, "Point logged. Reason: [" + reason + "]. Accuracy: " + location.getAccuracy() + "m");

		// Reschedule the timer for 10 seconds from now
		guaranteedLogHandler.postDelayed(guaranteedLogRunnable, GUARANTEED_LOG_INTERVAL_MS);
	}

	private boolean isLocationAcceptable(Location location) {
		if (location == null) return false;

		float maxAccuracy = location.getSpeed() < WALKING_SPEED_THRESHOLD_MS
				? WALKING_MAX_ACCEPTABLE_ACCURACY
				: DRIVING_MAX_ACCEPTABLE_ACCURACY;

		return location.getAccuracy() <= maxAccuracy;
	}

	private Location getBestLastKnownLocation() {
		return mostRecentLocation;
	}

	// --- Boilerplate Methods ---
	public class GPSLoggerBinder extends Binder {
		public GPSLogger getService() { return GPSLogger.this; }
	}
	private final IBinder binder = new GPSLoggerBinder();
	@Override
	public IBinder onBind(Intent intent) { return binder; }

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "GPS Logger", NotificationManager.IMPORTANCE_LOW);
			getSystemService(NotificationManager.class).createNotificationChannel(channel);
		}
	}

	private Notification getNotification() {
		Intent startTrackLogger = new Intent(this, TrackLogger.class);
		startTrackLogger.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, startTrackLogger, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
		return new NotificationCompat.Builder(this, CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_stat_track)
				.setContentTitle(getResources().getString(R.string.notification_title, (currentTrackId > -1) ? Long.toString(currentTrackId) : "?"))
				.setContentText(getResources().getString(R.string.notification_text))
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setContentIntent(contentIntent)
				.build();
	}

	@Override
	public void onProviderDisabled(String provider) { isGpsEnabled = false; }
	@Override
	public void onProviderEnabled(String provider) { isGpsEnabled = true; }
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	public boolean isGpsEnabled() { return isGpsEnabled; }
	public boolean isTracking() { return isTracking; }
}