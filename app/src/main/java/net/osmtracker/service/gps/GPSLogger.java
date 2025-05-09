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
 * GPS logging service.
 * 
 * @author Nicolas Guillaumin
 *
 */
public class GPSLogger extends Service implements LocationListener {

	private static final String TAG = GPSLogger.class.getSimpleName();

	/**
	 * Data helper.
	 */
	private DataHelper dataHelper;

	/**
	 * Are we currently tracking ?
	 */
	private boolean isTracking = false;
	
	/**
	 * Is GPS enabled ?
	 */
	private boolean isGpsEnabled = false;

	/**
	 * Use barometer yes/no ?
	 */
	private boolean use_barometer = false;
	
	/**
	 * System notification id.
	 */
	private static final int NOTIFICATION_ID = 1;
	private static String CHANNEL_ID = "GPSLogger_Channel";
	
	/**
	 * Last known location
	 */
	private Location lastLocation_gps;
	private Location lastLocation_network;

	/**
	 * LocationManager
	 */
	private LocationManager lmgr_gps;
	private LocationManager lmgr_network;

	/**
	 * Current Track ID
	 */
	private long currentTrackId = -1;

	/**
	 * the timestamp of the last GPS fix we used
	 */
	private long lastGPSTimestamp = 0;
	
	/**
	 * the interval (in ms) to log GPS fixes defined in the preferences
	 */
	private long gpsLoggingInterval;
	private long gpsLoggingMinDistance;
	
	/**
	 * sensors for magnetic orientation
	 */
	private SensorListener sensorListener = new SensorListener();

	/**
	 * sensor for atmospheric pressure
	 */
	private PressureListener pressureListener = new PressureListener();

	/**
	 * Receives Intent for way point tracking, and stop/start logging.
	 */
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(TAG, "Received intent " + intent.getAction());

