package com.atomtex.spectrumgenerator;

import android.app.Activity;
import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.atomtex.spectrumgenerator.domain.EnergyLine;
import com.atomtex.spectrumgenerator.domain.NucIdent;
import com.atomtex.spectrumgenerator.domain.Nuclide;
import com.atomtex.spectrumgenerator.exception.ProcessException;
import com.atomtex.spectrumgenerator.util.SpectrumGenerator;
import com.atomtex.spectrumgenerator.util.SqTimer;
import com.balsikandar.crashreporter.CrashReporter;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.LogRecord;

import static com.atomtex.spectrumgenerator.MainViewModel.GENERATED_SPECTRUM;
import static com.atomtex.spectrumgenerator.MainViewModel.REFERENCE_SPECTRUM;
import static com.atomtex.spectrumgenerator.domain.NucIdent.BAD_INDEX;
import static com.atomtex.spectrumgenerator.domain.Nuclide.State.IDENTIFIED;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {

    MainViewModel mViewModel;

    NavigationView navigationView;

//    Handler handler;

    Thread thread;

    SpectrumFragment fragment1;
    SpectrumFragment fragment2;

    public static final String TAG = "TAG!!!";

//    TextView spectrumTimeTV;
    TextView requiredTimeTV;
    Button genButton;
    EditText timeText;
    EditText delayText;

    float[] mPeakChannels;
    float[] mPeakEnergies;
    String[] mLineOwners;

    FragmentManager manager;

    //todo не понятно, для чего нужно
    private int mPrefIdenThreshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewModel = ViewModelProviders.of(this).get(MainViewModel.class);

//        Log.e(TAG, "onCreate: navView = " + findViewById(R.id.nav_view));
        if (findViewById(R.id.nav_view) != null) {
            navigationView = findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);
        }

        NucIdent.setNuclides(AllNuclidesList.getAllNuclides());

        mPrefIdenThreshold = 3;

//        handler = new Handler();

        setDisplayMode(mViewModel.getDisplayMode());
        setButtonMode(mViewModel.getButtonMode());
        setTimeLayoutMode(mViewModel.getTimeLayoutMode());

//        spectrumTimeTV = findViewById(R.id.spectrum_time);
        requiredTimeTV = findViewById(R.id.requiredTime);
        genButton = findViewById(R.id.gen_button);
        timeText = findViewById(R.id.dialog_required_time_text);
        delayText = findViewById(R.id.dialog_delay_text);

//        setSpectrumTime();
        setRequiredTime();
        setDelayTime();

        findViewById(R.id.mode_button).setOnClickListener(this);
        findViewById(R.id.open_ats).setOnClickListener(this);
        findViewById(R.id.gen_button).setOnClickListener(this);
        findViewById(R.id.time_layout).setOnClickListener(this);

        manager = getSupportFragmentManager();
        //Это чтобы при повороте устройства не создавать новый фрагмент:
        fragment1 = (SpectrumFragment) manager.findFragmentById(R.id.fragment_container1);
        fragment2 = (SpectrumFragment) manager.findFragmentById(R.id.fragment_container2);

        if (fragment1 == null) {
            fragment1 = SpectrumFragment.newInstance(REFERENCE_SPECTRUM);
            manager.beginTransaction().replace(R.id.fragment_container1, fragment1).commit();
        }

        if (fragment2 == null) {
            fragment2 = SpectrumFragment.newInstance(GENERATED_SPECTRUM);
            manager.beginTransaction().replace(R.id.fragment_container2, fragment2).commit();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mode_button: toggleMode(); break;
            case R.id.open_ats: openAts(); break;
            case R.id.gen_button: toggleGenButton(); break;
            case R.id.time_layout: toggleTimeLayoutMode(); break;
//            case R.id.time_layout: showDialog(); break;
        }
    }



