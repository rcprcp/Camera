package com.cottagecoders.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;

public class ListImages extends Activity {
    TableLayout table;
    final int THUMBSIZE = 64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_images);
        table = (TableLayout) findViewById(R.id.tab);

        drawMe();

    }

    void drawMe() {

        table.removeAllViews();

        //TODO: add FileFilter.
        //FileFilter...
        File[] dirFiles = Camera.ctx.getFilesDir().listFiles();

        for (File f : dirFiles) {
            if (!f.getName().contains(Camera.EXTENSION)) {
                Log.d(Camera.TAG, "SKIP: the file " + f.getAbsoluteFile());
                continue;
            } else {
                Log.d(Camera.TAG, "SHOW: the file " + f.getAbsoluteFile());
            }

            TableRow tr = new TableRow(getApplicationContext());

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f.getAbsoluteFile());
            } catch (Exception e) {
                Log.d(Camera.TAG, "FileInputStream failed ");
                e.printStackTrace();
            }
            Bitmap imageBitmap = BitmapFactory.decodeStream(fis);

            imageBitmap = Bitmap.createScaledBitmap(imageBitmap, THUMBSIZE, THUMBSIZE, false);

            ImageView iv = new ImageView(getApplicationContext());
            iv.setPadding(10, 10, 10, 10);
            iv.setImageBitmap(imageBitmap);
            final String finalF = f.getAbsoluteFile().toString();
            Log.d(Camera.TAG, "finalF " + finalF);
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispImage(finalF);
                }
            });
            tr.addView(iv);

            TextView tv = new TextView(getApplicationContext());
            tv.setTextSize(16f);
            tv.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.Black));
            tv.setText(f.getName());
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispImage(finalF);
                }
            });

            tr.addView(tv);

            Button b = new Button(getApplicationContext());
            //TODO: wire the button to delete the file.
            b.setText("Delete");
            b.setTag(f);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File f = (File) v.getTag();
                    Log.d(Camera.TAG, "deleting file name " + f.getName());
                    f.delete();
                    drawMe();
                }
            });


            tr.addView(b);
            table.addView(tr);
        }
    }

    private void dispImage(String fileName) {
        Intent intent = new Intent(getApplicationContext(), ShowImage.class);
        intent.putExtra("fileName", fileName);
        startActivityForResult(intent, 123);
    }
}
