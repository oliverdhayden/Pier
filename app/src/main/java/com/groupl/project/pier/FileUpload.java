
package com.groupl.project.pier;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;

import android.support.v4.content.ContextCompat;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.services.s3.AmazonS3Client;
// Following imports establish connection to AWS Mobile Services
import com.amazonaws.mobile.client.AWSMobileClient;
// Following imports handle uploading a file to S3 Bucket


import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;


import java.io.File;
import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import au.com.bytecode.opencsv.CSVReader;

public class FileUpload extends AppCompatActivity {

    public static boolean uploadButtonWasPressed = false;
    String TAG = "FileUpload";
    String PathHolder = "newTest.jpg";
    String identityID = "this failed";
    String filePath;
    ProgressDialog dialog;

    int percentDone = 0;
    ProgressBar progressBar;
    TextView progressText;
    Button buttonUpload;

    List<String[]> list = new ArrayList<String[]>();
    int groceries = 0, rent = 0, transport = 0, bills = 0, untagged = 0, eatingOut = 0, general = 0;

    int month = 0;
    int year = 0;


    Intent intent = null;
    AmazonS3 s3;
    Uri PathUri;
    File file;

    SQLiteDatabase pierDatabase;

    public void setPreference(boolean b, String option) {
        SharedPreferences prefs = this.getSharedPreferences("Preference", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("Option " + option, b);
        editor.apply();
    }

    static public boolean getPreference(Context context, String option) {
        SharedPreferences prefs = context.getSharedPreferences("Preference", MODE_PRIVATE);
        return prefs.getBoolean("Option " + option, false);
    }

    public void requestPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            System.out.println("hello");
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_upload);
        try {
            pierDatabase = FileUpload.this.openOrCreateDatabase("Statement", MODE_PRIVATE, null);
            pierDatabase.execSQL("DELETE FROM statement");
            Log.i("Database", "Table Cleared");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ------- ASK PERMISSION TO EDIT FILES -------------------
        ActivityCompat.requestPermissions(FileUpload.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);

        buttonUpload = findViewById(R.id.btn_upload);
        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressBarText);
        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadButtonWasPressed = true;
                uploadData(filePath);
            }
        });
        requestPermissions();
        credentialsProvider();
        intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, 7);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        switch (requestCode) {
            case 7:
                if (resultCode == RESULT_OK) {
                    try {

                        PathUri = data.getData();
                        filePath = FilePathUtil.getPath(getApplicationContext(), PathUri);
                        Log.i(TAG, filePath + "----------------------------------------------------------------");

                        File file = new File(filePath);
                        //check the extention is correct
                        String extension = FileExtentionUtil.getExtensionOfFile(file);
                        String csv = "csv";
                        Log.i(TAG, "onActivityResult:filepath = " + filePath);

                        //if incorrect extension restart the file manager
                        if (!extension.equals(csv)) {
                            Toast.makeText(this, "The chosen file has the extension " + extension + " which is not a csv file, please choose another file.", Toast.LENGTH_LONG).show();
                            intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("*/*");
                            startActivityForResult(intent, 7);
                        }
                        // ------------------------ SHOW SELECTED FILE NAME -----------------------------------------------
                        TextView filename = (TextView) findViewById(R.id.filename);
                        filename.setText(file.getName());
                        percentDone = 0;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }


    public void credentialsProvider() {
        AWSConfiguration a = new AWSConfiguration(this);
        CognitoUserPool userPool = new CognitoUserPool(this, a);
        CognitoUser user = userPool.getCurrentUser();

        // Implement callback handler for getting details
        GetDetailsHandler getDetailsHandler = new GetDetailsHandler() {
            @Override
            public void onSuccess(CognitoUserDetails cognitoUserDetails) {
                // The user detail are in cognitoUserDetails
                Map userAtts = new HashMap();
                userAtts = cognitoUserDetails.getAttributes().getAttributes();
                //identityID = userAtts.get("sub").toString();
                //System.out.println(identityID);
            }

            @Override
            public void onFailure(Exception exception) {
                System.out.println(exception);
            }
        };

// Fetch the user details
        user.getDetailsInBackground(getDetailsHandler);

    }

    public void uploadData(String path) {
        // Initialize AWSMobileClient if not initialized upon the app startup.
        // AWSMobileClient.getInstance().initialize(this).execute();
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();
        File file = new File(path);
        String fileName = file.getName();
        if (file == null) {
            Toast.makeText(this, "Could not find the filepath of  the selected file", Toast.LENGTH_LONG).show();
            // to make sure that file is not emapty or null
            return;
        }

        // ----------------------- BLOCK UPLOAD IF NOT AN CSV FILE ----------------------------------------
        //check the extention is correct
        String extension = FileExtentionUtil.getExtensionOfFile(file);
        String csv = "csv";
        //if incorrect extension restart the file manager
        if (!extension.equals(csv)) {
            Toast.makeText(this, "Please select an CSV type file!!", Toast.LENGTH_LONG).show();
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, 7);
            return;
        }


        //old bucket name = "pierandroid-userfiles-mobilehub-318679301/public/"+userName
        //new bucket "pierandroid-userfiles-mobilehub-318679301/public/incoming"
        //old key "newest_statement.csv"
        //new key userName+".csv"
        String userName = preference.getPreference(this, "username");
        TransferObserver uploadObserver =
                transferUtility.upload(
                        "pierandroid-userfiles-mobilehub-318679301/public/incoming",
                        userName + ".csv",
                        file);
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    Toast.makeText(FileUpload.this, "Upload Completed", Toast.LENGTH_LONG).show();

                    //***************** CHECK FILE ************

                    Log.i(TAG, "onStateChanged: start of delay --------");
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Do something after 5s = 5000ms
                            checkFile();
                            Log.i(TAG, "run: delayed function ran ---------");
                        }
                    }, 5000);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                percentDone = (int) percentDonef;

                progressBar.setProgress(percentDone);
                progressText.setText(percentDone + "/" + progressBar.getMax());

                Log.i(TAG, "onProgressChanged:" + "ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
                Log.d(TAG, "   ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {

                System.out.println(id + " " + ex);
                Toast.makeText(FileUpload.this, "error uploading", Toast.LENGTH_LONG).show();
            }
        });

        // If your upload does not trigger the onStateChanged method inside your
        // TransferListener, you can directly check the transfer state as shown here.
        if (TransferState.COMPLETED == uploadObserver.getState()) {
            System.out.println("COMPLETE");
            Toast.makeText(this, "upload complete", Toast.LENGTH_LONG).show();
        }
    }

    //this method analize the data from the csv file and returns a string with the value of every spending type separated by a comma
    public static String stringOfTotalSpendings(String data) {
        int countComma = 0;
        int step = 11;
        String paid = "";
        for (int i = 0; i < data.length(); i++) {
            if (data.charAt(i) == ',') {
                countComma++;
                if (countComma == step) {
                    while (data.charAt(i + 1) != ',') {
                        paid = paid + data.charAt(i + 1);
                        i++;
                    }
                    step += 6;
                    if (step + 100 < data.length()) {
                        paid = paid + ',';
                    }
                }
            }
        }
        return paid;
    }

    // ---------------------- CHECK FILE ---------------------------
    void checkFile() {

        final String userName = preference.getPreference(this, "username");

        //download last 6 months csv
        new Thread(new Runnable() {
            @Override
            public void run() {
                String folderName = "PierData";
                // CREATE FOLDER TO STORE THE CSV
                File dir = new File(Environment.getExternalStorageDirectory(), folderName);
                if (!dir.exists()) {
                    dir.mkdirs();
                    Log.d("Directory", "created");
                } else {
                    Log.d("Folder ->", "not created");
                }
                // FILE TO STORE THE CSV INFO
                File fileDown = new File(dir, "infoFile.csv");
                fileDown.delete();
                final String fileAbsolutePath = fileDown.getAbsolutePath();

                AmazonS3 S3_CLIENT = new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider());
                S3_CLIENT.setRegion(Region.getRegion(Regions.EU_WEST_2));
                // CHECK IF FILE EXIST
                boolean check = S3_CLIENT.doesObjectExist("/pierandroid-userfiles-mobilehub-318679301/public/" + userName, "last_six_months.csv");
                Log.d("CHECK_IF_EXIST", " -> " + check);

                // IF EXIST DOWNLOAD
                if (check) {
                    TransferUtility transferUtility =
                            TransferUtility.builder()
                                    .context(getApplicationContext())
                                    .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                                    .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                                    .build();
                    TransferObserver downloadObserver = transferUtility.download("/pierandroid-userfiles-mobilehub-318679301/public/" + userName, "last_six_months.csv", fileDown);

                    Log.d("FilePath", fileDown.getAbsolutePath());

                    // Attach a listener to the observer to get notified of the
                    // updates in the state and the progress
                    downloadObserver.setTransferListener(new TransferListener() {

                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            if (TransferState.COMPLETED == state) {
                                Toast.makeText(FileUpload.this, "Download Completed", Toast.LENGTH_SHORT).show();
                                //parseCSV(fileDown.getAbsolutePath());
                                parseCSV(fileAbsolutePath);
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                            int percentDone = (int) percentDonef;

                            Log.d("Download ->", "   ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            Log.d("ErrorDownload", "error id:" + id + "error->" + ex);
                        }

                    });
                } else {
                }

            }
        }).start();
        Log.i(TAG, "checkFile: end of check file (delay)");
    }

    void makeToast(String toast) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }

    public void parseCSV(String url) {
        String next[] = {};

        try {
            //************ PARSE CVS TO ARRAYLIST *****************
            CSVReader reader = new CSVReader(new FileReader(url));// file to parse
            for (; ; ) {
                next = reader.readNext();
                if (next != null) {
                    list.add(next);
                } else {
                    break;
                }
            }
            try {
                for (int i = 0; i < list.size(); i++) {
                    //if (list.get(i)[1].equals("3") && list.get(i)[2].equals("2018")) {
                    String desc = list.get(i)[3];
                    if (desc.toLowerCase().equals("scott's restaurant")) {
                        desc = "Scotts Restaurant";
                    }
                    Cursor cursordata = pierDatabase.rawQuery("SELECT * FROM tag WHERE description ='" + desc + "';", null);
                    Log.i("Querry", "SELECT * FROM tag WHERE description ='" + desc + "';");
                    int count = cursordata.getCount();
                    Log.i("Count", desc + String.valueOf(count));
                    String category = list.get(i)[4];
                    if (count != 0) {
                        int categoryIndex = cursordata.getColumnIndex("category");
                        cursordata.moveToFirst();
                        category = cursordata.getString(categoryIndex);
                    }
                    // add data to the database
                    pierDatabase.execSQL("INSERT INTO statement (day,month,year,description,category,value,balance) VALUES ('" + list.get(i)[0] + "','" + list.get(i)[1] + "','" + list.get(i)[2] + "','" + desc + "','" + category + "','" + list.get(i)[5] + "','" + list.get(i)[6] + "')");
                    Log.i("Database", "Data inserted!");
                    // ******************* SAVE TO PREFERENCE ************
                    //add data to the database


                }

                // get last date of data
                try {
                    Cursor getdate = pierDatabase.rawQuery("SELECT * FROM statement;",null);
                    getdate.moveToFirst();
                    Log.i("Date count", String.valueOf(getdate.getCount()));
                    int monthIndex = getdate.getColumnIndex("month");
                    int yearIndex = getdate.getColumnIndex("year");
                    month = getdate.getInt(monthIndex);
                    year = getdate.getInt(yearIndex);
                }
                catch (Exception e){
                    e.printStackTrace();
                }

                // add data to preference
                Cursor getmonthdata = pierDatabase.rawQuery("SELECT * FROM statement WHERE year ='" + year + "' and month='" + month + "';", null);
                Log.i("Cursor", "SELECT * FROM statement WHERE year ='" + year + "' and month='" + month + "';");
                Log.i("CursorSelected", String.valueOf(getmonthdata.getCount()));
                int categoryIndex = getmonthdata.getColumnIndex("category");
                int valueIndex = getmonthdata.getColumnIndex("value");
                getmonthdata.moveToFirst();
                int cursor = getmonthdata.getCount();
                try {
                    while (cursor != 0 ) {
                        Log.i("Category", getmonthdata.getString(categoryIndex));
                        if (getmonthdata.getString(categoryIndex).toLowerCase().equals("groceries")) {
                            groceries += getmonthdata.getInt(valueIndex);
                            Log.i("G value", String.valueOf(groceries));
                        }
                        if (getmonthdata.getString(categoryIndex).toLowerCase().equals("general")) {
                            general += getmonthdata.getInt(valueIndex);
                        }
                        if (getmonthdata.getString(categoryIndex).toLowerCase().equals("eating out")) {
                            eatingOut += getmonthdata.getInt(valueIndex);
                        }
                        if (getmonthdata.getString(categoryIndex).toLowerCase().equals("transport")) {
                            transport += getmonthdata.getInt(valueIndex);
                        }
                        if (getmonthdata.getString(categoryIndex).toLowerCase().equals("rent")) {
                            rent += getmonthdata.getInt(valueIndex);
                        }
                        if (getmonthdata.getString(categoryIndex).toLowerCase().equals("bills")) {
                            bills += getmonthdata.getInt(valueIndex);
                        }
                        if (getmonthdata.getString(categoryIndex).toLowerCase().equals("")) {
                            untagged += getmonthdata.getInt(valueIndex);
                        }
                        cursor--;
                        getmonthdata.moveToNext();
                    }
                    preference.setPreference(this, "groceries", String.valueOf(groceries));
                    preference.setPreference(this, "general", String.valueOf(general));
                    preference.setPreference(this, "eatingOut", String.valueOf(eatingOut));
                    preference.setPreference(this, "transport", String.valueOf(transport));
                    preference.setPreference(this, "rent", String.valueOf(rent));
                    preference.setPreference(this, "bills", String.valueOf(bills));
                    preference.setPreference(this, "untagged", String.valueOf(untagged));
                    int monthTotal = groceries + general + eatingOut + transport + rent + bills + untagged;
                    preference.setPreference(this, "monthTotal", String.valueOf(monthTotal));

                    Log.i("GroceriesF", preference.getPreference(this,"groceries"));
                } catch (Exception e) {
                    e.printStackTrace();
                }


                //****************** RESTART APP ***********************
                preference.setPreference(FileUpload.this, "alreadyDownloaded", "true");
                Intent i = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);


            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "parseCSV: end of parse csv (delays)");
        Intent mainActivity = new Intent(FileUpload.this, MainActivity.class);
        startActivity(mainActivity);
    }
}

