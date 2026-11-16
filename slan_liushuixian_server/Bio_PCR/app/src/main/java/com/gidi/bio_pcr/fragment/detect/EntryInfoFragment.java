package com.gidi.bio_pcr.fragment.detect;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdTimeoutEvent;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.bean.WellInfo;
import com.gidi.bio_pcr.bioway.activity.BiowayActivity;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.listener.FragmentKeyeventListener;
import com.gidi.bio_pcr.mgr.ScanKeyManager;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.WarnDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.LinkedList;

/**
 * 信息录入
 */
public class EntryInfoFragment extends BaseFragment implements View.OnClickListener,
        FragmentKeyeventListener {

    private TextView mNextBtn,mTemplateNameTv;
    private Button mClearAllBtn,mFillAllBtn;
    private WarnDialog mNoticeDialog;
    private LinkedList<WellInfo> wellInfos;
    private ImageView mHoleOneTv ,mHoleTwoTv,mHoleThreeTv,mHoleFourTv,mHoleSixTv,mHoleSevenTv,mHoleEightTv,mHoleFiveTv;
    private boolean isA1Check = true;
    private boolean isA2Check= true;
    private boolean isA3Check= true;
    private boolean isA4Check= true;
    private boolean isA5Check= true;
    private boolean isA6Check= true;
    private boolean isA7Check= true;
    private boolean isA8Check= true;
    private TextView mID_HoleEightA1Et, mID_HoleSevenA2Et,mID_HoleSixA3Et, mID_HoleFiveA4Et,
            mID_HoleFourA5Et, mID_HoleThreeA6Et, mID_HoleTwoA7Et, mID_HoleOneA8Et;


    private ImageView mOneIDClearImg,mTwoIDClearImg,mThreeIDClearImg,mFourIDClearImg,mFiveIDClearImg,
            mSixIDClearImg,mSevenIDClearImg,mEightIDClearImg;


    private TextView currentView;
    private RadioGroup modeGroup;
    private RadioButton manualModeRb,scanModeRb;
    private int cardView = 0;

    private boolean isOpenningDoor = false;//正在开门
    private String reagentCode = "";


    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_entry_info;
    }

    @Override
    protected void initView(View rootView) {
        mTemplateNameTv= (TextView)rootView.findViewById(R.id.slan_info_tempalte_name);
        mClearAllBtn = (Button) rootView.findViewById(R.id.detect_entry_info_clear_all_btn);
        mFillAllBtn = (Button)rootView.findViewById(R.id.detect_entry_info_filling_all_btn) ;
        mNextBtn = (TextView) rootView.findViewById(R.id.detect_entry_info_next_btn);

        mHoleOneTv = (ImageView) rootView.findViewById(R.id.detect_entry_info_hole_one_btn) ;
        mHoleTwoTv = (ImageView)rootView.findViewById(R.id.detect_entry_info_hole_two_btn);
        mHoleThreeTv = (ImageView)rootView.findViewById(R.id.detect_entry_info_hole_three_btn) ;
        mHoleFourTv = (ImageView)rootView.findViewById(R.id.detect_entry_info_hole_four_btn) ;
        mHoleFiveTv = (ImageView) rootView.findViewById(R.id.detect_entry_info_hole_five_btn) ;
        mHoleSixTv = (ImageView)rootView.findViewById(R.id.detect_entry_info_hole_six_btn);
        mHoleSevenTv = (ImageView)rootView.findViewById(R.id.detect_entry_info_hole_seven_btn) ;
        mHoleEightTv= (ImageView)rootView.findViewById(R.id.detect_entry_info_hole_eight_btn) ;

        mID_HoleEightA1Et = (TextView) rootView.findViewById(R.id.detect_entry_info_one_idcard_tv);
        mID_HoleSevenA2Et = (TextView) rootView.findViewById(R.id.detect_entry_info_two_idcard_tv);
        mID_HoleSixA3Et = (TextView) rootView.findViewById(R.id.detect_entry_info_three_idcard_tv);
        mID_HoleFiveA4Et = (TextView) rootView.findViewById(R.id.detect_entry_info_four_idcard_tv);
        mID_HoleFourA5Et = (TextView) rootView.findViewById(R.id.detect_entry_info_five_idcard_tv);
        mID_HoleThreeA6Et = (TextView) rootView.findViewById(R.id.detect_entry_info_six_idcard_tv);
        mID_HoleTwoA7Et = (TextView) rootView.findViewById(R.id.detect_entry_info_seven_idcard_tv);
        mID_HoleOneA8Et = (TextView) rootView.findViewById(R.id.detect_entry_info_eight_idcard_tv);

        mOneIDClearImg = (ImageView)rootView.findViewById(R.id.detect_entry_info_one_idcard_clear_img);
        mTwoIDClearImg = (ImageView)rootView.findViewById(R.id.detect_entry_info_two_idcard_clear_img);
        mThreeIDClearImg = (ImageView)rootView.findViewById(R.id.detect_entry_info_three_idcard_clear_img);
        mFourIDClearImg = (ImageView)rootView.findViewById(R.id.detect_entry_info_four_idcard_clear_img);
        mFiveIDClearImg = (ImageView)rootView.findViewById(R.id.detect_entry_info_five_idcard_clear_img);
        mSixIDClearImg = (ImageView)rootView.findViewById(R.id.detect_entry_info_six_idcard_clear_img);
        mSevenIDClearImg = (ImageView)rootView.findViewById(R.id.detect_entry_info_seven_idcard_clear_img);
        mEightIDClearImg = (ImageView)rootView.findViewById(R.id.detect_entry_info_eight_idcard_clear_img);


        modeGroup = (RadioGroup)rootView.findViewById(R.id.slan_info_mode_rg) ;
        manualModeRb = (RadioButton)rootView.findViewById(R.id.slan_info_manual_mode_rb) ;
        scanModeRb = (RadioButton)rootView.findViewById(R.id.slan_info_scan_mode_rb) ;

        initVariable();
        setHoleState(isA1Check,mHoleOneTv);
        setHoleState(isA2Check,mHoleTwoTv);
        setHoleState(isA3Check,mHoleThreeTv);
        setHoleState(isA4Check,mHoleFourTv);
        setHoleState(isA5Check,mHoleFiveTv);
        setHoleState(isA6Check,mHoleSixTv);
        setHoleState(isA7Check,mHoleSevenTv);
        setHoleState(isA8Check,mHoleEightTv);
    }

    @Override
    protected void initListener() {
        mFillAllBtn.setOnClickListener(this);
        mHoleOneTv.setOnClickListener(this);
        mHoleTwoTv.setOnClickListener(this);
        mHoleThreeTv.setOnClickListener(this);
        mHoleFourTv.setOnClickListener(this);
        mHoleFiveTv.setOnClickListener(this);
        mHoleSixTv.setOnClickListener(this);
        mHoleSevenTv.setOnClickListener(this);
        mHoleEightTv.setOnClickListener(this);

        mOneIDClearImg.setOnClickListener(this);
        mTwoIDClearImg.setOnClickListener(this);
        mThreeIDClearImg.setOnClickListener(this);
        mFourIDClearImg.setOnClickListener(this);
        mFiveIDClearImg.setOnClickListener(this);
        mSixIDClearImg.setOnClickListener(this);
        mSevenIDClearImg.setOnClickListener(this);
        mEightIDClearImg.setOnClickListener(this);

        mClearAllBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);

        modeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkId) {
                if(checkId == R.id.slan_info_manual_mode_rb){
                    //手动输入
                    isInput = true;
                    clearAllFocus();
                }else if(checkId == R.id.slan_info_scan_mode_rb){
                    //扫描
                    isInput = false;
                }
            }
        });


        //孔8A1
        mID_HoleEightA1Et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isInput){
                    String hole8A1IDcard = mID_HoleEightA1Et.getText().toString();
                    showInputNumDialog(8,hole8A1IDcard);
                }else{
                    cardView = 8;
                    requestFocusState(mID_HoleEightA1Et);
                }
            }
        });
        //孔7A2
        mID_HoleSevenA2Et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isInput){
                    String hole7A2IDcard = mID_HoleSevenA2Et.getText().toString();
                    showInputNumDialog(7,hole7A2IDcard);
                }else{
                    cardView = 7;
                    requestFocusState(mID_HoleSevenA2Et);
                }
            }
        });
        //孔6A3
        mID_HoleSixA3Et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isInput){
                    String hole6A3IDcard = mID_HoleSixA3Et.getText().toString();
                    showInputNumDialog(6,hole6A3IDcard);
                }else{
                    cardView = 6;
                    requestFocusState(mID_HoleSixA3Et);
                }
            }
        });
        //孔5A4
        mID_HoleFiveA4Et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isInput){
                    String hole5A4IDcard = mID_HoleFiveA4Et.getText().toString();
                    showInputNumDialog(5,hole5A4IDcard);
                }else{
                    cardView = 5;
                    requestFocusState(mID_HoleFiveA4Et);
                }
            }
        });

        //孔4A5
        mID_HoleFourA5Et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isInput){
                    String hole4A5IDcard = mID_HoleFourA5Et.getText().toString();
                    showInputNumDialog(4,hole4A5IDcard);
                }else{
                    cardView = 4;
                    requestFocusState(mID_HoleFourA5Et);
                }
            }
        });
        //孔3A6
        mID_HoleThreeA6Et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isInput){
                    String hole3A6IDcard = mID_HoleThreeA6Et.getText().toString();
                    showInputNumDialog(3,hole3A6IDcard);
                }else{
                    cardView = 3;
                    requestFocusState(mID_HoleThreeA6Et);
                }
            }
        });
        //孔2A7
        mID_HoleTwoA7Et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isInput){
                    String hole2A7IDcard = mID_HoleTwoA7Et.getText().toString();
                    showInputNumDialog(2,hole2A7IDcard);
                }else{
                    cardView = 2;
                    requestFocusState(mID_HoleTwoA7Et);
                }
            }
        });
        //孔1A8
        mID_HoleOneA8Et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isInput){
                    String hole1A8IDcard = mID_HoleOneA8Et.getText().toString();
                    showInputNumDialog(1,hole1A8IDcard);
                }else{
                    cardView = 1;
                    requestFocusState(mID_HoleOneA8Et);
                }
            }
        });
    }

    private CustomDialog mNumBerDialog;
    private EditText editText;


    private void showInputNumDialog(int dialogType,String inputText){
        if (null != mNumBerDialog) {
            mNumBerDialog.dismiss();
            mNumBerDialog = null;
        }
        if (null == mNumBerDialog) {
            mNumBerDialog = new CustomDialog(this.getActivity());
            LayoutInflater mInflater = (LayoutInflater) this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_entry_info_layout, null);

            editText = (EditText) view.findViewById(R.id.regent_code_et);
            editText.setText(inputText);
            CustomDialog.Builder builder = new CustomDialog.Builder(this.getActivity());
            builder.setContentView(view);
            switch (dialogType){
                case 1:
                    builder.setTitle(getActivity().getResources().getString(R.string.result_idcard )+ "1");
                    break;
                case 2:
                    builder.setTitle(getActivity().getResources().getString(R.string.result_idcard) + "2");
                    break;
                case 3:
                    builder.setTitle(getActivity().getResources().getString(R.string.result_idcard) + "3");
                    break;
                case 4:
                    builder.setTitle(getActivity().getResources().getString(R.string.result_idcard) + "4");
                    break;
                case 5:
                    builder.setTitle(getActivity().getResources().getString(R.string.result_idcard )+ "5");
                    break;
                case 6:
                    builder.setTitle(getActivity().getResources().getString(R.string.result_idcard) + "6");
                    break;
                case 7:
                    builder.setTitle(getActivity().getResources().getString(R.string.result_idcard) + "7");
                    break;
                case 8:
                    builder.setTitle(getActivity().getResources().getString(R.string.result_idcard) + "8");
                    break;

                default:
                    break;

            }

            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    String text = editText.getText().toString();
                    switch (dialogType){
                        //孔1，A8
                        case 1:
                            mID_HoleOneA8Et.setText(text);
                            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                    .setValueByName(SharedConstant.CARD_CODE_EIGHT,text);
                            break;
                        //孔2，A7
                        case 2:
                            mID_HoleTwoA7Et.setText(text);
                            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                    .setValueByName(SharedConstant.CARD_CODE_SEVEN,text);
                            break;
                        //孔3，A6
                        case 3:
                            mID_HoleThreeA6Et.setText(text);
                            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                    .setValueByName(SharedConstant.CARD_CODE_SIX,text);
                            break;
                        //孔4，A5
                        case 4:
                            mID_HoleFourA5Et.setText(text);
                            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                    .setValueByName(SharedConstant.CARD_CODE_FIVE,text);
                            break;
                        //孔5，A4
                        case 5:
                            mID_HoleFiveA4Et.setText(text);
                            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                    .setValueByName(SharedConstant.CARD_CODE_FOUR,text);
                            break;
                        //孔6，A3
                        case 6:
                            mID_HoleSixA3Et.setText(text);
                            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                    .setValueByName(SharedConstant.CARD_CODE_THREE,text);
                            break;
                        //孔7，A2
                        case 7:
                            mID_HoleSevenA2Et.setText(text);
                            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                    .setValueByName(SharedConstant.CARD_CODE_TWO,text);
                            break;
                        //孔8，A1
                        case 8:
                            mID_HoleEightA1Et.setText(text);
                            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                    .setValueByName(SharedConstant.CARD_CODE_ONE,text);
                            break;


                        default:
                            break;
                    }
                    mNumBerDialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    mNumBerDialog.dismiss();

                }
            });

            mNumBerDialog = builder.create();
            mNumBerDialog.show();
        }
    }



    private void initVariable() {
        mTemplateNameTv.setText(PcrApplication.rootTempName);
        wellInfos = new LinkedList<>();
        if(isInput){
            manualModeRb.setChecked(true);
            scanModeRb.setChecked(false);
        }else {
            manualModeRb.setChecked(false);
            scanModeRb.setChecked(true);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
        scanKeyManager = new ScanKeyManager(new ScanKeyManager.OnScanValueListener() {
            @Override
            public void onScanValue(String value) {
                if(null != currentView && !isInput){
                    if(!StringUtil.isEmpty(value)) {
                        currentView.setText(value);
                        //保存
                        switch (cardView){
                            //孔1，A8
                            case 1:
                                PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                        .setValueByName(SharedConstant.CARD_CODE_EIGHT,value);
                                break;
                            //孔2，A7
                            case 2:
                                PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                        .setValueByName(SharedConstant.CARD_CODE_SEVEN,value);
                                break;
                            //孔3，A6
                            case 3:
                                PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                        .setValueByName(SharedConstant.CARD_CODE_SIX,value);
                                break;
                            //孔4，A5
                            case 4:
                                PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                        .setValueByName(SharedConstant.CARD_CODE_FIVE,value);
                                break;
                            //孔5，A4
                            case 5:
                                PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                        .setValueByName(SharedConstant.CARD_CODE_FOUR,value);
                                break;
                            //孔6，A3
                            case 6:
                                PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                        .setValueByName(SharedConstant.CARD_CODE_THREE,value);
                                break;
                            //孔7，A2
                            case 7:
                                PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                        .setValueByName(SharedConstant.CARD_CODE_TWO,value);
                                break;
                            //孔8，A1
                            case 8:
                                PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                        .setValueByName(SharedConstant.CARD_CODE_ONE,value);
                                break;
                            }

                    }
                }
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        onResumeValue();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterEventBus();
        if (null != wellInfos) {
            wellInfos.clear();
            wellInfos = null;
        }
        if(null != mNumBerDialog){
            mNumBerDialog.dismiss();
            mNumBerDialog = null;
        }
        slanActivity = null;
        scanKeyManager = null;
    }

    /*
     * EventBus接收订阅的消息
     * @param
     */
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()) {
            case MsgType.MSG_OPEN_DOOR:
                //打开门
                openDoor();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {
        Log.e("RNA","正在打开操作门结束" + procedureFinishEvent.getTemplateName());
        if(procedureFinishEvent.getTemplateName().contains("opendoor")){
            if(procedureFinishEvent.getStatus().equals("finish")){
                PcrApplication.rNARunningMessage.setStatus("free");
                isOpenningDoor = false;
            }else if(procedureFinishEvent.getStatus().equals("finish error")){
                isOpenningDoor = true;
                showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_open_check_device),
                        getActivity().getResources().getString(R.string.dialog_message_open_tray_close));
                openDoorError("E11 " + getActivity().getResources().getString(R.string.dialog_message_open_tray_close));
            }
            stopRNARunning();
        }
    }

    private void openDoor(){
        Log.e("RNA","正在打开操作门");
        if (PcrApplication.is_RNA_connect) {
            ////异步全流程
            String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "96-opendoor.hex";
            File file = new File(downandopen_filePath);
            boolean result = startAysnTask(file);
            if (result) {
                isOpenningDoor = true;
            } else {
                isOpenningDoor = true;
                showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_prompt),
                        getActivity().getResources().getString(R.string.dialog_message_open_error));
            }
        } else {
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.toast_rna_serial_disconnect),Toast.LENGTH_LONG).show();
        }
    }


    private void openDoorError(String message){
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setMsgType(MsgType.MSG_SHOW_ALARM_MESSAGE);
        messageEvent.setData(message);
        EventBus.getDefault().post(messageEvent);
    }

    /*
     * 超时消息
     * @param sendCmdTimeoutEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SendCmdTimeoutEvent sendCmdTimeoutEvent) {
        stopRNARunning();
    }



    private void stopRNARunning(){
        Log.i("RNA", "结束开舱门操作");
        if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
            RNAAndPCRAsyncTask.getInstance().setIsinteruppt(true);
            RNAAndPCRAsyncTask.getInstance().setmIsrunning(false);
        }
    }



    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.detect_entry_info_clear_all_btn) {
            clearAllInfos();
        } else if (id == R.id.detect_entry_info_next_btn) {
            if(!isA1Check && !isA2Check && !isA3Check && !isA4Check
            && !isA5Check && !isA6Check && !isA7Check && !isA8Check){
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.entry_info_toast_select_hole),Toast.LENGTH_LONG).show();
                return;
            }else{
                saveDetectInfos();
            }

        } else if (id == R.id.detect_entry_info_one_idcard_clear_img) {
            mID_HoleEightA1Et.setText("");
        } else if (id == R.id.detect_entry_info_two_idcard_clear_img) {
            mID_HoleSevenA2Et.setText("");
        } else if (id == R.id.detect_entry_info_three_idcard_clear_img) {
            mID_HoleSixA3Et.setText("");
        } else if (id == R.id.detect_entry_info_four_idcard_clear_img) {
            mID_HoleFiveA4Et.setText("");
        } else if (id == R.id.detect_entry_info_five_idcard_clear_img) {
            mID_HoleFourA5Et.setText("");
        } else if (id == R.id.detect_entry_info_six_idcard_clear_img) {
            mID_HoleThreeA6Et.setText("");
        } else if (id == R.id.detect_entry_info_seven_idcard_clear_img) {
            mID_HoleTwoA7Et.setText("");
        } else if (id == R.id.detect_entry_info_eight_idcard_clear_img) {
            mID_HoleOneA8Et.setText("");
        }else if(id == R.id.detect_entry_info_hole_one_btn){
            isA1Check = !isA1Check;
            setHoleState(isA1Check,(ImageView) v);
        }else if(id == R.id.detect_entry_info_hole_two_btn){
            isA2Check = !isA2Check;
            setHoleState(isA2Check,(ImageView)v);
        }else if(id == R.id.detect_entry_info_hole_three_btn){
            isA3Check = !isA3Check;
            setHoleState(isA3Check,(ImageView)v);
        }else if(id == R.id.detect_entry_info_hole_four_btn){
            isA4Check = !isA4Check;
            setHoleState(isA4Check,(ImageView)v);
        }else if(id == R.id.detect_entry_info_hole_five_btn){
            isA5Check = !isA5Check;
            setHoleState(isA5Check,(ImageView)v);
        }else if(id == R.id.detect_entry_info_hole_six_btn){
            isA6Check = !isA6Check;
            setHoleState(isA6Check,(ImageView)v);
        }else if(id == R.id.detect_entry_info_hole_seven_btn){
            isA7Check = !isA7Check;
            setHoleState(isA7Check,(ImageView)v);
        }else if(id == R.id.detect_entry_info_hole_eight_btn){
            isA8Check = !isA8Check;
            setHoleState(isA8Check,(ImageView)v);
        }else if(id == R.id.detect_entry_info_filling_all_btn){
            fillAllInfo();
        }/*else if(id == R.id.detect_info_check_layout){

            if(!isHintCk){
                isHintCk = true;
                mNextBtn.setEnabled(true);
                mHintCkImg.setImageResource(R.drawable.icon_hint_check_full);
                mNextBtn.setBackgroundResource(R.drawable.oval_next_btn_normal_bg);
                mNextBtn.setTextColor(getActivity().getColor(R.color.white));
            }else{
                isHintCk = false;
                mNextBtn.setEnabled(false);

                mHintCkImg.setImageResource(R.drawable.icon_hint_uncheck);
                mNextBtn.setBackgroundResource(R.drawable.oval_next_btn_pressed_bg);
                mNextBtn.setTextColor(getActivity().getColor(R.color.blue));
            }

        }*/
    }


    private void setHoleState(boolean isCheck,ImageView v){
        if(isCheck){
            v.setImageResource(R.drawable.icon_info_check);
        }else {
            v.setImageResource(R.drawable.icon_info_uncheck);
        }
    }


    private void clearFocusState(TextView v){
        v.setBackgroundResource(0);
        v.setTextColor(getActivity().getResources().getColor(R.color.blue));
    }

    private void requestFocusState(TextView v){
        if(currentView != null){
            clearFocusState(currentView);
        }
       /* v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        v.requestFocus();*/
        v.setBackgroundResource(R.drawable.shape_rect_blue_bg);
        v.setTextColor(getActivity().getResources().getColor(R.color.white));
        this.currentView = (TextView) v;
    }

    private void clearAllFocus(){
        mID_HoleEightA1Et.setBackgroundResource(0);
        mID_HoleEightA1Et.setTextColor(getActivity().getResources().getColor(R.color.blue));
        mID_HoleSevenA2Et.setBackgroundResource(0);
        mID_HoleSevenA2Et.setTextColor(getActivity().getResources().getColor(R.color.blue));
        mID_HoleSixA3Et.setBackgroundResource(0);
        mID_HoleSixA3Et.setTextColor(getActivity().getResources().getColor(R.color.blue));
        mID_HoleFiveA4Et.setBackgroundResource(0);
        mID_HoleFiveA4Et.setTextColor(getActivity().getResources().getColor(R.color.blue));
        mID_HoleFourA5Et.setBackgroundResource(0);
        mID_HoleFourA5Et.setTextColor(getActivity().getResources().getColor(R.color.blue));
        mID_HoleThreeA6Et.setBackgroundResource(0);
        mID_HoleThreeA6Et.setTextColor(getActivity().getResources().getColor(R.color.blue));

        mID_HoleTwoA7Et.setBackgroundResource(0);
        mID_HoleTwoA7Et.setTextColor(getActivity().getResources().getColor(R.color.blue));
        mID_HoleOneA8Et.setBackgroundResource(0);
        mID_HoleOneA8Et.setTextColor(getActivity().getResources().getColor(R.color.blue));
    }


    /**
     * 一键清空
     */
    private void clearAllInfos() {
        mID_HoleEightA1Et.setText("");
        mID_HoleSevenA2Et.setText("");
        mID_HoleSixA3Et.setText("");
        mID_HoleFiveA4Et.setText("");
        mID_HoleFourA5Et.setText("");
        mID_HoleThreeA6Et.setText("");
        mID_HoleTwoA7Et.setText("");
        mID_HoleOneA8Et.setText("");

    }

    private void fillAllInfo(){
        String hole_eight_A1_ID = mID_HoleEightA1Et.getText().toString();
        String hole_seven_A2_ID = mID_HoleSevenA2Et.getText().toString();
        String hole_six_A3_ID = mID_HoleSixA3Et.getText().toString();
        String hole_five_A4_ID = mID_HoleFiveA4Et.getText().toString();
        String hole_four_A5_ID = mID_HoleFourA5Et.getText().toString();
        String hole_three_A6_ID = mID_HoleThreeA6Et.getText().toString();
        String hole_two_A7_ID = mID_HoleTwoA7Et.getText().toString();
        String hole_one_A8_ID = mID_HoleOneA8Et.getText().toString();
        if(StringUtil.isEmpty(hole_one_A8_ID)){
            Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.entry_info_toast_hole_one_idcard_no_empty),Toast.LENGTH_LONG)
                    .show();
            return;
        }else{
            if(!StringUtil.isEmpty(hole_seven_A2_ID) && !StringUtil.isEmpty(hole_six_A3_ID)
                    &&!StringUtil.isEmpty(hole_five_A4_ID)
                    && !StringUtil.isEmpty(hole_four_A5_ID) && !StringUtil.isEmpty(hole_three_A6_ID)
                    &&!StringUtil.isEmpty(hole_two_A7_ID) && !StringUtil.isEmpty(hole_eight_A1_ID)){
                Toast.makeText(getActivity().getApplicationContext()
                                ,getActivity().getResources().getString(R.string.entry_info_toast_no_auto_filling),Toast.LENGTH_LONG)
                        .show();
                return;
            }else{
                if(StringUtil.isEmpty(hole_seven_A2_ID) && StringUtil.isEmpty(hole_six_A3_ID)
                        &&StringUtil.isEmpty(hole_five_A4_ID)
                        && StringUtil.isEmpty(hole_four_A5_ID) && StringUtil.isEmpty(hole_three_A6_ID)
                        && StringUtil.isEmpty(hole_two_A7_ID) && StringUtil.isEmpty(hole_eight_A1_ID)){
                    //进行根据第一个的填充
                    //取后两位
                    if(hole_one_A8_ID.length()>=2){
                        String firstStart = hole_one_A8_ID.toString().substring(0,hole_one_A8_ID.length()-2);
                        String firstEnd = hole_one_A8_ID.toString().substring(hole_one_A8_ID.length()-2,hole_one_A8_ID.length());//取后2位
                        if(firstEnd.matches("\\d+")){
                            int start = Integer.parseInt(firstEnd);
                            mID_HoleEightA1Et.setText(firstStart+String.format("%02d", start+7));
                            mID_HoleSevenA2Et.setText(firstStart+String.format("%02d", start+6));
                            mID_HoleSixA3Et.setText(firstStart+String.format("%02d", start+5));
                            mID_HoleFiveA4Et.setText(firstStart+String.format("%02d", start+4));
                            mID_HoleFourA5Et.setText(firstStart+String.format("%02d", start+3));
                            mID_HoleThreeA6Et.setText(firstStart+String.format("%02d", start+2));
                            mID_HoleTwoA7Et.setText(firstStart+String.format("%02d", start+1));
                        }else{
                            Toast.makeText(getActivity().getApplicationContext(),"使用一键填充后两位必须是数字"
                                    ,Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(getActivity().getApplicationContext(),getActivity().getResources().getString(R.string.entry_info_toast_sample_too_short)
                                ,Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.entry_info_toast_clear_all_fill),Toast.LENGTH_LONG)
                            .show();
                }
            }
        }
    }

    private void onResumeValue(){
        reagentCode = PreferenceUtil.getInstance(getActivity().getApplicationContext()).
                getStringValue(SharedConstant.REAGENT_CODE,"");


        String card_id_hole8A1 = PreferenceUtil.getInstance(getActivity().getApplicationContext()).
                getStringValue(SharedConstant.CARD_CODE_ONE,"0008");
        String card_id_hole7A2 = PreferenceUtil.getInstance(getActivity().getApplicationContext()).
                getStringValue(SharedConstant.CARD_CODE_TWO,"0007");
        String card_id_hole6A3 = PreferenceUtil.getInstance(getActivity().getApplicationContext()).
                getStringValue(SharedConstant.CARD_CODE_THREE,"0006");
        String card_id_hole5A4= PreferenceUtil.getInstance(getActivity().getApplicationContext()).
                getStringValue(SharedConstant.CARD_CODE_FOUR,"0005");
        String card_id_hole4A5 = PreferenceUtil.getInstance(getActivity().getApplicationContext()).
                getStringValue(SharedConstant.CARD_CODE_FIVE,"0004");
        String card_id_hole3A6 = PreferenceUtil.getInstance(getActivity().getApplicationContext()).
                getStringValue(SharedConstant.CARD_CODE_SIX,"0003");
        String card_id_hole2A7 = PreferenceUtil.getInstance(getActivity().getApplicationContext()).
                getStringValue(SharedConstant.CARD_CODE_SEVEN,"0002");
        String card_id_hole1A8 = PreferenceUtil.getInstance(getActivity().getApplicationContext()).
                getStringValue(SharedConstant.CARD_CODE_EIGHT,"0001");//孔1A8

        mID_HoleEightA1Et.setText(card_id_hole8A1);
        mID_HoleSevenA2Et.setText(card_id_hole7A2);
        mID_HoleSixA3Et.setText(card_id_hole6A3);
        mID_HoleFiveA4Et.setText(card_id_hole5A4);
        mID_HoleFourA5Et.setText(card_id_hole4A5);
        mID_HoleThreeA6Et.setText(card_id_hole3A6);
        mID_HoleTwoA7Et.setText(card_id_hole2A7);
        mID_HoleOneA8Et.setText(card_id_hole1A8);

    }



    private void saveDetectInfos() {
        if(isOpenningDoor){
            showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_warning),
                    getActivity().getResources().getString(R.string.entry_info_dialog_cannot_opendoor));
            return;
        }

        if(!PcrApplication.is_RNA_connect||!PcrApplication.is_SLAN_PCR_connect ){
            showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_warning),
                    getActivity().getResources().getString(R.string.dialog_message_equipment_module_problem));
            return;
        }

        String hole_eight_A1_ID = mID_HoleEightA1Et.getText().toString().trim();
        String hole_seven_A2_ID = mID_HoleSevenA2Et.getText().toString().trim();
        String hole_six_A3_ID = mID_HoleSixA3Et.getText().toString().trim();
        String hole_five_A4_ID = mID_HoleFiveA4Et.getText().toString().trim();
        String hole_four_A5_ID = mID_HoleFourA5Et.getText().toString().trim();
        String hole_three_A6_ID = mID_HoleThreeA6Et.getText().toString().trim();
        String hole_two_A7_ID = mID_HoleTwoA7Et.getText().toString().trim();
        String hole_one_A8_ID = mID_HoleOneA8Et.getText().toString().trim();//孔1A8
        reagentCode = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.REAGENT_CODE,"");

        if(StringUtil.isEmpty(hole_eight_A1_ID)){
//            Toast.makeText(getActivity().getApplicationContext()
//                    ,getActivity().getResources().getString(R.string.toast_hole_one_id_null)
//            ,Toast.LENGTH_LONG).show();
            hole_eight_A1_ID = "8";
//            return;
        }else if(StringUtil.isEmpty(hole_seven_A2_ID)){
//            Toast.makeText(getActivity().getApplicationContext()
//                    ,getActivity().getResources().getString(R.string.toast_hole_two_id_null)
//                    ,Toast.LENGTH_LONG).show();
            hole_seven_A2_ID = "7";
//            return;
        }else if(StringUtil.isEmpty(hole_six_A3_ID)){
//            Toast.makeText(getActivity().getApplicationContext()
//                    ,getActivity().getResources().getString(R.string.toast_hole_three_id_null)
//                    ,Toast.LENGTH_LONG).show();
            hole_six_A3_ID = "6";
//            return;
        }else if(StringUtil.isEmpty(hole_five_A4_ID)){
//            Toast.makeText(getActivity().getApplicationContext()
//                    ,getActivity().getResources().getString(R.string.toast_hole_four_id_null)
//                    ,Toast.LENGTH_LONG).show();
            hole_five_A4_ID = "5";
//            return;
        }else if(StringUtil.isEmpty(hole_four_A5_ID)){
//            Toast.makeText(getActivity().getApplicationContext()
//                    , getActivity().getResources().getString(R.string.toast_hole_five_id_null)
//                    ,Toast.LENGTH_LONG).show();
            hole_four_A5_ID = "4";
//            return;
        }else if(StringUtil.isEmpty(hole_three_A6_ID)){
//            Toast.makeText(getActivity().getApplicationContext()
//                    ,getActivity().getResources().getString(R.string.toast_hole_six_id_null)
//                    ,Toast.LENGTH_LONG).show();
            hole_three_A6_ID = "3";
//            return;
        }else if(StringUtil.isEmpty(hole_two_A7_ID)){
//            Toast.makeText(getActivity().getApplicationContext()
//                    ,getActivity().getResources().getString(R.string.toast_hole_seven_id_null)
//                    ,Toast.LENGTH_LONG).show();
            hole_two_A7_ID = "2";
//            return;
        }else if(StringUtil.isEmpty(hole_one_A8_ID)) {
//            Toast.makeText(getActivity().getApplicationContext()
//                    ,getActivity().getResources().getString(R.string.toast_hole_eight_id_null)
//                    ,Toast.LENGTH_LONG).show();
            hole_one_A8_ID = "1";
//            return;
        }
        /*//判断的条码不能相同
        if(isA1Check){
            if(isA2Check){
                if(hole_one_ID.equals(hole_two_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                    ,getActivity().getResources().getString(R.string.entry_info_toast_hole_one_idcard_no_equal_hole_two),Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA3Check){
                if(hole_one_ID.equals(hole_three_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_one_idcard_no_equal_hole_three),Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA4Check){
                if(hole_one_ID.equals(hole_four_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_one_idcard_no_equal_hole_four),Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA5Check){
                if(hole_one_ID.equals(hole_five_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_one_idcard_no_equal_hole_five),Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA6Check){
                if(hole_one_ID.equals(hole_six_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_one_idcard_no_equal_hole_six),Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA7Check){
                if(hole_one_ID.equals(hole_seven_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_one_idcard_no_equal_hole_seven),Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA8Check){
                if(hole_one_ID.equals(hole_eight_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_one_idcard_no_equal_hole_eight),Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
        //
        if(isA2Check){
            if(isA3Check){
                if(hole_two_ID.equals(hole_three_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_two_idcard_no_equal_hole_three)
                            ,Toast.LENGTH_LONG).show();
                }
            }
            if(isA4Check){
                if(hole_two_ID.equals(hole_four_ID)){
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.entry_info_toast_hole_two_idcard_no_equal_hole_four)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA5Check){
                if(hole_two_ID.equals(hole_five_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_two_idcard_no_equal_hole_five),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA6Check){
                if(hole_two_ID.equals(hole_six_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_two_idcard_no_equal_hole_six)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA7Check){
                if(hole_two_ID.equals(hole_seven_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_two_idcard_no_equal_hole_seven)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA8Check){
                if(hole_two_ID.equals(hole_eight_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_two_idcard_no_equal_hole_eight)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        if(isA3Check){
            if(isA4Check){
                if(hole_three_ID.equals(hole_four_ID)){
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.entry_info_toast_hole_three_idcard_no_equal_hole_four)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA5Check){
                if(hole_three_ID.equals(hole_five_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_three_idcard_no_equal_hole_five)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA6Check){
                if(hole_three_ID.equals(hole_six_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_three_idcard_no_equal_hole_six)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA7Check){
                if(hole_three_ID.equals(hole_seven_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_three_idcard_no_equal_hole_seven)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA8Check){
                if(hole_three_ID.equals(hole_eight_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_three_idcard_no_equal_hole_eight)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
        //4
        if(isA4Check){
            if(isA5Check){
                if(hole_four_ID.equals(hole_five_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_four_idcard_no_equal_hole_five)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA6Check){
                if(hole_four_ID.equals(hole_six_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_four_idcard_no_equal_hole_six)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA7Check){
                if(hole_four_ID.equals(hole_seven_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_four_idcard_no_equal_hole_seven)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA8Check){
                if(hole_four_ID.equals(hole_eight_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_four_idcard_no_equal_hole_eight)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
        //5
        if(isA4Check){
            if(isA6Check){
                if(hole_five_ID.equals(hole_six_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_five_idcard_no_equal_hole_six)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA7Check){
                if(hole_five_ID.equals(hole_seven_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_five_idcard_no_equal_hole_seven)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA8Check){
                if(hole_five_ID.equals(hole_eight_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_five_idcard_no_equal_hole_eight)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        //6
        if(isA6Check){
            if(isA7Check){
                if(hole_six_ID.equals(hole_seven_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_six_idcard_no_equal_hole_seven)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
            if(isA8Check){
                if(hole_six_ID.equals(hole_eight_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_six_idcard_no_equal_hole_eight)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        //6
        if(isA7Check){
            if(isA8Check){
                if(hole_seven_ID.equals(hole_eight_ID)){
                    Toast.makeText(getActivity().getApplicationContext()
                            ,getActivity().getResources().getString(R.string.entry_info_toast_hole_seven_idcard_no_equal_hole_eight)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }*/

        PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .setValueByName(SharedConstant.CARD_CODE_ONE,hole_eight_A1_ID);
        PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .setValueByName(SharedConstant.CARD_CODE_TWO,hole_seven_A2_ID);
        PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .setValueByName(SharedConstant.CARD_CODE_THREE,hole_six_A3_ID);
        PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .setValueByName(SharedConstant.CARD_CODE_FOUR,hole_five_A4_ID);

        PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .setValueByName(SharedConstant.CARD_CODE_FIVE,hole_four_A5_ID);
        PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .setValueByName(SharedConstant.CARD_CODE_SIX,hole_three_A6_ID);//孔3A6
        PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .setValueByName(SharedConstant.CARD_CODE_SEVEN,hole_two_A7_ID);//孔2A7
        PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .setValueByName(SharedConstant.CARD_CODE_EIGHT,hole_one_A8_ID);//孔1A8

