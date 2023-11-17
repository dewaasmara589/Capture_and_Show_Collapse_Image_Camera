package com.alfastore.testphotocollage;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PictureCallback {

    private SurfaceHolder surfaceHolder;
    private Camera camera;

    public static final int REQUEST_CODE = 100;

    private String[] neededPermissions = new String[]{CAMERA, WRITE_EXTERNAL_STORAGE};
    private SurfaceView[] SVS = new SurfaceView[6];
    private ImageView[] IVS = new ImageView[5];

    private Bitmap bmp;
    private int indexCamera = 0;

    private ConstraintLayout clContent;
    private TextView tvSysDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SVS[0] = findViewById(R.id.surfaceView);
        SVS[1] = findViewById(R.id.surfaceView2);
        SVS[2] = findViewById(R.id.surfaceView3);
        SVS[3] = findViewById(R.id.surfaceView4);
        SVS[4] = findViewById(R.id.surfaceView5);
        SVS[5] = findViewById(R.id.surfaceView6);

        IVS[0] = findViewById(R.id.ivIcon1);
        IVS[1] = findViewById(R.id.ivIcon2);
        IVS[2] = findViewById(R.id.ivIcon3);
        IVS[3] = findViewById(R.id.ivIcon4);
        IVS[4] = findViewById(R.id.ivIcon5);

        if (SVS[0] != null) {
            boolean result = checkPermission();
            if (result) {
                setupSurfaceHolder();
            }
        }
    }

    private boolean checkPermission() {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            ArrayList<String> permissionsNotGranted = new ArrayList<>();
            for (String permission : neededPermissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNotGranted.add(permission);
                }
            }
            if (permissionsNotGranted.size() > 0) {
                boolean shouldShowAlert = false;
                for (String permission : permissionsNotGranted) {
                    shouldShowAlert = ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
                }
                if (shouldShowAlert) {
                    showPermissionAlert(permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]));
                } else {
                    requestPermissions(permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]));
                }
                return false;
            }
        }
        return true;
    }

    private void showPermissionAlert(final String[] permissions) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission Required");
        alertBuilder.setMessage("You must grant permission to access camera and external storage to run this application.");
        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                requestPermissions(permissions);
            }
        });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    private void requestPermissions(String[] permissions) {
        ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                for (int result : grantResults) {
                    if (result == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(MainActivity.this, "All permissions are required.", Toast.LENGTH_LONG).show();
                        setViewVisibility(R.id.showPermissionMsg, View.VISIBLE);
                        return;
                    }
                }

                setupSurfaceHolder();
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setViewVisibility(int id, int visibility) {
        View view = findViewById(id);
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    private void setupSurfaceHolder() {
        if (indexCamera < 6){
            setViewVisibility(R.id.captureBtn, View.VISIBLE);
            SVS[indexCamera].setVisibility(View.VISIBLE);

            surfaceHolder = SVS[indexCamera].getHolder();
            surfaceHolder.addCallback(this);
            setCaptureBtnClick();
        }else {
            setViewVisibility(R.id.captureBtn, View.GONE);

            if (camera != null) {
                camera.stopPreview();
                camera.release();
                camera = null;
            }

            setViewVisibility(R.id.saveBtn, View.VISIBLE);

            tvSysDate = (TextView) findViewById(R.id.tvSysDate);

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            String date = dateFormat.format(calendar.getTime());
            tvSysDate.setText(date);

            setViewVisibility(R.id.tvSysDate, View.VISIBLE);
            setSaveBtnClick();
        }
    }

    private void setCaptureBtnClick() {
        Button captureBtn = findViewById(R.id.captureBtn);
        if (captureBtn != null) {
            captureBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    captureImage();
                }
            });
        }
    }

    private void setSaveBtnClick() {
        Button saveBtn = findViewById(R.id.saveBtn);
        if (saveBtn != null) {
            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clContent = (ConstraintLayout) findViewById(R.id.clContent);
                    Bitmap getCaptureCollageAllImage = getBitMapFromView(clContent);

                    saveImage(getCaptureCollageAllImage);
                }
            });
        }
    }

    public static Bitmap getBitMapFromView(View view){
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        view.draw(canvas);

        return returnedBitmap;
    }

    public void captureImage() {
        if (camera != null) {
            camera.takePicture(null, null, this);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startCamera();
    }

    private void startCamera() {
        camera = Camera.open();
        camera.setDisplayOrientation(90);
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        resetCamera();
    }

    public void resetCamera() {
        if (surfaceHolder.getSurface() == null) {
            // Return if preview surface does not exist
            return;
        }

        if (camera != null) {
            // Stop if preview surface is already running.
            camera.stopPreview();
            try {
                // Set preview display
                camera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Start the camera preview...
            camera.startPreview();
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {

        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                Canvas c = holder.lockCanvas(null);

                c.drawBitmap(bmp, 10, 10, null);

                holder.unlockCanvasAndPost(c);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });

        bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        if (indexCamera<5) {
            IVS[indexCamera].setVisibility(View.GONE);
        }

        indexCamera++;
        setupSurfaceHolder();
        resetCamera();
    }

    private void saveImage(Bitmap getBitmap) {
        FileOutputStream outStream;
        try {
            String fileName = "Capture_" + System.currentTimeMillis() + ".jpg";
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
            outStream = new FileOutputStream(file);
            getBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
            outStream.close();
            Toast.makeText(MainActivity.this, "Picture Saved: " + fileName, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}