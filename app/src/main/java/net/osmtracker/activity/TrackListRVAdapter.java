package net.osmtracker.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.osmtracker.R;
import net.osmtracker.data.db.TracklistAdapter;

public class TrackListRVAdapter extends RecyclerView.Adapter<TrackListRVAdapter.TrackItemVH> {

    private static final String TAG = TrackListRVAdapter.class.getSimpleName();

    private final TracklistAdapter cursorAdapter;
    private final Context context;

    private final TrackListRecyclerViewAdapterListener mHandler;

    public TrackListRVAdapter(Context context, Cursor cursor,
                              TrackListRecyclerViewAdapterListener handler) {
        this.context = context;
        this.cursorAdapter = new TracklistAdapter(context, cursor);
        this.mHandler = handler;
    }

    public TracklistAdapter getCursorAdapter() {
        return cursorAdapter;
    }

    public void deleteTrack(int position, Runnable onCancel) {
        Cursor cursor = cursorAdapter.getCursor();
        if (cursor.moveToPosition(position)) {
            @SuppressLint("Range") long trackId = cursor.getLong(cursor.getColumnIndex("_id"));
            // نمایش دیالوگ تأیید
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.trackmgr_contextmenu_delete)
                    .setMessage(R.string.trackmgr_delete_confirm)
                    .setPositiveButton("بله", (dialog, which) -> {
                        mHandler.deleteTrackItem(trackId);

                    })
                    .setNegativeButton("خیر", (dialog, which) -> {
                        // اگه لغو شد، RecyclerView رو به‌روز کن
                        if (onCancel != null) {
                            onCancel.run();
                        }
                        notifyItemChanged(position);
                    })
                    .show();
        }
    }

    public interface TrackListRecyclerViewAdapterListener {
        void onClick(long trackId);

        void deleteTrackItem(long trackId);

        void stopTrack(long trackId, boolean stopOrResume);
        void endMission(long trackId);

        void onCreateContextMenu(ContextMenu contextMenu, View view,
                                 ContextMenu.ContextMenuInfo contextMenuInfo, long trackId);

        void onShowPopupMenu(PopupMenu popupMenu, long trackId);
    }

    /**
     * Provide a reference to the type of views
     */
    public class TrackItemVH extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnCreateContextMenuListener {

        private final TextView vId;
        private final TextView vNameOrStartDate;
        private final TextView vWps;
        private final TextView vTps;
        private final ImageView vStatus;
        private final ImageView vUploadStatus;
        private final ImageView vOptions;
        private final Button stopOrResume;
        private final Button end;

        public TrackItemVH(View view) {
            super(view);

            vId = (TextView) view.findViewById(R.id.trackmgr_item_id);
            vNameOrStartDate = (TextView) view.findViewById(R.id.trackmgr_item_nameordate);
            vWps = (TextView) view.findViewById(R.id.trackmgr_item_wps);
            vTps = (TextView) view.findViewById(R.id.trackmgr_item_tps);
            vStatus = (ImageView) view.findViewById(R.id.trackmgr_item_statusicon);
            vUploadStatus = (ImageView) view.findViewById(R.id.trackmgr_item_upload_statusicon);
            vOptions = (ImageView) view.findViewById(R.id.trackmgr_item_options);
            stopOrResume = (Button) view.findViewById(R.id.stop_or_resume);
            end = (Button) view.findViewById(R.id.end_mission);
            end.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    long trackId;
                    try {
                        trackId = Long.parseLong(getvId().getText().toString());
                    } catch (NumberFormatException e) {
                        trackId = -1; // مدیریت خطا
                    }

                    mHandler.endMission(trackId);
                }
            });
            stopOrResume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    long trackId;
                    try {
                        trackId = Long.parseLong(getvId().getText().toString());
                    } catch (NumberFormatException e) {
                        trackId = -1; // مدیریت خطا
                    }
                    if (stopOrResume.getText() == "توقف")
                        mHandler.stopTrack(trackId, true);
                    else
                        mHandler.stopTrack(trackId, false);

                }
            });
            // listeners
//            vOptions.setOnCreateContextMenuListener(this);
            vOptions.setOnClickListener(v -> {
                long trackId;
                try {
                    trackId = Long.parseLong(getvId().getText().toString());
                } catch (NumberFormatException e) {
                    trackId = -1; // مدیریت خطا
                }
                // ایجاد و نمایش PopupMenu
                PopupMenu popupMenu = new PopupMenu(context, vOptions);
                mHandler.onShowPopupMenu(popupMenu, trackId);
                popupMenu.show();
            });
            view.setOnClickListener(this);
//            view.setOnCreateContextMenuListener(this);
        }

        public TextView getvId() {
            return vId;
        }

        public TextView getvNameOrStartDate() {
            return vNameOrStartDate;
        }

        public TextView getvWps() {
            return vWps;
        }

        public TextView getvTps() {
            return vTps;
        }

        public ImageView getvStatus() {
            return vStatus;
        }

        public ImageView getvUploadStatus() {
            return vUploadStatus;
        }

        /**
         * This gets called by the child views during a click.
         *
         * @param v The View that was clicked
         */
        @Override
        public void onClick(View v) {
            long trackId = Long.parseLong(getvId().getText().toString());
            mHandler.onClick(trackId);
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            long trackId = Long.parseLong(getvId().getText().toString());
            mHandler.onCreateContextMenu(contextMenu, view, contextMenuInfo, trackId);
        }
    }


    @NonNull
    @Override
    public TrackItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TrackItemVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tracklist_item,
                        parent, false));
    }

    // Create new views (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull TrackItemVH holder, int position) {
        // Get element from database at this position and replace the
        // contents of the view with that element

        // Passing the binding operation to cursor loader
        cursorAdapter.getCursor().moveToPosition(position);
        cursorAdapter.bindView(holder.itemView, context, cursorAdapter.getCursor());
    }

    @Override
    public int getItemCount() {
        return cursorAdapter.getCount();
    }


}
