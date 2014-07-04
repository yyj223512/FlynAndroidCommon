package com.greatwall.ui;

import java.io.Serializable;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.greatwall.app.Application;

public abstract class BaseFragment extends FixedOnActivityResultBugFragment
{
    private BaseFragmentActivity mContext;
    private View                 mContextView;
    private boolean              isViewDetached = false;
    protected Handler            mHandler;
    private Dialog               dialog;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        view.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                return true;
            }
        });
        initView(savedInstanceState);
        setListener();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(getLayoutId(), container, false);
        this.mContextView = view;
        return view;
    }

    protected abstract int getLayoutId();

    protected abstract void initView(Bundle savedInstanceState);

    protected abstract void setListener();

    public void setSerializable(Serializable serializable)
    {
        Bundle bundle = getArguments();
        if (bundle == null)
            bundle = new Bundle();
        bundle.putSerializable("SERIALIZABLE", serializable);
        super.setArguments(bundle);
    }

    public Serializable getSerializable()
    {
        Bundle bundle = getArguments();
        if (bundle != null)
        {
            return bundle.getSerializable("SERIALIZABLE");
        }
        return null;
    }

    @Override
    public void setArguments(Bundle args)
    {
        throw new UnsupportedOperationException("use setSerializable(serializable) instead.");
    }

    public void refresh()
    {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.detach(this);
        ft.attach(this);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        this.mHandler = Application.getInstance().getHandler();
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        if (!(activity instanceof BaseFragmentActivity))
            throw new IllegalStateException("Activity must extends BaseFragmentActivity");

        mContext = (BaseFragmentActivity) activity;

    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        this.isViewDetached = true;

        if (null != dialog && dialog.isShowing())
        {
            dialog.dismiss();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        this.isViewDetached = false;

    }

    protected Dialog getDialog()
    {
        if (dialog == null)
        {
            throw new NullPointerException("dialog is null.");
        }

        return dialog;
    }

    public boolean isViewDetached()
    {
        return this.isViewDetached;
    }

    public BaseFragmentActivity getContext()
    {
        return this.mContext != null ? this.mContext : (BaseFragmentActivity) getActivity();
    }

    public View getContextView()
    {
        return this.mContextView != null ? this.mContextView : LayoutInflater.from(getActivity()).inflate(getLayoutId(), null);
    }

}
