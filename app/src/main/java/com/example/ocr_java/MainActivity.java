package com.example.ocr_java;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import static android.view.MotionEvent.INVALID_POINTER_ID;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Detector.Processor {

    public SurfaceView cameraView,transparentView;
    public SurfaceHolder holder, holderTransparent;
    public TextView txtView;
    public ImageButton copyButton;
    public Button right,left,up,down;
    public CameraSource cameraSource;
    public String stringToCopy;
    public float RectLeft, RectTop,RectRight,RectBottom ;
    public int  deviceHeight,deviceWidth;
    public Canvas canvas;
    public long hold_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        stringToCopy = "";
        deviceWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        deviceHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        RectLeft = 100;
        RectTop = deviceHeight/2 - 100;
        RectRight = deviceWidth - 100;
        RectBottom = deviceHeight/2 + 100;
        cameraView = findViewById(R.id.camera_surfaceview);
        holder = cameraView.getHolder();
        holder.addCallback(this);
        cameraView.setSecure(true);

        right = findViewById(R.id.inc_W);
        right.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    hold_time = System.currentTimeMillis();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    RectRight = RectRight + (System.currentTimeMillis()-hold_time)/10;
                    RectLeft = RectLeft - (System.currentTimeMillis()-hold_time)/10;
                    Redraw();
                }
                return true;
            }
        });
        left = findViewById(R.id.dec_W);
        left.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    hold_time = System.currentTimeMillis();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    RectRight = RectRight - (System.currentTimeMillis()-hold_time)/10;
                    RectLeft = RectLeft + (System.currentTimeMillis()-hold_time)/10;
                    Redraw();
                }
                return true;
            }
        });
        up = findViewById(R.id.inc_H);
        up.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    hold_time = System.currentTimeMillis();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    RectBottom = RectBottom + (System.currentTimeMillis()-hold_time)/10;
                    RectTop = RectTop - (System.currentTimeMillis()-hold_time)/10;
                    Redraw();
                }
                return true;
            }
        });
        down = findViewById(R.id.dec_H);
        down.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    hold_time = System.currentTimeMillis();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    RectBottom = RectBottom - (System.currentTimeMillis()-hold_time)/10;
                    RectTop = RectTop + (System.currentTimeMillis()-hold_time)/10;
                    Redraw();
                }

                return true;
            }
        });

        transparentView = findViewById(R.id.transparent_surfaceview);
        holderTransparent = transparentView.getHolder();
        holderTransparent.addCallback(this);
        holderTransparent.setFormat(PixelFormat.TRANSLUCENT);
        transparentView.setZOrderMediaOverlay(true);

        copyButton = findViewById(R.id.copy_button);
        txtView = findViewById(R.id.txt_view);
        createCameraSource();

        //Copy string to clipboard (nwm kak jest schowek po ang)
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text", stringToCopy);
                clipboard.setPrimaryClip(clip);

                Toast toast = Toast.makeText(getApplicationContext(), "skopiowano do schowka", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        createCameraSource();
    }


    private void createCameraSource() {

        // Create the TextRecognizer
        Context appContext = getApplicationContext();
        TextRecognizer txtRecognizer = new TextRecognizer.Builder(appContext).build();

        // Check if the TextRecognizer is operational.
        if (txtRecognizer.isOperational()) {
            // Create the cameraSource using the TextRecognizer.
            cameraSource = new CameraSource.Builder(appContext, txtRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(deviceHeight, deviceWidth)
                    .setRequestedFps(15.0f)
                    .setAutoFocusEnabled(true)
                    .build();
            txtRecognizer.setProcessor(this);
        } else {
            Log.w("MainActivity", "Detector dependencies are not yet available.");
        }

    }

    private void Draw()
    {
        canvas = holderTransparent.lockCanvas(null);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(3);
        Rect rect=new Rect((int) RectLeft,(int)RectTop,(int)RectRight,(int)RectBottom);
        canvas.drawRect(rect,paint);
        holderTransparent.unlockCanvasAndPost(canvas);
    }
    private void Redraw()
    {
        canvas = holderTransparent.lockCanvas(null);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
        holderTransparent.unlockCanvasAndPost(canvas);

        Draw();
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Draw();
        try {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},1);
                return;
            }
            cameraSource.start(cameraView.getHolder());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void release() {

    }

    //gdy wykryje zmiany w ocr/kamerce
    @Override
    public void receiveDetections(Detector.Detections detections) {
        SparseArray items = detections.getDetectedItems();
        final StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < items.size(); i++)
        {

            TextBlock item = (TextBlock)items.valueAt(i);
            Point p[] = item.getCornerPoints();
            if((p[0].x+0.1*deviceWidth >= RectLeft) && (p[0].y+0.1*deviceHeight >= RectTop) && (p[3].x-0.1*deviceWidth <= RectRight) && (p[3].y-0.1*deviceHeight <= RectBottom)){
                strBuilder.append(item.getValue());
            }
        }

        txtView.post(new Runnable() { // stwórz wątek textView
            @Override
            public void run() {
                txtView.setText(strBuilder.toString());
            }
        });
        stringToCopy = strBuilder.toString();
        
    }

}
