package com.finder.spot.pic.shak.picspotfinder;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Shak on 11/8/2017.
 */

public class ImagesAdapter extends ArrayAdapter<Bitmap> {

    ArrayList<Integer> imageHeights;
    ArrayList<Integer> imageWidths;

    public ImagesAdapter(Context context, ArrayList<Bitmap> images, ArrayList<Integer> imageHeights, ArrayList<Integer> imageWidths) {
        super(context, 0, images);
        this.imageHeights = imageHeights;
        this.imageWidths = imageWidths;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position (bitmap)
        Bitmap imageBitmap = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            // Lookup view for data population
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.single_image, parent, false);
        }

        // Populate the data into the template view using the data object
        ImageView imageView = ((ImageView) convertView);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(parent.getWidth()/2 - 20, Collections.max(imageHeights));
        imageView.setLayoutParams(layoutParams);
        imageView.setImageBitmap(imageBitmap);

        // Return the completed view to render on screen
        return imageView;
    }


}