/*        WellInfo oneWellInfo = new WellInfo("A1", hole_one_ID, reagentCode);
        WellInfo twoWellInfo = new WellInfo("A2", hole_two_ID, reagentCode);
        WellInfo threeWellInfo = new WellInfo("A3", hole_three_ID, reagentCode);
        WellInfo fourWellInfo = new WellInfo("A4", hole_four_ID, reagentCode);
        WellInfo fiveWellInfo = new WellInfo("A5", hole_five_ID, reagentCode);
        WellInfo sixWellInfo = new WellInfo("A6", hole_six_ID, reagentCode);
        WellInfo sevenWellInfo = new WellInfo("A7", hole_seven_ID, reagentCode);
        WellInfo eightWellInfo = new WellInfo("A8", hole_eight_ID, reagentCode);*/

        /**
         * 注明：
         * hole_eight_A1_ID 来自于 mID_HoleOneEt 对应的是孔8-A1而不是孔1
         */
        WellInfo hole1_A8WellInfo = new WellInfo("A8",hole_one_A8_ID,reagentCode);
        WellInfo hole2_A7WellInfo = new WellInfo("A7",hole_two_A7_ID,reagentCode);
        WellInfo hole3_A6WellInfo = new WellInfo("A6",hole_three_A6_ID,reagentCode);
        WellInfo hole4_A5WellInfo = new WellInfo("A5",hole_four_A5_ID,reagentCode);
        WellInfo hole5_A4WellInfo = new WellInfo("A4",hole_five_A4_ID,reagentCode);
        WellInfo hole6_A3WellInfo = new WellInfo("A3",hole_six_A3_ID,reagentCode);
        WellInfo hole7_A2WellInfo = new WellInfo("A2",hole_seven_A2_ID,reagentCode);
        WellInfo hole8_A1WellInfo = new WellInfo("A1",hole_eight_A1_ID,reagentCode);

        if(null != wellInfos){
            wellInfos.clear();
            if(isA1Check){//对应孔8-A1
                wellInfos.add(hole8_A1WellInfo);
            }
            if(isA2Check){//对应孔7-A2
                wellInfos.add(hole7_A2WellInfo);
            }
            if(isA3Check){//对应孔6-A3
                wellInfos.add(hole6_A3WellInfo);
            }
            if(isA4Check){//对应孔5-A4
                wellInfos.add(hole5_A4WellInfo);
            }
            if(isA5Check){//对应孔4-A5
                wellInfos.add(hole4_A5WellInfo);
            }
            if(isA6Check){//对应孔3-A6
                wellInfos.add(hole3_A6WellInfo);
            }
            if(isA7Check){//对应孔2-A7
                wellInfos.add(hole2_A7WellInfo);
            }
            if(isA8Check){//对应孔1-A8
                wellInfos.add(hole1_A8WellInfo);
            }
        }


        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setMsgType(MsgType.MSG_ENTRY_INFO_DONE);
        messageEvent.setData(wellInfos);
        EventBus.getDefault().post(messageEvent);

    }



    private void showNoticeDialog(String title, String message){
        if(null != mNoticeDialog){
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }
        if(null == mNoticeDialog){
            mNoticeDialog = new WarnDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_common_hint_layout, null);
            TextView mHintTxt = (TextView)view.findViewById(R.id.common_dialog_hint_txt);
            mHintTxt.setText(message);
            WarnDialog.Builder builder = new WarnDialog.Builder(getActivity());
            builder.setContentView(view);
            builder.setTitle(title);
            builder.setSingleButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    mNoticeDialog.dismiss();
                    mNoticeDialog = null;
                }
            });

            mNoticeDialog = builder.create();
            mNoticeDialog.show();
        }
    }


    private BiowayActivity slanActivity;
    private ScanKeyManager scanKeyManager;
    /*是否是输入状态（输入时扫码监听不拦截）*/
    private boolean isInput = false;//默认是扫描的

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        slanActivity = (BiowayActivity)activity;
        slanActivity.setFragmentKeyeventListener(this);
    }

    @Override
    public boolean onFragmentKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK && !isInput) {
            scanKeyManager.analysisKeyEvent(event);
            return true;
        }
        return false;
    }

}

