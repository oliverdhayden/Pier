package com.groupl.project.pier;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;


import com.amazonaws.http.HttpClient;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.SAXParserFactory;

import au.com.bytecode.opencsv.CSVReader;

public class settingPage extends AppCompatActivity {

    String TAG = "settingPage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_page);

        CompoundButton.OnCheckedChangeListener multiSwitch = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                switch (compoundButton.getId()) {
                    case R.id.option1:
                        setPreference(b, "option1");
                        break;
                    case R.id.option2:
                        setPreference(b, "option2");
                        break;
                    case R.id.option3:
                        setPreference(b, "option3");
                        break;
                    case R.id.option4:
                        setPreference(b, "option4");
                        break;
                }
            }
        };
        ((Switch) findViewById(R.id.option1)).setOnCheckedChangeListener(multiSwitch);
        ((Switch) findViewById(R.id.option2)).setOnCheckedChangeListener(multiSwitch);
        ((Switch) findViewById(R.id.option3)).setOnCheckedChangeListener(multiSwitch);
        ((Switch) findViewById(R.id.option4)).setOnCheckedChangeListener(multiSwitch);

        // Active selected Switch
        ((Switch) findViewById(R.id.option1)).setChecked(getPreference(this, "option1"));
        ((Switch) findViewById(R.id.option2)).setChecked(getPreference(this, "option2"));
        ((Switch) findViewById(R.id.option3)).setChecked(getPreference(this, "option3"));
        ((Switch) findViewById(R.id.option4)).setChecked(getPreference(this, "option4"));

    }

    private void setPreference(boolean b, String option) {
        SharedPreferences prefs = this.getSharedPreferences("Preference", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("Option " + option, b);
        editor.apply();
    }

    static public boolean getPreference(Context context, String option) {
        SharedPreferences prefs = context.getSharedPreferences("Preference", MODE_PRIVATE);
        return prefs.getBoolean("Option " + option, true);
    }

    public void reset(View view) {
        WelcomeSlider.setPreference(true, "firsttime", this);
        Toast.makeText(settingPage.this, "First Time Resetted", Toast.LENGTH_SHORT).show();
    }

    public void testFileAccess(View view) throws Exception {
        File csv = new File(Environment.getExternalStorageDirectory(), "PierData/infoFile.csv");
        Scanner scanner = new Scanner(csv);
        ArrayList<String> list = new ArrayList<String>();
        while (scanner.hasNext()) {
            //if(scanner.hasNext()) {
            Log.i(TAG, "testFileAccess: " + scanner.next());
            list.add(scanner.next());
            //}
        }
        //Toast.makeText(settingPage.this, "Finished unpacking csv", Toast.LENGTH_SHORT).show();
        Toast.makeText(settingPage.this, list.get(0), Toast.LENGTH_SHORT).show();
        //UserStatement user = new UserStatement();
        //preference.setPreferenceObject(this,"userStatement",user);
    }

    public void testGetSharedPrefObj(View view) throws Exception {
        //UserStatement user = preference.getPreferenceObject(this,"userStatement");
        //Log.i(TAG, "testGetSharedPrefObj: " +user.name);
    }





}
