package com.finder.spot.pic.shak.picspotfinder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class SplashActivity extends AppCompatActivity {


    private static final String TAG = "SpashActivity";

    private RequestQueue mRequestQueue;
    private String accessToken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Used to get strings out of strings.xml.
        final Resources res = getResources();

        // Stored and get access token out of SharedPreferences.
        SharedPreferences settings = getSharedPreferences(res.getString(R.string.shared_pref),
                                                            Context.MODE_PRIVATE);
        String defaultValue = res.getString(R.string.access_token_default);
        accessToken = settings.getString(res.getString(R.string.access_token), defaultValue);

        mRequestQueue = Volley.newRequestQueue(this);

        if (accessToken == res.getString(R.string.access_token_default)) {
            // User has not logged in, redirect them to login (InstaWebViewActivity).
            Intent loginIntent = new Intent(SplashActivity.this, InstaWebViewActivity.class);
            SplashActivity.this.startActivity(loginIntent);
            SplashActivity.this.finish();
        } else {
            // Send request to Instagram API to validate access token. On error send user to login.
            String url =  "https://api.instagram.com/v1/locations/214930159?access_token=" +
                            accessToken;
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                    null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Intent imagesIntent = new Intent(SplashActivity.this, ImagesActivity.class);
                    imagesIntent.putExtra(res.getString(R.string.access_token), accessToken);
                    SplashActivity.this.startActivity(imagesIntent);
                    SplashActivity.this.finish();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Intent loginIntent = new Intent(SplashActivity.this,
                                                        InstaWebViewActivity.class);
                    SplashActivity.this.startActivity(loginIntent);
                    SplashActivity.this.finish();
                }
            });
            // Set the tag on the request.
            jsonObjectRequest.setTag(TAG);
            // Add the request to the RequestQueue.
            mRequestQueue.add(jsonObjectRequest);
        }
    }


    @Override
    protected void onStop () {
        super.onStop();
        // Cancel any outstanding requests
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(TAG);
        }
    }


}
