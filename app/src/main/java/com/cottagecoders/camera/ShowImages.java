package com.cottagecoders.camera;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;

public class ShowImages extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_images);

        TableLayout table = (TableLayout) findViewById(R.id.tab);

        File dirFiles = getApplicationContext().getFilesDir();
        for (String strFile : dirFiles.list())
        {
            TextView tv = new TextView(getApplicationContext());
            tv.setText(strFile);

            TableRow tr = new TableRow(getApplicationContext());

            tr.addView(tv);
            table.addView(tr);

        }




    }

}
