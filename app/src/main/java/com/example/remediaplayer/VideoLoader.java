package com.example.remediaplayer;

import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.activity.result.IntentSenderRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VideoLoader {

    public static IntentSenderRequest getWriteRequestForId(Context ctx, long id) {
        Uri itemUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

        PendingIntent pi = MediaStore.createWriteRequest(
                ctx.getContentResolver(),
                Collections.singletonList(itemUri)
        );

        return new IntentSenderRequest.Builder(pi.getIntentSender()).build();
    }

    public static IntentSenderRequest getDeleteRequestForIds(Context ctx, List<Long> ids) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (long id : ids) {
            uris.add(ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id));
        }

        PendingIntent pi = MediaStore.createDeleteRequest(
                ctx.getContentResolver(), uris);

        return new IntentSenderRequest.Builder(pi.getIntentSender()).build();
    }

    public static ArrayList<VideoItem> loadVideos(Context ctx) {
        ArrayList<VideoItem> list = new ArrayList<>();

        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,  // <— updated
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_MODIFIED
        };

        Cursor c = ctx.getContentResolver().query(
                uri, projection, null, null,
                MediaStore.Video.Media.DATE_MODIFIED + " DESC");

        if (c != null) {
            while (c.moveToNext()) {

                long id = c.getLong(0);
                String title = c.getString(1);      // <— updated
                String path = c.getString(2);
                long size = c.getLong(3);
                long duration = c.getLong(4);
                long modifiedMs = c.getLong(5) * 1000L;

                list.add(VideoItem.fromLocal(
                        id, title, path, size, duration,
                        modifiedMs, path
                ));
            }
            c.close();
        }

        return list;
    }
}
