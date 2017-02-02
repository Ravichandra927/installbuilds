package com.example.android.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class MainActivity extends AppCompatActivity {

    SmbFile[] rootDirList = null;
    ArrayAdapter<SmbFile> smbFileArrayAdapter;
    NtlmPasswordAuthentication mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            connectToPC();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void connectToPC() throws IOException {

//        String ip = "eahy-l-5152607";
        String ip = "EAHY-D-5154790";
        String url = "smb://" + ip;

        //authenticate the connection using domain, username, password. Used in create Smb object
        mAuth = new NtlmPasswordAuthentication("eamobile.ad.ea.com", "radusumalli", "yugaNdhar0.");

        //create SmbFile object
        SmbFile root = new SmbFile(url, mAuth);

        //store the content of the root folder in rootDirList SmbFile array
        rootDirList = root.listFiles();
        List<SmbFile> rootDirListArray = new ArrayList<>();
        Collections.addAll(rootDirListArray, rootDirList);

        //initialize Adapter for SmbFile object's list view
        smbFileArrayAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1, rootDirListArray
                );

        //Create a list view as we dont have it in the .xml file
        final ListView listView = (ListView) findViewById(R.id.activity_main);

        //set the adapter for list view
        listView.setAdapter(smbFileArrayAdapter);


        //set on item click for the List view
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                SmbFile currentItem = (SmbFile) listView.getAdapter().getItem(position);
                SmbFile[] currentSmbArray;
                try {

                    if (currentItem.isDirectory()) {
                        currentSmbArray = currentItem.listFiles();

                        smbFileArrayAdapter.clear();
                        smbFileArrayAdapter.addAll(currentSmbArray);
                        smbFileArrayAdapter.notifyDataSetChanged();
                        currentSmbArray = currentItem.listFiles();

                    } else if (currentItem.getName().contains(".apk")){
                        File sdcard = Environment.getExternalStorageDirectory();
                        String strSdcardPath = sdcard.getPath();
                        Uri uri = Uri.parse("file://" + strSdcardPath + "/" + currentItem.getName() );
                        Log.e("Uri", uri.toString());
                        downloadConfigFileFromServer(currentItem.getURL().toString(), strSdcardPath);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
                        getApplicationContext().startActivity(intent);
                    }



                } catch (SmbException e) {
                    e.printStackTrace();
                }


            }
        });

    }

    public boolean downloadConfigFileFromServer(String strPCPath , String strSdcardPath)
    {
        SmbFile smbFileToDownload = null;

        try
        {
            File localFilePath = new File(strSdcardPath);

            // create sdcard path if not exist.
            if (!localFilePath.isDirectory())
            {
                localFilePath.mkdir();
            }
            try
            {
                smbFileToDownload = new SmbFile(strPCPath , mAuth);
                String smbFileName = smbFileToDownload.getName();

                if (smbFileName.toLowerCase().contains(".apk"))
                {
                    InputStream inputStream = smbFileToDownload.getInputStream();

                    //only folder's path of the sdcard and append the file name after.
                    localFilePath = new File(strSdcardPath+ "/" + smbFileName);

                    OutputStream out = new FileOutputStream(localFilePath);
                    byte buf[] = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0)
                    {
                        out.write(buf, 0, len);
                    }
                    out.flush();
                    out.close();
                    inputStream.close();
                    return true;
                }
                else
                    return false;
            }// End try
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }

    }

}

