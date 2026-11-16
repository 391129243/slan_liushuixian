package com.gidi.bio_pcr.fragment.system;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import java.util.ArrayList;

public class SystemController {
    private int containerId;
    private FragmentManager fm;
    private ArrayList<Fragment> fragments;
    private static SystemController controller;

    public static SystemController getInstance(FragmentActivity activity, int containerId) {
        if (controller == null) {
            controller = new SystemController(activity, containerId);
        }
        return controller;
    }

    private SystemController(FragmentActivity activity, int containerId) {
        this.containerId = containerId;
        fm = activity.getSupportFragmentManager();
        initFragment();
    }

    private void initFragment() {
        fragments = new ArrayList<Fragment>();
        fragments.add(new SystemInfoFragment());
        fragments.add(new GeneralSettingFragment());
        fragments.add(new WlanFragment());
        fragments.add(new SystemSettingFragment());
        fragments.add(new HelpFragment());
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
