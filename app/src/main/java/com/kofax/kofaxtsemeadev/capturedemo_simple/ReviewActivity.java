package com.kofax.kofaxtsemeadev.capturedemo_simple;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.kofax.kmc.ken.engines.ImageProcessor;
import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.ken.engines.data.ImagePerfectionProfile;
import com.kofax.kmc.ken.engines.data.QuickAnalysisFeedback;
import com.kofax.kmc.ken.engines.data.QuickAnalysisSettings;
import com.kofax.kmc.kui.uicontrols.ImgReviewEditCntrl;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;

import java.nio.ByteBuffer;

public class ReviewActivity extends Activity
        implements
        ImageProcessor.AnalysisCompleteListener,
        ImageProcessor.ImageOutListener,
        DialogInterface.OnClickListener
{


    private ImageProcessor imageProcessor;
    private ImgReviewEditCntrl imgReviewEditCntrl;
    private Button btnOk;
    private Button btnRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        btnOk = (Button)findViewById(R.id.btnOK);
        btnRetry = (Button)findViewById(R.id.btnRetry);
        imgReviewEditCntrl = (ImgReviewEditCntrl)findViewById(R.id.imgReviewControl);

        btnOk.setOnClickListener(okButtonPressed);
        btnRetry.setOnClickListener(retryButtonPressed);
    }

    @Override
    protected void onStart() {
        super.onStart();

        showImage(Global.currentCapturedImage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        doQuickAnalysis(Global.currentCapturedImage);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void showImage(Image img)
    {
        try {
            imgReviewEditCntrl.setImage(img);
        } catch (KmcException e) {
            e.printStackTrace();
        }
    }

    private void createImageProcessor()
    {
        if(imageProcessor==null){
            imageProcessor = new ImageProcessor();
            imageProcessor.addAnalysisCompleteEventListener(this);
            imageProcessor.addImageOutEventListener(this);
        }
    }

    View.OnClickListener retryButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showOkRetryButtons(false);
            imgReviewEditCntrl.clearImage();

            if(Global.processStatus == Global.PROCESSSTATUS.PS_IMAGE_PROCESSING_FINISHED)
                Global.theCapturedAndProcessedImageFiles.remove(Global.theCapturedAndProcessedImageFiles.size() - 1);

            finish();
        }
    };

    View.OnClickListener okButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showOkRetryButtons(false);

            if(Global.processStatus == Global.PROCESSSTATUS.PS_QUICK_ANALYSIS) {
                doImageProcessing(Global.currentCapturedImage);
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
                    doImageProcessing(Global.currentCapturedImage);
                else
                    showOkRetryButtons(true);
            }
        }
    }

    private void doImageProcessing(Image img)
    {
        Global.showProgressDialog(this, getResources().getString(R.string.statusImageProcessing));
        Global.processStatus = Global.PROCESSSTATUS.PS_IMAGE_PROCESSING;

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
                finish();
                break;

            case ALERT_KMC_SDK_ERROR:
                finish();
                break;
        }

        Global.currentAlert = Global.ALERTS.ALERT_NONE;
    }
}
