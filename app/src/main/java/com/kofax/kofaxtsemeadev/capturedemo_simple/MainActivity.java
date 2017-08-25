package com.kofax.kofaxtsemeadev.capturedemo_simple;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import com.kofax.kmc.ken.engines.ImageProcessor;
import com.kofax.kmc.ken.engines.data.DocumentDetectionSettings;
import com.kofax.kmc.kui.uicontrols.CameraInitializationEvent;
import com.kofax.kmc.kui.uicontrols.CameraInitializationListener;
import com.kofax.kmc.kui.uicontrols.ImageCaptureView;
import com.kofax.kmc.kui.uicontrols.ImageCapturedEvent;
import com.kofax.kmc.kui.uicontrols.ImageCapturedListener;
import com.kofax.kmc.kui.uicontrols.captureanimations.CaptureMessage;
import com.kofax.kmc.kui.uicontrols.captureanimations.DocumentCaptureExperience;
import com.kofax.kmc.kui.uicontrols.captureanimations.DocumentCaptureExperienceCriteriaHolder;
import com.kofax.kmc.kui.uicontrols.data.Flash;
import com.kofax.kmc.kui.uicontrols.data.GpsUsageLimits;
import com.kofax.kmc.kut.utilities.AppContextProvider;
import com.kofax.kmc.kut.utilities.Licensing;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;

public class MainActivity extends Activity
        implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        CameraInitializationListener,
        ImageProcessor.ImageOutListener
{

    private final PermissionsManager mPermissionsManager = new PermissionsManager(this);
    private ImageCaptureView mImageCaptureView;
    private DocumentCaptureExperience mExperience;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode == Global.REQ_CODE_PERMISSIONS_CAPTURE) {
            if (mPermissionsManager.isGranted(Global.PERMISSIONS_CAPTURE)) {
                setUp();
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.capture_permissions_rationale)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        } else {
            if (mPermissionsManager.isGranted(Global.PERMISSIONS_GALLERY)) {
                //pickImageFromGallery();
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.gallery_permissions_rationale)
                        .setPositiveButton(android.R.string.ok, null)
                        .setCancelable(true)
                        .show();
            }
        }
    }

    @Override
    public void onCameraInitialized(CameraInitializationEvent cameraInitializationEvent) {
        //The flash can't be set in the onCreate(), only once the camera is initialized
        if(Global.USE_AUTO_TORCH)
            mImageCaptureView.setFlash(Flash.AUTOTORCH);
        else
            mImageCaptureView.setFlash(Flash.OFF);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppContextProvider.setContext(getApplicationContext());

        super.onCreate(savedInstanceState);

        if(Licensing.setMobileSDKLicense(Global.PROCESS_PAGE_SDK_LICENSE) != ErrorInfo.KMC_SUCCESS)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("License Error");
            builder.setMessage("Kofax Mobile SDK license is invalid");

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }

        if (!mPermissionsManager.isGranted(Global.PERMISSIONS_CAPTURE)) {
            mPermissionsManager.request(Global.REQ_CODE_PERMISSIONS_CAPTURE, Global.PERMISSIONS_CAPTURE);
        }

        setContentView(R.layout.activity_main);
        setUp();
    }


    private void setUp()
    {
        mImageCaptureView = (ImageCaptureView) findViewById(R.id.imageVaptureView);

        if(Global.USE_LOCATION_SERVICES)
            mImageCaptureView.setGpsUsage(GpsUsageLimits.ALWAYS_USE_IF_ENABLED);
        else
            mImageCaptureView.setGpsUsage(GpsUsageLimits.NEVER_USE);

        mImageCaptureView.setUseVideoFrame(false);

        DocumentCaptureExperienceCriteriaHolder criteriaHolder = new DocumentCaptureExperienceCriteriaHolder();
        criteriaHolder.setPitchThreshold(15);
        criteriaHolder.setPitchThresholdEnabled(true);
        criteriaHolder.setRollThreshold(15);
        criteriaHolder.setRollThresholdEnabled(true);
        criteriaHolder.setStabilityThreshold(75);
        criteriaHolder.setStabilityThresholdEnabled(true);
        criteriaHolder.setFocusEnabled(true);
        criteriaHolder.setOrientationEnabled(true);

        DocumentDetectionSettings documentDetectionSettings = new DocumentDetectionSettings();
        documentDetectionSettings.setTargetFrameAspectRatio(29.7/21.0);
        documentDetectionSettings.setTargetFramePaddingPercent(5);
        documentDetectionSettings.setLongEdgeThreshold(0.75);
        documentDetectionSettings.setShortEdgeThreshold(0.75);
        documentDetectionSettings.setMinFillFraction(0.65);
        documentDetectionSettings.setMaxFillFraction(1.0);

        criteriaHolder.setDetectionSettings(documentDetectionSettings);
        criteriaHolder.setRefocusEnabled(true);

        mExperience = new DocumentCaptureExperience(mImageCaptureView, criteriaHolder);

        mExperience.addOnImageCapturedListener(mImageCapturedListener);

        mExperience.setGuidanceFrameColor(Color.argb(200, 255, 255, 255));
        mExperience.setSteadyGuidanceFrameColor(Color.GREEN);
        mExperience.setOuterViewFinderColor(Color.argb(200, 0, 0, 0));
        mExperience.setVibrationEnabled(false);

        CaptureMessage msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgUserInstruction));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        mExperience.setUserInstructionMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgZoomIn));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        mExperience.setZoomInMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgZoomOut));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        mExperience.setZoomOutMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgCenter));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        mExperience.setCenterMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgCaptureDone));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        mExperience.setCapturedMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgHoldSteady));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        mExperience.setHoldSteadyMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgHoldParallel));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        mExperience.setHoldParallelMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgRotate));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        mExperience.setRotateMessage(msg);

        mExperience.takePicture();
    }

    private final ImageCapturedListener mImageCapturedListener = new ImageCapturedListener() {
        @Override
        public void onImageCaptured(final ImageCapturedEvent event) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //reviewImage(event.getImage());
                }
            });
        }
    };

    @Override
    public void imageOut(ImageProcessor.ImageOutEvent imageOutEvent) {

    }
}
