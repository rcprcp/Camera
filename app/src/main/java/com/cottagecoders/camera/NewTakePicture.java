package com.cottagecoders.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class NewTakePicture extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "CameraCode";
    private String filename;

    private AsyncTask<byte[], Void, Integer> task = null;

    // UI elements...
    private Button take;
    private Button exit;
    private Button switchCamera;
    private Button flashButton;
    private ProgressBar spinner;
    private SurfaceView cameraView;

    private Camera camera;

    // state variables...
    private int requestedSize = 1024;
    private int numCameras = 0;
    private int currentCamera = 0; // 0 happens to default to back camera...

    private boolean isCameraOpen = false;
    private boolean isPreviewEnabled = false;

    // initialize this variable when the camera is ready to take pictures. we
    // can't just initialize it here since we will switching from front to back
    // cameras(and vice-versa) on some devices.
    private boolean isAutoFocusProcessing;

    // indicate if the flash is on (in torch_mode) or off.
    private boolean isFlashOn;

    // this is necessary so we can autofocus one more time before taking the
    // picture. the take picture code will actually happen in the autofocus
    // complete routine.
    private boolean shouldTakePicture = false;

    // these are for the zoom code.
    private double previousDistance = 0.0;
    private int actionCounter = 0;

    // device features...
    private boolean isZoomSupported;

    private Activity act;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate(): GOT HERE");
        super.onCreate(savedInstanceState);

        // go into full screen mode - remove the status bar and the action
        // bar...
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        act = this;

        // get the info passed to us in the intent.
        filename = getIntent().getStringExtra("name");
        if (filename.equals("")) {
            exitFail();
        }

        requestedSize = getIntent().getIntExtra("size", 1023);
        Log.d(TAG, "before initialize: filename = \"" + filename
                + "\" requestedSize = " + requestedSize);

    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause(): GOT HERE");
        if (task != null) {
            try {
                Log.d(TAG,
                        "onPause(): called during AsyncTask - before task.get()");
                task.get();
                Log.d(TAG, "onPause(): after task.get()");
            } catch (Exception e) {
                Log.d(TAG, "onPause(): exception during AsyncTask");
                e.printStackTrace();
            }
        }

        if (isCameraOpen) {
            isCameraOpen = false;
            camera.release();
            camera = null;
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume(): GOT HERE");
        super.onResume();

        isCameraOpen = false;
        isPreviewEnabled = false;

        initializeCamera();
    }

    @Override
    public void onBackPressed() {
        disableEverything();
        shutdown();
        exitFail();
        super.onBackPressed();

    }

    private void exitFail() {
        getIntent().putExtra("okpicture", "NO");
        setResult(Activity.RESULT_OK, getIntent());
        finish();
    }

    private void initializeCamera() {
        Log.d(TAG, "initializeCamera(): GOT HERE");
        // create the surface we'll use for the camera preview...
        setContentView(R.layout.new_see_camera);

        // get a handle on the surface - when tapped, autofocus.
        // two fingers spreading or pinching to zoom in or out...
        cameraView = (SurfaceView) this.findViewById(R.id.CameraView);
        cameraView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                Log.d(TAG, "onTouch(): GOT HERE");
                v.performClick();   //TODO: is this correct?

                // check if we're in the process of taking a picture...
                // if so, then we shouldn't permit any more clicks on the
                // surface which would trigger the autofocus code (and probably
                // crash).
                if (shouldTakePicture) {
                    Log.d(TAG, "touched surface after clicking \"Take\"");
                    return true;
                }

                if (e.getPointerCount() == 1) {
                    // when single touch do autofocus.
                    autoFocus();

                    // ending/starting zoom, initialize variables that control
                    // zooming.
                    previousDistance = 0.0;
                    actionCounter = 0;

                } else if (e.getPointerCount() > 1 && isZoomSupported) {
                    Log.d(TAG, "in zoom code");
                    // probably a zoom motion.

                    // check if we've skipped the right number of events to
                    // make the zooming less sensitive.
                    actionCounter++;
                    if (actionCounter % 8 != 0)
                        return true;

                    Camera.Parameters p = camera.getParameters();
                    int max = p.getMaxZoom();
                    int currentZoomLevel = p.getZoom();

                    double currentDistance = distanceBetweenPoints(e);

                    // previous is less than current when fingers are making a
                    // spread gesture.
                    if (previousDistance < currentDistance) {
                        if (currentZoomLevel < max) {
                            currentZoomLevel++;
                            p.setZoom(currentZoomLevel);
                            camera.setParameters(p);
                        }
                    } else {
                        // else, this is a pinch gesture.
                        if (currentZoomLevel >= 0) {
                            currentZoomLevel--;
                            p.setZoom(currentZoomLevel);
                            camera.setParameters(p);
                        }
                    }
                    // save for next time.
                    previousDistance = currentDistance;
                }
                return true;
            }
        });

        SurfaceHolder surfaceHolder;
        surfaceHolder = cameraView.getHolder();
        surfaceHolder.addCallback(this);

        // TODO: this is necessary for older phones (deprecated in API 11):
        pushBuffers(surfaceHolder);

        // cameraView.setFocusable(true);
        // cameraView.setFocusableInTouchMode(true);

        // put an overlay on the surface that has the buttons to control the
        // camera.
        LayoutInflater inflater;
        inflater = LayoutInflater.from(getBaseContext());
        View viewControl;
        viewControl = inflater.inflate(R.layout.new_camera_control, null);

        this.addContentView(viewControl, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // set up the progress bar...
        spinner = (ProgressBar) viewControl.findViewById(R.id.progress);

        // set up actions for the buttons...
        take = (Button) viewControl.findViewById(R.id.take);
        take.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show the progress indication.
                spinner.setVisibility(View.VISIBLE);

                // might be a timing problem here,
                // so we want to disable the buttons
                // before someone can tap another one...
                disableEverything();

                // set this to indicate we should take the picture in the
                // autofocus completion routine.
                shouldTakePicture = true;

                // make sure we're going to run the autofocus routine...
                isAutoFocusProcessing = false;

                // if the camera supports it, do autofocus...
                // otherwise take the picture here.
                Camera.Parameters p = camera.getParameters();
                if (p.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    autoFocus();
                } else {
                    camera.takePicture(shutter, null, null, jpegImage);
                    // side effect of takePicture is to disable the preview -
                    // update state variable for later...
                    isPreviewEnabled = false;

                }
            }
        });

        numCameras = Camera.getNumberOfCameras();
        if (numCameras == 0) {
            myDialog("Error", "This device does not have a camera.");
        }
        Log.d(TAG, "initializeCamera(): numCameras " + numCameras);

        switchCamera = (Button) viewControl.findViewById(R.id.switch_camera);
        if (numCameras > 1) {
            switchCamera.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchCamera();
                }
            });
        } else {
            switchCamera.setEnabled(false);
            switchCamera.setVisibility(View.INVISIBLE);
        }

        flashButton = (Button) viewControl.findViewById(R.id.flashbutton);
        flashButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                // help
                Camera.Parameters p = camera.getParameters();
                if (p.getFlashMode() != null) {
                    if (isFlashOn) {
                        p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        isFlashOn = false;
                        // button should say: turn it on.
                        flashButton.setText(R.string.flash_on);
                    } else {
                        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        isFlashOn = true;
                        // button should say: turn it off.
                        flashButton.setText(R.string.flash_off);
                    }
                    camera.setParameters(p);
                } else {
                    // no flash on this camera.
                    flashButton.setEnabled(false);
                    flashButton.setVisibility(View.INVISIBLE);
                }
            }
        });

        exit = (Button) viewControl.findViewById(R.id.exit);
        exit.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                // the following will only be disabled for a moment -
                // after cleaning up, we exit the activity.
                disableEverything();
                shutdown();

                exitFail();
            }
        });
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated()");
        openCamera();

        setupCamera(holder);
    }

    // put this in its own routine so the SuppressWarnings
    // has minimal impact.
    @SuppressWarnings("deprecation")
    private void pushBuffers(SurfaceHolder s) {
        s.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void openCamera() {
        Log.d(TAG, "openCamera(): GOT HERE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            try {
                camera = Camera.open(currentCamera);
            } catch (RuntimeException e) {
                Log.e(TAG, "SurfaceCreated(): Failed to open camera: " + e);
                exitFail();
            }
        } else {
            try {
                camera = Camera.open();
            } catch (RuntimeException e) {
                Log.e(TAG, "SurfaceCreated(): Failed to open camera: " + e);
                exitFail();
            }
        }

        if (camera != null) {
            isCameraOpen = true;
        }
    }

    private int figureCameraRotation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(currentCamera, info);

        // Log.d(TAG, "figureCameraRotation: orientation:" + orientation);
        // if (orientation < 0 || orientation > 360)
        // return 0;

        // orientation = (orientation + 45) / 90 * 90;

        return info.orientation;
    }

    private void setupCamera(SurfaceHolder holder) {
        Log.d(TAG, "setUpCamera(): GOT HERE");

        try {
            // get camera parameters, so we can get more camera details.
            Camera.Parameters parameters = camera.getParameters();

            isZoomSupported = parameters.isZoomSupported();

            List<Camera.Size> sizes;
            sizes = parameters.getSupportedPictureSizes();
            int my_width = 0;
            int my_height = 0;
            for (Size s : sizes) {
                if (s.width <= requestedSize && s.height <= requestedSize) {
                    my_width = s.width;
                    my_height = s.height;
                    parameters.setPictureSize(s.width, s.height);
                    break;
                }
            }
            Log.d(TAG, "PictureSize set to h = " + my_height + " width = "
                    + my_width);

            // debugging info
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(currentCamera, info);
            Log.d(TAG, "camera orientation (port1, land2): "
                    + this.getResources().getConfiguration().orientation);
            Log.d(TAG, "info.orientation " + info.orientation
                    + " facing  (back=0, front=1)" + info.facing);

            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // This is an undocumented although widely known feature...
                parameters.set("orientation", "portrait");

                camera.setDisplayOrientation(90);
                parameters.setRotation(figureCameraRotation());

            } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // This is an undocumented although widely known feature...
                parameters.set("orientation", "landscape");

                camera.setDisplayOrientation(0);
                parameters.setRotation(figureCameraRotation());

            } else {
                Log.d(TAG, "invalid orientation: "
                        + this.getResources().getConfiguration().orientation);

            }

            parameters.setPreviewFormat(ImageFormat.NV21);

            camera.setPreviewDisplay(holder);

            camera.setParameters(parameters);

        } catch (Exception e) {
            camera.release();
            Log.v(TAG, "setupCamera() error: " + e);
            exitFail();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged(): GOT HERE");

        Camera.Parameters p = camera.getParameters();
        camera.setParameters(p);
        Size s = p.getPreviewSize();
        Log.d(TAG, " with w = " + s.width + " height " + s.height);

        if (!p.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
            p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            try {
                camera.setParameters(p);
            } catch (Exception e) {
                Log.d(TAG, "Set Focus Mode to AUTO failed " + p.getFocusMode()
                        + " " + e);
                // reload the parameters - they seem to get trashed if setting
                // them fails.
                p = camera.getParameters();

            }
        }

        if (p.getWhiteBalance() != null) {
            p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            Log.d(TAG, "surfaceChanged 3");
            camera.setParameters(p);
        }

        p.setJpegQuality(100);
        Log.d(TAG, "surfaceChanged 5");
        camera.setParameters(p);

        isPreviewEnabled = true;
        camera.startPreview();

        // this is the right place for
        // initialization in case we swapped cameras from back-camera to
        // front-camera and back to the back-camera again. also this is
        // important in case one of the camera does not support autofocus.
        isAutoFocusProcessing = false;

        readyToTake();
        Log.v(TAG, "surfaceChanged() after readyToTake()");

    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed(): GOT HERE");
        if (isPreviewEnabled) {
            isPreviewEnabled = false;
            try {
                camera.stopPreview();
            } catch (Exception e) {
                Log.d(TAG, "surfaceDestroyed(): stop preview failed " + e);
            }
        }

        if (isCameraOpen) {
            try {
                isCameraOpen = false;
                camera.release();
                camera = null;
            } catch (Exception e) {
                Log.d(TAG,
                        "surfaceDestroyed(): exception in camera.release() - "
                                + e);
                exitFail();
            }
        }
    }

    private void readyToTake() {
        take.setEnabled(true);
        take.setVisibility(View.VISIBLE);

        if (numCameras > 1) {
            switchCamera.setEnabled(true);
            switchCamera.setVisibility(View.VISIBLE);
        }

        Camera.Parameters p = camera.getParameters();
        if (p.getFlashMode() != null) {
            isFlashOn = true;
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(p);

            // button to turn off the flash...
            flashButton.setEnabled(true);
            flashButton.setText(R.string.flash_off);
            flashButton.setVisibility(View.VISIBLE);

        } else {
            isFlashOn = false;
            // disable button to turn on the flash...
            flashButton.setEnabled(false);
            flashButton.setText(R.string.flash_on);
            flashButton.setVisibility(View.INVISIBLE);

        }

        exit.setEnabled(true);
        exit.setVisibility(View.VISIBLE);
    }

    private void disableEverything() {

        take.setEnabled(false);
        take.setVisibility(View.INVISIBLE);

        switchCamera.setEnabled(false);
        switchCamera.setVisibility(View.INVISIBLE);

        flashButton.setEnabled(false);
        flashButton.setVisibility(View.INVISIBLE);

        exit.setEnabled(false);
        exit.setVisibility(View.INVISIBLE);

    }

    private void shutdown() {
        if (camera != null) {
            if (isPreviewEnabled) {
                try {
                    isPreviewEnabled = false;
                    camera.stopPreview();
                } catch (Exception e) {
                    Log.d(TAG, "shutdown(): stop preview failed");
                }
            }

            if (isCameraOpen) {
                isCameraOpen = false;
                camera.release();
                camera = null;
            }
        }
    }

    ShutterCallback shutter = new ShutterCallback() {
        public void onShutter() {
            Log.d(TAG, "ShutterCallback: onShutter()");
        }
    };

    PictureCallback jpegImage = new PictureCallback() {
        public void onPictureTaken(final byte[] data, Camera c) {
            Log.d(TAG, "jpegImage(): onPictureTaken(): GOT HERE");

            // AsyncTask to process and save the photo.
            task = new SavePhoto().execute(data);
        }
    };

    AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
            Log.d(TAG, "autofocusCallback: onautofocus()");
            isAutoFocusProcessing = false;

            if (shouldTakePicture) {
                int orientation = act.getRequestedOrientation();
                int rotation = ((WindowManager) act
                        .getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay().getRotation();
                switch (rotation) {
                    case Surface.ROTATION_0:
                        orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                        break;
                    case Surface.ROTATION_90:
                        orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                        break;
                    case Surface.ROTATION_180:
                        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                        break;
                    default:
                        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                        break;
                }
                act.setRequestedOrientation(orientation);

                camera.takePicture(shutter, null, null, jpegImage);
                // side effect of takePicture is to disable the preview -
                // update state variable for later...
                isPreviewEnabled = false;
            }
        }
    };

    private void writePictureToFile(String filename, byte[] data) {
        Log.d(TAG, "writePictureToFile(): GOT HERE");

        // TODO: although this code might be good enough,
        // we need to keep an eye on this problem since it's probably
        // going to be phone brand specific.
        //
        // here we need to see if the image and the device have the same
        // orientation.
        //
        // On some Samsung devices, we sometimes get an image from
        // the back camera which is rotated 90 degrees.

        Bitmap src = null;
        byte[] theArray;

        if (data == null) {
            Log.d(TAG, "writePictureToFile(): data is null ");
            // this is a very bad error.
            exitFail();
        }

        theArray = data; // initial copy. not every case changes the byte [].

        try {
            // first convert from byte array to bitmap.
            src = BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            Log.d(TAG, "writePictureToFile(): decodeByteArray failed: " + e);
        }

        // check if landscape photo or portrait.
        if (src.getWidth() > src.getHeight()) {
            // landscape image... is device in landscape?

            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // landscape image, landscape device - do nothing.
                Log.d(TAG,
                        "writePictureToFile(): landscape image, landscape device - do nothing");

            } else { // landscape image, portrait device.
                // the idea here is to rotate the image to match the device
                // orientation.
                Log.d(TAG,
                        "writePictureToFile(): landscape image, portrait device.  ROTATE!");

                Bitmap newBitmap = rotateBitmap(src, figureCameraRotation());
                // rotated, so convert bitmap to jpg.
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                theArray = stream.toByteArray();

            }

        } else {
            // got here? image in portrait orientation.
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // got here? device in portrait orientation, too.
                // nothing to do.
                Log.d(TAG,
                        "writePictureToFile(): portrait image, portrait device - do nothing");
            } else { // portrait image, landscape device.
                // the idea here is to rotate the image to match the device
                // orientation.
                Log.d(TAG,
                        "writePictureToFile(): portrait image, landscape device. ROTATE!");
                Bitmap newBitmap = rotateBitmap(src, figureCameraRotation());
                // last step, convert bitmap to jpg.
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                theArray = stream.toByteArray();
            }
        }

        File f = new File(filename);
        if (f.exists())
            f.delete();

        try {
            f.createNewFile();
        } catch (Exception e) {
            Log.d(TAG, "writePictureToFile(): error creating file " + e);
            // very bad error.
            exitFail();
        }

        // write the bytes to file...
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(theArray);
            fos.flush();
            fos.close();

        } catch (Exception e) {
            Log.d(TAG,
                    "writePictureToFile(): ERROR creating or writing output FileOutputStream "
                            + e);
            // a very bad error.
            exitFail();

        }
        Log.d(TAG, "writePictureToFile(): done. filename = " + filename
                + " theArray.length = " + theArray.length);
    }

    private void switchCamera() {
        Log.d(TAG, "switchCamera(): GOT HERE");

        disableEverything();
        if (isPreviewEnabled) {
            camera.stopPreview();
            isPreviewEnabled = false;
        }
        if (isCameraOpen) {
            isCameraOpen = false;
            camera.release();
        }

        if (currentCamera == 0) {
            currentCamera = 1;
        } else {
            currentCamera = 0;
        }
        initializeCamera();
    }

    private void myDialog(String title, String message) {
        Log.d(TAG, "myDialog(): GOT HERE");

        final AlertDialog dlg = new AlertDialog.Builder(act)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog,
                                                final int which) {
                                // User clicked the button so do some stuff
                                dialog.dismiss();
                                exitFail();
                            }
                        }).create();
        dlg.show();
    }

    private double distanceBetweenPoints(MotionEvent e) {
        Log.d(TAG, "distanceBetweenPoints(): GOT HERE");

        double ans = 0;

        MotionEvent.PointerCoords one = new MotionEvent.PointerCoords();
        e.getPointerCoords(0, one);

        MotionEvent.PointerCoords two = new MotionEvent.PointerCoords();
        e.getPointerCoords(1, two);

        if (one.x < two.x) {
            ans += two.x - one.x;
        } else {
            ans += one.x - two.x;
        }

        if (one.y < two.y) {
            ans += two.y - one.y;
        } else {
            ans += one.y - two.y;
        }

        Log.d(TAG, "distanceBetweenPoints(): one.x, one.y, two.x, two.y, ans "
                + one.x + " " + one.y + " " + two.x + " " + two.y + " " + ans);
        return ans;
    }

    private void autoFocus() {

        // does the camera support autofocus?
        Camera.Parameters p = camera.getParameters();
        Log.d(TAG, "autoFocus(): GOT HERE. mode = " + p.getFocusMode());
        if (p.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
            if (isAutoFocusProcessing == false) { // a little easier to read.
                isAutoFocusProcessing = true;
                camera.autoFocus(myAutoFocusCallback);
            }
        }
    }

    private static Bitmap rotateBitmap(Bitmap src, double degrees) {
        Log.d(TAG, "rotateBitmap(): GOT HERE");

        Matrix matrix = new Matrix();
        matrix.postRotate((float) degrees);

        Bitmap bmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(),
                src.getHeight(), matrix, true);
        return bmp;
    }

    private class SavePhoto extends AsyncTask<byte[], Void, Integer> {

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "AsyncTask - onPreExecute");
            shutdown();
        }

        @Override
        protected Integer doInBackground(byte[]... data) {
            Log.d(TAG, "AsyncTask - doInBackground");
            writePictureToFile(filename, data[0]);
            return 1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log.d(TAG, "AsyncTask - onPostExecute");
            getIntent().putExtra("okpicture", "yes");
            setResult(Activity.RESULT_OK, getIntent());
            finish();
        }
    }
}