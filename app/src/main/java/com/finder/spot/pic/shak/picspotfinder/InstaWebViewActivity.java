package com.finder.spot.pic.shak.picspotfinder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class InstaWebViewActivity extends AppCompatActivity {


    private final String TAG = "InstaWebViewActivity";
    private final String loginURL =
            "https://www.instagram.com/accounts/login/?force_classic_login=&next=/oauth/" +
                    "authorize/%3Fclient_id%3De029feaadaa44e1ab7002d97e5f16b0b%26redirect_uri" +
                    "%3Dhttp%3A//services.chrisriversdesign.com/instagram-token/result%" +
                    "26response_type%3Dtoken%26scope%3Dpublic_content";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insta_web_view);

        WebView webView = (WebView) findViewById(R.id.instaWebView);
        //  load the url of the oAuth login page
        webView
                .loadUrl(loginURL);
        //  Set the web client.
        webView.setWebViewClient(new MyWebViewClient());
        //  Activates JavaScript (just in case).
        webView.getSettings().setJavaScriptEnabled(true);

    }


    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            //  Check if the login was successful and the access token was returned.
            String requestUrl = request.getUrl().toString();
            Resources res = getResources();
            if (requestUrl.contains("access_token=")) {
                String accessToken = request.getUrl().toString().split("=")[1];
                SharedPreferences settings = getSharedPreferences(
                                                res.getString(R.string.shared_pref),
                                                Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(res.getString(R.string.access_token), accessToken);
                editor.commit();
                Intent mainIntent = new Intent(InstaWebViewActivity.this, ImagesActivity.class);
                mainIntent.putExtra(res.getString(R.string.access_token), accessToken);
                InstaWebViewActivity.this.startActivity(mainIntent);
                InstaWebViewActivity.this.finish();
                return true;
            }
            return false;
        }

    }


}
