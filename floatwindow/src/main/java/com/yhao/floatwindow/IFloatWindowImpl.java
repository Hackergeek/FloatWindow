package com.yhao.floatwindow;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;

/**
 * Created by yhao on 2017/12/22.
 * https://github.com/yhaolpz
 */

public class IFloatWindowImpl extends IFloatWindow {


    private FloatWindow.FloatWindowBuilder mFloatWindowBuilder;
    private FloatView mFloatView;
    private FloatLifecycle mFloatLifecycle;
    private boolean isShow;
    private boolean isInit = true;
    private ValueAnimator mAnimator;
    private TimeInterpolator mDecelerateInterpolator;
    private float downX;
    private float downY;
    private float upX;
    private float upY;
    private boolean mClick = false;
    private int mTouchSlop;


    private IFloatWindowImpl() {
    }

    IFloatWindowImpl(FloatWindow.FloatWindowBuilder floatWindowBuilder) {
        mFloatWindowBuilder = floatWindowBuilder;
        if (mFloatWindowBuilder.mMoveType == MoveType.fixed) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                mFloatView = new FloatPhone(floatWindowBuilder.mApplicationContext, mFloatWindowBuilder.mPermissionListener);
            } else {
                mFloatView = new FloatToast(floatWindowBuilder.mApplicationContext);
            }
        } else {
            mFloatView = new FloatPhone(floatWindowBuilder.mApplicationContext, mFloatWindowBuilder.mPermissionListener);
            initTouchEvent();
        }
        mFloatView.setSize(mFloatWindowBuilder.mWidth, mFloatWindowBuilder.mHeight);
        mFloatView.setGravity(mFloatWindowBuilder.gravity, mFloatWindowBuilder.xOffset, mFloatWindowBuilder.yOffset);
        mFloatView.setView(mFloatWindowBuilder.mView);
        mFloatLifecycle = new FloatLifecycle(mFloatWindowBuilder.mApplicationContext, mFloatWindowBuilder.mShow, mFloatWindowBuilder.mActivities, new LifecycleListener() {
            @Override
            public void onShow() {
                show();
            }

            @Override
            public void onHide() {
                hide();
            }

            @Override
            public void onBackToDesktop() {
                if (!mFloatWindowBuilder.mDesktopShow) {
                    hide();
                }
                if (mFloatWindowBuilder.mViewStateListener != null) {
                    mFloatWindowBuilder.mViewStateListener.onBackToDesktop();
                }
            }
        });
    }

    @Override
    public void show() {
        if (isInit) {
            mFloatView.init();
            isInit = false;
            isShow = true;
        } else {
            if (isShow) {
                return;
            }
            getView().setVisibility(View.VISIBLE);
            isShow = true;
        }
        if (mFloatWindowBuilder.mViewStateListener != null) {
            mFloatWindowBuilder.mViewStateListener.onShow();
        }
    }

    @Override
    public void hide() {
        if (isInit || !isShow) {
            return;
        }
        getView().setVisibility(View.INVISIBLE);
        isShow = false;
        if (mFloatWindowBuilder.mViewStateListener != null) {
            mFloatWindowBuilder.mViewStateListener.onHide();
        }
    }

    @Override
    public boolean isShowing() {
        return isShow;
    }

    @Override
    void dismiss() {
        mFloatView.dismiss();
        isShow = false;
        if (mFloatWindowBuilder.mViewStateListener != null) {
            mFloatWindowBuilder.mViewStateListener.onDismiss();
        }
    }

    @Override
    public void updateX(int x) {
        checkMoveType();
        mFloatWindowBuilder.xOffset = x;
        mFloatView.updateX(x);
    }

    @Override
    public void updateY(int y) {
        checkMoveType();
        mFloatWindowBuilder.yOffset = y;
        mFloatView.updateY(y);
    }

    @Override
    public void updateX(int screenType, float ratio) {
        checkMoveType();
        mFloatWindowBuilder.xOffset = (int) ((screenType == Screen.width ?
                Util.getScreenWidth(mFloatWindowBuilder.mApplicationContext) :
                Util.getScreenHeight(mFloatWindowBuilder.mApplicationContext)) * ratio);
        mFloatView.updateX(mFloatWindowBuilder.xOffset);

    }

    @Override
    public void updateY(int screenType, float ratio) {
        checkMoveType();
        mFloatWindowBuilder.yOffset = (int) ((screenType == Screen.width ?
                Util.getScreenWidth(mFloatWindowBuilder.mApplicationContext) :
                Util.getScreenHeight(mFloatWindowBuilder.mApplicationContext)) * ratio);
        mFloatView.updateY(mFloatWindowBuilder.yOffset);

    }

    @Override
    public int getX() {
        return mFloatView.getX();
    }

    @Override
    public int getY() {
        return mFloatView.getY();
    }


    @Override
    public View getView() {
        mTouchSlop = ViewConfiguration.get(mFloatWindowBuilder.mApplicationContext).getScaledTouchSlop();
        return mFloatWindowBuilder.mView;
    }


    private void checkMoveType() {
        if (mFloatWindowBuilder.mMoveType == MoveType.fixed) {
            throw new IllegalArgumentException("FloatWindow of this tag is not allowed to move!");
        }
    }


    private void initTouchEvent() {
        switch (mFloatWindowBuilder.mMoveType) {
            case MoveType.inactive:
                break;
            default:
                getView().setOnTouchListener(new View.OnTouchListener() {
                    float lastX, lastY, changeX, changeY;
                    int newX, newY;

                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {

                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                downX = event.getRawX();
                                downY = event.getRawY();
                                lastX = event.getRawX();
                                lastY = event.getRawY();
                                cancelAnimator();
                                break;
                            case MotionEvent.ACTION_MOVE:
                                changeX = event.getRawX() - lastX;
                                changeY = event.getRawY() - lastY;
                                newX = (int) (mFloatView.getX() + changeX);
                                newY = (int) (mFloatView.getY() + changeY);
                                mFloatView.updateXY(newX, newY);
                                if (mFloatWindowBuilder.mViewStateListener != null) {
                                    mFloatWindowBuilder.mViewStateListener.onPositionUpdate(newX, newY);
                                }
                                lastX = event.getRawX();
                                lastY = event.getRawY();
                                break;
                            case MotionEvent.ACTION_UP:
                                upX = event.getRawX();
                                upY = event.getRawY();
                                mClick = (Math.abs(upX - downX) > mTouchSlop) || (Math.abs(upY - downY) >
                                        mTouchSlop);
                                switch (mFloatWindowBuilder.mMoveType) {
                                    case MoveType.slide:
                                        int startX = mFloatView.getX();
                                        int endX = (startX * 2 + v.getWidth() > Util.getScreenWidth(
                                                mFloatWindowBuilder.mApplicationContext)) ?
                                                Util.getScreenWidth(mFloatWindowBuilder.mApplicationContext) - v.getWidth() - mFloatWindowBuilder.mSlideRightMargin :
                                                mFloatWindowBuilder.mSlideLeftMargin;
                                        mAnimator = ObjectAnimator.ofInt(startX, endX);
                                        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                            @Override
                                            public void onAnimationUpdate(ValueAnimator animation) {
                                                int x = (int) animation.getAnimatedValue();
                                                mFloatView.updateX(x);
                                                if (mFloatWindowBuilder.mViewStateListener != null) {
                                                    mFloatWindowBuilder.mViewStateListener.onPositionUpdate(x, (int) upY);
                                                }
                                            }
                                        });
                                        startAnimator();
                                        break;
                                    case MoveType.back:
                                        PropertyValuesHolder pvhX = PropertyValuesHolder.ofInt("x", mFloatView.getX(), mFloatWindowBuilder.xOffset);
                                        PropertyValuesHolder pvhY = PropertyValuesHolder.ofInt("y", mFloatView.getY(), mFloatWindowBuilder.yOffset);
                                        mAnimator = ObjectAnimator.ofPropertyValuesHolder(pvhX, pvhY);
                                        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                            @Override
                                            public void onAnimationUpdate(ValueAnimator animation) {
                                                int x = (int) animation.getAnimatedValue("x");
                                                int y = (int) animation.getAnimatedValue("y");
                                                mFloatView.updateXY(x, y);
                                                if (mFloatWindowBuilder.mViewStateListener != null) {
                                                    mFloatWindowBuilder.mViewStateListener.onPositionUpdate(x, y);
                                                }
                                            }
                                        });
                                        startAnimator();
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            default:
                                break;
                        }
                        return mClick;
                    }
                });
        }
    }


    private void startAnimator() {
        if (mFloatWindowBuilder.mInterpolator == null) {
            if (mDecelerateInterpolator == null) {
                mDecelerateInterpolator = new DecelerateInterpolator();
            }
            mFloatWindowBuilder.mInterpolator = mDecelerateInterpolator;
        }
        mAnimator.setInterpolator(mFloatWindowBuilder.mInterpolator);
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimator.removeAllUpdateListeners();
                mAnimator.removeAllListeners();
                mAnimator = null;
                if (mFloatWindowBuilder.mViewStateListener != null) {
                    mFloatWindowBuilder.mViewStateListener.onMoveAnimEnd();
                }
            }
        });
        mAnimator.setDuration(mFloatWindowBuilder.mDuration).start();
        if (mFloatWindowBuilder.mViewStateListener != null) {
            mFloatWindowBuilder.mViewStateListener.onMoveAnimStart();
        }
    }

    private void cancelAnimator() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
    }

}
