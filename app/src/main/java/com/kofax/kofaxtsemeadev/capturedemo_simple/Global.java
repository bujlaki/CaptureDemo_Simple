package com.kofax.kofaxtsemeadev.capturedemo_simple;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.pdf.PdfDocument;
import android.os.Build;

import com.kofax.kmc.ken.engines.ImageProcessor;
import com.kofax.kmc.ken.engines.data.Image;

import java.util.List;

/**
 * Created by balazs.ujlaki on 11/08/2017.
 */

public class Global {

    //********************
    // The license string
    //********************
    public static final String PROCESS_PAGE_SDK_LICENSE = "4[RqE;k0HLP=!0k%,UpPz52ld&^'08`[tFPr[6fdvmhP$`8fIr?!#krLzZAbfOn(IF!,0W7cKhlN!lkt0!zGVgUh9BL!bNNUcvjp";

    //************************
    // The Extraction service
    //************************
    // Enable/Disable Real-time Extraction
    public static final boolean DO_EXTRACTION = false;
    // Extraction Server type "RTTI" or "KTA"
    public static final String EXTRACTION_SERVER_TYPE = "RTTI";

    // The RTTI Extraction Server URL
    public static final String RTTI_URL = "https://rttiserver/mobilesdk/api/RTTIProject";
    // If neded, specify the RTTI Class
    public static final String RTTI_CLASS = "";

    // The KTA Extraction Server URL
    public static final String KTA_URL = "http://ktaserver/TotalAgility/Services/SDK";
    // The KTA Extraction Process
    public static final String KTA_PROCESS = "KofaxMobileIDCaptureSync";
    // The KTA User ID & Password
    public static final String KTA_USER = "user";
    public static final String KTA_PASSWORD = "password";


    //*********************
    // Additional settings
    //*********************
    // Do Quick Analysis of captured page. Recommended.
    public static final boolean DO_QUICK_ANALYSIS = true;
    // 1 - n ... fixed number of pages; 0 .. user is asked if he wants to capture another page
    public static final int NUMBER_OF_PAGES_TO_CAPTURE = 0;
    // If we need location services
    public static final boolean USE_LOCATION_SERVICES = true;
    // Automatic torch
    public static final boolean USE_AUTO_TORCH = true;
    // Immediately call EVRS if quick analysis is OK?
    public static final boolean IMMEDIATELY_CALL_EVRS = false;
    public static final boolean IMMEDIATELY_CALL_DATA_EXTRACTION = false;


    //******
    // EVRS
    //******
    public static boolean evrsCropToTargetFrame = false;
    public static boolean evrsAutoCrop = true;
    public static boolean evrsDocumentDetectorBasedCrop = false;
    public static boolean evrsAutoRotate = false;
    public static int evrsDeskewMethod = 0; //0=no, 1=content, 2=layout
    public static int evrsColorMode = 0;    //0=BW, 1=Gray, 2=Color, 3=ColorDetect
    public static int evrsScaleDPI = 300;
    public static boolean evrsBackgrounSmoothing = false;
    public static int evrsSharpenImage = 0; //0=no, 1=little, 2=more, 3=most
    public static int evrsDespeckle = 0;    //0=no, 1-50=remove speckle up to that size
    public static boolean evrsSpecifyDocumentLongSide = false;
    public static float evrsLongSideLength = 11.69f;    //A4 size: 29.7mm/2.54mm ~ 11.69mm
    public static boolean evrsSpecifyDocumentShortSide = false;
    public static float evrsShortSideLength = 8.27f;    //A4 size: 21.0mm/2.54mm ~ 8.27mm
    public static boolean evrsIlluminationCorrection = false;
    public static boolean evrsEdgeCleanup = false;      // _DoCropCorrection_ and _DoSkewCorrection_ must be used in order to have any effect


    //*************
    // Permissions
    //*************
    public static final String[] PERMISSIONS_CAPTURE = {
            Manifest.permission.CAMERA
    };
    public static final String[] PERMISSIONS_GALLERY = getGalleryPermissions();
    public static final int REQ_CODE_PICK_IMAGE = 1;
    public static final int REQ_CODE_PERMISSIONS_CAPTURE = 2;
    public static final int REQ_CODE_PERMISSIONS_GALLERY = 3;

