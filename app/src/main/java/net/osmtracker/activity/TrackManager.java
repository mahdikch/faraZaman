package net.osmtracker.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import net.osmtracker.OSMTracker;
import net.osmtracker.R;
import net.osmtracker.data.db.DataHelper;
import net.osmtracker.data.db.TrackContentProvider;
import net.osmtracker.exception.CreateTrackException;
import net.osmtracker.gpx.ZipHelper;
import net.osmtracker.util.FileSystemUtils;
import net.osmtracker.util.GpxWriter;
import net.osmtracker.AppConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import saman.zamani.persiandate.PersianDate;

public class TrackManager extends AppCompatActivity
        implements TrackListRVAdapter.TrackListRecyclerViewAdapterListener {

    private static final String TAG = TrackManager.class.getSimpleName();
    private static final long TRACK_ID_NO_TRACK = -1;
    private final int RC_WRITE_PERMISSIONS_UPLOAD = 4;
    private final int RC_WRITE_PERMISSIONS_DISPLAY_TRACK = 3;


    private long currentTrackId = TRACK_ID_NO_TRACK;
    private long contextMenuSelectedTrackid = TRACK_ID_NO_TRACK;
    private long permissionRequestTrackId = TRACK_ID_NO_TRACK;

    private RecyclerView recyclerView;
    private TrackListRVAdapter recyclerViewAdapter;
    private ProgressBar uploadProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trackmanager);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setTitle("ماموریت ها");
        setSupportActionBar(myToolbar);

        findViewById(R.id.start_track).setOnClickListener(v -> startTrackLoggerForNewTrack());
        recyclerView = findViewById(R.id.recyclerview);
        uploadProgressBar = findViewById(R.id.upload_progressbar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRecyclerView();
        checkEmptyViewVisibility();

        if (recyclerViewAdapter.getItemCount() > 0) {
            currentTrackId = DataHelper.getActiveTrackId(getContentResolver());
            if (currentTrackId != TRACK_ID_NO_TRACK) {
                Snackbar.make(findViewById(R.id.start_track),
                                getResources().getString(R.string.trackmgr_continuetrack_hint)
                                        .replace("{0}", Long.toString(currentTrackId)), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }
    }

    private void setRecyclerView() {
        Cursor cursor = getContentResolver().query(
                TrackContentProvider.CONTENT_URI_TRACK, null, null, null,
                TrackContentProvider.Schema.COL_START_DATE + " desc");

        recyclerViewAdapter = new TrackListRVAdapter(this, cursor, this);
        recyclerView.setAdapter(recyclerViewAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(this, recyclerViewAdapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    // ... (سایر متدهای on*OptionsMenu و onContextItemSelected بدون تغییر) ...
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trackmgr_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.trackmgr_menu_deletetracks).setVisible(recyclerViewAdapter.getItemCount() > 0);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.trackmgr_menu_deletetracks) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.trackmgr_contextmenu_delete)
                    .setMessage(getResources().getString(R.string.trackmgr_deleteall_confirm))
                    .setCancelable(true)
                    .setPositiveButton(R.string.menu_deletetracks, (dialog, which) -> deleteAllTracks())
                    .setNegativeButton("بیخیال", (dialog, which) -> dialog.cancel())
                    .show();
        } else if (itemId == R.id.trackmgr_menu_settings) {
            startActivity(new Intent(this, Preferences.class));
        } else if (itemId == R.id.trackmgr_menu_about) {
            startActivity(new Intent(this, About.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onShowPopupMenu(PopupMenu popupMenu, long trackId) {
        contextMenuSelectedTrackid = trackId;
        popupMenu.getMenuInflater().inflate(R.menu.trackmgr_contextmenu, popupMenu.getMenu());
        if (currentTrackId == contextMenuSelectedTrackid) {
            popupMenu.getMenu().removeItem(R.id.trackmgr_contextmenu_delete);
        }
        popupMenu.setOnMenuItemClickListener(this::onContextItemSelected);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.trackmgr_contextmenu_delete) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.trackmgr_contextmenu_delete)
                    .setMessage(getResources().getString(R.string.trackmgr_delete_confirm).replace("{0}", Long.toString(contextMenuSelectedTrackid)))
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> deleteTrack(contextMenuSelectedTrackid))
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                    .show();
        } else if (itemId == R.id.trackmgr_contextmenu_display) {
            displayTrack(contextMenuSelectedTrackid);
        }
        return super.onContextItemSelected(item);
    }


    private void tryStartTrackLogger(Intent intent) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 5);
        }
    }

    private void startTrackLoggerForNewTrack() {
        try {
            currentTrackId = createNewTrack();
            Intent startTrackingIntent = new Intent(OSMTracker.INTENT_START_TRACKING);
            startTrackingIntent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
            sendBroadcast(startTrackingIntent);

            Intent i = new Intent(this, DisplayTrackMap.class);
            i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
            tryStartTrackLogger(i);
        } catch (CreateTrackException cte) {
            Toast.makeText(this, getResources().getString(R.string.trackmgr_newtrack_error).replace("{0}", cte.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void displayTrack(long trackId, boolean playbackMode) {
        Intent i = new Intent(this, DisplayTrackMap.class);
        i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
        i.putExtra("playback_mode", playbackMode);
        startActivity(i);
    }

    private void displayTrack(long trackId) {
        displayTrack(trackId, true);
    }

//    private boolean writeExternalStoragePermissionGranted() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
//            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
//        }
//        return true;
//    }

    private boolean writeExternalStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // On Android 11+ (API 30+), WRITE_EXTERNAL_STORAGE is ignored for most apps
            // If you really need broad storage access, you must use MANAGE_EXTERNAL_STORAGE (not recommended for most apps)
            return true;
        }
        // For Android 10 and below, check the permission
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onClick(long trackId) {
//        if (writeExternalStoragePermissionGranted()) {
            boolean isClickedTrackActive = (trackId == this.currentTrackId);
            boolean playbackMode = !isClickedTrackActive;
            displayTrack(trackId, playbackMode);
//        } else {
//            permissionRequestTrackId = trackId;
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_WRITE_PERMISSIONS_DISPLAY_TRACK);
//        }
    }

    @Override
    public void deleteTrackItem(long trackId) {
        deleteTrack(trackId);
    }

    @Override
    public void stopTrack(long trackId, boolean stopOrResume) {
        if (stopOrResume) {
            stopActiveTrack();
        } else {
            if (currentTrackId != trackId) {
                setActiveTrack(trackId);
            }
            Intent startTrackingIntent = new Intent(OSMTracker.INTENT_START_TRACKING);
            startTrackingIntent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
            sendBroadcast(startTrackingIntent);
            Intent i = new Intent(this, DisplayTrackMap.class);
            i.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, trackId);
            tryStartTrackLogger(i);
        }
    }

    private void checkEmptyViewVisibility() {
        TextView emptyView = findViewById(R.id.trackmgr_empty);
        if (recyclerViewAdapter == null || recyclerViewAdapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void endMission(long trackId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("اتمام و آپلود ماموریت")
                .setMessage("با آپلود، این ماموریت از لیست شما حذف خواهد شد. آیا ادامه می‌دهید؟")
                .setCancelable(true)
                .setPositiveButton("تایید و آپلود", (dialog, which) -> {
//                    if (writeExternalStoragePermissionGranted()) {
                        startUploadProcess(trackId);
//                    } else {
//                        permissionRequestTrackId = trackId;
//                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
//                            if (!writeExternalStoragePermissionGranted()) {
//                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_WRITE_PERMISSIONS_DISPLAY_TRACK);

//                            }
//                        }
//                    }
                })
                .setNegativeButton("لغو", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void startUploadProcess(long trackId) {
        if (this.currentTrackId == trackId) {
            Intent intent = new Intent(OSMTracker.INTENT_STOP_TRACKING);
            intent.setPackage(this.getPackageName());
            sendBroadcast(intent);
            this.currentTrackId = TRACK_ID_NO_TRACK;
        }
        DataHelper dataHelper = new DataHelper(this);
        dataHelper.stopTracking(trackId);
        updateTrackItemsInRecyclerView();
        new UploadTrackTask(this, trackId).execute();
    }

    private long createNewTrack() throws CreateTrackException {
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_NAME, DataHelper.PERSIAN_FILENAME_FORMATTER.format(new PersianDate()));
        values.put(TrackContentProvider.Schema.COL_START_DATE, new Date().getTime());
        values.put(TrackContentProvider.Schema.COL_ACTIVE, TrackContentProvider.Schema.VAL_TRACK_ACTIVE);
        Uri trackUri = getContentResolver().insert(TrackContentProvider.CONTENT_URI_TRACK, values);
        long trackId = ContentUris.parseId(trackUri);
        setActiveTrack(trackId);
        return trackId;
    }

    private void deleteTrack(long id) {
        getContentResolver().delete(ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, id), null, null);
        updateTrackItemsInRecyclerView();
        checkEmptyViewVisibility();
        File trackStorageDirectory = DataHelper.getTrackDirectory(id, this);
        if (trackStorageDirectory.exists()) {
            FileSystemUtils.delete(trackStorageDirectory, true);
        }
    }

// TrackManager.java

    private void updateTrackItemsInRecyclerView() {
        if (recyclerViewAdapter != null && recyclerViewAdapter.getCursorAdapter() != null) {
            // از requery() استفاده نکنید. به جای آن یک کوئری جدید بزنید و کرسر را تعویض کنید.
            Cursor newCursor = getContentResolver().query(
                    TrackContentProvider.CONTENT_URI_TRACK, null, null, null,
                    TrackContentProvider.Schema.COL_START_DATE + " desc");

            recyclerViewAdapter.getCursorAdapter().swapCursor(newCursor);
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    private void deleteAllTracks() {
        if (currentTrackId != TRACK_ID_NO_TRACK) {
            stopActiveTrack();
        }
        Cursor cursor = getContentResolver().query(TrackContentProvider.CONTENT_URI_TRACK, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id_col = cursor.getColumnIndex(TrackContentProvider.Schema.COL_ID);
                do {
                    deleteTrack(cursor.getLong(id_col));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    private void setActiveTrack(long trackId) {
        stopActiveTrack();
        ContentValues values = new ContentValues();
        values.put(TrackContentProvider.Schema.COL_ACTIVE, TrackContentProvider.Schema.VAL_TRACK_ACTIVE);
        getContentResolver().update(TrackContentProvider.CONTENT_URI_TRACK, values, TrackContentProvider.Schema.COL_ID + " = ?", new String[]{Long.toString(trackId)});
        currentTrackId = trackId;
    }

    private void stopActiveTrack() {
        if (currentTrackId != TRACK_ID_NO_TRACK) {
            Intent intent = new Intent(OSMTracker.INTENT_STOP_TRACKING);
            intent.setPackage(this.getPackageName());
            sendBroadcast(intent);
            DataHelper dataHelper = new DataHelper(this);
            dataHelper.stopTracking(currentTrackId);
            currentTrackId = TRACK_ID_NO_TRACK;
            updateTrackItemsInRecyclerView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == RC_WRITE_PERMISSIONS_UPLOAD) {
                startUploadProcess(permissionRequestTrackId);
            } else if (requestCode == RC_WRITE_PERMISSIONS_DISPLAY_TRACK) {
                onClick(permissionRequestTrackId);
            }
        } else {
            Toast.makeText(this, "دسترسی برای انجام عملیات لازم است.", Toast.LENGTH_LONG).show();
        }
    }

    // ====================================================================================
    // A S Y N C   T A S K   F O R   U P L O A D I N G
    // ====================================================================================
    @SuppressLint("StaticFieldLeak")
    private class UploadTrackTask extends AsyncTask<Void, Void, Boolean> {
        private final Context context;
        private final long trackId;
        private String errorMessage = "خطای نامشخص";

        UploadTrackTask(Context context, long trackId) {
            this.context = context;
            this.trackId = trackId;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            uploadProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            // مرحله ۱: ساخت فایل GPX به صورت مستقیم
            File gpxFile;
            try {
                // یک فایل موقت برای GPX ایجاد می‌کنیم
                gpxFile = File.createTempFile("track_" + trackId, ".gpx", context.getCacheDir());
                FileWriter writer = new FileWriter(gpxFile);
                // نوشتن محتوای GPX با استفاده از GpxWriter
                new GpxWriter(writer).write(context, trackId);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to create GPX file", e);
                errorMessage = "خطا در ساخت فایل GPX";
                return false;
            }

            if (!gpxFile.exists()) {
                errorMessage = "فایل GPX پس از ساخت یافت نشد.";
                return false;
            }

            // مرحله ۲: فشرده‌سازی فایل‌ها
            File zipFile = ZipHelper.zipCacheFiles(context, trackId, gpxFile);
            if (zipFile == null || !zipFile.exists()) {
                errorMessage = "خطا در ساخت فایل فشرده.";
                return false;
            }

            // مرحله ۳: آپلود فایل
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String token = prefs.getString("ACCESS_TOKEN", null);
            if (token == null || token.isEmpty()) {
                errorMessage = "توکن دسترسی یافت نشد. لطفاً دوباره وارد شوید.";
                return false;
            }

            OkHttpClient client = new OkHttpClient();
            RequestBody fileBody = RequestBody.create(zipFile, MediaType.parse("application/zip"));
            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", zipFile.getName(), fileBody)
                    .build();
            Request request = new Request.Builder()
                    .url(AppConstants.BASE_URL + "api/GisGeolocation/upload")
                    .addHeader("Authorization", "Bearer " + token)
                    .post(requestBody)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    return true;
                } else {
                    errorMessage = "خطای سرور: " + response.code() + " " + response.message();
                    Log.e(TAG, "Upload failed: " + (response.body() != null ? response.body().string() : "No response body"));
                    return false;
                }
            } catch (IOException e) {
                errorMessage = "خطای شبکه: " + e.getMessage();
                Log.e(TAG, "Upload failed with network error", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            uploadProgressBar.setVisibility(View.GONE);
            if (success) {
                Toast.makeText(context, "ماموریت با موفقیت آپلود شد.", Toast.LENGTH_SHORT).show();
                // پس از آپلود موفق، ماموریت را حذف کن
                deleteTrack(trackId);
            } else {
                Toast.makeText(context, "آپلود ناموفق بود: " + errorMessage, Toast.LENGTH_LONG).show();
                // اگر آپلود ناموفق بود، آیتم را در لیست نگه می‌داریم تا کاربر دوباره تلاش کند
                updateTrackItemsInRecyclerView();
            }
        }
    }

    private static class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        private final TrackListRVAdapter adapter;
        private final Drawable deleteIcon;
        private final Drawable background;

        SwipeToDeleteCallback(Context context, TrackListRVAdapter adapter) {
            super(0, ItemTouchHelper.RIGHT);
            this.adapter = adapter;
            this.deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
            this.background = ContextCompat.getDrawable(context, R.drawable.delete_background);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            adapter.deleteTrack(position, () -> adapter.notifyItemChanged(position));
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            View itemView = viewHolder.itemView;
            if (deleteIcon != null && background != null) {
                int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + iconMargin;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                if (dX > 0) {
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconRight = iconLeft + deleteIcon.getIntrinsicWidth();
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX), itemView.getBottom());
                } else {
                    background.setBounds(0, 0, 0, 0);
                }
                background.draw(c);
                deleteIcon.draw(c);
            }
        }
    }
}