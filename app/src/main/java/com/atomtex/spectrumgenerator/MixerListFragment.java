package com.atomtex.spectrumgenerator;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;

import static com.atomtex.spectrumgenerator.MainActivity.TAG;

public class MixerListFragment extends Fragment implements View.OnClickListener {

    private static MainViewModel mViewModel;
    View v;
    SourceAdapter sourceAdapter;
    Button bt;
    ListView lvMain;

    public static MixerListFragment newInstance() {
        return new MixerListFragment();
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // находим список
        lvMain = v.findViewById(R.id.my_list);

        // создаем адаптер
        sourceAdapter = new SourceAdapter(getActivity(),
                R.layout.mixer_list_item, mViewModel.getSourceList(), mViewModel);

        // присваиваем адаптер списку
        lvMain.setAdapter(sourceAdapter);

        //Лисенер для элемента ListView
        lvMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                int pos = sourceList.get((int)id).getID();
                Log.e(TAG, "onItemClick: " + (int)id);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_mixer_list, null);
        mViewModel = ViewModelProviders.of(getActivity()).get(MainViewModel.class);//todo in newInstance?
        bt = v.findViewById(R.id.add_new_spectrum);
        bt.setOnClickListener(this);
        return v;


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//            6mixer_button1: ((MainActivity)getActivity()).openAts(0) ;break;
            case R.id.add_new_spectrum:
/*                mViewModel.addNewSpectrum();
                lvMain.setAdapter(sourceAdapter);//обновить адаптер после добавления новых элементов*/

                ((MainActivity)getActivity()).openAts(0);
            break;
        }
    }

    public void updateAdapter() {
        lvMain.setAdapter(sourceAdapter);//обновить адаптер после добавления новых элементов*/
    }


}
