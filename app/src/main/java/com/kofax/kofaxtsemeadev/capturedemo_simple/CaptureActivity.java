package com.kofax.kofaxtsemeadev.capturedemo_simple;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.widget.Button;
import android.widget.Toast;

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

import java.util.ArrayList;


public class CaptureActivity extends Activity
        implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        ImageCapturedListener,
        CameraInitializationListener
{
    private final PermissionsManager mPermissionsManager = new PermissionsManager(this);
    private ImageCaptureView imageCaptureView;
    private Button btnManual;
    private DocumentCaptureExperience documentCaptureExperience;

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode == Global.REQ_CODE_PERMISSIONS_CAPTURE) {
            if (mPermissionsManager.isGranted(Global.PERMISSIONS_CAPTURE)) {
                initializeCaptureControlWithDocumentCaptureExperience();
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
            imageCaptureView.setFlash(Flash.AUTOTORCH);
        else
            imageCaptureView.setFlash(Flash.OFF);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppContextProvider.setContext(getApplicationContext());

        super.onCreate(savedInstanceState);

        if(Licensing.setMobileSDKLicense(Global.PROCESS_PAGE_SDK_LICENSE) != ErrorInfo.KMC_SUCCESS)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(CaptureActivity.this);
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

        setContentView(R.layout.activity_capture);
        imageCaptureView = (ImageCaptureView)findViewById(R.id.imageCaptureView);
        btnManual = (Button)findViewById(R.id.btnCapture);
        initializeCaptureControlWithDocumentCaptureExperience();

        Global.theCapturedAndProcessedImageFiles = new ArrayList<>();
    }

    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCaptureProcess();
    }

    @Override
    protected void onPause() {
        super.onPause();
     }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void showManualCaptureButton(boolean show)
    {
        if(show){
            btnManual.setEnabled(true);
            btnManual.setTextColor(getResources().getColor(R.color.colorPrimary));
        } else {
            btnManual.setEnabled(false);
            btnManual.setTextColor(Color.TRANSPARENT);
        }
    }

    private void initializeCaptureControlWithDocumentCaptureExperience()
    {
        imageCaptureView.addCameraInitializationListener(this);
        imageCaptureView.addOnImageCapturedListener(this);

        if(Global.USE_LOCATION_SERVICES)
            imageCaptureView.setGpsUsage(GpsUsageLimits.ALWAYS_USE_IF_ENABLED);
        else
            imageCaptureView.setGpsUsage(GpsUsageLimits.NEVER_USE);

        imageCaptureView.setUseVideoFrame(false);

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

        documentCaptureExperience = new DocumentCaptureExperience(imageCaptureView, criteriaHolder);
        documentCaptureExperience.setGuidanceFrameColor(Color.argb(200, 255, 255, 255));
        documentCaptureExperience.setSteadyGuidanceFrameColor(Color.GREEN);
        documentCaptureExperience.setOuterViewFinderColor(Color.argb(200, 0, 0, 0));
        documentCaptureExperience.setVibrationEnabled(false);

        CaptureMessage msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgUserInstruction));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        documentCaptureExperience.setUserInstructionMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgZoomIn));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        documentCaptureExperience.setZoomInMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgZoomOut));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        documentCaptureExperience.setZoomOutMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgCenter));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        documentCaptureExperience.setCenterMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgCaptureDone));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        documentCaptureExperience.setCapturedMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgHoldSteady));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        documentCaptureExperience.setHoldSteadyMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgHoldParallel));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        documentCaptureExperience.setHoldParallelMessage(msg);

        msg = new CaptureMessage();
        msg.setMessage(getResources().getString(R.string.msgRotate));
        msg.setOrientation(CaptureMessage.KUIMessageOrientation.PORTRAIT);
        documentCaptureExperience.setRotateMessage(msg);
    }

    private void startCaptureProcess()
    {
        if(Global.theCapturedAndProcessedImageFiles==null)
            Global.theCapturedAndProcessedImageFiles = new ArrayList<>();

        Global.processStatus = Global.PROCESSSTATUS.PS_START;
        showManualCaptureButton(true);
        documentCaptureExperience.takePicture();
    }

    @Override
    public void onImageCaptured(ImageCapturedEvent imageCapturedEvent) {
        Global.currentCapturedImage = imageCapturedEvent.getImage();
        Intent intent = new Intent(CaptureActivity.this, ReviewActivity.class);
        startActivity(intent);
    }
}