/*    SwitchCompat drawerSwitch = (SwitchCompat) navigationView.getMenu().findItem(R.id.nav_switch).getActionView();
    drawerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                // do stuff
            } else {
                // do other stuff
            }
        }
    });*/


    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            // do stuff
        } else {
            // do other stuff
        }
    }


    private void openAts() {
        Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory().toString());
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(selectedUri, "*/*");
        startActivityForResult(intent, 1);//TODO request code сделать psf
    }

    private void gen() {
        if (iCanGenerate()) generateSpectrum(mViewModel.getPathForAts(), mViewModel.getSpectrumTime(), mViewModel.getRequiredTime());
        else makeToast("Сначала загрузите файл .ats");
    }

    private void genTeak() {
        getParamFromTimeField();
        getDtoFromPath();
        mViewModel.getTempDTO().setSpectrum(new int[mViewModel.getTempDTO().getSpectrum().length]);
        start();
    }

    public boolean iCanGenerate() {
        return !TextUtils.isEmpty(mViewModel.getPathForAts());// if not empty or not null
    }

    private void showDialog() {
        final View view = this.getLayoutInflater().inflate(R.layout.dialog, null);
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setView(view);
//        final EditText editText1 = view.findViewById(R.id.dialog_spectrum_time_text);
        final EditText editText2 = view.findViewById(R.id.dialog_required_time_text);
//        editText1.setText(String.valueOf(mViewModel.getSpectrumTime()));
        editText2.setText(String.valueOf(mViewModel.getRequiredTime()));
        alert.setPositiveButton("Понял", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
//                mViewModel.setSpectrumTime(Integer.parseInt(editText1.getText().toString()));
                mViewModel.setRequiredTime(Integer.parseInt(editText2.getText().toString()));
//                setSpectrumTime();
                setRequiredTime();
                dialog.cancel();
            }
        });
        alert.show();
    }

    private void getParamFromTimeField() {
        mViewModel.setRequiredTime(Integer.parseInt(timeText.getText().toString()));
        mViewModel.setDelay(Integer.parseInt(delayText.getText().toString()));
    }

