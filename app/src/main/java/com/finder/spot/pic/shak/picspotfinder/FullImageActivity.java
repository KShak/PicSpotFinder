package com.finder.spot.pic.shak.picspotfinder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileInputStream;


public class FullImageActivity extends AppCompatActivity {

    private static final String TAG = "FullImageActivity";

    private ImageView imageView;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        imageView = (ImageView) findViewById(R.id.fullScreenImageView);
        textView = (TextView) findViewById(R.id.noImageTextView);

        textView.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);

        // Get the Filename of the image to get off disk.
        String imageFilename = getIntent().getStringExtra(getResources().getString(R.string.image_filename));

        // Upload image to the imageView off of the Main Thread.
        new OpenImage().execute(imageFilename);
    }

    private class OpenImage extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... URL) {
            // Get the image off disk
            String imageFilename = URL[0];
            Bitmap imageBitmap = null;
            try {
                FileInputStream is = FullImageActivity.this.openFileInput(imageFilename);
                imageBitmap = BitmapFactory.decodeStream(is);
                is.close();
            } catch (Exception e) {
                Log.i(TAG, getResources().getString(R.string.image_upload_from_disk_error));
                return null;
            }
            return imageBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // Populate this activity's ImageView with the image off disk
            if (result == null) {
                imageView.setVisibility(View.GONE);
                textView.setText(R.string.image_upload_from_disk_error);
                textView.setVisibility(View.VISIBLE);
                return;
            }
            imageView.setImageBitmap(result);
        }

    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        // Activity set to launchMode=singleTask, so if this activity is called multiple times,
        // a new Intent is delivered to the already running activity, and this method is called.
        super.onNewIntent(intent);

        textView.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);

        String imageFilename = getIntent().getStringExtra(getResources().getString(R.string.image_filename));

        new OpenImage().execute(imageFilename);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        // If back button is pressed, destroy this activity.
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

}
