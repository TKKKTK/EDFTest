package com.wg.edftest;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.wg.edftest.EDFlib.EDFException;
import com.wg.edftest.EDFlib.EDFwriter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {

    private Button import_button;
    private Button load_button;
    private static final int IMPORT_CODE = 100;
    private static final int LOAD_CODE = 200;
    private int[] buf;
    private Queue<int[]> dataQueue = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkNeedPermissions();

        import_button = (Button) findViewById(R.id.import_txt);
        load_button = (Button) findViewById(R.id.load_edf);

        //导入txt文件
        import_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 ImportFile();
            }
        });

        //导出edf文件
        load_button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                WriteEdf();
            }
        });
    }

    /**
     * 写入EDF文件
     */
    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void WriteEdf()  {
        int i, err,
                sf1=500, // 通道1的采样频率
                edfsignals = 1; //通道数

        EDFwriter hdl;
        try
        {
            hdl = new EDFwriter("xyz.edf", EDFwriter.EDFLIB_FILETYPE_BDFPLUS, edfsignals,MainActivity.this);
        }
        catch(IOException e)
        {
           e.printStackTrace();
            return;
        }
        catch(EDFException e)
        {
            e.printStackTrace();
            return;
        }

        //设置信号的最大物理值
        hdl.setPhysicalMaximum(0, 3000);
        //设置信号的最小物理值
        hdl.setPhysicalMinimum(0, -3000);
        //设置信号的最大数字值
        hdl.setDigitalMaximum(0, 32767);
        //设置信号的最小数字值
        hdl.setDigitalMinimum(0, -32768);
        //设置信号的物理单位
        hdl.setPhysicalDimension(0, String.format("uV"));

        //设置采样频率
        hdl.setSampleFrequency(0, sf1);

        //设置信号标签
        hdl.setSignalLabel(0, String.format("sine 500Hz", 0 + 1));

        try
        {
            for(i=0; i<dataQueue.size(); i++)
            {

                err = hdl.writeDigitalSamples(dataQueue.poll());
                if(err != 0)
                {
                    System.out.printf("writePhysicalSamples() returned error: %d\n", err);
                    return;
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return;
        }

        hdl.writeAnnotation(0, -1, "Recording starts");

        hdl.writeAnnotation(dataQueue.size() * 10000, -1, "Recording ends");

        try
        {
            hdl.close();
            Toast.makeText(MainActivity.this, "导出EDF文件成功", Toast.LENGTH_SHORT).show();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return;
        }
        catch(EDFException e)
        {
            e.printStackTrace();
            return;
        }
    }

    private Uri getUri(){
        //采取分区存储方式
        //在Android R 下创建文件
        //获取到一个路径
        Uri uri = MediaStore.Files.getContentUri("external");
        //创建一个ContentValue对象，用来给存储文件数据的数据库进行插入操作
        ContentValues contentValues = new ContentValues();
        //首先创建zee.txt要存储的路径 要创建的文件的上一级存储目录
        String path = Environment.DIRECTORY_DOWNLOADS + "/ZEE/";
        //Log.d(TAG, "createFile: "+path);
        //给路径的字段设置键值对
        contentValues.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/ZEE");
        //设置文件的名字
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME,"xyz.edf");
        //可有可无
        contentValues.put(MediaStore.Downloads.TITLE,"Zee");

        //插入一条数据，然后把生成的这个文件的路径返回回来
        Uri insert = getContentResolver().insert(uri,contentValues);
//        OutputStream outputStream  = null;
//
//        try {
//            outputStream  = getContentResolver().openOutputStream(insert);
//        }catch (Exception e){
//           e.printStackTrace();
//        }

        return insert;
    }

    private String getPath( Uri uri) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            return null;
        }
        if (cursor.moveToFirst()) {
            try {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cursor.close();
        return path;
    }


    private void checkNeedPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //多个权限一起申请
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }
    }

    /**
     * 文件导入
     */
    private void ImportFile(){
        Uri uri = MediaStore.Files.getContentUri("external");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
         intent.addCategory(Intent.CATEGORY_OPENABLE);
         intent.setType("*/*");
         intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,uri);
         startActivityIfNeeded(intent,IMPORT_CODE);
    }

    /**
     * 页面结果回调
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMPORT_CODE && resultCode == RESULT_OK){
            Uri uri = data.getData();
            InputStream inputStream = null;
            StringBuilder stringBuilder = new StringBuilder();

            try {
                inputStream = getContentResolver().openInputStream(uri);
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line;
                while((line = reader.readLine()) != null){
                    stringBuilder.append(line);
                }
                DataSolution(stringBuilder.toString());
                Toast.makeText(MainActivity.this, "文件导入成功", Toast.LENGTH_SHORT).show();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 数据解析
     */
    private void DataSolution(String dataStr){
        String[] dataArr = dataStr.split(" ");
        Log.d("DataSolution", "解析后的数据: "+ Arrays.toString(dataArr));
        buf = new int[500];
        int index = 0;
        for (int i = 0; i < dataArr.length; i++){
            if (index == 500){
                dataQueue.add(buf);
                buf = new int[500];
                index = 0;
            }
            buf[index] = Integer.parseInt(dataArr[i]);
            index++;
        }
    }



}