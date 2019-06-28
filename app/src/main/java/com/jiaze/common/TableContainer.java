package com.jiaze.common;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.jiaze.autotestapp.R;

import java.util.Hashtable;

public class TableContainer {
    private Context mContext;
    private TableLayout mTableLayout;
    private ScrollView mContainerLayout;
    private Hashtable<Integer, String> mChildCtlId;
    private String mTitle;

    public TableContainer(Context context){
        mContext = context;
        mTitle = Constant.PARAMS_NAME;
        mChildCtlId = new Hashtable<Integer, String>();
        mContainerLayout = (ScrollView) LayoutInflater.from(context).inflate(R.layout.test_activity, null);
        mTableLayout = (TableLayout) mContainerLayout.findViewById(R.id.test_table_layout);
    }

    public void addParamsInput(String paramName, boolean isDefaultValue, String paramKey,
                               String value, int editTextControlId, String type){
        TableRow tableRow = new TableRow(mContext);
        TextView tvParamName = new TextView(mContext);
        tvParamName.setText(paramName + ":");
        tvParamName.setTextSize(18);
        tableRow.addView(tvParamName);

        EditText etParamInput = new EditText(mContext);
        if (isDefaultValue){
            etParamInput.setText(value);
        }else {
            etParamInput.setHint(value);
        }
        if (Constant.EnumDataType.DATA_TYPE_INT.getType().equals(type)){
            etParamInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        }else if (Constant.EnumDataType.DATA_TYPE_PHONE.getType().equals(type)){
            etParamInput.setInputType(InputType.TYPE_CLASS_PHONE);
        }
        etParamInput.setId(editTextControlId);
        etParamInput.setWidth(400);
        mChildCtlId.put(editTextControlId, paramKey);
        tableRow.addView(etParamInput);

        this.mTableLayout.addView(tableRow);
    }

    public TableRow createTableRow(){
        return new TableRow(mContext);
    }

    public Button createButton(String btnName, int btnControlId, String btnKey, TableRow tableRow){
        Button button = new Button(mContext);
        button.setText(btnName);
        button.setId(btnControlId);
        tableRow.addView(button);
        mChildCtlId.put(btnControlId, btnKey);
        return button;
    }

    public TextView creatResultTextView(String tvResultKey, int textViewControlId, TableRow tableRow){
        TextView tvTestResult = new TextView(mContext);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvTestResult.setId(textViewControlId);
        tvTestResult.setTextSize(20);
        tableRow.addView(tvTestResult ,layoutParams);
        mChildCtlId.put(textViewControlId, tvResultKey);
        return tvTestResult;
    }

    public View getChildById(int id){
        return mTableLayout.findViewById(id);
    }

    public Context getmContext() {
        return mContext;
    }

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }

    public TableLayout getmTableLayout() {
        return mTableLayout;
    }

    public void setmTableLayout(TableLayout mTableLayout) {
        this.mTableLayout = mTableLayout;
    }

    public ScrollView getmContainerLayout() {
        return mContainerLayout;
    }

    public void setmContainerLayout(ScrollView mContainerLayout) {
        this.mContainerLayout = mContainerLayout;
    }

    public Hashtable<Integer, String> getmChildCtlId() {
        return mChildCtlId;
    }

    public void setmChildCtlId(Hashtable<Integer, String> mChildCtlId) {
        this.mChildCtlId = mChildCtlId;
    }

    public String getmTitle() {
        return mTitle;
    }

    public void setmTitle(String mTitle) {
        this.mTitle = mTitle;
    }
}
