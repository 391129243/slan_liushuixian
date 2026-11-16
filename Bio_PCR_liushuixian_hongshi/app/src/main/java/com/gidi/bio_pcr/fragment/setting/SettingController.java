package com.gidi.bio_pcr.fragment.setting;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.gidi.bio_pcr.fragment.system.SystemInfoFragment;
import com.paul.pipeline.fragment.UpdateLinuxFragment;

import java.util.ArrayList;

public class SettingController {
    private int containerId;
    private FragmentManager fm;
    private ArrayList<Fragment> fragments;
    private static SettingController controller;

    public static SettingController getInstance(FragmentActivity activity, int containerId) {
        if (controller == null) {
            controller = new SettingController(activity, containerId);
        }
        return controller;
    }

    private SettingController(FragmentActivity activity, int containerId) {
        this.containerId = containerId;
        fm = activity.getSupportFragmentManager();
        initFragment();
    }

    private void initFragment() {
        fragments = new ArrayList<Fragment>();
        fragments.add(new FileOperateFragment());
        fragments.add(new ConfigPCRFragment());
        fragments.add(new LoginInfoFragment());
        fragments.add(new UpdateLinuxFragment());
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