			if (OSMTracker.INTENT_TRACK_WP.equals(intent.getAction())) {
				// Track a way point
				Bundle extras = intent.getExtras();
				if (extras != null) {
					// because of the gps logging interval our last fix could be very old
					// so we'll request the last known location from the gps provider
					if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
						lastLocation_gps = lmgr_gps.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						lastLocation_network = lmgr_network.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						if (lastLocation_gps != null) {
							Long trackId = extras.getLong(TrackContentProvider.Schema.COL_TRACK_ID);
							String uuid = extras.getString(OSMTracker.INTENT_KEY_UUID);
							String name = extras.getString(OSMTracker.INTENT_KEY_NAME);
							String link = extras.getString(OSMTracker.INTENT_KEY_LINK);

							dataHelper.wayPoint(trackId, lastLocation_gps, name, link, uuid, sensorListener.getAzimuth(), sensorListener.getAccuracy(), pressureListener.getPressure());

							// If there is a waypoint in the track, there should also be a trackpoint
							dataHelper.track(currentTrackId, lastLocation_gps, sensorListener.getAzimuth(), sensorListener.getAccuracy(), pressureListener.getPressure());
						}if (lastLocation_network != null) {
							Long trackId = extras.getLong(TrackContentProvider.Schema.COL_TRACK_ID);
							String uuid = extras.getString(OSMTracker.INTENT_KEY_UUID);
							String name = extras.getString(OSMTracker.INTENT_KEY_NAME);
							String link = extras.getString(OSMTracker.INTENT_KEY_LINK);

							dataHelper.wayPoint(trackId, lastLocation_network, name, link, uuid, sensorListener.getAzimuth(), sensorListener.getAccuracy(), pressureListener.getPressure());

							// If there is a waypoint in the track, there should also be a trackpoint
							dataHelper.track(currentTrackId, lastLocation_network, sensorListener.getAzimuth(), sensorListener.getAccuracy(), pressureListener.getPressure());
						}
					}
				}
			} else if (OSMTracker.INTENT_UPDATE_WP.equals(intent.getAction())) {
				// Update an existing waypoint
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Long trackId = extras.getLong(TrackContentProvider.Schema.COL_TRACK_ID);
					String uuid = extras.getString(OSMTracker.INTENT_KEY_UUID);
					String name = extras.getString(OSMTracker.INTENT_KEY_NAME);
					String link = extras.getString(OSMTracker.INTENT_KEY_LINK);
					dataHelper.updateWayPoint(trackId, uuid, name, link);
				}
			} else if (OSMTracker.INTENT_DELETE_WP.equals(intent.getAction())) {
				// Delete an existing waypoint
				Bundle extras = intent.getExtras();
				if (extras != null) {
					Long trackId = extras.getLong(TrackContentProvider.Schema.COL_TRACK_ID);
					String uuid = extras.getString(OSMTracker.INTENT_KEY_UUID);
					String link = extras.getString(OSMTracker.INTENT_KEY_LINK);
					String filePath = null;
					try {
						filePath = link.equals("null") ? null : DataHelper.getTrackDirectory(trackId, context) + "/" + link;
					}
					catch(NullPointerException ne){}
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
	
	/**
	 * Binder for service interaction
	 */
	private final IBinder binder = new GPSLoggerBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "Service onBind()");
		return binder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "Service onUnbind()");
		// If we aren't currently tracking we can
		// stop ourselves
		if (! isTracking ) {
			Log.v(TAG, "Service self-stopping");
			stopSelf();
		}
		
		// We don't want onRebind() to be called, so return false.
		return false;
	}

	/**
	 * Bind interface for service interaction
	 */
	public class GPSLoggerBinder extends Binder {

		/**
		 * Called by the activity when binding.
		 * Returns itself.
		 * @return the GPS Logger service
		 */
		public GPSLogger getService() {			
			return GPSLogger.this;
		}
	}
	
	@Override
	public void onCreate() {
		Log.v(TAG, "Service onCreate()");
		dataHelper = new DataHelper(this);

		//read the logging interval from preferences
		gpsLoggingInterval = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString(
				OSMTracker.Preferences.KEY_GPS_LOGGING_INTERVAL, OSMTracker.Preferences.VAL_GPS_LOGGING_INTERVAL)) * 1000;
		gpsLoggingMinDistance = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getString(
				OSMTracker.Preferences.KEY_GPS_LOGGING_MIN_DISTANCE, OSMTracker.Preferences.VAL_GPS_LOGGING_MIN_DISTANCE));
		use_barometer = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean(
				OSMTracker.Preferences.KEY_USE_BAROMETER, OSMTracker.Preferences.VAL_USE_BAROMETER);

		// Register our broadcast receiver
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

		// Register ourselves for location updates
		lmgr_gps = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lmgr_network = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			lmgr_gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsLoggingInterval, gpsLoggingMinDistance, this);
			lmgr_network.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, gpsLoggingInterval, gpsLoggingMinDistance, this);
		}
		
		//register for Orientation updates
		sensorListener.register(this);

		// register for atmospheric pressure updates
		pressureListener.register(this, use_barometer);

		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "Service onStartCommand(-,"+flags+","+startId+")");
		createNotificationChannel();
		startForeground(NOTIFICATION_ID, getNotification());
		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "Service onDestroy()");
		if (isTracking) {
			// If we're currently tracking, save user data.
			stopTrackingAndSave();
		}

		// Unregister listener
		lmgr_gps.removeUpdates(this);
		lmgr_network.removeUpdates(this);

		// Unregister broadcast receiver
		unregisterReceiver(receiver);
		
		// Cancel any existing notification
		stopNotifyBackgroundService();
		
		// stop sensors
		sensorListener.unregister();
		pressureListener.unregister();

		super.onDestroy();
	}

	/**
	 * Start GPS tracking.
	 */
	private void startTracking(long trackId) {
		currentTrackId = trackId;
		Log.v(TAG, "Starting track logging for track #" + trackId);
		// Refresh notification with correct Track ID
		NotificationManager nmgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nmgr.notify(NOTIFICATION_ID, getNotification());
		isTracking = true;
	}

	/**
	 * Stops GPS Logging
	 */
	private void stopTrackingAndSave() {
		isTracking = false;
		dataHelper.stopTracking(currentTrackId);
		currentTrackId = -1;
		this.stopSelf();
	}

	@Override
	public void onLocationChanged(Location location) {		
		// We're receiving location, so GPS is enabled
		isGpsEnabled = true;
		
		// first of all we check if the time from the last used fix to the current fix is greater than the logging interval
		if((lastGPSTimestamp + gpsLoggingInterval) < System.currentTimeMillis()){
			lastGPSTimestamp = System.currentTimeMillis(); // save the time of this fix
		
			lastLocation_gps = location;
			lastLocation_network=location;
			if (isTracking) {
				dataHelper.track(currentTrackId, location, sensorListener.getAzimuth(), sensorListener.getAccuracy(), pressureListener.getPressure());
			}
		}
	}

	/**
	 * Builds the notification to display when tracking in background.
	 */
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
		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			// FIXME: following two strings must be obtained from 'R.string' to support translations
			CharSequence name = "GPS Logger";
			String description = "Display when tracking in Background";
			int importance = NotificationManager.IMPORTANCE_LOW;
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
			channel.setDescription(description);
			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.createNotificationChannel(channel);
		}
	}


	/**
	 * Stops notifying the user that we're tracking in the background
	 */
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
		// Not interested in provider status			
	}

	/**
	 * Getter for gpsEnabled
	 * @return true if GPS is enabled, otherwise false.
	 */
	public boolean isGpsEnabled() {
		return isGpsEnabled;
	}
	
	/**
	 * Setter for isTracking
	 * @return true if we're currently tracking, otherwise false.
	 */
	public boolean isTracking() {
		return isTracking;
	}

}
