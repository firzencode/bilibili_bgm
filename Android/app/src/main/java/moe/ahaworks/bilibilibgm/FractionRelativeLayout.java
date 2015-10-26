package moe.ahaworks.bilibilibgm;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;


/**
 * This class is used by fragment, and do animation with fraction
 */
public class FractionRelativeLayout extends RelativeLayout {
    public FractionRelativeLayout(Context context) {
        super(context);
    }

    public FractionRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FractionRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setYFraction(final float fraction) {
        float translationY = getHeight() * fraction;
        setTranslationY(translationY);
    }

    public float getYFraction() {
        if (getHeight() == 0) {
            return 0;
        }
        return getTranslationY() / getHeight();
    }
}
