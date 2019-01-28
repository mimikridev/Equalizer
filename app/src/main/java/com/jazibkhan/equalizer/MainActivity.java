package com.jazibkhan.equalizer;

import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Virtualizer;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.kobakei.ratethisapp.RateThisApp;
import com.marcinmoskala.arcseekbar.ArcSeekBar;
import com.marcinmoskala.arcseekbar.ProgressListener;

import java.util.ArrayList;
import java.util.Observable;


public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, Switch.OnCheckedChangeListener {

    EqualizerViewModel equalizerViewModel;
    static final int MAX_SLIDERS = 5; // Must match the XML layout
    private static final String TAG = "MainActivity";
    public static final String AD_ID = "ca-app-pub-3247504109469111~8021644228";
    public static final String TEST_DEVICE = "9CAE76FEB9BFA8EA6723EEED1660711A";
    Equalizer equalizer = null;
    BassBoost bassBoost = null;
    Virtualizer virtualizer = null;
    LoudnessEnhancer loudnessEnhancer = null;
    Switch enableEq = null;
    Switch enableBass, enableVirtual, enableLoud;
    Spinner spinner;
    int minLevel = 0;
    int maxLevel = 100;
    SeekBar sliders[] = new SeekBar[MAX_SLIDERS];
    ArcSeekBar bassSlider, virtualSlider, loudSlider;
    TextView sliderLabels[] = new TextView[MAX_SLIDERS];
    int numSliders = 0;
    boolean canEnable;
    ArrayList<String> eqPreset;
    int spinnerPos = 0;
    boolean dontcall = false;
    boolean canPreset;
    private AdView mAdView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        equalizerViewModel = ViewModelProviders.of(this).get(EqualizerViewModel.class);
        setContentView(R.layout.activity_main);
        RateThisApp.onCreate(this);
        RateThisApp.showRateDialogIfNeeded(this);

        MobileAds.initialize(this, AD_ID);
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(TEST_DEVICE).build();
        mAdView.loadAd(adRequest);


        enableEq = findViewById(R.id.switchEnable);
        enableEq.setChecked(true);
        spinner = findViewById(R.id.spinner);
        sliders[0] = findViewById(R.id.mySeekBar0);
        sliderLabels[0] = findViewById(R.id.centerFreq0);
        sliders[1] = findViewById(R.id.mySeekBar1);
        sliderLabels[1] = findViewById(R.id.centerFreq1);
        sliders[2] = findViewById(R.id.mySeekBar2);
        sliderLabels[2] = findViewById(R.id.centerFreq2);
        sliders[3] = findViewById(R.id.mySeekBar3);
        sliderLabels[3] = findViewById(R.id.centerFreq3);
        sliders[4] = findViewById(R.id.mySeekBar4);
        sliderLabels[4] = findViewById(R.id.centerFreq4);
        bassSlider = findViewById(R.id.bassSeekBar);
        virtualSlider = findViewById(R.id.virtualSeekBar);
        enableBass = findViewById(R.id.bassSwitch);
        enableVirtual = findViewById(R.id.virtualSwitch);
        enableLoud = findViewById(R.id.volSwitch);
        loudSlider = findViewById(R.id.volSeekBar);
        bassSlider.setMaxProgress(1000);
        virtualSlider.setMaxProgress(1000);
        loudSlider.setMaxProgress(10000);
        enableLoud.setChecked(true);
        enableBass.setChecked(true);
        enableVirtual.setChecked(true);
        eqPreset = new ArrayList<>();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, eqPreset);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        equalizer = equalizerViewModel.getEqualizer();
        bassBoost = equalizerViewModel.getBassBoost();
        virtualizer = equalizerViewModel.getVirtualizer();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            loudnessEnhancer = equalizerViewModel.getLoudnessEnhancer();
        else {
            enableLoud.setChecked(false);
            loudSlider.setVisibility(View.GONE);
            enableLoud.setVisibility(View.GONE);
        }
        numSliders = equalizer.getNumberOfBands();
        short r[] = equalizer.getBandLevelRange();
        minLevel = r[0];
        maxLevel = r[1];
        for (int i = 0; i < numSliders && i < MAX_SLIDERS; i++) {
            int freq_range = equalizer.getCenterFreq((short) i);
            sliders[i].setOnSeekBarChangeListener(this);
            sliderLabels[i].setText(milliHzToString(freq_range));
        }
        short noOfPresets = equalizer.getNumberOfPresets();
        for (short i = 0; i < noOfPresets; i++) {
            eqPreset.add(equalizer.getPresetName(i));
        }
        eqPreset.add("Custom");
        spinner.setAdapter(spinnerAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i < eqPreset.size() - 1) {
                    try {
                        equalizer.usePreset((short) i);
                        equalizerViewModel.setSpinnerPos(i);
                        equalizerViewModel.setIsCustomSelected(false);
                    } catch (Throwable e) {
                        disablePreset();
                    }
                } else {
                    equalizerViewModel.setIsCustomSelected(true);
                    equalizerViewModel.setSpinnerPos(i);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        virtualSlider.setOnProgressChangedListener(new ProgressListener() {
            @Override
            public void invoke(int j) {
                try {
                    virtualizer.setStrength((short) j);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                equalizerViewModel.setVirSlider(j);
            }
        });

        bassSlider.setOnProgressChangedListener(new ProgressListener() {
            @Override
            public void invoke(int i) {
                try {
                    bassBoost.setStrength((short) i);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                equalizerViewModel.setBBSlider(i);
            }
        });

        loudSlider.setOnProgressChangedListener(new ProgressListener() {
            @Override
            public void invoke(int j) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    try {
                        loudnessEnhancer.setTargetGain(j);
                    } catch (Throwable e) {

                        e.printStackTrace();
                    }
                }
                equalizerViewModel.setLoudSlider(j);
            }
        });
        enableVirtual.setOnCheckedChangeListener(this);
        enableBass.setOnCheckedChangeListener(this);
        enableLoud.setOnCheckedChangeListener(this);
        enableEq.setOnCheckedChangeListener(this);

        equalizerViewModel.getBBSlider().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                bassSlider.setProgress(integer);
            }
        });
        equalizerViewModel.getLoudSlider().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                loudSlider.setProgress(integer);
            }
        });
        equalizerViewModel.getVirSlider().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                virtualSlider.setProgress(integer);
            }
        });
        equalizerViewModel.getSlider().observe(this, new Observer<ArrayList<Integer>>() {
            @Override
            public void onChanged(@Nullable ArrayList<Integer> integers) {
                for (int i = 0; i < integers.size(); i++) {
                    sliders[i].setProgress(integers.get(i));
                }
            }
        });
        equalizerViewModel.getbBSwitch().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                enableBass.setChecked(aBoolean);
            }
        });
        equalizerViewModel.getEqSwitch().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                enableEq.setChecked(aBoolean);
            }
        });
        equalizerViewModel.getLoudSwitch().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                enableLoud.setChecked(aBoolean);
            }
        });
        equalizerViewModel.getVirSwitch().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                enableVirtual.setChecked(aBoolean);
            }
        });
        equalizerViewModel.getDarkTheme().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if (aBoolean == true) {
                    setTheme(R.style.AppTheme_Dark);
                } else setTheme(R.style.AppTheme);
            }
        });
        equalizerViewModel.getIsCustomSelected().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {

            }
        });
        equalizerViewModel.getSpinnerPos().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                spinner.setSelection(integer);
            }
        });
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == enableEq) {
            spinner.setEnabled(isChecked);
            equalizer.setEnabled(isChecked);
            for (int i = 0; i < 5; i++) {
                sliders[i].setEnabled(isChecked);
            }
            equalizerViewModel.setEqSwitch(isChecked);

        } else if (buttonView == enableBass) {
            bassBoost.setEnabled(isChecked);
            bassSlider.setEnabled(isChecked);
            equalizerViewModel.setbBSwitch(isChecked);
            if (isChecked)
                bassSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
            else
                bassSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));

        } else if (buttonView == enableLoud) {
            Toast.makeText(getApplicationContext(), R.string.warning,
                    Toast.LENGTH_SHORT).show();
            loudnessEnhancer.setEnabled(isChecked);
            loudSlider.setEnabled(isChecked);
            equalizerViewModel.setLoudSwitch(isChecked);
            if (isChecked)
                loudSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
            else
                loudSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));

        } else if (buttonView == enableVirtual) {
            virtualizer.setEnabled(isChecked);
            virtualSlider.setEnabled(isChecked);
            equalizerViewModel.setVirSwitch(isChecked);
            if (isChecked)
                virtualSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
            else
                virtualSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
        }
        serviceChecker();
    }


    public String milliHzToString(int milliHz) {
        if (milliHz < 1000) return "";
        if (milliHz < 1000000)
            return "" + (milliHz / 1000) + "Hz";
        else
            return "" + (milliHz / 1000000) + "kHz";
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int level, boolean b) {
        int newLevel = minLevel + (maxLevel - minLevel) * level / 100;
        for (int i = 0; i < numSliders; i++) {
            if (sliders[i] == seekBar) {
                try {
                    equalizer.setBandLevel((short) i, (short) newLevel);
                    equalizerViewModel.setSingleSlider(newLevel,i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        spinner.setSelection(eqPreset.size() - 1);
        spinnerPos = eqPreset.size() - 1;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
            MainActivity.this.startActivity(myIntent);
            return true;
        }
        if (id == R.id.action_about) {
            Intent myIntent = new Intent(MainActivity.this, AboutActivity.class);
            MainActivity.this.startActivity(myIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    public void updateUI() {
//        applyChanges();
//        serviceChecker();
//
//        if (enableBass.isChecked()) {
//            bassSlider.setEnabled(true);
//            bassSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
//            bassBoost.setEnabled(true);
//        } else {
//            bassSlider.setEnabled(false);
//            bassSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
//            bassBoost.setEnabled(false);
//        }
//        if (enableVirtual.isChecked()) {
//            virtualizer.setEnabled(true);
//            virtualSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
//            virtualSlider.setEnabled(true);
//        } else {
//            virtualizer.setEnabled(false);
//            virtualSlider.setEnabled(false);
//            virtualSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
//        }
//        if (enableLoud.isChecked()) {
//            loudnessEnhancer.setEnabled(true);
//            loudSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
//            loudSlider.setEnabled(true);
//        } else {
//            loudnessEnhancer.setEnabled(false);
//            loudSlider.setEnabled(false);
//            loudSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
//        }
//
//        if (enableEq.isChecked()) {
//            spinner.setEnabled(true);
//            for (int i = 0; i < 5; i++)
//                sliders[i].setEnabled(true);
//            equalizer.setEnabled(true);
//        } else {
//            spinner.setEnabled(false);
//            for (int i = 0; i < 5; i++)
//                sliders[i].setEnabled(false);
//            equalizer.setEnabled(false);
//        }
//        spinner.setSelection(spinnerPos);
//        updateSliders();
//        updateBassBoost();
//        updateVirtualizer();
//        updateLoudness();
//
//    }
//
//    public void updateSliders() {
//        for (int i = 0; i < numSliders; i++) {
//            int level;
//            if (equalizer != null)
//                level = equalizer.getBandLevel((short) i);
//            else
//                level = 0;
//            int pos = 100 * level / (maxLevel - minLevel) + 50;
//            sliders[i].setProgress(pos);
//        }
//    }
//
//    public void updateBassBoost() {
//        if (bassBoost != null)
//            bassSlider.setProgress(bassBoost.getRoundedStrength());
//        else
//            bassSlider.setProgress(0);
//    }
//
//    public void updateVirtualizer() {
//        if (virtualizer != null)
//            virtualSlider.setProgress(virtualizer.getRoundedStrength());
//        else
//            virtualSlider.setProgress(0);
//    }
//
//    public void updateLoudness() {
//        if (loudnessEnhancer != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
//                try {
//                    loudSlider.setProgress((int) (loudnessEnhancer.getTargetGain()));
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
//
//        } else
//            loudSlider.setProgress(0);
//    }
//
//    public void saveChanges() {
//        SharedPreferences myPreferences
//                = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor myEditor = myPreferences.edit();
//        myEditor.putBoolean("initial", true);
//        myEditor.putBoolean("eqswitch", enableEq.isChecked());
//        myEditor.putBoolean("bbswitch", enableBass.isChecked());
//        myEditor.putBoolean("virswitch", enableVirtual.isChecked());
//        myEditor.putBoolean("loudswitch", enableLoud.isChecked());
//        myEditor.putInt("spinnerpos", spinnerPos);
//        try {
//            if (bassBoost != null)
//                myEditor.putInt("bbslider", (int) bassBoost.getRoundedStrength());
//            else {
//                bassBoost = new BassBoost(100, 0);
//                myEditor.putInt("bbslider", (int) bassBoost.getRoundedStrength());
//
//            }
//            if (virtualizer != null)
//                myEditor.putInt("virslider", (int) virtualizer.getRoundedStrength());
//            else {
//                virtualizer = new Virtualizer(100, 0);
//                myEditor.putInt("virslider", (int) virtualizer.getRoundedStrength());
//            }
//            if (loudnessEnhancer != null) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
//                    myEditor.putFloat("loudslider", loudnessEnhancer.getTargetGain());
//            } else {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    loudnessEnhancer = new LoudnessEnhancer(0);
//                    myEditor.putFloat("loudslider", loudnessEnhancer.getTargetGain());
//                }
//            }
//        } catch (Throwable e) {
//            myEditor.putInt("bbslider", (int) 0);
//            myEditor.putInt("virslider", (int) 0);
//            myEditor.putFloat("loudslider", 0);
//            e.printStackTrace();
//        }
//
//
//        if ((spinnerPos == eqPreset.size() - 1) && !dontcall) {
//            myEditor.putInt("slider0", 100 * equalizer.getBandLevel((short) 0) / (maxLevel - minLevel) + 50);
//            myEditor.putInt("slider1", 100 * equalizer.getBandLevel((short) 1) / (maxLevel - minLevel) + 50);
//            myEditor.putInt("slider2", 100 * equalizer.getBandLevel((short) 2) / (maxLevel - minLevel) + 50);
//            myEditor.putInt("slider3", 100 * equalizer.getBandLevel((short) 3) / (maxLevel - minLevel) + 50);
//            myEditor.putInt("slider4", 100 * equalizer.getBandLevel((short) 4) / (maxLevel - minLevel) + 50);
//        }
//        myEditor.apply();
//    }
//
//
//    public void applyChanges() {
//        SharedPreferences myPreferences
//                = PreferenceManager.getDefaultSharedPreferences(this);
//        spinnerPos = myPreferences.getInt("spinnerpos", 0);
//        enableEq.setChecked(myPreferences.getBoolean("eqswitch", true));
//        enableBass.setChecked(myPreferences.getBoolean("bbswitch", true));
//        enableVirtual.setChecked(myPreferences.getBoolean("virswitch", true));
//        enableLoud.setChecked(myPreferences.getBoolean("loudswitch", false));
//
//        try {
//            if (bassBoost != null) {
//                try {
//                    bassBoost.setStrength((short) myPreferences.getInt("bbslider", 0));
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
//            } else {
//                bassBoost = new BassBoost(100, 0);
//
//                try {
//                    bassBoost.setStrength((short) myPreferences.getInt("bbslider", 0));
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
//            }
//            if (virtualizer != null) {
//
//                try {
//                    virtualizer.setStrength((short) myPreferences.getInt("virslider", 0));
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
//            } else {
//                virtualizer = new Virtualizer(100, 0);
//
//                try {
//                    virtualizer.setStrength((short) myPreferences.getInt("virslider", 0));
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
//            }
//            if (loudnessEnhancer != null) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    try {
//                        loudnessEnhancer.setTargetGain((int) myPreferences.getFloat("loudslider", 0));
//                    } catch (Throwable e) {
//                        e.printStackTrace();
//                    }
//                }
//            } else {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    loudnessEnhancer = new LoudnessEnhancer(0);
//                    try {
//                        loudnessEnhancer.setTargetGain((int) myPreferences.getFloat("loudslider", 0));
//                    } catch (Throwable e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        } catch (Throwable e) {
//
//            e.printStackTrace();
//        }
//        if (spinnerPos != eqPreset.size() - 1) {
//
//            try {
//                equalizer.usePreset((short) spinnerPos);
//            } catch (Throwable e) {
//                disablePreset();
//                e.printStackTrace();
//            }
//        } else {
//            try {
//                equalizer.setBandLevel((short) 0, (short) (minLevel + (maxLevel - minLevel) * myPreferences.getInt("slider0", 0) / 100));
//                equalizer.setBandLevel((short) 1, (short) (minLevel + (maxLevel - minLevel) * myPreferences.getInt("slider1", 0) / 100));
//                equalizer.setBandLevel((short) 2, (short) (minLevel + (maxLevel - minLevel) * myPreferences.getInt("slider2", 0) / 100));
//                equalizer.setBandLevel((short) 3, (short) (minLevel + (maxLevel - minLevel) * myPreferences.getInt("slider3", 0) / 100));
//                equalizer.setBandLevel((short) 4, (short) (minLevel + (maxLevel - minLevel) * myPreferences.getInt("slider4", 0) / 100));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        //       Log.d("WOW", "bass level *************************** " + (short) myPreferences.getInt("bbslider", 0));
//        //       Log.d("WOW", "virtualizer level *************************** " + (short) myPreferences.getInt("virslider", 0));
//    }
//
//    public void disableEvery() {
//        Toast.makeText(this, R.string.disableOther,
//                Toast.LENGTH_LONG).show();
//        spinner.setEnabled(false);
//        enableEq.setChecked(false);
//        enableVirtual.setChecked(false);
//        enableBass.setChecked(false);
//        enableLoud.setChecked(false);
//        canEnable = false;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
//            loudnessEnhancer.setEnabled(false);
//        loudSlider.setEnabled(false);
//        loudSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
//        virtualizer.setEnabled(false);
//        virtualSlider.setEnabled(false);
//        virtualSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
//        bassSlider.setEnabled(false);
//        bassSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
//        bassBoost.setEnabled(false);
//        for (int i = 0; i < 5; i++)
//            sliders[i].setEnabled(false);
//        equalizer.setEnabled(false);
//    }

    public void disablePreset() {
        spinner.setVisibility(View.GONE);
        canPreset = false;
    }

//    public void initialize() {
//        SharedPreferences myPreferences
//                = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor myEditor = myPreferences.edit();
//        if (!myPreferences.contains("initial")) {
//            myEditor.putBoolean("initial", true);
//            myEditor.putBoolean("eqswitch", false);
//            myEditor.putBoolean("bbswitch", false);
//            myEditor.putBoolean("virswitch", false);
//            myEditor.putInt("bbslider", (int) bassBoost.getRoundedStrength());
//            myEditor.putBoolean("loudswitch", false);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
//                myEditor.putFloat("loudslider", loudnessEnhancer.getTargetGain());
//            myEditor.putInt("virslider", (int) virtualizer.getRoundedStrength());
//            myEditor.putInt("slider0", 100 * equalizer.getBandLevel((short) 0) / (maxLevel - minLevel) + 50);
//            myEditor.putInt("slider1", 100 * equalizer.getBandLevel((short) 1) / (maxLevel - minLevel) + 50);
//            myEditor.putInt("slider2", 100 * equalizer.getBandLevel((short) 2) / (maxLevel - minLevel) + 50);
//            myEditor.putInt("slider3", 100 * equalizer.getBandLevel((short) 3) / (maxLevel - minLevel) + 50);
//            myEditor.putInt("slider4", 100 * equalizer.getBandLevel((short) 4) / (maxLevel - minLevel) + 50);
//            myEditor.putInt("spinnerpos", 0);
//            myEditor.apply();
//        }
//    }

    public void serviceChecker() {
        if (enableEq.isChecked() || enableBass.isChecked() || enableVirtual.isChecked() || enableLoud.isChecked()) {
            Intent startIntent = new Intent(MainActivity.this, ForegroundService.class);
            startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
            startService(startIntent);
        } else {
            Intent stopIntent = new Intent(MainActivity.this, ForegroundService.class);
            stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
            startService(stopIntent);
        }
    }
}
