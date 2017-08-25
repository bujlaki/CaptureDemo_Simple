package com.kofax.kofaxtsemeadev.capturedemo_simple;

import android.Manifest;
import android.os.Build;

/**
 * Created by balazs.ujlaki on 11/08/2017.
 */

public class Global {

    //********************
    // The license string
    //********************
    public static final String PROCESS_PAGE_SDK_LICENSE = "";

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

    public enum IMAGEFILETYPE {
        FT_TIFF,
        FT_JPEG,
        FT_PNG,
        FT_PDF,
        FT_UNKNOWN
    }
}
