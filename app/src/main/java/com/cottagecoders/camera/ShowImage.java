package com.cottagecoders.camera;

import android.app.Activity;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class ShowImage extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_image);

        ImageView image = (ImageView) findViewById(R.id.image);
        TextView tv = (TextView) findViewById(R.id.metaData);

        String fileName = getIntent().getStringExtra("fileName");

        try {
            Uri uri = Uri.parse(fileName);
            image.setImageURI(uri);
        } catch (Exception ex) {
            Log.d(Camera.TAG, "failed to setImage = " + ex);
        }

        ExifInterface e = null;
        try {
            Log.d(Camera.TAG, "before ExifInterface fileName = \'" + fileName
                    + "\'");
            e = new ExifInterface(fileName);
        } catch (Exception ee) {
            Log.d(Camera.TAG, "ExifInterface error: " + ee);
        }

        if (e != null) {
            String ans = "";
            ans += "orientation "
                    + e.getAttribute(ExifInterface.TAG_ORIENTATION) + "\n";
            ans += "imageLength "
                    + e.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) + "\n";
            ans += "imageWidth "
                    + e.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) + "\n";
            ans += "dateTime " + e.getAttribute(ExifInterface.TAG_DATETIME)
                    + "\n";
            ans += "flash " + e.getAttribute(ExifInterface.TAG_FLASH)
                    + "\n";
            ans += "focalLength "
                    + e.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) + "\n";
            // ans += "ISO "+ e.getAttribute(ExifInterface.TAG_ISO) + "\n" ;
            ans += "make " + e.getAttribute(ExifInterface.TAG_MAKE) + "\n";
            ans += "model " + e.getAttribute(ExifInterface.TAG_MODEL)
                    + "\n";
            // ans += "aperture " +
            // e.getAttribute(ExifInterface.TAG_APERTURE)
            // + "\n";
            // ans += "exposureTime "
            // + e.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
            // + "\n";

            ans += "userComment " + e.getAttribute("UserComment") + "\n";
            if (e.getAttribute("UserComment") == null) {
                Log.d(Camera.TAG, "set UserComment attribute");
                e.setAttribute("UserComment", "Well, Hello!");
                try {
                    e.saveAttributes();
                } catch (Exception ee) {
                    Log.d(Camera.TAG, "error on e.saveAttributes(): " + ee);
                }
            }
            ans += "whiteBalance "
                    + e.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
                    + "\n";
            tv.setText(ans);

        }
    }
}
