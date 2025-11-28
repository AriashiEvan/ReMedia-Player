package com.example.remediaplayer;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;

public class VideoLoader {

    public static ArrayList<VideoItem> loadVideos(Context context) {

        ArrayList<VideoItem> list = new ArrayList<>();

        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] proj = {
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED
        };

        Cursor c = context.getContentResolver().query(
                uri,
                proj,
                null,
                null,
                MediaStore.Video.Media.DATE_MODIFIED + " DESC"
        );

        if (c != null) {
            while (c.moveToNext()) {

                String path = c.getString(0);
                String title = c.getString(1);
                long duration = c.getLong(2);
                long size = c.getLong(3);
                long date = c.getLong(4);

                list.add(new VideoItem(path, title, duration, size, date));
            }
            c.close();
        }

        return list;
    }
}
