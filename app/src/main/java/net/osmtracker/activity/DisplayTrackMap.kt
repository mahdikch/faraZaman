package net.osmtracker.activity

import android.content.ContentUris
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.database.ContentObserver
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.osmtracker.OSMTracker
import net.osmtracker.R
import net.osmtracker.data.db.TrackContentProvider
import net.osmtracker.layout.GpsStatusRecordDisplay
import net.osmtracker.listener.SensorListener
import net.osmtracker.overlay.WayPointsOverlay
import net.osmtracker.service.gps.GPSLogger
import net.osmtracker.service.gps.GPSLoggerServiceConnectionDisplay
import net.osmtracker.service.remote.RoadService
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import javax.inject.Inject

@AndroidEntryPoint
class DisplayTrackMap : AppCompatActivity() {

    companion object {
        private const val TAG = "DisplayTrackMap"
        private const val KEY_PLAYBACK_MODE = "playback_mode"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        private const val ROAD_INFO_UPDATE_INTERVAL_MS = 5000L
    }

    private lateinit var osmView: MapView
    private lateinit var osmViewController: IMapController
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var polyline: Polyline
    private lateinit var wayPointsOverlay: WayPointsOverlay
    private lateinit var prefs: SharedPreferences
    private var currentTrackId: Long = 0
    private var zoomedToTrackAlready = false
    private var isPlaybackMode = false
    private lateinit var trackpointContentObserver: ContentObserver
    private var gpsLoggerServiceIntent: Intent? = null
    private var sensorListener: SensorListener? = null
    private var gpsLogger: GPSLogger? = null
    private var gpsLoggerConnection: ServiceConnection = GPSLoggerServiceConnectionDisplay(this)

    @Inject
    lateinit var roadService: RoadService
    private lateinit var roadNameTextView: TextView
    private lateinit var roadTypeTextView: TextView
    private lateinit var speedLimitTextView: TextView
    private val roadInfoUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var roadInfoRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        Configuration.getInstance().load(this, prefs)
        setContentView(R.layout.displaytrackmap)

        isPlaybackMode = intent.getBooleanExtra(KEY_PLAYBACK_MODE, false)
        currentTrackId = intent.extras?.getLong(TrackContentProvider.Schema.COL_TRACK_ID) ?: 0

        if (currentTrackId <= 0) {
            finish()
            return
        }

