package com.gidi.bio_pcr.user.running;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;

public class UserController {
    private int containerId;

    private FragmentManager fm;

    private ArrayList<Fragment> fragments;

    private static UserController controller;

    public static UserController getInstance(Fragment parentFragment, int containerId){
        if(null == controller){
            controller = new UserController(parentFragment, containerId);
        }
        return controller;
    }

    private UserController(Fragment fragment, int containerId){
        this.containerId = containerId;
        this.fm = fragment.getChildFragmentManager();
        initFragments();
    }

    private void initFragments() {
        fragments = new ArrayList<Fragment>();
        fragments.add(new UserFirstRNAFragment());
        fragments.add(new UserBioWayResultFragment());

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

    public void showFragment1(int position){
        hideFragment1();
        Fragment fragment = fragments.get(position);
        FragmentTransaction ft = fm.beginTransaction();
        ft.show(fragment);
        ft.commitAllowingStateLoss();
    }

    public void hideFragment1(){
        FragmentTransaction ft = fm.beginTransaction();
        for(Fragment fragment : fragments){
            if(null != fragment){
                ft.hide(fragment);
            }
        }
        ft.commitAllowingStateLoss();
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
