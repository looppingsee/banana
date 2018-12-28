package com.github.shadowsocks.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.github.shadowsocks.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import static android.content.ContentValues.TAG;


/**
 * Created by leeyu on 2018/5/28.
 */

public class BottomSheetDialog extends AppCompatDialog {

    private View view;
    private FrameLayout bottomSheet;
    private CoordinatorLayout coordinator;
    private BottomSheetBehavior mBehavior;
    private Rect r = new Rect();

    public BottomSheetDialog(Context context) {
        this(context,0);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public BottomSheetDialog(Context context, int theme) {
        super(context, getThemeResId(context, theme));
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    protected BottomSheetDialog(Context context, boolean cancelable, DialogInterface.OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();

        if (Build.VERSION.SDK_INT >= 21) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(wrapInBottomSheet(view));
    }

    private View wrapInBottomSheet(View view) {
        FrameLayout container = (FrameLayout)View.inflate(getContext(), R.layout.custom_sheet_layout,null);
        coordinator = container.findViewById(R.id.c);
        bottomSheet = (FrameLayout) container.findViewById(R.id.sub);
        mBehavior = BottomSheetBehavior.from(bottomSheet);
        mBehavior.setBottomSheetCallback(mBottomSheetCallback);
        // We treat the CoordinatorLayout as outside the dialog though it is technically inside
        coordinator.findViewById(R.id.touch_outside).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isShowing()) {
                    cancel();
                }
            }
        });
        bottomSheet.addView(view);
        return container;
    }

    private static int getThemeResId(Context context, int themeId) {
        if (themeId == 0) {
            // If the provided theme is 0, then retrieve the dialogTheme from our theme
            TypedValue outValue = new TypedValue();
            if (context.getTheme().resolveAttribute(
                    R.attr.bottomSheetDialogTheme, outValue, true)) {
                themeId = outValue.resourceId;
            } else {
                // bottomSheetDialogTheme is not provided; we default to our light theme
                themeId = R.style.Theme_Design_Light_BottomSheetDialog;
            }
        }
        return themeId;
    }


    private BottomSheetBehavior.BottomSheetCallback mBottomSheetCallback
            = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet,
                                   @BottomSheetBehavior.State int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                cancel();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    /**
     * 添加 top 距离顶部多少的时候触发收缩效果
     * @param targetLimitH int 高度限制
     */
    @SuppressWarnings("all")
    public void addSpringBackDisLimit(final int targetLimitH){
        if(coordinator == null)
            return;
        final int totalHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        coordinator.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int top = 0;
                        switch (event.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                Rect rect = new Rect();
                                bottomSheet.getGlobalVisibleRect(rect);
                                top = rect.top;
                            case MotionEvent.ACTION_MOVE:
                                // 计算相对于屏幕的 坐标
                                Log.d(TAG, "ACTION_MOVE: "+r.top);

                                bottomSheet.getGlobalVisibleRect(r);
                                break;
                            case MotionEvent.ACTION_UP:
                                int limitH;
                                if(targetLimitH < 0)
                                    limitH = totalHeight*2/3;
                                else
                                    limitH = targetLimitH;
                                if(r.top < limitH) {
                                    if (mBehavior != null)
                                        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                                }
                                break;
                        }
                        return false;
                    }
                }
        );
    }
}
