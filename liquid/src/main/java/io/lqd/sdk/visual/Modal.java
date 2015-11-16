package io.lqd.sdk.visual;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.lang.reflect.Method;

import io.lqd.sdk.LQLog;
import io.lqd.sdk.Liquid;
import io.lqd.sdk.R;
import io.lqd.sdk.model.LQInAppMessage;

public class Modal{

    private LQInAppMessage mModalModel;
    private PopupWindow mPopupWindow;
    private ViewGroup container;
    private View mRoot;
    private TextView mViewMessage;
    private Context mContext;

    public Modal(Context context, View root, LQInAppMessage modalModel) {
        mRoot = root;
        mModalModel = modalModel;
        mContext = context;

        setUpModal();
        setUpButtons();
    }

    public void show() {
        show(0);
    }

    public void show(final int milliseconds){
        mRoot.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPopupWindow.showAtLocation(mRoot, Gravity.CENTER, 0, 0);
            }
        }, milliseconds);
    }

    private void setUpModal() {
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        container = (ViewGroup) layoutInflater.inflate(R.layout.activity_modal, null);

        TextView mViewTitle = (TextView) container.findViewById(R.id.inAppMessage);
        mViewMessage = (TextView) container.findViewById(R.id.textView);

        Typeface HeavyLato = Typeface.createFromAsset(mContext.getAssets(), "fonts/Lato-Heavy.ttf");
        Typeface RegularLato = Typeface.createFromAsset(mContext.getAssets(), "fonts/Lato-Regular.ttf");

        // Set font
        mViewTitle.setTypeface(HeavyLato);
        mViewMessage.setTypeface(RegularLato);

        // Background Color
        ((GradientDrawable) container.findViewById(R.id.modal_linear_bg_color).getBackground()).
                    setColor(Color.parseColor(mModalModel.getBgColor()));

        // Set the text
        mViewTitle.setText(mModalModel.getTitle());
        mViewMessage.setText(mModalModel.getMessage());

        // Set Text Color
        mViewTitle.setTextColor(Color.parseColor(mModalModel.getTitleColor()));
        mViewMessage.setTextColor(Color.parseColor(mModalModel.getMessageColor()));

        mPopupWindow = new PopupWindow(container, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setAnimationStyle(R.style.FadeInOutAnimation);
        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                Liquid.getInstance().showInAppMessages();
            }
        });
    }

    private void setUpButtons() {
        Button xClose = (Button) container.findViewById(R.id.xClose);
        xClose.setTransformationMethod(null);

        LinearLayout ctasContainer = (LinearLayout) container.findViewById(R.id.modal_buttons_container);
        LinearLayout.LayoutParams layout_params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout_params.setMargins(px(), px(), px(), px());
        ctasContainer.setLayoutParams(layout_params);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // Buttons parameters
        params.weight = 1.0f;
        params.width = 0;
        params.height = Math.round(mViewMessage.getTextSize() * 2.66F) ;
        params.setMargins(Math.round(mViewMessage.getTextSize() * 0.66F), Math.round(mViewMessage.getTextSize() * 0.66F), Math.round(mViewMessage.getTextSize() * 0.66F), Math.round(mViewMessage.getTextSize() * 0.66F));

        Typeface boldLato = Typeface.createFromAsset(mContext.getAssets(), "fonts/Lato-Bold.ttf");

        for(final LQInAppMessage.Cta cta : mModalModel.getCtas()) {
            Button ctaBtn = new Button(mContext);
            ctaBtn.setBackgroundResource(R.drawable.buttonshape);
            ctaBtn.setLayoutParams(params);
            ctaBtn.setTypeface(boldLato);
            ctaBtn.setTransformationMethod(null);

            // Character Limit
            if(cta.getButtonText().length() > 14)
                ctaBtn.setText(cta.getButtonText().substring(0, 14));
            else
                ctaBtn.setText(cta.getButtonText());

            ctaBtn.setTextSize(TypedValue.COMPLEX_UNIT_PX, getButtonTextSize());
            ctaBtn.getBackground().setColorFilter(Color.parseColor(cta.getButtonColor()), PorterDuff.Mode.SRC_IN);
            ctaBtn.setTextColor(Color.parseColor(cta.getButtonTextColor()));

            ctaBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   Intent mIntent = new Intent(Intent.ACTION_VIEW);
                    if (cta.getDeepLink() != null) {
                        try {
                            mIntent.setData(Uri.parse(cta.getDeepLink()));
                            Liquid.getInstance().trackCta(cta);
                            mContext.startActivity(mIntent);
                        } catch (Exception e) {
                            LQLog.error("No activity to manage deeplink or typo in the deeplink's name!");
                        }
                    }
                    mPopupWindow.dismiss();
                }
            });
            ctasContainer.addView(ctaBtn);
        }

        // Change x button color
        xClose.setTextColor(Color.parseColor(mModalModel.getTitleColor()));

        xClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Liquid.getInstance().trackDismiss(mModalModel);
                mPopupWindow.dismiss();
            }
        });
    }

    private int px() {
        return Math.round(getRealDimensions() * 0.013F);
    }

    private float getButtonTextSize() {
        return mViewMessage.getTextSize();
    }

    private float getRealDimensions(){
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        Display display = wm.getDefaultDisplay();
        int realWidth;
        int realHeight;

        if (Build.VERSION.SDK_INT >= 17){
            // New way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        } else if (Build.VERSION.SDK_INT >= 14) {
            // Reflection for this weird in-between time
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer) mGetRawW.invoke(display);
                realHeight = (Integer) mGetRawH.invoke(display);
            } catch (Exception e) {
                realWidth = display.getWidth();
                realHeight = display.getHeight();
                Log.e("Display Info", "Couldn't use reflection to get the real display metrics.");
            }

        } else {
            // This should be close, as lower API devices should not have window navigation bars
            realWidth = display.getWidth();
            realHeight = display.getHeight();
        }

        if (realHeight < realWidth) {
            realWidth = realHeight;
        }

        if (realWidth == 480)
            realWidth = 700;
        return realWidth;
    }

}
