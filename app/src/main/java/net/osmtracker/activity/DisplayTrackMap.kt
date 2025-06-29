package net.osmtracker.activity

import android.app.AlertDialog
import android.content.ContentUris
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Color
import android.graphics.Paint
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
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
import net.osmtracker.activity.TrackLogger.Companion
import net.osmtracker.data.db.TrackContentProvider
import net.osmtracker.data.model.RoadData
import net.osmtracker.layout.GpsStatusRecord
import net.osmtracker.layout.GpsStatusRecordDisplay
import net.osmtracker.listener.PressureListener
import net.osmtracker.listener.SensorListener
import net.osmtracker.overlay.WayPointsOverlay
import net.osmtracker.service.gps.GPSLogger
import net.osmtracker.service.gps.GPSLoggerServiceConnection
import net.osmtracker.service.gps.GPSLoggerServiceConnectionDisplay
import net.osmtracker.service.remote.RoadService
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay
import javax.inject.Inject

/**
 * Display current track over an OSM map.
 * Based on <a href="http://osmdroid.googlecode.com/">osmdroid code</a>
 *<P>
 * Used only if {@link OSMTracker.Preferences#KEY_UI_DISPLAYTRACK_OSM} is set.
 * Otherwise {@link DisplayTrack} is used (track only, no OSM background tiles).
 *
 * @author Viesturs Zarins
 */
@AndroidEntryPoint
class DisplayTrackMap : AppCompatActivity() {

    companion object {

        private const val TAG = "DisplayTrackMap"
        private const val CURRENT_ZOOM = "currentZoom"
        private const val CURRENT_SCROLL_X = "currentScrollX"
        private const val CURRENT_SCROLL_Y = "currentScrollY"
        private const val CURRENT_CENTER_TO_GPS_POS = "currentCenterToGpsPos"
        private const val CURRENT_ZOOMED_TO_TRACK = "currentZoomedToTrack"
        private const val LAST_ZOOM = "lastZoomLevel"
        private const val DEFAULT_ZOOM = 16
        private const val CENTER_DEFAULT_ZOOM_LEVEL = 18.0
        private const val ANIMATION_DURATION_MS = 1000L
    }

    private lateinit var osmView: MapView
    private lateinit var osmViewController: IMapController
    private lateinit var myLocationOverlay: SimpleLocationOverlay
    private lateinit var polyline: Polyline
    private lateinit var wayPointsOverlay: WayPointsOverlay
    private lateinit var scaleBarOverlay: ScaleBarOverlay
    private lateinit var prefs: SharedPreferences

    private var currentTrackId: Long = 0
    private var centerToGpsPos = true
    private var zoomedToTrackAlready = false
    private var currentPosition: GeoPoint? = null
    private var lastTrackPointIdProcessed: Int? = null
    private lateinit var trackpointContentObserver: ContentObserver
    private var gpsLoggerServiceIntent: Intent? = null
    private var sensorListener: SensorListener? = null
    private var pressureListener: PressureListener? = null
    @Inject
    lateinit var roadService: RoadService
    private var checkGPSFlag = true
    private var gpsLogger: GPSLogger? = null

