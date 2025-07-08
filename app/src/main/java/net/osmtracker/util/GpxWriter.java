package net.osmtracker.util;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import net.osmtracker.OSMTracker;
import net.osmtracker.data.db.TrackContentProvider;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class GpxWriter {

    private static final String TAG = GpxWriter.class.getSimpleName();
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
    private static final String GPX_TAG = "<gpx version=\"1.1\" creator=\"" + "ساخته شده با اپلیکیشن Farazaman"
            + "\" xmlns=\"http://www.topografix.com/GPX/1/1\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">";
    private static final SimpleDateFormat POINT_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    static {
        POINT_DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final Writer writer;

    public GpxWriter(Writer writer) {
        this.writer = writer;
    }

    public void write(Context context, long trackId) throws IOException {
        Log.v(TAG, "Writing GPX for track id " + trackId);

        writer.write(XML_HEADER + "\n");
        writer.write(GPX_TAG + "\n");

        Cursor c = context.getContentResolver().query(
                ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId),
                null, null, null, null);

        if (c == null || !c.moveToFirst()) {
            if (c != null) c.close();
            throw new IOException("Track with id " + trackId + " not found.");
        }

        String trackName = c.getString(c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_NAME));
        String trackTags = c.getString(c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_TAGS));

        writer.write("\t" + "<metadata>" + "\n");
        writer.write("\t\t" + "<name>" + trackName + "</name>" + "\n");
        writer.write("\t\t" + "<keywords>" + trackTags + "</keywords>" + "\n");
        writer.write("\t" + "</metadata>" + "\n");

        writer.write("\t" + "<trk>" + "\n");
        writer.write("\t\t" + "<name>" + trackName + "</name>" + "\n");
        writer.write("\t\t" + "<trkseg>" + "\n");
        c.close();

        c = context.getContentResolver().query(
                TrackContentProvider.trackPointsUri(trackId),
                null, null, null, TrackContentProvider.Schema.COL_ID + " asc");

        if (c != null) {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                writer.write("\t\t\t" + "<trkpt lat=\"" + c.getString(c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_LATITUDE))
                        + "\" lon=\"" + c.getString(c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_LONGITUDE))
                        + "\">" + "\n");
                // خط زیر حذف شده است چون ستون ارتفاع وجود ندارد
                // writer.write("\t\t\t\t" + "<ele>" + c.getString(c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_ALTITUDE)) + "</ele>" + "\n");
                writer.write("\t\t\t\t" + "<time>" + POINT_DATE_FORMATTER.format(new Date(c.getLong(c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_TIMESTAMP)))) + "</time>" + "\n");
                writer.write("\t\t\t</trkpt>" + "\n");
            }
            c.close();
        }

        writer.write("\t\t" + "</trkseg>" + "\n");
        writer.write("\t" + "</trk>" + "\n");

        c = context.getContentResolver().query(
                TrackContentProvider.waypointsUri(trackId),
                null, null, null, TrackContentProvider.Schema.COL_ID + " asc");

        if (c != null) {
            for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                writer.write("\t" + "<wpt lat=\"" + c.getString(c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_LATITUDE))
                        + "\" lon=\"" + c.getString(c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_LONGITUDE))
                        + "\">" + "\n");
                writer.write("\t\t" + "<name>" + c.getString(c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_NAME)) + "</name>" + "\n");
                writer.write("\t\t" + "<time>" + POINT_DATE_FORMATTER.format(new Date(c.getLong(c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_TIMESTAMP)))) + "</time>" + "\n");
                String link = c.getString(c.getColumnIndexOrThrow(TrackContentProvider.Schema.COL_LINK));
                if (link != null) {
                    writer.write("\t\t" + "<link href=\"" + link + "\">" + "\n");
                    writer.write("\t\t\t" + "<text>" + link.substring(link.lastIndexOf('/')+1) + "</text>" + "\n");
                    writer.write("\t\t" + "</link>" + "\n");
                }
                writer.write("\t" + "</wpt>" + "\n");
            }
            c.close();
        }

        writer.write("</gpx>");
    }
}