package com.finder.spot.pic.shak.picspotfinder;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class ImagesActivity extends AppCompatActivity {


    private final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;
    private static final String TAG = "ImagesActivity";
    private final int PLACE_PICKER_REQUEST = 1;

    private Resources res;
    private ArrayList<Bitmap> imagesDownloaded;
    private ArrayList<Integer> imageHeights;
    private ArrayList<String> imageURLs;
    private ArrayList<Integer> imageWidths;
    private ImagesAdapter adapt;
    private String lat;
    private String lon;
    private String accessToken;
    private TextView featuresDisabledTextView;
    private Button getLocationButton;
    private Button navigateToLocationButton;
    private RequestQueue mRequestQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);

        // Used to get string resources
        res = getResources();

        // Get access to activity views
        featuresDisabledTextView = (TextView) findViewById(R.id.featuresDisabled);
        getLocationButton = (Button) findViewById(R.id.getLocationButton);
        navigateToLocationButton = (Button) findViewById(R.id.navigateToLocationButton);

        // Should not be getting here without accessToken to make Instagram requests
        Bundle bundle = getIntent().getExtras();
        accessToken = bundle.getString(getResources().getString(R.string.access_token));

        // If location permissions have been granted, start Place Picker (Google Maps API)
        boolean haveLocationPermission = hasUserProvidedLocationPermissions();
        if (haveLocationPermission) {
            startPlacePicker();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permissions were granted.
                    if (mRequestQueue == null) {
                        mRequestQueue = Volley.newRequestQueue(this);
                    }
                    startPlacePicker();
                } else {
                    // Permissions not granted. Disable the functionality that depends on this
                    // permission if user permanently blocked permissions notification. Otherwise,
                    // continue requesting location permissions from the user.
                    boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    );
                    if (! showRationale) {
                        // User permanently blocked permissions notification. Disable features of
                        // the app and inform user that features are disabled.
                        getLocationButton.setEnabled(false);
                        navigateToLocationButton.setEnabled(false);
                        featuresDisabledTextView.setVisibility(View.VISIBLE);
                        featuresDisabledTextView.setText(
                                res.getString(R.string.features_disabled_message));
                    } else {
                        // Continue requesting location permissions from the user.
                        ActivityCompat.requestPermissions(ImagesActivity.this,
                                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                MY_PERMISSIONS_ACCESS_FINE_LOCATION);
                    }
                }
            }
        }
    }

    @Override
    protected void onStop () {
        super.onStop();
        // Cancel all Volley image requests still on the queue.
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(TAG);
        }
    }

    public void displayImages() {
        // Initialize GridView to display images.
        imagesDownloaded = new ArrayList<Bitmap>();
        adapt = new ImagesAdapter(this, imagesDownloaded, imageHeights, imageWidths);
        GridView gridview = (GridView) findViewById(R.id.gridView);
        gridview.setAdapter(adapt);
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                // When image is clicked, save image to be loaded fullscreen in FullImageActivity
                new SaveImageToDisk().execute(adapt.getItem(position));
            }
        });

        // Set up dialog informing user that images are loading.
        final ProgressDialog progressDialog =
                                                new ProgressDialog(
                                                        this,
                                                        R.style.LoadingImagesProgressDialogStyle);
        progressDialog.setTitle(res.getString(R.string.progress_dialog_title));
        progressDialog.setMessage(res.getString(R.string.progress_dialog_message));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);

        if (imageURLs != null || imageURLs.size() != 0) {
            // Display dialog informing user that images are loading.
            progressDialog.show();

            // Download each image on separate thread.
            int count = imageURLs.size();
            for (int i = 0; i < count; i++) {
                new DownloadImage().execute(imageURLs.get(i));
            }

            // Display loading dialog for 2 seconds.
            Runnable progressRunnable = new Runnable() {

                @Override
                public void run() {
                    progressDialog.cancel();
                }

            };
            Handler pdCanceller = new Handler();
            pdCanceller.postDelayed(progressRunnable, 2000);
        } else {
            // Inform the user know that there are no images to display via a Toast pop up.
            Toast.makeText(
                    ImagesActivity.this,
                    res.getString(R.string.no_images_toast),
                    Toast.LENGTH_LONG).show();
        }
    }

    public void navigateToLocationButton(View view) {
        // Start Google Maps navigation to location lat, lon
        Uri gmmIntentUri = Uri.parse(res.getString(R.string.google_navigation_request_uri) + lat +
                                                    "," + lon);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage(res.getString(R.string.google_map_package));
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }

    public void getLocationButton(View view) {
        startPlacePicker();
    }

    private void startPlacePicker() {
        // Start Google Place Picker to allow user to choose a location to display images from.
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            Log.i(TAG, res.getString(R.string.play_services_repairable_exception));
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.i(TAG, res.getString(R.string.play_services_not_available_exception));
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which Activity produced a result.
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                // If dealing with a positive Place Picker result, get location chosen by user via
                // the Place Picker.
                Place place = PlacePicker.getPlace(this, data);
                LatLng placeLatLng = place.getLatLng();
                lat = Double.toString(placeLatLng.latitude);
                lon = Double.toString(placeLatLng.longitude);

                // Enable the navigateToLocation Button now that we have a location.
                navigateToLocationButton.setVisibility(View.VISIBLE);
                navigateToLocationButton.setEnabled(true);
                
                // Initialize queue to download pictures from the chosen location.
                if (mRequestQueue == null) {
                    mRequestQueue = Volley.newRequestQueue(this);
                }
                
                getInstaPicsFromLocation();
            }
        }
    }

    private void getInstaPicsFromLocation() {
        // Request a JSON object response from the provided URL.
        String url = "https://api.instagram.com/v1/media/search?lat=" + lat +
                "&lng=" + lon + "&distance=1610&access_token=" +
                accessToken;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray placesJSONArr = response.getJSONArray("data");
                    imageURLs = new ArrayList<String>();
                    imageWidths = new ArrayList<>();
                    imageHeights = new ArrayList<>();
                    for (int i = 0; i < placesJSONArr.length(); i += 1) {
                        JSONObject place = placesJSONArr.getJSONObject(i);
                        JSONObject images = place.getJSONObject("images");
                        JSONObject standResImage = images.getJSONObject("standard_resolution");
                        imageWidths.add(Integer.parseInt(standResImage.getString("width")));
                        imageHeights.add(Integer.parseInt(standResImage.getString("height")));
                        imageURLs.add(standResImage.getString("url"));
                    }
                    displayImages();
                } catch (JSONException e) {
                    Log.i(
                            TAG,
                            "Failed to download image URLS from Instagram. (JSONException)");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(
                        TAG,
                        "Failed to download image URLS from from Instagram. (Error Listener)");
            }
        });
        // Set the tag on the request.
        jsonObjectRequest.setTag(TAG);
        // Add the request to the RequestQueue.
        mRequestQueue.add(jsonObjectRequest);
    }


    private boolean hasUserProvidedLocationPermissions() {
        if(ContextCompat.checkSelfPermission(ImagesActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(ImagesActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION);
            return false;
        }
    }

    private class SaveImageToDisk extends AsyncTask<Bitmap, Void, String> {

        @Override
        protected String doInBackground(Bitmap... URL) {
            Bitmap imageBitmap = URL[0];
            String filename = null;
            try {
                // Write image BitMap to disk.
                filename = "bitmap.png";
                FileOutputStream stream = ImagesActivity.this.openFileOutput(filename, Context.MODE_PRIVATE);
                imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                // Cleanup
                stream.close();
            } catch (Exception e) {
                Log.i(TAG, res.getString(R.string.save_image_to_disk_exception));
            }
            return filename;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Intent fullImageIntent =
                        new Intent(ImagesActivity.this, FullImageActivity.class);
                fullImageIntent.putExtra(getResources().getString(R.string.image_filename), result);
                ImagesActivity.this.startActivity(fullImageIntent);
            }
        }

    }

    private class DownloadImage extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... Path) {
            String imagePath = Path[0];
            Bitmap bitmap = null;
            try {
                // Get image off of disk.
                InputStream input = new java.net.URL(imagePath).openStream();
                // Decode saved image to get BitMap.
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;

        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // Add downloaded image to the Grid View adapter.
            adapt.add(result);
        }
    }

}