    private lateinit var roadNameTextView: TextView
    private lateinit var roadTypeTextView: TextView
    private lateinit var speedLimitTextView: TextView
    private var gpsLoggerConnection: ServiceConnection = GPSLoggerServiceConnectionDisplay(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentTrackId = intent.extras?.getLong(TrackContentProvider.Schema.COL_TRACK_ID) ?: 0
        
        // Validate track ID
        if (currentTrackId <= 0) {
            Log.e(TAG, "Invalid track ID: $currentTrackId")
            Toast.makeText(this, "خطا: شناسه مسیر نامعتبر است", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        gpsLoggerServiceIntent = Intent(this, GPSLogger::class.java).apply {
            putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId)
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setContentView(R.layout.displaytrackmap)

        title = "${title}: #$currentTrackId"

        initializeViews()
        initializeMap(savedInstanceState)
        setupZoomControls()
        setupSubmitButton()
        sensorListener = SensorListener()
        pressureListener = PressureListener()
    }

    private fun checkGPSProvider() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.tracklogger_gps_disabled)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getString(R.string.tracklogger_gps_disabled_hint))
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton(android.R.string.no) { dialog, _ ->
                    dialog.cancel()
                }
                .create().show()
            checkGPSFlag = false
        }
    }
    private fun setupSubmitButton() {
        findViewById<Button>(R.id.submit_violation).setOnClickListener {
            startActivity(Intent(this, SubmitViolationFormActivity::class.java))
        }
    }
    private fun initializeViews() {
        roadNameTextView = findViewById(R.id.road_name)
        roadTypeTextView = findViewById(R.id.road_type)
        speedLimitTextView = findViewById(R.id.speed_limit)
    }
    fun setGpsLogger(l: GPSLogger) { gpsLogger = l }
    private fun initializeMap(savedInstanceState: Bundle?) {
        Configuration.getInstance().load(this, prefs)

        osmView = findViewById(R.id.displaytrackmap_osmView)
        osmView.setMultiTouchControls(true)
        osmView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        osmView.keepScreenOn = prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON)
        osmViewController = osmView.controller

        if (savedInstanceState != null) {
            osmViewController.setZoom(savedInstanceState.getInt(CURRENT_ZOOM, DEFAULT_ZOOM))
            osmView.scrollTo(
                savedInstanceState.getInt(CURRENT_SCROLL_X, 0),
                savedInstanceState.getInt(CURRENT_SCROLL_Y, 0)
            )
            centerToGpsPos = savedInstanceState.getBoolean(CURRENT_CENTER_TO_GPS_POS, centerToGpsPos)
            zoomedToTrackAlready = savedInstanceState.getBoolean(CURRENT_ZOOMED_TO_TRACK, zoomedToTrackAlready)
        } else {
            val settings = getPreferences(MODE_PRIVATE)
            osmViewController.setZoom(settings.getInt(LAST_ZOOM, DEFAULT_ZOOM))
        }

        selectTileSource()
        setTileDpiScaling()
        createOverlays()

        trackpointContentObserver = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                pathChanged()
            }
        }
    }

    private fun setupZoomControls() {
        findViewById<View>(R.id.displaytrackmap_imgZoomIn).setOnClickListener { osmViewController.zoomIn() }
        findViewById<View>(R.id.displaytrackmap_imgZoomOut).setOnClickListener { osmViewController.zoomOut() }
        findViewById<View>(R.id.displaytrackmap_imgZoomCenter).setOnClickListener {
            centerToGpsPos = true
            currentPosition?.let { pos ->
                osmViewController.animateTo(pos, CENTER_DEFAULT_ZOOM_LEVEL, ANIMATION_DURATION_MS)
            }
        }
    }

    private fun fetchRoadData(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            try {
                val token = prefs.getString("ACCESS_TOKEN", null)
                
                if (token == null) {
                    Log.e(TAG, "No access token found")
                    return@launch
                }

                val roadData = roadService.getRoadData(latitude, longitude, 30, "Bearer $token")
                roadData.firstOrNull()?.let {
                    updateRoadInfo(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching road data", e)
            }
        }
    }

    private fun updateRoadInfo(roadData: RoadData) {
        roadNameTextView.text = "نام خیابان: ${roadData.name}"
        roadTypeTextView.text = "نوع خیابان: ${roadData.fclass}"
        speedLimitTextView.text = "محدودیت سرعت: ${roadData.maxspeed} km/h"
    }

    fun selectTileSource() {
        val mapTile = prefs.getString(OSMTracker.Preferences.KEY_UI_MAP_TILE, OSMTracker.Preferences.VAL_UI_MAP_TILE_MAPNIK)
        Log.e("TileMapName active", mapTile ?: "")
        osmView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
    }

    fun setTileDpiScaling() {
        osmView.setTilesScaledToDpi(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_ZOOM, osmView.zoomLevel)
        outState.putInt(CURRENT_SCROLL_X, osmView.scrollX)
        outState.putInt(CURRENT_SCROLL_Y, osmView.scrollY)
        outState.putBoolean(CURRENT_CENTER_TO_GPS_POS, centerToGpsPos)
        outState.putBoolean(CURRENT_ZOOMED_TO_TRACK, zoomedToTrackAlready)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        resumeActivity()

        if (checkGPSFlag && prefs?.getBoolean(OSMTracker.Preferences.KEY_GPS_CHECKSTARTUP, OSMTracker.Preferences.VAL_GPS_CHECKSTARTUP) == true) {
            checkGPSProvider()
        }
        (findViewById<View>(R.id.gpsStatus) as GpsStatusRecordDisplay).requestLocationUpdates(true)
        startService(gpsLoggerServiceIntent)
        gpsLoggerServiceIntent?.let { bindService(it, gpsLoggerConnection, 0) }
        sensorListener?.register(this)
        pressureListener?.register(this, prefs?.getBoolean(OSMTracker.Preferences.KEY_USE_BAROMETER, OSMTracker.Preferences.VAL_USE_BAROMETER) == true)

    }

    private fun resumeActivity() {
        osmView.keepScreenOn = prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON)

        contentResolver.registerContentObserver(
            TrackContentProvider.trackPointsUri(currentTrackId),
            true, trackpointContentObserver
        )

        lastTrackPointIdProcessed = null
        pathChanged()
        selectTileSource()
        setTileDpiScaling()
        wayPointsOverlay.refresh()
    }

    override fun onPause() {
        contentResolver.unregisterContentObserver(trackpointContentObserver)
        polyline.setPoints(emptyList())
        (findViewById<View>(R.id.gpsStatus) as GpsStatusRecordDisplay).requestLocationUpdates(false)

        gpsLogger?.let {
            if (!it.isTracking) {
                Log.v(TAG, "Service is not tracking, trying to stopService()")
                unbindService(gpsLoggerConnection)
                stopService(gpsLoggerServiceIntent)
            } else {
                unbindService(gpsLoggerConnection)
            }
        }
        sensorListener?.unregister()
        pressureListener?.unregister()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        val settings = getPreferences(MODE_PRIVATE)
        settings.edit().apply {
            putInt(LAST_ZOOM, osmView.zoomLevel)
            apply()
        }
    }
    fun getGpsLogger(): GPSLogger? = gpsLogger
    fun getCurrentTrackId(): Long = currentTrackId
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.displaytrackmap_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.displaytrackmap_menu_center_to_gps).isEnabled = !centerToGpsPos && currentPosition != null
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.displaytrackmap_menu_center_to_gps -> {
                centerToGpsPos = true
                currentPosition?.let { osmViewController.animateTo(it) }
            }
            R.id.displaytrackmap_menu_settings -> {
                startActivity(Intent(this, Preferences::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_MOVE && currentPosition != null) {
            centerToGpsPos = false
        }
        return super.onTouchEvent(event)
    }

    private fun createOverlays() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        polyline = Polyline()
        val paint = polyline.outlinePaint
        paint.color = Color.BLUE
        paint.strokeWidth = (metrics.densityDpi / 25.4f / 2)
        osmView.overlayManager.add(polyline)

        myLocationOverlay = SimpleLocationOverlay(this)
        osmView.overlays.add(myLocationOverlay)

        wayPointsOverlay = WayPointsOverlay(this, currentTrackId)
        osmView.overlays.add(wayPointsOverlay)

        scaleBarOverlay = ScaleBarOverlay(osmView)
        osmView.overlays.add(scaleBarOverlay)
    }

    private fun pathChanged() {
        if (isFinishing) return

        var doInitialBoundsCalc = false
        var minLat = 91.0
        var minLon = 181.0
        var maxLat = -91.0
        var maxLon = -181.0

        if (!zoomedToTrackAlready && lastTrackPointIdProcessed == null) {
            val projActive = arrayOf(TrackContentProvider.Schema.COL_ACTIVE)
            contentResolver.query(
                ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, currentTrackId),
                projActive, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val colIndex = cursor.getColumnIndex(TrackContentProvider.Schema.COL_ACTIVE)
                    if (colIndex != -1) {
                        doInitialBoundsCalc = cursor.getInt(colIndex) == TrackContentProvider.Schema.VAL_TRACK_INACTIVE
                    }
                }
            }
        }

        val projection = arrayOf(
            TrackContentProvider.Schema.COL_LATITUDE,
            TrackContentProvider.Schema.COL_LONGITUDE,
            TrackContentProvider.Schema.COL_ID
        )

        var selection: String? = null
        var selectionArgs: Array<String>? = null

        if (lastTrackPointIdProcessed != null) {
            selection = "${TrackContentProvider.Schema.COL_ID} > ?"
            selectionArgs = arrayOf(lastTrackPointIdProcessed.toString())
        }

        contentResolver.query(
            TrackContentProvider.trackPointsUri(currentTrackId),
            projection, selection, selectionArgs, "${TrackContentProvider.Schema.COL_ID} asc"
        )?.use { c ->
            val numberOfPointsRetrieved = c.count
            if (numberOfPointsRetrieved > 0) {
                c.moveToFirst()
                var lastLat = 0.0
                var lastLon = 0.0
                val primaryKeyColumnIndex = c.getColumnIndex(TrackContentProvider.Schema.COL_ID)
                val latitudeColumnIndex = c.getColumnIndex(TrackContentProvider.Schema.COL_LATITUDE)
                val longitudeColumnIndex = c.getColumnIndex(TrackContentProvider.Schema.COL_LONGITUDE)

                while (!c.isAfterLast) {
                    lastLat = c.getDouble(latitudeColumnIndex)
                    lastLon = c.getDouble(longitudeColumnIndex)
                    lastTrackPointIdProcessed = c.getInt(primaryKeyColumnIndex)
                    polyline.addPoint(GeoPoint(lastLat, lastLon))
                    if (doInitialBoundsCalc) {
                        if (lastLat < minLat) minLat = lastLat
                        if (lastLon < minLon) minLon = lastLon
                        if (lastLat > maxLat) maxLat = lastLat
                        if (lastLon > maxLon) maxLon = lastLon
                    }
                    c.moveToNext()
                }

                currentPosition = GeoPoint(lastLat, lastLon)
                myLocationOverlay.setLocation(currentPosition)
                if (centerToGpsPos) {
                    osmViewController.setCenter(currentPosition)
                }

                // Fetch road data for the current position
                fetchRoadData(lastLat, lastLon)

                osmView.invalidate()
                if (doInitialBoundsCalc && numberOfPointsRetrieved > 1) {
                    val north = maxLat
                    val east = maxLon
                    val south = minLat
                    val west = minLon
                    osmView.post {
                        osmViewController.zoomToSpan((north - south).toInt(), (east - west).toInt())
                        osmViewController.setCenter(GeoPoint((north + south) / 2, (east + west) / 2))
                        zoomedToTrackAlready = true
                    }
                }
            }
        }
    }
} 