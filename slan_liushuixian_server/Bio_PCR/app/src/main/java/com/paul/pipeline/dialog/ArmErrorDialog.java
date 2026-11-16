package com.paul.pipeline.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;


public class ArmErrorDialog extends Dialog {
	public ArmErrorDialog(Context context) {
		super(context);
	}
	public ArmErrorDialog(Context context, int theme) {
		super(context, theme);  
	}  

	public static class Builder {  
	private Context context;
	private String title;
	private String message;
	private String positiveButtonText;
	private String negativeButtonText;
	private View contentView;
	private OnClickListener positiveButtonClickListener;
	private OnClickListener negativeButtonClickListener;

	private OnClickListener continueButtonClickListener;
	private String continueButtonText;

	private int backRes = -1;

	public Builder(Context context) {
		this.context = context;  
	}  
	

	public Builder setMessage(String message) {
		this.message = message;  
		return this;  
	}  

     /** 
      * Set the Dialog message from resource 
      *  
      *
      * @return 
      */  
	public Builder setMessage(int message) {  
		this.message = (String) context.getText(message);
		return this;  
	}  
	/** 
	 * Set the Dialog title from resource 
	 *  
	 * @param title 
	 * @return 
	 */  
	public Builder setTitle(int title) {  
		this.title = (String) context.getText(title);
		return this;  
	 }  
	
	/** 
	* Set the Dialog title from String 
	*  
	* @param title 
	* @return 
	 */  

	public Builder setTitle(String title) {
		this.title = title;  
		return this;  
	}  
 
	public Builder setContentView(View v) {
		this.contentView = v;  
		return this;  
	}

	public Builder setTitleColor(int res){
		this.backRes = res;
		return this;
	}


		/**
		 * Set the positive button resource and it's listener
		 *
		 * @return
		 */
	public Builder setResetButton(OnClickListener listener) {
//			this.positiveButtonText = (String) context.getText(positiveButtonText);
			this.positiveButtonClickListener = listener;  
			return this;  
		}  

	 public Builder setCotinueButton(OnClickListener listener) {
//		     this.continueButtonText = continueButtonText;
		     this.continueButtonClickListener = listener;
		     return this;  
	 }  
	 
	 public Builder setNegativeButton( OnClickListener listener) {
//		 	this.negativeButtonText = (String) context.getText(negativeButtonText);
		 	this.negativeButtonClickListener = listener;  
		 	return this;  
	 }

	 public ArmErrorDialog create() {
		 LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		 // instantiate the dialog with the custom Theme  
		 final ArmErrorDialog dialog = new ArmErrorDialog(context, R.style.Dialog);
		 View layout = inflater.inflate(R.layout.dialog_armerror_layout, null);

		 dialog.addContentView(layout, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		 

		 if(title != null){
			 ((TextView) layout.findViewById(R.id.title)).setText(title);
		 }else{
			 ((TextView) layout.findViewById(R.id.title)).setVisibility(View.GONE);
		 }
		 
//		 if (positiveButtonText != null) {
//			 ((TextView) layout.findViewById(R.id.reset_btn)).setText(positiveButtonText);
		 	if (null != positiveButtonClickListener ) {
			 ((TextView) layout.findViewById(R.id.reset_btn))
			 				.setOnClickListener(new View.OnClickListener() {
				 	public void onClick(View v) {
                              positiveButtonClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
		 								}  
			 						});  
		 					}  
//		  } else {
//				// if no confirm button just set the visibility to GONE
//				layout.findViewById(R.id.reset_btn).setVisibility(View.GONE);
//		  }

//		 if (continueButtonText != null) {
//			 ((TextView) layout.findViewById(R.id.continue_btn)).setText(continueButtonText);
			 if (null != continueButtonClickListener ) {
				 ((TextView) layout.findViewById(R.id.continue_btn))
						 .setOnClickListener(new View.OnClickListener() {
							 public void onClick(View v) {
								 continueButtonClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
							 }
						 });
			 }
//		 } else {
//			 // if no confirm button just set the visibility to GONE
//			 layout.findViewById(R.id.continue_btn).setVisibility(View.GONE);
//		 }
//		 // set the cancel button
//		 if (negativeButtonText != null) {
//			 ((TextView) layout.findViewById(R.id.negative_btn)).setText(negativeButtonText);
			 if (negativeButtonClickListener != null) {
				 ((TextView) layout.findViewById(R.id.negative_btn)).setOnClickListener(new View.OnClickListener() {
					 public void onClick(View v) {
						 negativeButtonClickListener.onClick(dialog,  
		 									DialogInterface.BUTTON_NEGATIVE);
		 									}  
		 							});  
		 						}  
//		 	} else {
//		 		// if no confirm button just set the visibility to GONE
//		 		layout.findViewById(R.id.negative_btn).setVisibility(View.GONE);
//		 	}
//		 					// set the content message
		 	if (null != message) {  
		 		((TextView) layout.findViewById(R.id.message)).setText(message);
		 	} else if (null != contentView) {
		 		((LinearLayout)layout.findViewById(R.id.content)).removeAllViews();
				((LinearLayout) layout.findViewById(R.id.content)).addView(contentView,
						new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		 	} 
		 						// if no message set  
		 						// add the contentView to the dialog body

		 if(backRes != -1){
			 ((TextView) layout.findViewById(R.id.title))
					 .setTextColor(context.getColor(backRes));
		 }

		 dialog.setContentView(layout);
		 	return dialog;  
	 	}  
	}

	@Override
	public void dismiss() {
		// TODO Auto-generated method stub
		View view = getCurrentFocus();
        if(view instanceof TextView){
            InputMethodManager mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
        super.dismiss();
		
	}
	
	
}
