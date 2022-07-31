package com.rahul.customview;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.example.awss3.R;

import java.io.File;
import java.util.List;

public class AwsView extends ConstraintLayout implements View.OnClickListener {
    ProgressDialog progressDialog;
    Context context;
    public static final Integer CHOOSE_FILE=5;
    public static final Integer CHOOSE_VIDEO=55;
    public static  final Integer FILE_PERMISSION=111;
    private String AWS_KEY;
    private Integer SELECT_TYPE;
    private String AWS_SECRET;
    private String AWS_BUCKET_NAME;
    private String FILE_TYPE;
    private Integer FILE_SIZE;
    private String FILE_URI;
    private String FILE_NAME=String.valueOf(System.currentTimeMillis());
    private FileuploadListener listener;
    TypedArray typedArray;
    LayoutInflater layoutInflater;
    public AwsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context=context;
        init(attrs,context);
    }


    public AwsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context=context;
        init(attrs,context);
    }

    public AwsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context=context;
        init(attrs,context);
    }

    public  void  setFileSelectType(Integer code)
    {
        SELECT_TYPE=code;
    }
    public void init(AttributeSet attributeSet,Context context)
    {
        layoutInflater=LayoutInflater.from(context);
        typedArray=context.getTheme().obtainStyledAttributes(attributeSet,R.styleable.AwsView,0,0);
        AWS_KEY=typedArray.getString(R.styleable.AwsView_awsKey);
        AWS_SECRET=typedArray.getString(R.styleable.AwsView_awsSecret);
        AWS_BUCKET_NAME=typedArray.getString(R.styleable.AwsView_bucketname);
        FILE_TYPE=typedArray.getString(R.styleable.AwsView_uploadtype);
        FILE_SIZE=typedArray.getInteger(R.styleable.AwsView_maxsizeMb,40);

        ConstraintLayout layout=(ConstraintLayout) layoutInflater.inflate(R.layout.custom_layout,this);
        View button=layout.findViewById(R.id.textupload);
        button.setOnClickListener(this);
       // button.setClickable(true);
            }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);   // this super call is important !!!
        // YOUR LOGIC HERE
        return true;
    }
    public void setFileName(String name)
    {
        this.FILE_NAME=name;
    }
    private boolean checkPermission(String[] permission)
    {
        for(int i=0;i<permission.length;i++) {
            if (ContextCompat.checkSelfPermission(context, permission[i]) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    public void selectFile(Integer code) {
        if(code==CHOOSE_FILE) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.setType(FILE_TYPE);
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.startActivityForResult(intent, CHOOSE_FILE);
        }
        else if(code==CHOOSE_VIDEO)
        {
            AppCompatActivity activity=(AppCompatActivity)context;
            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            if (takeVideoIntent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivityForResult(takeVideoIntent, CHOOSE_VIDEO);
            }
        }
    }
    public void uploadFile(String uri)
    {
        File file=new File(getPath(context,uri));
      //  Log.e("filesize",(file.length()/(1024*1024))+"");
        if((file.length()/(1024*1024))>FILE_SIZE)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context,"File Size Limit Exceeded",Toast.LENGTH_LONG).show();
                }
            });
            if(progressDialog.isShowing())
                progressDialog.dismiss();
            return;
        }
        AWSCredentials credentials = new BasicAWSCredentials(AWS_KEY, AWS_SECRET);
        AmazonS3 s3 = new AmazonS3Client(credentials);
        java.security.Security.setProperty("networkaddress.cache.ttl" , "60");
        s3.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1));
        s3.setEndpoint("https://s3.amazonaws.com/");
        List<Bucket> buckets=s3.listBuckets();
        TransferUtility transferUtility = new TransferUtility(s3, context);
        TransferObserver observer = transferUtility.upload(AWS_BUCKET_NAME,FILE_NAME,file);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                // do something
                if(state==TransferState.COMPLETED)
                {

                    if(progressDialog!=null&&progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                    String url="https://"+AWS_BUCKET_NAME+".s3.amazonaws.com/"+FILE_NAME;
                    listener.onfileuploadSuccess(url);
                }
                else if(state==TransferState.FAILED)
                {
                    if(progressDialog!=null&&progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                    listener.onfileuploadFailed();
                }

            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) (bytesCurrent / bytesTotal * 100);

            }

            @Override
            public void onError(int id, Exception ex) {
                // do something
                Log.e("Error  ",""+ex );
            }

        });
    }

    @Override
    public void onClick(View view) {
        if(checkPermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA}))
        {
            selectFile(SELECT_TYPE);

        }
        else
            askPermission(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE});
    }

    public static String getPath(Context context, String uri ) {
        String result = null;
        Uri fileuri=Uri.parse(uri);
        String[] proj = { MediaStore.MediaColumns.DATA};
        Cursor cursor = context.getContentResolver( ).query( fileuri, proj, null, null, null );
        if(cursor != null){
            if ( cursor.moveToFirst( ) ) {
                int column_index = cursor.getColumnIndexOrThrow( proj[0] );
                result = cursor.getString( column_index );
            }
            cursor.close( );
        }
        return result;
    }
    public class BackgroundTask extends AsyncTask<String,Void,Void>
    {
        public BackgroundTask() {
            super();
            progressDialog=new ProgressDialog(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Uploading...");
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);

        }

        @Override
        protected Void doInBackground(String... strings) {
            uploadFile(strings[0]);
            return null;
        }

    }



    public  void setFileUri(String uri)
    {
        FILE_URI=uri;
        new BackgroundTask().execute(uri);
    }
    private void askPermission(String [] permission)
    {
        AppCompatActivity activity=(AppCompatActivity) context;
        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.M)
            activity.requestPermissions(permission,FILE_PERMISSION);
    }

    public void setFileUploadListener(FileuploadListener listener)
    {
        this.listener=listener;
    }
    public interface FileuploadListener
    {
        public void onfileuploadSuccess(String url);
        public void onfileuploadFailed();
    }

}
