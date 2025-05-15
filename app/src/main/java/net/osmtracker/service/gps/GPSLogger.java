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
import android.os.IBinder;
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
 * GPS logging service with dynamic MAX_ACCEPTABLE_DISTANCE based on user speed.
 *
 * @author Nicolas Guillaumin
 */
public class GPSLogger extends Service implements LocationListener {

	private static final String TAG = GPSLogger.class.getSimpleName();

	// Data helper
	private DataHelper dataHelper;

	// Are we currently tracking?
	private boolean isTracking = false;

	// Is GPS enabled?
	private boolean isGpsEnabled = false;

	// Use barometer?
	private boolean use_barometer = false;

	// System notification ID and channel
	private static final int NOTIFICATION_ID = 1;
	private static final String CHANNEL_ID = "GPSLogger_Channel";

	// Last known location
	private Location lastLocation;

	// Location Managers
	private LocationManager lmgr_gps;
	private LocationManager lmgr_network;

	// Current Track ID
	private long currentTrackId = -1;

	// Timestamp of the last GPS fix used
	private long lastGPSTimestamp = 0;

	// Preferences for logging interval and distance
	private long gpsLoggingInterval;
	private long gpsLoggingMinDistance;

	// Maximum acceptable accuracy (meters)
	private static final float MAX_ACCEPTABLE_ACCURACY = 20.0f; // 20 meters

	// Maximum acceptable speed change (meters per second)
	private static final float MAX_SPEED_DELTA = 5.56f; // 20 km/h (~5.56 m/s)

	// Minimum and maximum acceptable distance (meters)
	private static final float MIN_ACCEPTABLE_DISTANCE = 10.0f; // 10 meters
	private static final float MAX_ACCEPTABLE_DISTANCE = 1000.0f; // 1000 meters

	// Safety factor for dynamic distance calculation
	private static final float DISTANCE_SAFETY_FACTOR = 1.5f; // 50% extra margin

	// Sensors for magnetic orientation and pressure
	private SensorListener sensorListener = new SensorListener();
	private PressureListener pressureListener = new PressureListener();

	// Broadcast receiver for waypoints and tracking control
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(TAG, "Received intent " + intent.getAction());