/*    private void setSpectrumTime() {
        spectrumTimeTV.setText(String.valueOf(mViewModel.getSpectrumTime()));//todo может есть смысл хранить в стринге?
    }*/

    private void setRequiredTime() {
        int time = mViewModel.getRequiredTime();
        requiredTimeTV.setText(String.valueOf(time));//todo может есть смысл хранить в стринге?
        timeText.setText(String.valueOf(time));
    }

    private void setDelayTime() {
        delayText.setText(String.valueOf(mViewModel.getDelay()));
    }

    private void toggleMode() {
        int mode = 0;
        if (mViewModel.getDisplayMode() == 0) mode = 1;
        if (mViewModel.getDisplayMode() == 1) mode = 2;
        mViewModel.setDisplayMode(mode);
        setDisplayMode(mode);
    }

    private void setDisplayMode(int mode) {
        switch (mode) {
            case 0:
                findViewById(R.id.fragment_container1).setVisibility(View.VISIBLE);
                findViewById(R.id.fragment_container2).setVisibility(View.VISIBLE);
                break;
            case 1:
                findViewById(R.id.fragment_container1).setVisibility(View.VISIBLE);
                findViewById(R.id.fragment_container2).setVisibility(View.GONE);
                break;
            case 2:
                findViewById(R.id.fragment_container1).setVisibility(View.GONE);
                findViewById(R.id.fragment_container2).setVisibility(View.VISIBLE);
                break;
        }
    }

    private void toggleTimeLayoutMode() {
        getParamFromTimeField();
        setDelayTime();
        setRequiredTime();

        int mode = 0;
        if (mViewModel.getTimeLayoutMode() == 0) mode = 1;
        mViewModel.setTimeLayoutMode(mode);
        setTimeLayoutMode(mode);
    }

    private void toggleButtonMode() {
        int mode = 0;
        if (mViewModel.getButtonMode() == 0) mode = 1;
        mViewModel.setButtonMode(mode);
        setButtonMode(mode);
    }

    private void toggleGenButton() {
        if (iCanGenerate()) {
            if (mViewModel.isGenButtonIsPressed()) {
                stop();
//                genButton.setCompoundDrawablesWithIntrinsicBounds( R.drawable.my_button_shape,0, 0, 0);
                genButton.setText("Генератор");
                mViewModel.setGenButtonIsPressed(false);
            } else {
                genTeak();
//                genButton.setCompoundDrawablesWithIntrinsicBounds( R.drawable.my_button_shape_pressed,0, 0, 0);
//                genButton.setBackgroundColor(getResources().getColor(R.color.colorOrange));
//                genButton.setBackgroundDrawable(R.drawable.my_button_shape_pressed);
                genButton.setText("Остановить");
                mViewModel.setGenButtonIsPressed(true);
            }
        } else {
            makeToast("Сначала загрузите файл .ats");
        }

    }

    private void setButtonMode(int mode) {
        switch (mode) {
            case 0:
                findViewById(R.id.time_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.button_layout).setVisibility(View.VISIBLE);
                break;
            case 1:
                findViewById(R.id.time_layout).setVisibility(View.GONE);
                findViewById(R.id.button_layout).setVisibility(View.GONE);
                break;
        }
    }

    private void setTimeLayoutMode(int mode) {
        switch (mode) {
            case 0:
                findViewById(R.id.time_layout_small).setVisibility(View.VISIBLE);
                findViewById(R.id.time_layout_big).setVisibility(View.GONE);
                break;
            case 1:
                findViewById(R.id.time_layout_small).setVisibility(View.GONE);
                findViewById(R.id.time_layout_big).setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case 1:
                    Uri uri = data.getData();
                    if (uri != null) {
                        String pathHolder = uri.toString();
                        Log.e(TAG, "PATH: = " + pathHolder);
                        if (pathHolder.endsWith(".ats")) {
                            mViewModel.setPathForAts(pathHolder); //save ats path for specGenerator
                            openAtsFile(pathHolder);
                            makeToast("Открытие...");
                        } else {
                            makeToast("Неверный формат");
                        }
                    }
                    break;
                case 2:
                    //...
                    break;
            }
//            updateUI();
        }
    }


    //todo вынести метод в отдельный класс (Controller)
    private void openAtsFile(String path) {
        SpecDTO dto = AtsReader.parseFile(path);
        if (dto != null) {
            int[] spectrum = dto.getSpectrum();
            Log.e(TAG, "run: " + dto.getSpectrum()[5]);
            float[] energy = dto.getEnergy();
            float[] sigma = dto.getSigma();
            NucIdent nuc = null;
            try {
                nuc = nuclidesIdent(spectrum.length, spectrum, sigma, energy, mPrefIdenThreshold);
            } catch (ProcessException e) {
                e.printStackTrace();
            }
            processIdenResult(nuc);
        }
        mViewModel.setSpectrumTime(dto.getMeasTim()[0]);
        //todo сделать не через новый инстанс, а через модификацию уже существующего объекта
        manager.beginTransaction().replace(R.id.fragment_container1, SpectrumFragment.newInstance(dto, mPeakChannels, mPeakEnergies, mLineOwners, REFERENCE_SPECTRUM, path)).commitAllowingStateLoss();
    }

    //todo remove path
    private void generateSpectrum(String path, int spTime, int rqTime) {
        SpecDTO dto = AtsReader.parseFile(path);
        dto.setMeasTim(new int[]{rqTime, 1});//todo здесь вторая переменная перезаписывается, можно сделать, чтобы сохранялась, но пока не надо -- она все равно всегда 0
        SpectrumGenerator mSpectrumGenerator = new SpectrumGenerator();
        dto.setSpectrum(mSpectrumGenerator.generatedSpectrum(dto.getSpectrum(), spTime, rqTime));
        int[] spectrum = dto.getSpectrum();
        float[] energy = dto.getEnergy();
        float[] sigma = dto.getSigma();
        NucIdent nuc = null;
        try {
            nuc = nuclidesIdent(spectrum.length, spectrum, sigma, energy, mPrefIdenThreshold);
        } catch (ProcessException e) {
            e.printStackTrace();
        }
        processIdenResult(nuc);
        //todo сделать не через новый инстанс, а через модификацию уже существующего объекта
        manager.beginTransaction().replace(R.id.fragment_container2, SpectrumFragment.newInstance(dto, mPeakChannels, mPeakEnergies, mLineOwners, "Сгенерированный спектр")).commitAllowingStateLoss();
    }


    void getDtoFromPath() {//todo масло маслянное...
        mViewModel.setTempDTO(AtsReader.parseFile(mViewModel.getPathForAts()));
    }
    //todo remove path
    private void generateSpectrumTeak(int spTime, int rqTime, int count) {
        SpecDTO refDto = AtsReader.parseFile(mViewModel.getPathForAts());
        SpecDTO dto = mViewModel.getTempDTO();
        dto.setMeasTim(new int[]{count, 1});//todo здесь вторая переменная перезаписывается, можно сделать, чтобы сохранялась, но пока не надо -- она все равно всегда 0
        SpectrumGenerator mSpectrumGenerator = new SpectrumGenerator();
//        dto.addSpectrumToCurrent(mSpectrumGenerator.generatedSpectrum(refDto.getSpectrum(), spTime, rqTime));
        dto.addSpectrumToCurrent(mSpectrumGenerator.generatedSpectrum(refDto.getSpectrum(), spTime, rqTime));
        int[] spectrum = dto.getSpectrum();
        float[] energy = dto.getEnergy();
        float[] sigma = dto.getSigma();
        NucIdent nuc = null;
        try {
            nuc = nuclidesIdent(spectrum.length, spectrum, sigma, energy, mPrefIdenThreshold);
        } catch (ProcessException e) {
            e.printStackTrace();
        }
        processIdenResult(nuc);
        //todo сделать не через новый инстанс, а через модификацию уже существующего объекта
        manager.beginTransaction().replace(R.id.fragment_container2, SpectrumFragment.newInstance(dto, mPeakChannels, mPeakEnergies, mLineOwners, "Сгенерированный спектр")).commitAllowingStateLoss();
    }

    private void processIdenResult(NucIdent nuc) {

        List<Nuclide> nuclides = NucIdent.getNuclides();
        Log.e(TAG, "NUCLIDES = " + nuclides.size());
        int numLines = nuc.getnLine();

        Log.e(TAG, "processIdenResult: NUMLINES = " + numLines);

        if (numLines > 0) {
            float[] peaks = nuc.getNiChannels();
            Log.e(TAG, "processIdenResult: PEAKS" + peaks.length);
            int[] energies = nuc.getEnergies();
            float[] peakChannels = new float[numLines];
            float[] peakEnergies = new float[numLines];
            String[] lineOwners = new String[numLines];

            int count = 0;

            for (int i = 0; i < peaks.length; i++) {
                if (peaks[i] > 0) {
                    peakChannels[count] = peaks[i];
                    peakEnergies[count] = energies[i];
                    count++;
                }
            }


            for (Nuclide nuclide : nuclides) {
                if (nuclide.getState() == IDENTIFIED) {
                    EnergyLine[] lines = nuclide.getEnergyLines();
                    for (EnergyLine line : lines) {
                        int index = line.getIndex();
                        if (index != BAD_INDEX
                                && line.getFactorsNoShield() > 0
                                && line.getFactorsShield() > 0) {
                            lineOwners[index] = nuclide.getName() + " " + nuclide.getNumStr();
                        }
                    }
                }
            }
            mPeakChannels = peakChannels;
            Log.e(TAG, "processIdenResult: " + peakChannels.length);
            mPeakEnergies = peakEnergies;
            mLineOwners = lineOwners;
        } else {
            mPeakChannels = null;
        }

//        return nuclides;
    }

    //todo вынести метод в отдельный класс (Controller)
    public NucIdent nuclidesIdent(int channelNumber, int[] spectrum,  //todo was private
                                  float[] sigma, float[] energy, int threshold) throws ProcessException {

        NucIdent nuc = new NucIdent(channelNumber,
                16, 20, 32, 24,
                20, 20, 0, true);

        nuc.setSpectrum(spectrum);
        nuc.setSpectrumSigma(sigma);
        nuc.setSpectrumEnergy(energy);

        nuc.detectLines(threshold);

        nuc.makeNuclideIdentification(threshold);

        return nuc;
    }

    private void makeToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_open_ats) {
            openAts();
        } else if (id == R.id.nav_generate) {
            gen();
        } else if (id == R.id.nav_set_time) {
            showDialog();
        } else if (id == R.id.nav_toggle_mode) {
            toggleMode();
        } else if (id == R.id.nav_toggle_button) {
            toggleButtonMode();
        } else if (id == R.id.nav_send) {
            getDtoFromPath();
            mViewModel.getTempDTO().setSpectrum(new int[mViewModel.getTempDTO().getSpectrum().length]);
            start();
        } else if (id == R.id.nav_stop) {
            stop();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    final Handler handler = new Handler();
    Runnable runnable;

    void start(){
            handler.postDelayed(runnable = new Runnable() {
                int count = 0;
                public void run() {
                    if (count++ < mViewModel.getRequiredTime()) {
                        generateSpectrumTeak(mViewModel.getSpectrumTime(), 1, count);
                        handler.postDelayed(runnable, mViewModel.getDelay());
                    } else {
                        genButton.setText("Генератор");
                        mViewModel.setGenButtonIsPressed(false);
                    }
                }
            }, 250);

    }

    void stop() {
        handler.removeCallbacks(runnable);
    }

/*    Thread thread = new Thread() {
        @Override
        public void run() {
            try {
                while(true) {
                    sleep(1000);
                    handler.post(this);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

thread.start();*/

}
