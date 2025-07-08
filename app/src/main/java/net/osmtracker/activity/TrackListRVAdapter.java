package net.osmtracker.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
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
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.trackmgr_contextmenu_delete)
                    .setMessage(R.string.trackmgr_delete_confirm)
                    .setPositiveButton("بله", (dialog, which) -> mHandler.deleteTrackItem(trackId))
                    .setNegativeButton("خیر", (dialog, which) -> {
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
        void onShowPopupMenu(PopupMenu popupMenu, long trackId);
    }

    public class TrackItemVH extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView vId;
        private final ImageView vOptions;
        private final Button stopOrResume;
        private final Button end;

        public TrackItemVH(View view) {
            super(view);
            vId = view.findViewById(R.id.trackmgr_item_id);
            vOptions = view.findViewById(R.id.trackmgr_item_options);
            stopOrResume = view.findViewById(R.id.stop_or_resume);
            end = view.findViewById(R.id.end_mission);

            end.setOnClickListener(v -> {
                try {
                    long trackId = Long.parseLong(vId.getText().toString());
                    mHandler.endMission(trackId);
                } catch (NumberFormatException e) {
                    // Handle error
                }
            });

            stopOrResume.setOnClickListener(v -> {
                try {
                    long trackId = Long.parseLong(vId.getText().toString());
                    mHandler.stopTrack(trackId, stopOrResume.getText().toString().equals("توقف"));
                } catch (NumberFormatException e) {
                    // Handle error
                }
            });

            vOptions.setOnClickListener(v -> {
                try {
                    long trackId = Long.parseLong(vId.getText().toString());
                    PopupMenu popupMenu = new PopupMenu(context, vOptions);
                    mHandler.onShowPopupMenu(popupMenu, trackId);
                    popupMenu.show();
                } catch (NumberFormatException e) {
                    // Handle error
                }
            });

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            try {
                long trackId = Long.parseLong(vId.getText().toString());
                mHandler.onClick(trackId);
            } catch (NumberFormatException e) {
                // Handle error
            }
        }
    }

    @NonNull
    @Override
    public TrackItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TrackItemVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tracklist_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TrackItemVH holder, int position) {
        cursorAdapter.getCursor().moveToPosition(position);
        cursorAdapter.bindView(holder.itemView, context, cursorAdapter.getCursor());
    }

    @Override
    public int getItemCount() {
        return cursorAdapter.getCount();
    }
}