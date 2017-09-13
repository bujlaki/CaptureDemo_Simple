package com.kofax.kofaxtsemeadev.capturedemo_simple;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.kofax.kmc.ken.engines.ImageProcessor;
import com.kofax.kmc.ken.engines.data.DocumentDetectionSettings;
import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.ken.engines.data.ImagePerfectionProfile;
import com.kofax.kmc.ken.engines.data.QuickAnalysisFeedback;
import com.kofax.kmc.ken.engines.data.QuickAnalysisSettings;
import com.kofax.kmc.kui.uicontrols.CameraInitializationEvent;
import com.kofax.kmc.kui.uicontrols.CameraInitializationListener;
import com.kofax.kmc.kui.uicontrols.ImageCaptureView;
import com.kofax.kmc.kui.uicontrols.ImageCapturedEvent;
import com.kofax.kmc.kui.uicontrols.ImageCapturedListener;
import com.kofax.kmc.kui.uicontrols.ImgReviewEditCntrl;
import com.kofax.kmc.kui.uicontrols.captureanimations.CaptureMessage;
import com.kofax.kmc.kui.uicontrols.captureanimations.DocumentCaptureExperience;
import com.kofax.kmc.kui.uicontrols.captureanimations.DocumentCaptureExperienceCriteriaHolder;
import com.kofax.kmc.kui.uicontrols.data.Flash;
import com.kofax.kmc.kui.uicontrols.data.GpsUsageLimits;
import com.kofax.kmc.kut.utilities.AppContextProvider;
import com.kofax.kmc.kut.utilities.Licensing;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;

import java.nio.ByteBuffer;
import java.util.ArrayList;