        if (!isPlaybackMode) {
            gpsLoggerServiceIntent = Intent(this, GPSLogger::class.java).apply {
                action = OSMTracker.INTENT_START_TRACKING
                putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId)
            }
            sensorListener = SensorListener()
        }

        title = if (isPlaybackMode) "نمایش مسیر: #$currentTrackId" else "ردیابی مسیر: #$currentTrackId"

        initializeViews()
        initializeMap()
        setupSubmitButton()
        initializeRoadInfoUpdater()
    }

    private fun initializeViews() {
        roadNameTextView = findViewById(R.id.road_name)
        roadTypeTextView = findViewById(R.id.road_type)
        speedLimitTextView = findViewById(R.id.speed_limit)
    }

    // این متد حالا مسئول شروع تایمر است
    fun setGpsLogger(l: GPSLogger) {
        gpsLogger = l
        // بعد از اینکه سرویس متصل شد، بررسی می‌کنیم که آیا باید تایمر را روشن کنیم
        if (!isPlaybackMode && gpsLogger?.isTracking == true) {
            // هر فراخوانی قبلی را حذف می‌کنیم تا تایمر چندبار اجرا نشود
            roadInfoUpdateHandler.removeCallbacks(roadInfoRunnable)
            // تایمر را برای اولین بار فعال می‌کنیم
            roadInfoUpdateHandler.post(roadInfoRunnable)
        }
    }

    fun getGpsLogger(): GPSLogger? = gpsLogger
    fun getCurrentTrackId(): Long = currentTrackId

    private fun initializeMap() {
        osmView = findViewById(R.id.displaytrackmap_osmView)
        osmView.setTileSource(TileSourceFactory.MAPNIK)
        osmView.setMultiTouchControls(true)
        osmView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        osmView.keepScreenOn = prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, true)
        osmViewController = osmView.controller
        osmViewController.setZoom(16.0)
        createOverlays()
        setupMapButtons()
        trackpointContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                pathChanged()
            }
        }
    }

    private fun setupMapButtons() {
        val zoomInButton = findViewById<View>(R.id.displaytrackmap_imgZoomIn)
        val zoomOutButton = findViewById<View>(R.id.displaytrackmap_imgZoomOut)
        val myLocationButton = findViewById<View>(R.id.displaytrackmap_imgZoomCenter)
        zoomInButton.setOnClickListener { osmViewController.zoomIn() }
        zoomOutButton.setOnClickListener { osmViewController.zoomOut() }
        myLocationButton.setOnClickListener {
            if (!isPlaybackMode) {
                myLocationOverlay.myLocation?.let {
                    osmViewController.animateTo(it)
                    osmViewController.setZoom(18.0)
                } ?: Toast.makeText(this, "در حال یافتن موقعیت مکانی...", Toast.LENGTH_SHORT).show()
            }
        }
        if (isPlaybackMode) myLocationButton.visibility = View.GONE
    }

    private fun initializeRoadInfoUpdater() {
        roadInfoRunnable = Runnable {
            if (!isPlaybackMode && gpsLogger?.isTracking == true) {
                myLocationOverlay.myLocation?.let { location ->
                    fetchAndDisplayRoadInfo(location.latitude, location.longitude)
                }
                roadInfoUpdateHandler.postDelayed(roadInfoRunnable, ROAD_INFO_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun fetchAndDisplayRoadInfo(lat: Double, lon: Double) {
        val token = prefs.getString("ACCESS_TOKEN", null)
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Access token is missing.")
            return
        }
        lifecycleScope.launch {
            try {
                val roadDataList = roadService.getRoadData(latitude = lat, longitude = lon, token = "Bearer $token")
                if (roadDataList.isNotEmpty()) {
                    val firstRoadResult = roadDataList[0]
                    roadNameTextView.text = firstRoadResult.name
                    roadTypeTextView.text = firstRoadResult.fclass
                    speedLimitTextView.text = firstRoadResult.maxspeed.toString()
                } else {
                    roadNameTextView.text = "آدرسی یافت نشد"
                    roadTypeTextView.text = ""
                    speedLimitTextView.text = ""
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch road data", e)
                roadNameTextView.text = "خطا در دریافت اطلاعات"
                roadTypeTextView.text = ""
                speedLimitTextView.text = ""
            }
        }
    }

    private fun createOverlays() {
        polyline = Polyline().apply {
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = 8.0f
        }
        osmView.overlayManager.add(polyline)
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), osmView).apply {
            enableMyLocation()
            enableFollowLocation()
            isEnabled = !isPlaybackMode
        }
        osmView.overlays.add(myLocationOverlay)
        wayPointsOverlay = WayPointsOverlay(this, currentTrackId)
        osmView.overlays.add(wayPointsOverlay)
    }

    override fun onResume() {
        super.onResume()
        osmView.onResume()
        if (!isPlaybackMode) {
            findViewById<GpsStatusRecordDisplay>(R.id.gpsStatus).requestLocationUpdates(true)
            myLocationOverlay.enableMyLocation()
            startService(gpsLoggerServiceIntent)
            gpsLoggerServiceIntent?.let { bindService(it, gpsLoggerConnection, 0) }
            sensorListener?.register(this)
            // فراخوانی تایمر از اینجا حذف شد
        } else {
            findViewById<View>(R.id.gpsStatus).visibility = View.GONE
        }
        contentResolver.registerContentObserver(TrackContentProvider.trackPointsUri(currentTrackId), true, trackpointContentObserver)
        zoomedToTrackAlready = false
        pathChanged()
        wayPointsOverlay.refresh()
    }

    override fun onPause() {
        super.onPause()
        osmView.onPause()
        roadInfoUpdateHandler.removeCallbacks(roadInfoRunnable)
        contentResolver.unregisterContentObserver(trackpointContentObserver)
        if (!isPlaybackMode) {
            myLocationOverlay.disableMyLocation()
            try {
                if (gpsLogger != null) unbindService(gpsLoggerConnection)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Service was not registered or already unbound.")
            }
            findViewById<GpsStatusRecordDisplay>(R.id.gpsStatus).requestLocationUpdates(false)
            sensorListener?.unregister()
        }
    }

    private fun pathChanged() {
        if (isFinishing) return

        val points = mutableListOf<GeoPoint>()
        var minLat = 91.0
        var minLon = 181.0
        var maxLat = -91.0
        var maxLon = -181.0

        val projection = arrayOf(TrackContentProvider.Schema.COL_LATITUDE, TrackContentProvider.Schema.COL_LONGITUDE)
        contentResolver.query(
            TrackContentProvider.trackPointsUri(currentTrackId),
            projection, null, null, "${TrackContentProvider.Schema.COL_ID} asc"
        )?.use { c ->
            if (c.moveToFirst()) {
                val latCol = c.getColumnIndex(TrackContentProvider.Schema.COL_LATITUDE)
                val lonCol = c.getColumnIndex(TrackContentProvider.Schema.COL_LONGITUDE)
                do {
                    val lat = c.getDouble(latCol)
                    val lon = c.getDouble(lonCol)
                    points.add(GeoPoint(lat, lon))

                    if (lat < minLat) minLat = lat
                    if (lon < minLon) minLon = lon
                    if (lat > maxLat) maxLat = lat
                    if (lon > maxLon) maxLon = lon
                } while (c.moveToNext())
            }
        }
        polyline.setPoints(points)
        osmView.invalidate()

        if (!zoomedToTrackAlready && points.isNotEmpty()) {
            if (points.size > 1) {
                osmView.post {
                    val boundingBox = BoundingBox(maxLat, maxLon, minLat, minLon)
                    osmView.zoomToBoundingBox(boundingBox, true, 50)
                    zoomedToTrackAlready = true
                }
            } else {
                osmViewController.animateTo(points.first())
                osmViewController.setZoom(18.0)
                zoomedToTrackAlready = true
            }
        }
    }

    private fun setupSubmitButton() {
        val submitButton = findViewById<Button>(R.id.submit_violation)
        if (isPlaybackMode) {
            submitButton.visibility = View.GONE
        } else {
            submitButton.setOnClickListener {
                // دریافت موقعیت مکانی فعلی از لایه نقشه
                val currentLocation: GeoPoint? = myLocationOverlay.myLocation
                // دریافت متن آدرس فعلی از TextView
                val currentAddress: String = roadNameTextView.text.toString()

                if (currentLocation != null) {
                    // ارسال هر سه مقدار به اکتیویتی فرم
                    val intent = Intent(this, SubmitViolationFormActivity::class.java).apply {
                        putExtra(EXTRA_LATITUDE, currentLocation.latitude)
                        putExtra(EXTRA_LONGITUDE, currentLocation.longitude)
                        putExtra("extra_address", currentAddress) // ارسال آدرس به عنوان یک رشته
                        putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId) // <-- این خط را اضافه کنید
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "موقعیت مکانی هنوز یافت نشده است.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}