    private static String[] getGalleryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE };
        } else {
            return new String[0];
        }
    }


    //*******
    // ENUMS
    //*******
    public enum PROCESSSTATUS {
        PS_START,
        PS_QUICK_ANALYSIS,
        PS_IMAGE_PROCESSING,
        PS_IMAGE_PROCESSING_FINISHED,
        PS_DATA_EXTRACTION,
        PS_FINISHED
    }

    public enum ALERTS {
        ALERT_NONE,
        ALERT_BAD_IMAGE_QUALITY,
        ALERT_EXTRACTION_ERROR,
        ALERT_CONTINUE_CAPTURE,
        ALERT_EXTRACTION_RESULTS,
        ALERT_KMC_SDK_ERROR
    }

    public enum IMAGEFILETYPE {
        FT_TIFF,
        FT_JPEG,
        FT_PNG,
        FT_PDF,
        FT_UNKNOWN
    }


    //****************
    // GLOBAL OBJECTS
    //****************
    // Images and image processor
    public static Image currentCapturedImage;
    public static Image currentProcessedImage;

    public static List<Object> theCapturedAndProcessedImageFiles;

    // RTTI HTTP Request
    public static  String connectionData;
    public static  String connection;
    public static  int connectionStatusCode;

    // Session ID for KTA Extraction
    public static  String ktaSessionID;

    // Array for extracted fields
    public static  Object[] extractionResult;

    // The current process step
    public static  Global.PROCESSSTATUS processStatus;

    // Current alert dialog
    public static  Global.ALERTS currentAlert = Global.ALERTS.ALERT_NONE;
    public static  AlertDialog alertDialog;


    //******************
    // HELPER FUNCTIONS
    //******************
    //Not implemented, as it is not used at all in the iPhone app.
    public IMAGEFILETYPE fileTypeByParsingTheHeaderBytes(byte[] img) {
        return IMAGEFILETYPE.FT_UNKNOWN;
    }

    //NOTE PdfDocument is only working with API 19 or above.
    //Need to make sure this function is not called when using lower API.
    @TargetApi(19)
    public PdfDocument createPDFforImages()
    {
        PdfDocument document = new PdfDocument();

        //...

        return document;
    }

    public static void cleanObjects()
    {
        if(Global.theCapturedAndProcessedImageFiles!=null) {
            Global.theCapturedAndProcessedImageFiles.clear();
            Global.theCapturedAndProcessedImageFiles = null;
        }

        Global.currentCapturedImage = null;
        Global.currentProcessedImage = null;
    }

    // Progress indicator
    private static ProgressDialog progressDialog;

    public static void hideProgressDialog()
    {
        if(progressDialog!=null)
        {
            progressDialog.cancel();
            progressDialog=null;
        }
    }

    public static void showProgressDialog(Context context, String message)
    {
        if(progressDialog==null)
        {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(message);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        progressDialog.show();
    }

    public static void ShowDialog(Context context, Global.ALERTS type, String additionalMsg, DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder;
        Global.currentAlert = type;
        switch(type)
        {
            case ALERT_BAD_IMAGE_QUALITY:
                builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.qaBadTitle);
                builder.setMessage(
                        context.getResources().getString(R.string.qaBadText1) +
                                "\n\n" +
                                additionalMsg +
                                "\n\n" +
                                context.getResources().getString(R.string.qaBadText2));
                builder.setNegativeButton(R.string.textBtnOk, listener);
                alertDialog = builder.create();
                alertDialog.show();
                break;

            case ALERT_CONTINUE_CAPTURE:
                builder = new AlertDialog.Builder(context);
                builder.setMessage(context.getResources().getString(R.string.textAnotherPage));
                builder.setPositiveButton(R.string.textAnotherPageBtnYes, listener);
                builder.setNegativeButton(R.string.textAnotherPageBtnNo, listener);
                alertDialog = builder.create();
                alertDialog.show();
                break;
        }
    }
}
