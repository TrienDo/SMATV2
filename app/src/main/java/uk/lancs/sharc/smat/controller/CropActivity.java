package uk.lancs.sharc.smat.controller;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import uk.lancs.sharc.smat.R;
import uk.lancs.sharc.smat.service.SharcLibrary;

public class CropActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        Bitmap bmp = BitmapFactory.decodeFile(getIntent().getExtras().getString("from"));
        final CropImageView croppedImageView = (CropImageView) findViewById(R.id.cropImageView);
        croppedImageView.setImageBitmap(bmp);
        croppedImageView.setFixedAspectRatio(true);
        croppedImageView.setAspectRatio(1,1);
        Button saveButton = (Button)findViewById(R.id.btnSaveCropping);
        Button cancelButton = (Button)findViewById(R.id.btnCancelCropping);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bmp = croppedImageView.getCroppedImage();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, bos);
                FileOutputStream fos = null;
                String imagePath = getIntent().getExtras().getString("to");
                try {
                    fos = new FileOutputStream(imagePath);
                    bos.writeTo(fos);
                    fos.flush();
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent returnRes = new Intent();
                returnRes.putExtra("imagePath",imagePath);
                setResult(Activity.RESULT_OK, returnRes);
                finish();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_crop, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.


        return super.onOptionsItemSelected(item);
    }
}
