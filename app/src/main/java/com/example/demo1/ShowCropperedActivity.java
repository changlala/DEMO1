package com.example.demo1;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.demo1.utils.Utils;
import com.googlecode.tesseract.android.TessBaseAPI;

public class ShowCropperedActivity extends AppCompatActivity {

    //sd卡路径
    private static String LANGUAGE_PATH = "";
    //识别语言
    private static final String LANGUAGE = "eng";//chi_sim | eng

    private static final String TAG = "ShowCropperedActivity";

    private TextView barcodeTV;
    private TextView ocrCodeTV;

    private Uri uri;
    private String result;
    private TessBaseAPI baseApi = new TessBaseAPI();
    private Handler handler = new Handler();
    private ProgressDialog dialog;

    int endWidth, endHeight;
    private ColorMatrix colorMatrix;
    /**
     * 识别线程
     */
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
//            baseApi.setImage(binaryzation(getBitmapFromUri(uri), 100));
            baseApi.setImage(convertGray(getBitmapFromUri(uri)));
            result = baseApi.getUTF8Text();
            baseApi.end();

            if(result.contains("\n")){
                result = result.substring(0,result.indexOf("\n"));
            }
            Log.d(TAG, "run: "+result);
            result.replaceAll("s","");
            Log.d(TAG, "run: "+result);
            Log.d(TAG, "run: "+result);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ocrCodeTV.setText("ocrCode:"+result);
                    String barcode = getBarcode();
                    barcodeTV.setText("barcode:"+barcode);
                    dialog.dismiss();
                }
            });
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_croppered);

        LANGUAGE_PATH = getExternalFilesDir("") + "/";
        Log.e("---------", LANGUAGE_PATH);
        //获取截图uri
        uri = getIntent().getData();

        Thread myThread = new Thread(runnable);
        dialog = new ProgressDialog(this);
        dialog.setMessage("正在识别...");
        dialog.setCancelable(false);
        dialog.show();

        barcodeTV = (TextView)findViewById(R.id.barcodeTV);
        ocrCodeTV = (TextView)findViewById(R.id.ocrCodeTV);

        uri = getIntent().getData();
        boolean flag = baseApi.init(LANGUAGE_PATH, LANGUAGE);
        Log.d(TAG, "onCreate: "+flag);
        //设置设别模式
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);

//        设置只识别数字
        baseApi.setVariable("tessedit_char_whitelist","1234567890");
        baseApi.setVariable("classify_bln_numeric_mode","1234567890");

        myThread.start();
    }


    /**
     * uri转bitmap
     *
     * @param uri
     * @return
     */
    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            // 读取uri所在的图片
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            return bitmap;
        } catch (Exception e) {
            Log.e("[Android]", e.getMessage());
            Log.e("[Android]", "目录为：" + uri);
            e.printStackTrace();
            return null;
        }
    }

    private String getBarcode(){
        SharedPreferences sp = getSharedPreferences("data",MODE_PRIVATE);
        String barcode = sp.getString("barcode","");
        if(TextUtils.isEmpty(barcode)){
            return "未获取到";
        } else{
            return barcode;
        }
    }

    /**
     * 灰度化处理
     *
     * @param bitmap3
     * @return
     */
    public Bitmap convertGray(Bitmap bitmap3) {
        colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);

        Paint paint = new Paint();
        paint.setColorFilter(filter);
        Bitmap result = Bitmap.createBitmap(bitmap3.getWidth(), bitmap3.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        canvas.drawBitmap(bitmap3, 0, 0, paint);
        return result;
    }

    /**
     * 二值化
     *
     * @param bitmap22
     * @param tmp      二值化阈值 默认100
     * @return
     */
    private Bitmap binaryzation(Bitmap bitmap22, int tmp) {
        // 获取图片的宽和高
        int width = bitmap22.getWidth();
        int height = bitmap22.getHeight();
        // 创建二值化图像
        Bitmap bitmap = null;
        bitmap = bitmap22.copy(Bitmap.Config.ARGB_8888, true);
        // 遍历原始图像像素,并进行二值化处理
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // 得到当前的像素值
                int pixel = bitmap.getPixel(i, j);
                // 得到Alpha通道的值
                int alpha = pixel & 0xFF000000;
                // 得到Red的值
                int red = (pixel & 0x00FF0000) >> 16;
                // 得到Green的值
                int green = (pixel & 0x0000FF00) >> 8;
                // 得到Blue的值
                int blue = pixel & 0x000000FF;

                if (red > tmp) {
                    red = 255;
                } else {
                    red = 0;
                }
                if (blue > tmp) {
                    blue = 255;
                } else {
                    blue = 0;
                }
                if (green > tmp) {
                    green = 255;
                } else {
                    green = 0;
                }

                // 通过加权平均算法,计算出最佳像素值
                int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                // 对图像设置黑白图
                if (gray <= 95) {
                    gray = 0;
                } else {
                    gray = 255;
                }
                // 得到新的像素值
                int newPiexl = alpha | (gray << 16) | (gray << 8) | gray;
                // 赋予新图像的像素
                bitmap.setPixel(i, j, newPiexl);
            }
        }
        return bitmap;
    }



    @Override
    public void onBackPressed() {
        startActivity(new Intent(this,TakePhotoActivity.class));
        super.onBackPressed();
    }
}
