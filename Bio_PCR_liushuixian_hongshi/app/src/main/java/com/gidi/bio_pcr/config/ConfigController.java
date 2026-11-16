package com.gidi.bio_pcr.config;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.gidi.bio_pcr.config.fragment.ConfigInfoFragment;
import com.gidi.bio_pcr.config.fragment.ConfigSLANFragment;
import com.gidi.bio_pcr.config.fragment.RunConfigFragment;

import java.util.ArrayList;

public class ConfigController {
    private int containerId;
    private FragmentManager fm;
    private ArrayList<Fragment> fragments;
    private static ConfigController controller;

    public static ConfigController getInstance(FragmentActivity activity, int containerId) {
        if (controller == null) {
            controller = new ConfigController(activity, containerId);
        }
        return controller;
    }

    private ConfigController(FragmentActivity activity, int containerId) {
        this.containerId = containerId;
        fm = activity.getSupportFragmentManager();
        initFragment();
    }

    private void initFragment() {
        fragments = new ArrayList<Fragment>();
        fragments.add(new ConfigInfoFragment());
        fragments.add(new ConfigSLANFragment());
        fragments.add(new RunConfigFragment());
        FragmentTransaction ft = fm.beginTransaction();
        for(Fragment fragment : fragments) {
            ft.add(containerId, fragment);
        }
        ft.commit();
    }

    public void showFragment(int position) {
        hideFragments();
        Fragment fragment = fragments.get(position);
        FragmentTransaction ft = fm.beginTransaction();
        ft.show(fragment);
        ft.commit();
    }

    public void hideFragments() {
        FragmentTransaction ft = fm.beginTransaction();
        for(Fragment fragment : fragments) {
            if(fragment != null) {
                ft.hide(fragment);
            }
        }
        ft.commit();
    }

    public Fragment getFragment(int position) {
        return fragments.get(position);
    }

    public void destoryController(){
        controller = null;
        if(null != fragments){
            fragments.clear();
            fragments = null;
        }

    }
}
