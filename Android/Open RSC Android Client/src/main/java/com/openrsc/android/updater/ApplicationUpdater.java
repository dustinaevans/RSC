package com.openrsc.android.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.widget.TextView;
import android.widget.Toast;

import com.openrsc.client.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import orsc.Config;

public class ApplicationUpdater extends Activity {

    private TextProgressBar progressBar;
    private TextView tv1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applicationupdater);

        progressBar = findViewById(R.id.progressBar1);
        progressBar.setTextSize(18);
        progressBar.setIndeterminate(false);
        tv1 = findViewById(R.id.textView1);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    new CheckVersionTask().execute().get();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ApplicationUpdater.this, "Unable to check for updates", Toast.LENGTH_LONG).show();
                }
            }
        }, 1000);

    }

    public int getVersion() {
        PackageManager manager = getApplicationContext().getPackageManager();
        PackageInfo info;
        try {
            info = manager.getPackageInfo(getApplicationContext().getPackageName(), 0);
            return Config.ANDROID_CLIENT_VERSION;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to check application version", Toast.LENGTH_LONG).show();
        }
        return -1;
    }

    public void showUpdateDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ApplicationUpdater.this);
        alertDialogBuilder.setTitle("New version available!");
        alertDialogBuilder
                .setMessage("There's a new client update available,"
                        + " you might not be able to play until you install it.")
                .setCancelable(false).setPositiveButton("Install", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                try {
                    if(Build.VERSION.SDK_INT>=24){
                        try{
                            Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                            m.invoke(null);
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    tv1.setText("Downloading update...");
                    new DownloadApplication().execute().get();

                    File downloadedFile = new File(getFilesDir() + File.separator + "openrsc.apk");
                    downloadedFile.setReadable(true, false);

                    Uri fileLoc = Uri.fromFile(downloadedFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setDataAndType(fileLoc, "application/vnd.android.package-archive");
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).setNegativeButton("Don't install", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent mainIntent = new Intent(ApplicationUpdater.this, CacheUpdater.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainIntent);
                finish();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    class CheckVersionTask extends AsyncTask<String, String, String> {

        private boolean shouldUpdate = false;

        @Override
        protected String doInBackground(String... aurl) {
            URL updatePage;
            try {
                updatePage = new URL(Config.ANDROID_DOWNLOAD_PATH + "android_version.txt");
                BufferedReader in = new BufferedReader(new InputStreamReader(updatePage.openStream()));
                String inputLine = in.readLine();
                in.close();
                if (Integer.parseInt(inputLine) != getVersion()) {
                    shouldUpdate = true;
                    publishProgress("Newer version available!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                publishProgress("Couldn't check for application updates");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values[0].toLowerCase().contains("couldn't")) {
                Toast.makeText(ApplicationUpdater.this, "Unable to check for updates", Toast.LENGTH_LONG).show();
            }
            tv1.setText(values[0]);
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            if (shouldUpdate) {
                showUpdateDialog();
            } else {
                Intent mainIntent = new Intent(ApplicationUpdater.this, CacheUpdater.class);
                startActivity(mainIntent);
                finish();
            }
            super.onPostExecute(result);
        }
    }

    class DownloadApplication extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... aurl) {

            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(Config.ANDROID_DOWNLOAD_PATH + "openrsc.apk").openConnection();
                connection.connect();

                int fileLength = connection.getContentLength();
                FileOutputStream fos = openFileOutput("openrsc.apk", Context.MODE_PRIVATE);
                try {
                    InputStream in = connection.getInputStream();
                    byte[] buffer = new byte[4096];
                    long total = 0;
                    int len = 0;
                    while ((len = in.read(buffer)) > 0) {
                        total += len;
                        fos.write(buffer, 0, len);
                        if (fileLength > 0)
                            publishProgress("Downloading update...", "" + (int) ((total * 100) / fileLength));
                    }
                    fos.flush();
                } finally {
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            progressBar.setText(values[0] + " - " + Integer.parseInt(values[1]) + "%");
            progressBar.setProgress(Integer.parseInt(values[1]));
            progressBar.postInvalidate();
            super.onProgressUpdate(values);
        }
    }

}