			if (OSMTracker.INTENT_TRACK_WP.equals(intent.getAction())) {
				Bundle extras = intent.getExtras();
				if (extras != null && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					Location bestLocation = getBestLastKnownLocation();
					if (bestLocation != null) {
						Long trackId = extras.getLong(TrackContentProvider.Schema.COL_TRACK_ID);
						String uuid = extras.getString(OSMTracker.INTENT_KEY_UUID);
						String name = extras.getString(OSMTracker.INTENT_KEY_NAME);
						String link = extras.getString(OSMTracker.INTENT_KEY_LINK);

						dataHelper.wayPoint(trackId, bestLocation, name, link, uuid, sensorListener.getAzimuth(), sensorListener.getAccuracy(), pressureListener.getPressure());
						dataHelper.track(currentTrackId, bestLocation, sensorListener.getAzimuth(), sensorListener.getAccuracy(), pressureListener.getPressure());
					} else {
						Log.w(TAG, "No valid location available for waypoint");
					}
				}
			} else if (OSMTracker.INTENT_UPDATE_WP.equals(intent.getAction())) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Long trackId = extras.getLong(TrackContentProvider.Schema.COL_TRACK_ID);
					String uuid = extras.getString(OSMTracker.INTENT_KEY_UUID);
					String name = extras.getString(OSMTracker.INTENT_KEY_NAME);
					String link = extras.getString(OSMTracker.INTENT_KEY_LINK);
					dataHelper.updateWayPoint(trackId, uuid, name, link);
				}
			} else if (OSMTracker.INTENT_DELETE_WP.equals(intent.getAction())) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Long trackId = extras.getLong(TrackContentProvider.Schema.COL_TRACK_ID);
					String uuid = extras.getString(OSMTracker.INTENT_KEY_UUID);
					String link = extras.getString(OSMTracker.INTENT_KEY_LINK);
					String filePath = null;
					try {
						filePath = link.equals("null") ? null : DataHelper.getTrackDirectory(trackId, context) + "/" + link;
					} catch (NullPointerException ne) {
					}
					dataHelper.deleteWayPoint(uuid, filePath);
				}
			} else if (OSMTracker.INTENT_START_TRACKING.equals(intent.getAction())) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Long trackId = extras.getLong(TrackContentProvider.Schema.COL_TRACK_ID);
					startTracking(trackId);
				}
			} else if (OSMTracker.INTENT_STOP_TRACKING.equals(intent.getAction())) {
				stopTrackingAndSave();
			}
		}
	};

	// Binder for service interaction
	private final IBinder binder = new GPSLoggerBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "Service onBind()");
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "Service onUnbind()");
		if (!isTracking) {
			Log.v(TAG, "Service self-stopping");
			stopSelf();
		}
		return false;
	}

	public class GPSLoggerBinder extends Binder {
		public GPSLogger getService() {
			return GPSLogger.this;
		}
	}

	@Override
	public void onCreate() {
		Log.v(TAG, "Service onCreate()");
		dataHelper = new DataHelper(this);

		// Read preferences
		gpsLoggingInterval = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString(
				OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL)) * 1000;
		gpsLoggingMinDistance = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString(
				OSMTracker.Preferences.KEY_GPS_LOGGING_MIN_DISTANCE, OSMTracker.Preferences.VAL_GPS_LOGGING_MIN_DISTANCE));
		use_barometer = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean(
				OSMTracker.Preferences.KEY_USE_BAROMETER, OSMTracker.Preferences.VAL_USE_BAROMETER);

		// Register broadcast receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(OSMTracker.INTENT_TRACK_WP);
		filter.addAction(OSMTracker.INTENT_UPDATE_WP);
		filter.addAction(OSMTracker.INTENT_DELETE_WP);
		filter.addAction(OSMTracker.INTENT_START_TRACKING);
		filter.addAction(OSMTracker.INTENT_STOP_TRACKING);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
		} else {
			registerReceiver(receiver, filter);
		}

		// Initialize Location Managers
		lmgr_gps = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lmgr_network = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			lmgr_gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsLoggingInterval, gpsLoggingMinDistance, this);
			lmgr_network.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, gpsLoggingInterval, gpsLoggingMinDistance, this);
		}

		// Register sensors
		sensorListener.register(this);
		pressureListener.register(this, use_barometer);

		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "Service onStartCommand(-," + flags + "," + startId + ")");
		createNotificationChannel();
		startForeground(NOTIFICATION_ID, getNotification());
		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "Service onDestroy()");
		if (isTracking) {
			stopTrackingAndSave();
		}

		lmgr_gps.removeUpdates(this);
		lmgr_network.removeUpdates(this);
		unregisterReceiver(receiver);
		stopNotifyBackgroundService();
		sensorListener.unregister();
		pressureListener.unregister();

		super.onDestroy();
	}

	private void startTracking(long trackId) {
		currentTrackId = trackId;
		Log.v(TAG, "Starting track logging for track #" + trackId);
		NotificationManager nmgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nmgr.notify(NOTIFICATION_ID, getNotification());
		isTracking = true;
	}

	private void stopTrackingAndSave() {
		isTracking = false;
		dataHelper.stopTracking(currentTrackId);
		currentTrackId = -1;
		this.stopSelf();
	}

	@Override
	public void onLocationChanged(Location location) {
		isGpsEnabled = true;

		// Log location details for debugging
		Log.d(TAG, "Location received: Provider=" + location.getProvider() +
				", Accuracy=" + location.getAccuracy() +
				", Speed=" + (location.hasSpeed() ? location.getSpeed() : "N/A") + " m/s" +
				", Lat=" + location.getLatitude() +
				", Lon=" + location.getLongitude() +
				", Time=" + location.getTime());

		// Check if the location is acceptable
		if (!isLocationAcceptable(location)) {
			float dynamicMaxDistance = calculateDynamicMaxDistance(location);
			Log.w(TAG, "Location rejected: Accuracy=" + location.getAccuracy() +
					", Speed=" + (location.hasSpeed() ? location.getSpeed() : "N/A") +
					", SpeedDelta=" + (lastLocation != null && location.hasSpeed() && lastLocation.hasSpeed() ? Math.abs(location.getSpeed() - lastLocation.getSpeed()) : "N/A") +
					", Distance=" + (lastLocation != null ? location.distanceTo(lastLocation) : "N/A") +
					", DynamicMaxDistance=" + dynamicMaxDistance);
			return;
		}

		// Check time interval
		if ((lastGPSTimestamp + gpsLoggingInterval) < System.currentTimeMillis()) {
			lastGPSTimestamp = System.currentTimeMillis();
			lastLocation = location;
			if (isTracking) {
				dataHelper.track(currentTrackId, location, sensorListener.getAzimuth(), sensorListener.getAccuracy(), pressureListener.getPressure());
				Log.d(TAG, "Location accepted and tracked: Lat=" + location.getLatitude() +
						", Lon=" + location.getLongitude() +
						", Speed=" + (location.hasSpeed() ? location.getSpeed() : "N/A") + " m/s");
			}
		}
	}

	/**
	 * Checks if a location is acceptable based on accuracy, speed delta, and dynamic distance.
	 */
	private boolean isLocationAcceptable(Location location) {
		// Check accuracy
		if (location.getAccuracy() > MAX_ACCEPTABLE_ACCURACY) {
			return false;
		}

		// Check speed delta (change in speed)
		if (lastLocation != null && location.hasSpeed() && lastLocation.hasSpeed()) {
			float speedDelta = Math.abs(location.getSpeed() - lastLocation.getSpeed());
			if (speedDelta > MAX_SPEED_DELTA) {
				return false;
			}
		}

		// Check dynamic distance
		if (lastLocation != null) {
			float distance = location.distanceTo(lastLocation);
			float dynamicMaxDistance = calculateDynamicMaxDistance(location);
			if (distance > dynamicMaxDistance) {
				return false;
			}
		}

		// Prefer GPS_PROVIDER over NETWORK_PROVIDER
		if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider()) && isGpsProviderAvailable()) {
			return false;
		}

		return true;
	}

	/**
	 * Calculates dynamic maximum acceptable distance based on speed and time interval.
	 */
	private float calculateDynamicMaxDistance(Location location) {
		float speed = location.hasSpeed() ? location.getSpeed() : 0; // m/s
		// Use gpsLoggingInterval (in seconds) as the time interval
		float timeInterval = gpsLoggingInterval / 1000.0f; // Convert ms to seconds
		// Calculate distance: distance = speed * time * safety_factor
		float dynamicDistance = speed * timeInterval * DISTANCE_SAFETY_FACTOR;
		// Apply minimum and maximum bounds
		return Math.max(MIN_ACCEPTABLE_DISTANCE, Math.min(dynamicDistance, MAX_ACCEPTABLE_DISTANCE));
	}

	/**
	 * Checks if GPS_PROVIDER is available and has a recent location.
	 */
	private boolean isGpsProviderAvailable() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			Location lastGps = lmgr_gps.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			return lastGps != null && (System.currentTimeMillis() - lastGps.getTime()) < gpsLoggingInterval * 2;
		}
		return false;
	}

	/**
	 * Gets the best last known location (prefers GPS_PROVIDER).
	 */
	private Location getBestLastKnownLocation() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			Location gpsLocation = lmgr_gps.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			Location networkLocation = lmgr_network.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

			if (gpsLocation != null && isLocationAcceptable(gpsLocation)) {
				return gpsLocation;
			} else if (networkLocation != null && isLocationAcceptable(networkLocation)) {
				return networkLocation;
			}
		}
		return null;
	}

	private Notification getNotification() {
		Intent startTrackLogger = new Intent(this, TrackLogger.class);
		startTrackLogger.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, startTrackLogger, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_stat_track)
				.setContentTitle(getResources().getString(R.string.notification_title).replace("{0}", (currentTrackId > -1) ? Long.toString(currentTrackId) : "?"))
				.setContentText(getResources().getString(R.string.notification_text))
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setContentIntent(contentIntent)
				.setAutoCancel(true);
		return mBuilder.build();
	}

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = "GPS Logger";
			String description = "Display when tracking in Background";
			int importance = NotificationManager.IMPORTANCE_LOW;
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
			channel.setDescription(description);
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.createNotificationChannel(channel);
		}
	}

	private void stopNotifyBackgroundService() {
		NotificationManager nmgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nmgr.cancel(NOTIFICATION_ID);
	}

	@Override
	public void onProviderDisabled(String provider) {
		isGpsEnabled = false;
	}

	@Override
	public void onProviderEnabled(String provider) {
		isGpsEnabled = true;
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public boolean isGpsEnabled() {
		return isGpsEnabled;
	}

	public boolean isTracking() {
		return isTracking;
	}
}