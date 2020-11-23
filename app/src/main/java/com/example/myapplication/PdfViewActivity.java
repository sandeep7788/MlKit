package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.example.myapplication.databinding.ActivityPdfviewBinding;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class PdfViewActivity extends AppCompatActivity {
    ActivityPdfviewBinding binding;
    ProgressDialog mProgressDialog;
    public Context context;
    public String samplePDF = "http://www.africau.edu/images/default/sample.pdf";
    public static final int PERMISSIONS_MULTIPLE_REQUEST_CERTIFICATE = 123;
    @Override  protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pdfview);
        context = PdfViewActivity.this;
        mProgressDialog = new ProgressDialog(context);
        checkAndroidVersionCertificate();
    }

    private void downloadFile() {
        mProgressDialog.show();
        mProgressDialog.setMessage("downloading");
        mProgressDialog.setMax(100);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        DownloadFileTask task = new DownloadFileTask( PdfViewActivity.this,  samplePDF,  "/download/pdf_file.pdf");
        task.startTask();
    }
    public class DownloadFileTask {
        public static final String TAG = "DownloadFileTask";
        private PdfViewActivity context;
        private GetTask contentTask;
        private String url;
        private String fileName;
        public DownloadFileTask(PdfViewActivity context, String url, String fileName) {
            this.context = context;
            this.url = url;
            this.fileName = fileName;
        }
        public void startTask() {
            doRequest();
        }
        private void doRequest() {
            contentTask = new GetTask();
            contentTask.execute();
        }
        private class GetTask extends AsyncTask< String, Integer, String > {


            @Override
            protected String doInBackground(String... strings) {
                int count;
                try {
                    URL _url = new URL(url);
                    URLConnection conection = _url.openConnection();
                    conection.connect();
                    String extension = url.substring(url.lastIndexOf('.') + 1).trim();
                    InputStream input = new BufferedInputStream(_url.openStream(),  8192);
                    OutputStream output = new FileOutputStream( Environment.getExternalStorageDirectory() + fileName);
                    byte data[] = new byte[1024];
                    while ((count = input.read(data))  != -1) {
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();
                } catch (Exception e) {
                    Log.e("Error: ", e.getMessage());
                }
                return null;
            }

            protected void onPostExecute(String data) {
                context.onFileDownloaded();
            }
        }
    }
    public void onFileDownloaded() {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        File file = new File(Environment.getExternalStorageDirectory() .getAbsolutePath()  + "/download/pdf_file.pdf");
        if (file.exists()) {
            binding.pdfView.fromFile(file)  //.pages(0, 2, 1, 3, 3, 3) // all pages are displayed by default
                    .enableSwipe(true) .swipeHorizontal(true) .enableDoubletap(true) .defaultPage(0) .enableAnnotationRendering(true) .password(null) .scrollHandle(null) .onLoad(new OnLoadCompleteListener() {
                @Override  public void loadComplete(int nbPages) {
                    binding.pdfView.setMinZoom(1f);
                    binding.pdfView.setMidZoom(5f);
                    binding.pdfView.setMaxZoom(10f);
                    binding.pdfView.zoomTo(2f);
                    binding.pdfView.scrollTo(100, 0);
                    binding.pdfView.moveTo(0f, 0f);
                }
            }) .load();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)  private void checkPermission_Certificate() {
        if (ContextCompat.checkSelfPermission(context,  Manifest.permission.READ_EXTERNAL_STORAGE) + ContextCompat .checkSelfPermission(context,  Manifest.permission.WRITE_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale ((Activity) context, Manifest.permission.READ_EXTERNAL_STORAGE) ||  ActivityCompat.shouldShowRequestPermissionRationale ((Activity) context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                requestPermissions( new String[] {
                        Manifest.permission .READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
                },  PERMISSIONS_MULTIPLE_REQUEST_CERTIFICATE);
            } else {
                requestPermissions( new String[] {
                        Manifest.permission .READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
                },  PERMISSIONS_MULTIPLE_REQUEST_CERTIFICATE);
            }
        } else {  // write your logic code if permission already granted
            if (!samplePDF.equalsIgnoreCase("")) {
                downloadFile();
            }
        }
    }
    private void checkAndroidVersionCertificate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission_Certificate();
        } else {
            if (!samplePDF.equalsIgnoreCase("")) {
                downloadFile();
            }
        }
    }
    @Override  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST_CERTIFICATE:
                if (grantResults.length > 0) {
                    boolean writePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean readExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writePermission && readExternalFile) {
                        if (!samplePDF.equalsIgnoreCase("")) {
                            downloadFile();
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Build.VERSION.SDK_INT >= 23 &&  !shouldShowRequestPermissionRationale(permissions[0])) {
                            Intent intent = new Intent();
                            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        } else {
                            requestPermissions( new String[] {
                                    Manifest.permission .READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
                            },  PERMISSIONS_MULTIPLE_REQUEST_CERTIFICATE);
                        }
                    }
                }
                break;
        }
    }
}