public class CaptureActivity extends Activity
        implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        DialogInterface.OnClickListener,
        ImageCapturedListener,
        CameraInitializationListener,
        ImageProcessor.ImageOutListener,
        ImageProcessor.AnalysisCompleteListener
{
    private final PermissionsManager mPermissionsManager = new PermissionsManager(this);

    private ImageCaptureView imageCaptureView;
    private ImgReviewEditCntrl imgReviewEditCntrl;
    private Button btnOk;
    private Button btnRetry;
    private Button btnManual;
    private DocumentCaptureExperience documentCaptureExperience;
    private ImageProcessor imageProcessor;

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
        imgReviewEditCntrl = (ImgReviewEditCntrl)findViewById(R.id.imgReviewControl);
        btnOk = (Button)findViewById(R.id.btnOK);
        btnRetry = (Button)findViewById(R.id.btnRetry);
        btnManual = (Button)findViewById(R.id.btnCapture);

        btnOk.setOnClickListener(okButtonPressed);
        btnRetry.setOnClickListener(retryButtonPressed);

        showOkRetryButtons(false);
        initializeCaptureControlWithDocumentCaptureExperience();
    }

    View.OnClickListener retryButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showOkRetryButtons(false);
            imgReviewEditCntrl.clearImage();
            imgReviewEditCntrl.setVisibility(View.INVISIBLE);

            if(Global.processStatus == Global.PROCESSSTATUS.PS_IMAGE_PROCESSING_FINISHED)
                Global.theCapturedAndProcessedImageFiles.remove(Global.theCapturedAndProcessedImageFiles.size() - 1);

            startCaptureProcess();
        }
    };

    View.OnClickListener okButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showOkRetryButtons(false);

            if(Global.processStatus== Global.PROCESSSTATUS.PS_QUICK_ANALYSIS) {
                doImageProcessing(Global.currentImage);
            } else {
                if(Global.NUMBER_OF_PAGES_TO_CAPTURE<=0){
                    //ask for further pages
                } else {
                    if(Global.DO_EXTRACTION){
                        // call server extraction
                    } else {
                        //finish
                    }
                }
            }
        }
    };

    protected void onStart() {
        super.onStart();
        Global.theCapturedAndProcessedImageFiles = new ArrayList<>();
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
        cleanObjects();
    }

    private void showOkRetryButtons(boolean show)
    {
        if(show){
            btnOk.setEnabled(true);
            btnOk.setTextColor(getResources().getColor(R.color.colorPrimary));
            btnRetry.setEnabled(true);
            btnRetry.setTextColor(getResources().getColor(R.color.colorPrimary));
        } else {
            btnOk.setEnabled(false);
            btnOk.setTextColor(Color.TRANSPARENT);
            btnRetry.setEnabled(false);
            btnRetry.setTextColor(Color.TRANSPARENT);
        }
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

        showOkRetryButtons(false);
        showManualCaptureButton(true);

        documentCaptureExperience.takePicture();
    }

    @Override
    public void onImageCaptured(ImageCapturedEvent imageCapturedEvent) {
        doQuickAnalysis(imageCapturedEvent.getImage());
        Global.currentImage = imageCapturedEvent.getImage();
        //Global.ShowDialog(this, Global.ALERTS.ALERT_CONTINUE_CAPTURE, "", this);
    }

    private void showImage(Image img)
    {
        imgReviewEditCntrl.setVisibility(View.VISIBLE);
        try {
            imgReviewEditCntrl.setImage(img);
        } catch (KmcException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(CaptureActivity.this, ReviewActivity.class);
        intent.putExtra("image_to_show", img);
        startActivity(intent);
    }

    private void createImageProcessor()
    {
        if(imageProcessor==null){
            imageProcessor = new ImageProcessor();
            imageProcessor.addAnalysisCompleteEventListener(this);
            imageProcessor.addImageOutEventListener(this);
        }
    }

    private void cleanObjects()
    {
        if(Global.theCapturedAndProcessedImageFiles!=null) {
            Global.theCapturedAndProcessedImageFiles.clear();
            Global.theCapturedAndProcessedImageFiles = null;
        }
    }

    private void doQuickAnalysis(Image img)
    {
        Global.showProgressDialog(this, getResources().getString(R.string.statusQuickAnalysis));
        Global.processStatus = Global.PROCESSSTATUS.PS_QUICK_ANALYSIS;

        //Init ImageProcessor
        createImageProcessor();

        QuickAnalysisSettings qaSettings = new QuickAnalysisSettings();
        qaSettings.setBlurDetection(true);
        qaSettings.setSaturationDetection(true);
        qaSettings.setGlareDetection(true);
        qaSettings.setGlareDetectedThreshold(0.04);
        qaSettings.setGlareDetectionIntensityFraction(0.03);
        qaSettings.setGlareDetectionIntensityThreshold(230);
        qaSettings.setGlareDetectionMinimumGlareAreaFraction(0.03);
        qaSettings.setGlareDetectionNumberOfTiles(100);

        try {
            //imageCaptureView.setVisibility(View.INVISIBLE);
            imageProcessor.doQuickAnalysis(img, true, qaSettings);
        } catch (KmcException e) {
            e.printStackTrace();
            Global.ShowDialog(this, Global.ALERTS.ALERT_KMC_SDK_ERROR, e.getMessage(), this);
        }
    }

    @Override
    public void analysisComplete(ImageProcessor.AnalysisCompleteEvent analysisCompleteEvent) {
        if(analysisCompleteEvent.getStatus() != ErrorInfo.KMC_SUCCESS)
        {
            Global.ShowDialog(this,
                    Global.ALERTS.ALERT_KMC_SDK_ERROR,
                    "Error: " +
                    String.valueOf(analysisCompleteEvent.getStatus().getErr()) + "\n" +
                    analysisCompleteEvent.getStatus().getErrMsg(),
                    this);
        }else
        {
            QuickAnalysisFeedback qaFeedback = analysisCompleteEvent.getImage().getImageQuickAnalysisFeedBack();

            String qaResults = "";
            if(qaFeedback.isBlurry()) qaResults += getResources().getString(R.string.qaBlurry) + "\n";
            if(qaFeedback.isGlareDetected()) qaResults += getResources().getString(R.string.qaGlare) + "\n";
            if(qaFeedback.isOversaturated()) qaResults += getResources().getString(R.string.qaOversaturated) + "\n";
            if(qaFeedback.isUndersaturated()) qaResults += getResources().getString(R.string.qaUndersaturated) + "\n";

            Global.hideProgressDialog();

            if(qaFeedback.getViewBoundariesImage()!=null)
                showImage(new Image(qaFeedback.getViewBoundariesImage()));

            if(qaResults.length()>0){
                Global.ShowDialog(this, Global.ALERTS.ALERT_BAD_IMAGE_QUALITY, qaResults, this);
            } else {
                //call EVRS processing
                if(Global.IMMEDIATELY_CALL_EVRS)
                    doImageProcessing(Global.currentImage);
                else
                    showOkRetryButtons(true);
            }
        }
    }

    private void doImageProcessing(Image img)
    {
        Global.showProgressDialog(this, getResources().getString(R.string.statusImageProcessing));

        //Init ImageProcessor
        createImageProcessor();

        String strOperations = "";

        if(Global.evrsAutoCrop)
        {
            if(Global.evrsDocumentDetectorBasedCrop)
                strOperations += "_DoDocumentDetectorBasedCrop__DoCropCorrection_";
            else
                strOperations += "_DoCropCorrection_";
        }

        if(Global.evrsAutoRotate)
            strOperations += "_Doc90DegreeRotation_4";

        switch(Global.evrsDeskewMethod)
        {
            case 0:
                break;
            case 1:
                strOperations += "_DoSkewCorrectionAlt_"; break;
            case 2:
                strOperations += "_DoSkewCorrectionPage_"; break;
        }

        switch(Global.evrsColorMode)
        {
            case 0:
                strOperations += "_DoBinarization__DoEnhancedBinarization_"; break;
            case 1:
                strOperations += "_DoGrayOutput_"; break;
            case 2:
                break;
            case 3:
                strOperations += "_DoColorDetection_"; break;
        }

        if(Global.evrsScaleDPI>0)
        {
            switch(Global.evrsColorMode)
            {
                case 0:
                    strOperations += "_DoScaleBWImageToDPI_" + String.valueOf(Global.evrsScaleDPI); break;
                case 1:
                    strOperations += "_DoScaleCGImageToDPI_" + String.valueOf(Global.evrsScaleDPI); break;
                case 2:
                    strOperations += "_DoScaleCGImageToDPI_" + String.valueOf(Global.evrsScaleDPI); break;
                case 3:
                    strOperations += "_DoScaleImageToDPI_" + String.valueOf(Global.evrsScaleDPI); break;
            }
        }

        if(Global.evrsBackgrounSmoothing)
            strOperations += "_DoBackgroundSmoothing_";

        if(Global.evrsSharpenImage>0 && Global.evrsSharpenImage<=3)
            strOperations += "_DoSharpen_" + String.valueOf(Global.evrsSharpenImage);

        if(Global.evrsDespeckle>0 && Global.evrsDespeckle<=50)
            strOperations += "_DoDespeck_" + String.valueOf(Global.evrsDespeckle);

        if(Global.evrsSpecifyDocumentLongSide)
            strOperations += "_DocDimLarge_" + String.valueOf(Global.evrsLongSideLength);

        if(Global.evrsSpecifyDocumentShortSide)
            strOperations += "_DocDimSmall_" + String.valueOf(Global.evrsShortSideLength);

        if(Global.evrsIlluminationCorrection)
            strOperations += "_DoIlluminationCorrection_";
        else
            strOperations += "_LoadInlineSetting_[CSkewDetect.correct.illumination.Bool=0]";

        if(Global.evrsEdgeCleanup) {
            strOperations += "_DoEdgeCleanup_";
            //strOperations += "_LoadInlineSetting_[EdgeCleanup.enable=1]";
            //strOperations += "_LoadInlineSetting_[CBrdCrop.Crop_Dist.Int=8]";
            //strOperations += "_LoadInlineSetting_[CBrdCrop_Dist_Protate_for_DPI.BOOL=0";
        } else {
            strOperations += "_LoadInlineSetting_[EdgeCleanup.enable=0]";
        }

        // Sample Image Perfection String for ID Cards
        // strOperations = "_DoCropCorrection__DoSkewCorrectionPage__Do90DegreeRotation_4__DoScaleImageToDPI_300_DocDimSmall_2.125_DocDimLarge_3.375_LoadSetting_<Property Name=\"CSkewDetect.prorate_error_sum_thr_bkg_brightness.Bool\" Value=\"1\" Comment=\"DEFAULT 0\" />_LoadSetting_<Property Name=\"CSkwCor.Do_Fast_Rotation.Bool\" Value=\"0\" Comment=\"DEFAULT 1\" />_LoadSetting_<Property Name=\"CSkewDetect.correct_illumination.Bool\" Value=\"0\" Comment=\"DEFAULT 1\" />_LoadSetting_<Property Name=\"CSkwCor.Fill_Color_Scanner_Bkg.Bool\" Value=\"0\" Comment=\"DEFAULT 1 \" />_LoadSetting_<Property Name=\"CSkwCor.Fill_Color_Red.Byte\" Value=\"255\" Comment=\"DEFAULT 0 \" />_LoadSetting_<Property Name=\"CSkwCor.Fill_Color_Green.Byte\" Value=\"255\" Comment=\"DEFAULT 0 \" />_LoadSetting_<Property Name=\"CSkwCor.Fill_Color_Blue.Byte\" Value=\"255\" Comment=\"DEFAULT 0 \" />";

        ImagePerfectionProfile imagePerfectionProfile = new ImagePerfectionProfile();
        imagePerfectionProfile.setIpOperations(strOperations);

        if(Global.evrsCropToTargetFrame)
            imagePerfectionProfile.setUseTargetFrameCrop(ImagePerfectionProfile.UseTargetFrameCrop.ON);

        imageProcessor.setImagePerfectionProfile(imagePerfectionProfile);

        Global.processStatus = Global.PROCESSSTATUS.PS_IMAGE_PROCESSING;

        try {
            imageProcessor.processImage(img);
        } catch (KmcException e) {
            e.printStackTrace();
            Global.ShowDialog(this, Global.ALERTS.ALERT_KMC_SDK_ERROR, e.getMessage(), this);
        }
    }

    @Override
    public void imageOut(ImageProcessor.ImageOutEvent imageOutEvent) {
        if(imageOutEvent.getStatus()!=ErrorInfo.KMC_SUCCESS)
        {
            Global.hideProgressDialog();
            Global.ShowDialog(this,
                    Global.ALERTS.ALERT_KMC_SDK_ERROR,
                    "Error: " +
                            String.valueOf(imageOutEvent.getStatus().getErr()) + "\n" +
                            imageOutEvent.getStatus().getErrMsg(),
                    this);
        }
        else
        {
            if(imageOutEvent.getImage().getImageOutputColor() == Image.OutputColor.BITDEPTH_BITONAL) {
                imageOutEvent.getImage().setImageMimeType(Image.ImageMimeType.MIMETYPE_TIFF);
            } else {
                imageOutEvent.getImage().setImageMimeType(Image.ImageMimeType.MIMETYPE_JPEG);
                imageOutEvent.getImage().setImageJpegQuality(80);
            }

            //Force 96 DPI if less, or not set (IMPORTANT!)
            if(imageOutEvent.getImage().getImageDPI()<96)
                imageOutEvent.getImage().setImageDPI(96);

            try {
                imageOutEvent.getImage().imageWriteToFileBuffer();
            } catch (KmcException e) {
                e.printStackTrace();
                Global.ShowDialog(this, Global.ALERTS.ALERT_KMC_SDK_ERROR, e.getMessage(), this);
                return;
            }

            ByteBuffer imageData = imageOutEvent.getImage().getImageFileBuffer();

            try {
                imageOutEvent.getImage().imageClearFileBuffer();
            } catch (KmcException e) {
                e.printStackTrace();
                Global.ShowDialog(this, Global.ALERTS.ALERT_KMC_SDK_ERROR, e.getMessage(), this);
                return;
            }

            Global.theCapturedAndProcessedImageFiles.add(imageData);
            Global.processStatus = Global.PROCESSSTATUS.PS_IMAGE_PROCESSING_FINISHED;

            Global.hideProgressDialog();
            showImage(imageOutEvent.getImage());

            if(Global.IMMEDIATELY_CALL_DATA_EXTRACTION) {
                if(Global.DO_EXTRACTION){
                    //call extraction
                } else {
                    // finish
                }
            } else {
                showOkRetryButtons(true);
            }
        }
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch(Global.currentAlert)
        {
            case ALERT_BAD_IMAGE_QUALITY:
                imgReviewEditCntrl.clearImage();
                imgReviewEditCntrl.setVisibility(View.INVISIBLE);
                startCaptureProcess();
                break;

            case ALERT_CONTINUE_CAPTURE:
                switch(which)
                {
                    case DialogInterface.BUTTON_POSITIVE:
                        Toast.makeText(getApplicationContext(), "continue", Toast.LENGTH_SHORT).show();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        documentCaptureExperience.stopCapture();
                        break;
                }
                break;

            case ALERT_KMC_SDK_ERROR:
                cleanObjects();
                startCaptureProcess();
                break;
        }

        Global.currentAlert = Global.ALERTS.ALERT_NONE;
    }
}
