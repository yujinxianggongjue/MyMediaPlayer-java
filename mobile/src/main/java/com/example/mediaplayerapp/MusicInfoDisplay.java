package com.example.mediaplayerapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

public class MusicInfoDisplay {

    private static final String TAG = "MusicInfoDisplay"; // 调试标识

    private Context context;
    private TextView tvLyrics, tvArtist, tvAlbumName;
    private ImageView ivAlbumCover;

    public MusicInfoDisplay(Context context, TextView tvArtist, TextView tvAlbumName, ImageView ivAlbumCover) {
        this.context = context;
        this.tvArtist = tvArtist;
        this.tvAlbumName = tvAlbumName;
        this.ivAlbumCover = ivAlbumCover;
    }

    public void displayMusicInfo(Uri musicUri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            Log.d(TAG, "Initializing MediaMetadataRetriever for URI: " + musicUri);

            retriever.setDataSource(context, musicUri);
            Log.d(TAG, "MediaMetadataRetriever successfully set data source.");

            // 获取歌手
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (artist == null || artist.isEmpty()) {
                artist = "Unknown Artist";
                Log.d(TAG, "Artist metadata not found. Using default: Unknown Artist");
            } else {
                Log.d(TAG, "Artist retrieved: " + artist);
            }
            tvArtist.setText("Artist: " + artist);

            // 获取专辑名
            String albumName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if (albumName == null || albumName.isEmpty()) {
                albumName = "Unknown Album";
                Log.d(TAG, "Album metadata not found. Using default: Unknown Album");
            } else {
                Log.d(TAG, "Album name retrieved: " + albumName);
            }
            tvAlbumName.setText("Album: " + albumName);

            // 获取专辑封面
            byte[] albumArt = retriever.getEmbeddedPicture();
            if (albumArt != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
                ivAlbumCover.setImageBitmap(bitmap);
                Log.d(TAG, "Album cover retrieved and set.");
            } else {
                ivAlbumCover.setImageResource(R.drawable.default_album_cover);
                Log.d(TAG, "Album cover not found. Using default image.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving music info: " + e.getMessage(), e);
            tvLyrics.setText("Error retrieving music info");
            tvArtist.setText("Artist: Unknown");
            tvAlbumName.setText("Album: Unknown");
            ivAlbumCover.setImageResource(R.drawable.default_album_cover);
        } finally {
            try {
                retriever.release();
                Log.d(TAG, "MediaMetadataRetriever released successfully.");
            } catch (IOException e) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever: " + e.getMessage(), e);
            }
        }
    }
}