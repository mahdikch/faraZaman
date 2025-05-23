package net.osmtracker.activity;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.data.db.TrackContentProvider;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class SubmitViolationActivity extends AppCompatActivity {
    private LocationManager locationManager;
    private SharedPreferences prefs = null;
    private MapView osmView;
    private IMapController osmViewController;
    private static final int DEFAULT_ZOOM = 16;
    private static final String CURRENT_SCROLL_X = "currentScrollX";
    private boolean centerToGpsPos = true;
    private GeoPoint currentPosition=new GeoPoint(35.7627,51.3353);

    private Marker userLocationMarker;
    /**
     * Key for keeping scrolled top position of OSM view across activity re-creation
     */
    private static final String CURRENT_CENTER_TO_GPS_POS = "currentCenterToGpsPos";
    private static final String CURRENT_SCROLL_Y = "currentScrollY";
    private static final double CENTER_DEFAULT_ZOOM_LEVEL = 18;
    private static final String CURRENT_ZOOM = "currentZoom";
    private static final String CURRENT_ZOOMED_TO_TRACK = "currentZoomedToTrack";
    private boolean zoomedToTrackAlready = false;
    private static final String LAST_ZOOM = "lastZoomLevel";
    private static final long ANIMATION_DURATION_MS = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_submit_violation);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
        } else {
            startGettingLocation();
        }


        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Button submitViolation = findViewById(R.id.submit_violation);
        Intent intent = new Intent(this, SubmitViolationFormActivity.class);
        submitViolation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(intent);
            }
        });
        // Initialize OSM view
        Configuration.getInstance().load(this, prefs);

        osmView = findViewById(R.id.map_view);
        // pinch to zoom
        osmView.setMultiTouchControls(true);
        osmView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        // we'll use osmView to define if the screen is always on or not
        osmView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));
        osmViewController = osmView.getController();
//osmViewController.animateTo();
        // Check if there is a saved zoom level
        if (savedInstanceState != null) {
            osmViewController.setZoom(savedInstanceState.getInt(CURRENT_ZOOM, DEFAULT_ZOOM));
            osmView.scrollTo(savedInstanceState.getInt(CURRENT_SCROLL_X, 0),
                    savedInstanceState.getInt(CURRENT_SCROLL_Y, 0));
            centerToGpsPos = savedInstanceState.getBoolean(CURRENT_CENTER_TO_GPS_POS, centerToGpsPos);
            zoomedToTrackAlready = savedInstanceState.getBoolean(CURRENT_ZOOMED_TO_TRACK, zoomedToTrackAlready);
        } else {
            // Try to get last zoom Level from Shared Preferences
            SharedPreferences settings = getPreferences(MODE_PRIVATE);
            osmViewController.setZoom(settings.getInt(LAST_ZOOM, DEFAULT_ZOOM));
        }

        selectTileSource();

        setTileDpiScaling();

        // Register listeners for zoom buttons
        findViewById(R.id.displaytrackmap_imgZoomIn).setOnClickListener(v -> osmViewController.zoomIn());
        findViewById(R.id.displaytrackmap_imgZoomOut).setOnClickListener(v -> osmViewController.zoomOut());
        findViewById(R.id.displaytrackmap_imgZoomCenter).setOnClickListener(view -> {
            centerToGpsPos = true;
            if (currentPosition != null) {
                osmViewController.animateTo(currentPosition, CENTER_DEFAULT_ZOOM_LEVEL, ANIMATION_DURATION_MS);
            }
        });
    }
    private void startGettingLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // ثبت درخواست لوکیشن از GPS و Network
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, locationListener);

                // دریافت آخرین موقعیت موجود (اگر هست)
//                Location lastKnownGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                Location lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//
//                if (lastKnownGps != null) {
//                    currentPosition = new GeoPoint(lastKnownGps.getLatitude(), lastKnownGps.getLongitude());
//                    osmViewController.animateTo(currentPosition, CENTER_DEFAULT_ZOOM_LEVEL, ANIMATION_DURATION_MS);
//                } else if (lastKnownNetwork != null) {
//                    currentPosition = new GeoPoint(lastKnownNetwork.getLatitude(), lastKnownNetwork.getLongitude());
//                    osmViewController.animateTo(currentPosition, CENTER_DEFAULT_ZOOM_LEVEL, ANIMATION_DURATION_MS);
//                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            currentPosition = new GeoPoint(location.getLatitude(), location.getLongitude());
            if (userLocationMarker == null) {
                userLocationMarker = new Marker(osmView);
                userLocationMarker.setPosition(currentPosition);
                userLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                userLocationMarker.setTitle("موقعیت فعلی");
                osmView.getOverlays().add(userLocationMarker);
            } else {
                userLocationMarker.setPosition(currentPosition);
            }

            if (centerToGpsPos) {
                osmViewController.animateTo(currentPosition, CENTER_DEFAULT_ZOOM_LEVEL, ANIMATION_DURATION_MS);
            }

            osmView.invalidate();
        }

        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onProviderDisabled(String provider) {}
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                startGettingLocation();
            } else {
                Log.w("SubmitViolationActivity", "Location permission denied");
            }
        }
    }
    public void selectTileSource() {
        String mapTile = prefs.getString(OSMTracker.Preferences.KEY_UI_MAP_TILE, OSMTracker.Preferences.VAL_UI_MAP_TILE_MAPNIK);
        Log.e("TileMapName active", mapTile);
        //osmView.setTileSource(selectMapTile(mapTile));
        osmView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
    }

    public void setTileDpiScaling() {
        osmView.setTilesScaledToDpi(true);
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startGettingLocation();
        }
    }
}