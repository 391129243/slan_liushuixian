package com.gidi.bio_pcr.fragment.result;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import java.util.ArrayList;

public class ResultController {
    private int containerId;
    private FragmentManager fm;
    private ArrayList<Fragment> fragments;
    private static ResultController controller;

    public static ResultController getInstance(FragmentActivity activity, int containerId) {
        if (controller == null) {
            controller = new ResultController(activity, containerId);
        }
        return controller;
    }

    private ResultController(FragmentActivity activity, int containerId) {
        this.containerId = containerId;
        fm = activity.getSupportFragmentManager();
        initFragment();
    }
//    public static ResultController getInstance(Fragment activity, int containerId) {
//        if (controller == null) {
//            controller = new ResultController(activity, containerId);
//        }
//        return controller;
//    }
//    private ResultController(Fragment activity, int containerId) {
//        this.containerId = containerId;
//        fm = activity.getChildFragmentManager();
//        initFragment();
//    }
    private void initFragment() {
        fragments = new ArrayList<Fragment>();
        fragments.add(new ResultExFragment());
        fragments.add(new ResultBiowayWellFragment());

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
