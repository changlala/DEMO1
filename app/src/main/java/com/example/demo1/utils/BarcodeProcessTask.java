package com.example.demo1.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.example.demo1.ShowCropperedActivity;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

public class BarcodeProcessTask extends AsyncTask<Void,Void,String> {
    private static final String TAG = "BarcodeProcessTask";
    private WeakReference<Context> mContextRef;
    private Uri mBitmapUri;
    private DoneCallback mCallback;
    private Bitmap mBitmap;
    private ImageScanner mScanner;

    public BarcodeProcessTask(Context c , Uri bitmapUri){
        this.mContextRef = new WeakReference<Context>(c);
        this.mBitmapUri = bitmapUri;

    }

    public void perform(){
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mCallback = new DoneCallback() {
            @Override
            public void onSuccess(String result) {
                SharedPreferences.Editor editor = mContextRef.get().getSharedPreferences("data",Context.MODE_PRIVATE).edit();
                editor.putString("barcode",result);
                Log.d(TAG, "onSuccess: "+result);
                editor.apply();
            }

            @Override
            public void onFailed() {
                SharedPreferences.Editor editor = mContextRef.get().getSharedPreferences("data",Context.MODE_PRIVATE).edit();
                editor.putString("barcode","");
                Log.d(TAG, "onFailed: ");
                editor.apply();
            }
        };
        setCallback(mCallback);
        setupReader();

        mBitmap = getBitmap();
    }

    @Override
    protected void onPostExecute(String s) {
        if(mCallback == null){
            Log.d(TAG, "onPostExecute: ");
        }
        if(s == null)
            mCallback.onFailed();
        else
            mCallback.onSuccess(s);
    }

    @Override
    protected String doInBackground(Void... voids) {
        if(mBitmap == null){
            Log.d(TAG, "doInBackground: bitmap没获取到");
            return null;
        }else{
            return processBitmapData();
        }

    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mContextRef.clear();
        mBitmap = null;
        mCallback = null;
    }
    protected void setupReader() {
        mScanner = new ImageScanner();
        mScanner.setConfig(0, Config.X_DENSITY, 3);
        mScanner.setConfig(0, Config.Y_DENSITY, 3);

        mScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
        mScanner.setConfig(BarcodeFormat.CODE128.getId(), Config.ENABLE, 1);

    }

    public void setCallback(DoneCallback callback){
        this.mCallback = callback;
    }



    private Bitmap getBitmap(){
        ContentResolver cr = mContextRef.get().getContentResolver();
        InputStream is = null;
        //图片宽高两者中 大的一方
        int bigger = 0;
        BitmapFactory.Options options = new BitmapFactory.Options();

        //获取图片宽高中大的一方
        try{
            is= cr.openInputStream(mBitmapUri);
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is,null,options);
            bigger = Math.max(options.outWidth,options.outHeight);
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }finally {
            try{
                if(is != null)
                    is.close();
            }catch (IOException e){
                e.printStackTrace();
                is = null;
            }
        }

        //根据图片的宽高对图片进行合理的压缩，并得到bitmap对象
        try {
            is= cr.openInputStream(mBitmapUri);
            int sampleSize = 0;
            if(bigger>4000){
                sampleSize = 4;
            }else if(bigger > 2000){
                sampleSize = 2;
            }else{
                sampleSize = 1;
            }
            options.inSampleSize = sampleSize;
            options.inJustDecodeBounds = false;

//            saveBitmap("barcodetest",BitmapFactory.decodeFile(picturePath, options));

//            return BitmapFactory.decodeFile(picturePath, options);

            Bitmap pic = BitmapFactory.decodeStream(is,null,options);


            //存于外部存储SD卡根目录下
            saveBitmap("demo1",pic);


            return pic;

        } catch (Exception e) {

            e.printStackTrace();
            return null;
        }finally {
            try{
                if(is != null)
                    is.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private String processData(Image barcode) {
        if (mScanner.scanImage(barcode) == 0) {
            return null;
        }

        for (Symbol symbol : mScanner.getResults()) {
            // 未能识别的格式继续遍历
            if (symbol.getType() == Symbol.NONE) {
                continue;
            }

            String symData;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                symData = new String(symbol.getDataBytes(), StandardCharsets.UTF_8);
            } else {
                symData = symbol.getData();
            }
            // 空数据继续遍历
            if (TextUtils.isEmpty(symData)) {
                continue;
            }

            return symData;
        }
        return null;
    }

    private String processBitmapData() {
        try {
            int picWidth = mBitmap.getWidth();
            int picHeight = mBitmap.getHeight();
            Log.d("demo1", "processBitmapData: width,height"+picWidth+" "+picHeight);
            Image barcode = new Image(picWidth, picHeight, "RGB4");
            int[] pix = new int[picWidth * picHeight];
            mBitmap.getPixels(pix, 0, picWidth, 0, 0, picWidth, picHeight);
            barcode.setData(pix);
//------
//            Rect scanBoxAreaRect = mScanBoxView.getScanBoxAreaRect(picHeight);
//            if (scanBoxAreaRect != null && scanBoxAreaRect.left + scanBoxAreaRect.width() <= width
//                    && scanBoxAreaRect.top + scanBoxAreaRect.height() <= picHeight) {
//                barcode.setCrop(scanBoxAreaRect.left, scanBoxAreaRect.top, scanBoxAreaRect.width(), scanBoxAreaRect.height());
//            }
//            --------
//            String result = processData(barcode.convert("GRAY"));
            String result = processData(barcode.convert("Y800"));
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    interface DoneCallback{
        public void onSuccess(String result);
        public void onFailed();

    }
    private static void saveBitmap(String bitName, Bitmap mBitmap) {
        String pathName = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ bitName + ".jpg";
        File f = new File(pathName);
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
