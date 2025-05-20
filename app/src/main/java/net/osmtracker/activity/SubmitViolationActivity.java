package net.osmtracker.activity;



import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.data.db.TrackContentProvider;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;

public class SubmitViolationActivity extends AppCompatActivity {
    private SharedPreferences prefs = null;
    private MapView osmView;
    private IMapController osmViewController;
    private static final int DEFAULT_ZOOM = 16;
    private static final String CURRENT_SCROLL_X = "currentScrollX";
    private boolean centerToGpsPos = true;
    private GeoPoint currentPosition;
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
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialize OSM view
        Configuration.getInstance().load(this, prefs);

        osmView = findViewById(R.id.map_view);
        // pinch to zoom
        osmView.setMultiTouchControls(true);
        osmView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        // we'll use osmView to define if the screen is always on or not
        osmView.setKeepScreenOn(prefs.getBoolean(OSMTracker.Preferences.KEY_UI_DISPLAY_KEEP_ON, OSMTracker.Preferences.VAL_UI_DISPLAY_KEEP_ON));
        osmViewController = osmView.getController();

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
                osmViewController.animateTo(currentPosition,CENTER_DEFAULT_ZOOM_LEVEL, ANIMATION_DURATION_MS);
            }
        });
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

}