package com.kofax.kofaxtsemeadev.capturedemo_simple;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.kofax.mobile.sdk.extract.server.IServerExtractor;
import com.kofax.mobile.sdk.extract.server.ServerBuilder;
import com.kofax.mobile.sdk.extract.server.ServerExtractionParameters;

public class SubmitRTTI extends Activity {

    String rttiURL;
    IServerExtractor serverExtractor;
    ServerExtractionParameters parameters;

    Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_rtti);

        rttiURL = Global.RTTI_URL;

        serverExtractor = ServerBuilder.build(
                this.getApplicationContext(),
                ServerBuilder.ServerType.RTTI);

        submitButton = (Button)findViewById(R.id.btnSubmit);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitImages();
            }
        });
    }

    private void submitImages()
    {
        parameters = new ServerExtractionParameters(
                rttiURL,
                Global.theCapturedAndProcessedImageFiles,
                null,
                null,
                null,
                null);

    }
}
