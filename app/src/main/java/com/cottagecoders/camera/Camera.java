package com.cottagecoders.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class Camera extends Activity {
	//test
	Context ctx;
	TextView tv;
	public static String TAG = "CameraApp";
	private static String filename;
	private int num = 0;

	private ImageView image;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		Log.d(TAG, " camera onCreate -- got here.");

		ctx = getApplicationContext();

		tv = (TextView) findViewById(R.id.status);
		tv.setTextSize((float) 16.0);

		image = (ImageView) findViewById(R.id.image);

		Button start = (Button) findViewById(R.id.start);
		start.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				filename = ctx.getFilesDir() + "/my_file_" + num + ".jpg";
				File f = new File(filename);
				if (f.exists())
					f.delete();

				Intent intent = new Intent(ctx, NewTakePicture.class);
				intent.putExtra("name", filename);
				intent.putExtra("size", 1024);
				Log.d(TAG, "start the camera intent...");
				startActivityForResult(intent, 234);
				tv.setText("");
			}
		});

		Button images = (Button) findViewById(R.id.images);
		start.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ctx, ShowImages.class);
				startActivity(intent);

			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.camera, menu);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult(): -- got here. requestCode "
				+ requestCode + " resultCode " + resultCode);
		String ans = "";
		if (data.getStringExtra("okpicture").equalsIgnoreCase("no")) {
			ans = "ERROR - no image";
			image.setImageURI(null);
		} else {
			ExifInterface e = null;
			try {
				Log.d(TAG, "before ExifInterface filename = \'" + filename
						+ "\'");
				e = new ExifInterface(filename);
			} catch (Exception ee) {
				Log.d(TAG, "ExifInterface error: " + ee);
			}

			if (e != null) {
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
					Log.d(TAG, "set UserComment attribute");
					e.setAttribute("UserComment", "Well, Hello!");
					try {
						e.saveAttributes();
					} catch (Exception ee) {
						Log.d(TAG, "error on e.saveAttributes(): " + ee);
					}
				}
				ans += "whiteBalance "
						+ e.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
						+ "\n";
			}
			try {
				Uri uri = Uri.parse(filename);
				image.setImageURI(uri);
			} catch (Exception ex) {
				Log.d(TAG, "failed to setImage = " + ex);
			}

			File f = new File(filename);
			if (f.exists())
				f.delete();

		}
		tv.setText(ans);

		num++;
		return;
	}
}
