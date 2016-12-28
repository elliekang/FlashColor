package com.wordpress.kangellie.colorflash;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.app.AlertDialog;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.security.Policy;
//tutorial used: http://programmerguru.com/android-tutorial/how-to-change-screen-brightness-programmatically/


public class MainActivity extends AppCompatActivity {
    //switch of button
    private ImageButton btnSwitch;
    private SeekBar intensityBar;
    private View screenView;
    private View root;
    TextView intensityText;

    private Camera camera;
    private boolean isFlashOn;
    private boolean hasFlash;
    private Camera.Parameters params;
    private MediaPlayer mp;

    private int intensity_val;
    private ContentResolver cResolver;
    private Window window;

    private boolean firstStart = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSwitch = (ImageButton) findViewById(R.id.btnSwitch);
        intensityBar = (SeekBar) findViewById(R.id.intensity_scroller);
        intensityText = (TextView) findViewById(R.id.intensity_perc);
        screenView = (View) findViewById(R.id.screen);
        root = screenView.getRootView();

        hasFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!hasFlash) {
            AlertDialog alert = new AlertDialog.Builder(MainActivity.this)
                    .create();
            alert.setTitle("INCOMPATIBLE");
            alert.setMessage("The device does not support flash light");
            alert.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            alert.show();
            return;
        }

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFlashOn) {
                    turnOffFlash();
                } else {
                    if (!firstStart) {
                        turnOnFlash();
                    } else {
                        firstStart = false;
                    }
                }
            }
        });

        cResolver = getContentResolver();
        window = getWindow();
        intensityBar.setMax(255);
        intensityBar.setKeyProgressIncrement(1);
        try
        {
            intensity_val = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
        }
        catch (Settings.SettingNotFoundException e)
        {
            //Throw an error case it couldn't be retrieved
            Log.e("Error", "Cannot access system brightness");
            e.printStackTrace();
        }
        intensityBar.setProgress(intensity_val);
        setIntensityText();
        intensityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                //Set the system brightness using the brightness variable value
                Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, intensity_val);
                //Get the current window attributes
                WindowManager.LayoutParams layoutpars = window.getAttributes();
                //Set the brightness of this window
                layoutpars.screenBrightness = intensity_val / (float)255;
                //Apply attribute changes to this window
                window.setAttributes(layoutpars);
            }

            public void onStartTrackingTouch(SeekBar seekBar)
            {
                //Nothing handled here
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                //Set the minimal brightness level
                //if seek bar is 20 or any value below
                if(progress<=20)
                {
                    //Set the brightness to 20
                    intensity_val=20;
                }
                else //brightness is greater than 20
                {
                    //Set brightness variable based on the progress bar
                    intensity_val = progress;
                }
                setIntensityText();
            }
        });
    }

    //android.R.color.white
    private void setColor(int color) {
        root.setBackgroundColor(getResources().getColor(color));
    }

    private void setIntensityText() {
        //Calculate the brightness percentage
        float perc = (intensity_val /(float)255)*100;
        //Set the brightness percentage
        intensityText.setText((int)perc +" %");
    }
    private void getCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
                params = camera.getParameters();
            } catch (RuntimeException e) {
                Log.e("Failed to Open. Error: ", e.getMessage());
            }
        }
    }

    private void turnOnFlash() {
        if (!isFlashOn) {
            if (camera == null || params == null) {
                return;
            }
            playSound();
            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();
            isFlashOn = true;
            toggleButtonImage();
        }
    }

    private void turnOffFlash() {
        if (isFlashOn) {
            if (camera == null || params == null) {
                return;
            }
            playSound();
            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();
            isFlashOn = false;
            toggleButtonImage();
        }
    }

    private void playSound() {
        if (isFlashOn) {
            mp = MediaPlayer.create(MainActivity.this, R.raw.light_switch_off);
        } else {
            mp = MediaPlayer.create(MainActivity.this, R.raw.light_switch_on);
        }
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                mp.release();
            }
        });
        mp.start();
    }

    private void toggleButtonImage() {
        if (isFlashOn) {
            btnSwitch.setImageResource(R.drawable.btn_switch_on);
        } else {
            btnSwitch.setImageResource(R.drawable.btn_switch_off);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // on pause turn off the flash
        turnOffFlash();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // on resume turn on the flash
//        if(hasFlash)
//            turnOnFlash();
        firstStart = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // on starting the app get the camera params
        getCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // on stop release the camera
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }
}
