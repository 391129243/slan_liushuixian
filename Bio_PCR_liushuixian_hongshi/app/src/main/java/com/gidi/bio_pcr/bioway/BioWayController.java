package com.gidi.bio_pcr.bioway;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.gidi.bio_pcr.bioway.running.RunBioWayFirstPCRFragment;
import com.gidi.bio_pcr.bioway.running.RunBioWayResultFragment;
import com.gidi.bio_pcr.bioway.running.RunBioWaySecondPCRFragment;
import com.gidi.bio_pcr.fragment.running.RunFirstRNAFragment;
import com.gidi.bio_pcr.fragment.running.RunSecondRNAFragment;


import java.util.ArrayList;

public class BioWayController {
    private int containerId;

    private FragmentManager fm;

    private ArrayList<Fragment> fragments;

    private static BioWayController controller;

    public static BioWayController getInstance(Fragment parentFragment, int containerId){
        if(null == controller){
            controller = new BioWayController(parentFragment, containerId);
        }
        return controller;
    }

    private BioWayController(Fragment fragment, int containerId){
        this.containerId = containerId;
        this.fm = fragment.getChildFragmentManager();
        initFragments();
    }

    private void initFragments() {
        fragments = new ArrayList<Fragment>();
        fragments.add(new RunFirstRNAFragment());
        fragments.add(new RunBioWayFirstPCRFragment());
        fragments.add(new RunSecondRNAFragment());
        fragments.add(new RunBioWaySecondPCRFragment());
        fragments.add(new RunBioWayResultFragment());

        FragmentTransaction ft = fm.beginTransaction();
        for(Fragment fragment: fragments){
            ft.add(containerId, fragment);
        }
        ft.commit();
    }

    public void showFragment(int position){
        hideFragment();
        Fragment fragment = fragments.get(position);
        FragmentTransaction ft = fm.beginTransaction();
        ft.show(fragment);
        ft.commit();
    }

    public void hideFragment(){
        FragmentTransaction ft = fm.beginTransaction();
        for(Fragment fragment : fragments){
            if(null != fragment){
                ft.hide(fragment);
            }
        }
        ft.commit();
    }

    public Fragment getFragment(int position){
        Fragment fragment = null;
        if(null != fragments && fragments.size()>0){
            fragment =  fragments.get(position);
        }
        return fragment;
    }

    public void destoryController(){
        controller = null;
        if(null != fragments){
            fragments.clear();
            fragments = null;
        }
    }
}
