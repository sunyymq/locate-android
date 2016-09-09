package com.feifan.locate.setting;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.feifan.locate.R;
import com.feifan.locate.ToolbarActivity.IResultable;
import com.feifan.locate.utils.NumberUtils;
import com.feifan.locate.widget.ui.BaseFragment;

/**
 * 设置单一参数界面
 *
 * Created by xuchunlei on 16/9/8.
 */
public class SettingSingleFragment extends BaseFragment implements IResultable{

    public static final String EXTRA_KEY_TITLE = "title";
    public static final String EXTRA_KEY_VALUE = "value";
    public static final String EXTRA_KEY_RESULT = "result";

    private EditText mEdit;
    private String mValue; // 原始值

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting_single, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView titleV = findView(R.id.setting_single_title);
        titleV.setText(getArguments().getString(EXTRA_KEY_TITLE));

        mEdit = findView(R.id.setting_single_value);
        mValue = getArguments().getString(EXTRA_KEY_VALUE);
        mEdit.setText(mValue);
        if(NumberUtils.isNumeric(mValue)) {
            mEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
    }

    @Override
    protected int getTitleResource() {
        return R.string.setting_title_sampling;
    }

    @Override
    public Intent getResult() {
        Intent intent = new Intent();
        if(!TextUtils.isEmpty(mEdit.getText())) {
            intent.putExtra(EXTRA_KEY_RESULT, mEdit.getText().toString());
        }else {
            intent.putExtra(EXTRA_KEY_RESULT, mValue);
        }
        return intent;
    }
}