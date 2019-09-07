package com.example.barath.syndvoicer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity  {

    private Button mrecord;
    private TextView mTextView;
    private TextView numberphone;

    private MediaRecorder mRecorder;

    private String mFileName = null;

    private static final String LOG_TAG = "Record_log";


    private StorageReference mStorage;
    private ProgressDialog mProgress;

    private FirebaseAuth mAuth;
    private DatabaseReference ref;


    private static double lat =0.0;
    private static double lon = 0.0;


    String save;
    String millisInString;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        ref = FirebaseDatabase.getInstance().getReference();



        mTextView = (TextView)findViewById(R.id.recordLabel);
        mrecord = (Button)findViewById(R.id.recordBtn);
        numberphone = (TextView)findViewById(R.id.number);

        mProgress =  new ProgressDialog(this);

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/"+millisInString;

        mStorage = FirebaseStorage.getInstance().getReference();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         millisInString  = dateFormat.format(new Date());

        CheckUserPermsions();

        TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        @SuppressLint("MissingPermission") String number = tm.getLine1Number();
        System.out.println(number);
        numberphone.setText(number);

        mrecord.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                ref.child(save).child("Latitude").setValue(lat);
                ref.child(save).child("Longitude").setValue(lon);
                ref.child(save).child("Contact").setValue(save);
                ref.child(save).child("Status").setValue("Pending");

                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    startRecording();
                    mTextView.setText("Recording Started..");

                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    stopRecording();
                    mTextView.setText("Recording Stopped..");
                }

                return false;
            }
        });

    }



    void CheckUserPermsions(){
        if ( Build.VERSION.SDK_INT >= 23){
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED  ){
                requestPermissions(new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_ASK_PERMISSIONS);
                return ;
            }
        }

        runlisner();
    }

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runlisner();
                } else {

                    Toast.makeText( this,"cannot access " , Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    void runlisner(){
        locationlisner myloc=new locationlisner();
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,3, 1000, myloc);

        mythread myth = new mythread();
        myth.start();

    }

    class mythread extends  Thread{
        public void  run(){


            while(true){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if(locationlisner.location != null) {
                            LatLng present = new LatLng(locationlisner.location.getLatitude(), locationlisner.location.getLongitude());
                            lat = locationlisner.location.getLatitude();
                            lon = locationlisner.location.getLongitude();

                        }
                    }
                });


                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private void startRecording() {
         mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        uploadAudio();

    }

    private void uploadAudio(){

        mProgress.setMessage("Uploading Audio");
        mProgress.show();

      save = PhoneNumber.no;

        StorageReference filepath = mStorage.child(save).child(millisInString);

        Uri uri = Uri.fromFile(new File(mFileName));

        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                mProgress.dismiss();

                mTextView.setText("Uploading Finished");

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null)
        {
            Intent auth = new Intent(MainActivity.this,PhoneNumber.class);
            startActivity(auth);
            finish();
        }


    }


}
