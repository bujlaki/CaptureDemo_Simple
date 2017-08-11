package com.kofax.kofaxtsemeadev.capturedemo_simple;

/**
 * Created by balazs.ujlaki on 11/08/2017.
 */

public class Settings {

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
}
