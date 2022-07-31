package com.rahul.awsupload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.rahul.customview.AwsView;

public class MainActivity extends AppCompatActivity {

    AwsView awsView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        awsView=findViewById(R.id.awsupload);
        awsView.setFileName("I am Testing");
        awsView.setFileSelectType(AwsView.CHOOSE_VIDEO);
        awsView.setFileUploadListener(new AwsView.FileuploadListener() {
            @Override
            public void onfileuploadSuccess(String url) {
                Log.e("filename",url);
                Toast.makeText(MainActivity.this,"File Uploaded Successfully",Toast.LENGTH_LONG).show();

            }

            @Override
            public void onfileuploadFailed() {
                Toast.makeText(MainActivity.this,"Something Went Wrong",Toast.LENGTH_LONG).show();
            }
        });

    }




    private boolean checkPermission(String[] permission)
    {
        for(int i=0;i<permission.length;i++) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, permission[i]) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==AwsView.FILE_PERMISSION&&checkPermission(permissions))
        {
            awsView.selectFile(AwsView.CHOOSE_VIDEO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK)
        {
            Uri selectedImageUri = data.getData();
            awsView.setFileUri(String.valueOf(selectedImageUri));

        }

    }

}