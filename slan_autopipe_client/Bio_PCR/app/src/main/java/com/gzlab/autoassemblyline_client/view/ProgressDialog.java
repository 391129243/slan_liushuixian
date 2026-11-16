package com.gzlab.autoassemblyline_client.view;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gzlab.autoassemblyline_client.R;

public class ProgressDialog extends Dialog {
    private Context context;
    private TextView titleTv;
    private ProgressBar progressBar;
    private TextView cancelTv;
    private TextView nameTv;
    private TextView progressTv;

    private TextView destTv;
    private String title;

    public ProgressDialog(@NonNull Context context,String title) {
        super(context, R.style.loading_dialog);
        this.context = context;
        this.title = title;
        initView();
    }

    private void initView(){
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_progress_layout, null);
        titleTv = (TextView)view.findViewById(R.id.dialog_copy_title_tv);
        progressBar = (ProgressBar) view.findViewById(R.id.dialog_progress_bar);
        progressBar.setMax(100);
        destTv = (TextView)view.findViewById(R.id.dialog_copy_dest_tv) ;
        cancelTv = (TextView) view.findViewById(R.id.dialog_cancel_tv);
        nameTv = (TextView) view.findViewById(R.id.file_name_tv);
        progressTv = (TextView) view.findViewById(R.id.copy_file_progress_tv);
        titleTv.setText(title);
        super.setContentView(view);
    }

    public void dismiss() {
        // TODO Auto-generated method stub
        View view = getCurrentFocus();
        if(view instanceof TextView){
            InputMethodManager mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }

        super.dismiss();

    }
    public void setFileName(String name){
        nameTv.setText(name);
    }

    public void setDestination(String destination){
        destTv.setText(destination);
    }

    public void setOnCanlceListener(View.OnClickListener listener) {
        cancelTv.setOnClickListener(listener);
    }

    public void setProgress(int progress){
        progressTv.setText(progress + "%");
        progressBar.setProgress(progress);
    }

}
