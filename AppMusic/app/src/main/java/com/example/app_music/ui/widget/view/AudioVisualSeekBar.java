package com.fhm.musicr.ui.widget.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.fhm.musicr.service.MusicPlayerRemote;
import com.fhm.musicr.util.Animation;
import com.fhm.musicr.coordinate.MCoordinate.MPoint;
import com.fhm.musicr.util.Tool;
import com.fhm.musicr.ui.widget.soundfile.CheapSoundFile;

import java.io.File;

/**
 * Created by huan on 09:05 AM 24 Jan 2018.
 */

public class AudioVisualSeekBar extends View {
    // current played time  in ms
;
    // total time
    protected int duration;

    // Controller call it to update visual seek bar
    public void setProgress(int progress) {
        if(STATE==State.VISUALIZING|| !MusicPlayerRemote.isPlaying()) {
            currentSeek = progress;
            invalidate();
        }
    }

    // Set duration
    public void setMax(int duration) {
        this.duration = duration;
    }

    public static final String TAG = "AudioVisualSeekBar";
    private double NumberFrameInAPen;
    protected boolean mInitialized;
    protected float range;
    protected float scaleFactor;
    protected float minGain;

    public AudioVisualSeekBar(Context context) {
        super(context);
        init(context);

    }

    public AudioVisualSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);

    }

    public AudioVisualSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private float amount_hide_paint = 0.25f;
    private float amount_translucent_hide_paint = 0.15f;

    private void init(Context context) {
        // do the initialization here
        oneDp = Tool.getOneDps(getContext());
        strokeWidthPen = 1.75f * oneDp;
        distancePen = 1f*oneDp;
        mFileName = "";
        updateState(Command.BEGIN);
        mHandler = new Handler();
        mHandler.postDelayed(mTimerRunnable, (long) nextDelayedTime);
        mActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mActivePaint.setStyle(Paint.Style.FILL);
        mActivePaint.setStrokeWidth((float) strokeWidthPen);
        mTranslucentActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTranslucentActivePaint.setStyle(Paint.Style.FILL);
        mTranslucentActivePaint.setStrokeWidth(strokeWidthPen);

        mHidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHidePaint.setStyle(Paint.Style.FILL);
        mHidePaint.setStrokeWidth(strokeWidthPen);

        mTranslucentHidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTranslucentHidePaint.setStyle(Paint.Style.FILL);
        mTranslucentHidePaint.setStrokeWidth(strokeWidthPen);
   //     initDrawSetSize(getMeasuredWidth(),getMeasuredHeight());

        setGesture(context);
        reformatNothing();
        reformatPreparing();
        reformatSwitching();
        reformatVisualizing();
    }

    protected boolean mIsTouchDragging;
    protected int mOffset;
    protected int mOffsetGoal;
    protected int mFlingVelocity;

    protected void waveformFling(float vx) {
        mActivePaint.setAlpha(255);
        mTranslucentActivePaint.setAlpha((int) (amount_translucent_paint * 255));
        mIsTouchDragging = false;
        // mOffsetGoal = mOffset;
        mFlingVelocity = (int) (vx);
        doFlingTransition();
    }

    protected float mTouchDown;
    protected int mTouchInitialOffset;
    protected long mWaveformTouchStartMsec;
    protected float tempCurrentWavePos = 0;

    void waveformTouchDown(float x) {
        mIsTouchDragging = true; // ngón tay đang nhấn vào màn hình
        mTouchDown = x; // ghi lại vị trí nhấn
        //mTouchInitialOffset = mOffset;
        tempCurrentWavePos = currentWavePos; // ghi lại vị trí line hiện tại vào biến tạm
        mFlingVelocity = 0; // set lại vận tốc
        mWaveformTouchStartMsec = System.currentTimeMillis(); // ghi lại thời gian để kiểm tra click khi hết nhấn

        /*
         *
         */
        mActivePaint.setAlpha((int) ((1 - touchDown_alphaAdd) * 255));
        mTranslucentActivePaint.setAlpha((int) ((amount_translucent_paint - touchDown_alphaAdd) * 255));

    }

    void waveformTouchMove(float x) {
        //   mOffset = trap((int) (mTouchInitialOffset + (mTouchDown - x)));
        //      Log.d (TAG,"onTouchMove : x = "+x+", mOffset = "+mOffset);
        //  updateDisplay();
        if (runningTrailer && va != null && va.isRunning()) va.cancel();
        float deltaX = x - mTouchDown;
        calculateAndDrawWaveform(tempCurrentWavePos - deltaX);
    }

    void waveformTouchUp() {
        mActivePaint.setAlpha(255);
        mTranslucentActivePaint.setAlpha((int) (amount_translucent_paint * 255));
        mActivePaint.setAlpha(255);
        mIsTouchDragging = false;
        //mOffsetGoal = mOffset; // ?

        //updateDisplay();
        long elapsedMsec = System.currentTimeMillis() - mWaveformTouchStartMsec;
        runningTrailer = false;
        if (elapsedMsec < 300) { // A Quick Touch - A Click
            //Log.d(TAG, "Elapsed");
            runTrailer();
            /*
            if (mIsPlaying) {
                int seekMsec = mWaveformView.pixelsToMillisecs((int) (mTouchDown + mOffset));
                if (seekMsec >= mPlayStartMsec && seekMsec < mPlayEndMsec) {
                    mPlayer.seekTo(seekMsec - mPlayStartOffset);
                } else {
                    handlePause();
                }
            } else {
                onPlay((int) (mTouchDown + mOffset));
            }
            */
        }

    }

    Handler FlingHandler = new Handler();
    private long timeBegin = 0;
    private long timeFling = 0;
    MediaPlayer mediaPlayer;
    private Runnable FlingRunnable = new Runnable() {
        @Override
        public void run() {
            timeFling = System.currentTimeMillis() - timeBegin;
            int delta = (int) ((mFlingVelocity + 0.0f) / (60)); // khung hình tiếp theo sẽ di chuyển từng này
            if (mFlingVelocity > delta) { // ?
                mFlingVelocity -= delta;
            } else if (mFlingVelocity < -delta) {
                mFlingVelocity -= delta;
            } else {
                mFlingVelocity = 0;
            }
            calculateAndDrawWaveform(currentWavePos - delta);
            if (mFlingVelocity != 0) {
                isFlingTransiting = true;
                FlingHandler.post(FlingRunnable);
            } else isFlingTransiting = false;
        }
    };
    private boolean isFlingTransiting = false;

    protected void doFlingTransition() {
        if (isFlingTransiting) {
            // Vào đây nghĩa là 1 fling đang chồng lên mà fling trước chưa chạy xong
            //  Ta sẽ huỷ fling cũ đi để chạy fling mới.

        }
        // bắt đầu một fling mới
        timeBegin = System.currentTimeMillis();
        isFlingTransiting = true;
        FlingHandler.post(FlingRunnable);
    }

    /**
     * Call this to redraw the wave form
     *
     * @param CurrentWavePos the position of the scroller in pixel unit
     */
    protected void calculateAndDrawWaveform(float CurrentWavePos) {
        if (CurrentWavePos > ruler) CurrentWavePos = ruler;
        else if (CurrentWavePos < 0) CurrentWavePos = 0;
        currentWavePos = CurrentWavePos; // ?
        translateX = (int) (mSeekBarCenter.X - currentWavePos);
        float percentage = currentWavePos / ruler;
        if (percentage < 0) percentage = 0;
        else if (percentage > 1) percentage = 1;

        lineFrom = (int) (percentage * TotalPens - NumberPensAppearInScreen / 2.0f - 10);
        lineTo = (int) (percentage * TotalPens + NumberPensAppearInScreen / 2.0f + 10);
        if (lineFrom < 0) lineFrom = 0;
        if (lineTo > TotalPens) lineTo = TotalPens;
        //Log.d(TAG, "percent = " + percentage + ", lineFrom = " + lineFrom + ", lineTo = " + lineTo);
        invalidate(); // TODO :  should  we redraw all the view?
    }

    private boolean runningTrailer = false;
    ValueAnimator va;

    protected void runTrailer() {
        runningTrailer = true;
        va = ValueAnimator.ofFloat(0, ruler);
        va.setInterpolator(Animation.getInterpolator(0));
        va.setDuration((long) (mDuration * 1000));
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                calculateAndDrawWaveform(value);
            }
        });
        va.start();
    }

    protected void updateDisplay() {

    }

    protected float oneDp;
    protected int mWidth;
    protected int mHeight;
    protected MPoint mSeekBarCenter = new MPoint(0,0);
    protected MPoint mRectCenter = new MPoint(0,0);

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

            initDrawSetSize(w,h);

    }
    private boolean isValidProperties() {
        return mWidth>0&&mHeight>0;
    }

    private void initDrawSetSize(int w, int h) {
        //Log.d(TAG, "initDrawSetSize: w = " +w+", h = "+h);
        mWidth = w;
        mHeight = h;
        lineHeight = maxLineHeight = top_bottom_ratio * (mHeight / 2.0f - oneDp);
        min_seek_height = min_seek_value*(mHeight/2.0f-oneDp);
        max_seek_height = max_seek_value*(mHeight/2.0f - oneDp);
        mSeekBarCenter.X = mWidth / 2;
        mSeekBarCenter.Y = mHeight / 2;
    }

    protected Paint mActivePaint;
    protected Paint mTranslucentActivePaint;
    protected Paint mHidePaint;
    protected Paint mTranslucentHidePaint;
    public Paint mLinearPaint;

    protected double nextDelayedTime = 30;
    protected double pp_point_counting = 0;
    protected double pp_point = 0;
    protected boolean pp_way = true;
    protected double pp_timeOneRound = 700.f;

    protected final android.view.animation.Interpolator pp_interpolator = Animation.getInterpolator(9);

    protected Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {

            calculating();
            invalidate();
            // the repeater of drawing begins here
            if (STATE ==State.VISUALIZING) // until STATE be different from NOTHING, it won't draw visual again.
                return;
            mHandler.postDelayed(mTimerRunnable, (long) nextDelayedTime);
        }
    };

    protected void updateScreen(Canvas canvas) {
        switch (STATE) {
            case NOTHING:
                onDrawNothing(canvas);
                break;
            case PREPARING:
                onDrawPreparing(canvas);
                break;
            case VISUALIZING:
                onDrawVisualizing(canvas);
                break;
                case ON_SEEKING:
                    onDrawOnSeek(canvas);
                    break;
            case SWITCHING:
                onDrawSwitching(canvas);
                break;
            default:
                break;
        }
    }

    protected void calculating() {
        switch (STATE) {
            case NOTHING:
                onCalculateNothing();
                break;
            case PREPARING:
                onCalculatePreparing();
                break;
            case VISUALIZING:
                onCalculateVisualizing();
                break;
            case SWITCHING:
                onCalculateSwitching();
                break;
            default:
                break;
        }
    }

    protected void onCalculateNothing() {

    }

    protected void reformat() {
        switch (STATE) {
            case NOTHING:
                reformatNothing();
                break;
            case PREPARING:
                reformatPreparing();
                break;
            case VISUALIZING:
                reformatVisualizing();
                break;
            case SWITCHING:
                reformatSwitching();
                break;
            default:
                break;
        }
    }

    protected void reformatPreparing() {
        pp_point = 0;
        pp_way = true;
        pp_point_counting = 0;
        pp_timeOneRound = 700;
        nextDelayedTime = 30;
    }

    protected void reformatNothing() {

    }

    protected void reformatVisualizing() {
        currentFractionComplete = 0;
        PenWidth = (float) (distancePen+strokeWidthPen);
        // Chiều dài thực của visualSeekbar
        WaveLength = (int) (PenWidth*TotalPens);
    }

    protected void reformatSwitching() {

    }


    protected void onCalculatePreparing() {
        if (currentFractionComplete >= 1
        //        && ((pp_point >= 0.5f && pp_way) || (pp_point <= 0.5f && !pp_way))
        )
            updateState(Command.PREPARED_ALREADY);
        else { // do work
            if (pp_point_counting >= pp_timeOneRound / nextDelayedTime) {
                pp_point_counting = 0;
                pp_way = !pp_way;
            }
            pp_point_counting++;
            //Log.d(TAG, "onCalculatePreparing: pp_counting = "+pp_point_counting +" / "+(pp_timeOneRound/nextDelayedTime));
            pp_point = pp_point_counting / (pp_timeOneRound / nextDelayedTime);
            pp_point = pp_interpolator.getInterpolation((float) pp_point);
            if (!pp_way) pp_point = 1 - pp_point;
            //  Log.d(TAG,pp_point_counting+" & "+pp_point);

        }
    }

    protected void onCalculateVisualizing() {

    }

    protected void onCalculateSwitching() {

    }

    protected float tape = 0.01f;
    protected float cross = 0.1f;
    protected final int color_linear[] = new int[]{
            0xff00dbde,
            0xfffc00ff
    };

    @NonNull
    private LinearGradient getLinearShader() {
      int color_2 = Tool.getBaseColor();
        int color_1 =  Color.argb(0x22,Color.red(color_2),Color.green(color_2),Color.blue(color_2));

        int[] color = new int[]{
                color_1,
                color_2,
                color_2,
                color_1
        };
        float sum = tape + 2 * cross;
        float[] pos = new float[]{
                0,
                cross / sum,
                1 - cross / sum,
                1
        };
        return new LinearGradient(-sum * mWidth / 2, 0, sum * mWidth / 2, 0, color, pos,
                Shader.TileMode.CLAMP);
    }

    protected void onDrawNothing(Canvas canvas) {

    }

    protected void onDrawPreparing(Canvas canvas) {
        // canvas.drawLine(0,(float)mSeekBarCenter.Y,(float) (mWidth*currentFractionComplete),(float)mSeekBarCenter.Y,mActivePaint);
        canvas.save();
        canvas.translate((float) (pp_point * mWidth), (float) mSeekBarCenter.Y);
        canvas.drawLine((float) (-pp_point * mWidth), 0, (float) (mWidth - pp_point * mWidth), 0, mLinearPaint);
        canvas.restore();
    }
    public int currentSeek = 0;
    private boolean in_on_seek = false;
    private boolean in_animate =false;
    private ValueAnimator switch_state_va;
    protected void onDrawVisualizing(Canvas canvas) {
        if(in_on_seek) {
            if (in_animate) {
                if (switch_state_va != null && switch_state_va.isRunning())
                    switch_state_va.cancel();
                in_animate = false;
            }
            in_animate = true;
            in_on_seek = false;
            switch_state_va = ValueAnimator.ofFloat(on_seek_animate_value,0);
            switch_state_va.setInterpolator(Animation.getInterpolator(4));
         //   switch_state_va.setInterpolator(new FeaturePlaylistAdapter.BounceInterpolator(0.1f,30));
            switch_state_va.setDuration(350);
            switch_state_va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    on_seek_animate_value = (float)valueAnimator.getAnimatedValue();
                    if(on_seek_animate_value==0) in_animate = false;
                    invalidate();
                }
            });
            switch_state_va.start();
        }

        if(on_seek_animate_value==0)
     drawVisualWave(canvas, currentSeek);
         else
             drawVisualWaveOnSeek(canvas,currentSeek);
    }
    protected void onDrawOnSeek(Canvas canvas) {
        if(!in_on_seek) {
            if (in_animate) {
                if (switch_state_va != null && switch_state_va.isRunning())
                    switch_state_va.cancel();
            }
            in_animate = true;
            in_on_seek = true;
            switch_state_va = ValueAnimator.ofFloat(on_seek_animate_value,1);
            switch_state_va.setInterpolator(Animation.getInterpolator(4));
       //     switch_state_va.setInterpolator(new FeaturePlaylistAdapter.BounceInterpolator(0.1f,30));

            switch_state_va.setDuration(350);
            switch_state_va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    on_seek_animate_value = (float)valueAnimator.getAnimatedValue();
                    if(on_seek_animate_value==1) in_animate = false;
                    invalidate();
                }
            });
            switch_state_va.start();
        }
        drawVisualWaveOnSeek(canvas,currentSeek);
    }
            // normal pen= 4/7f
    float min_seek_value = 3/8f;
    float max_seek_value =10/15f;
    float on_seek_animate_value=0; //0 up to 1
    float min_seek_height , max_seek_height;
    // hàm thực hiện vẽ wave khi đang seek bởi người dùng
    // nằm càng xa tâm thì sẽ càng nhỏ
    protected void drawVisualWaveOnSeek(Canvas canvas, int millisecond) {

        // Tại thời điểm T, tương ứng với đoạn :
        float CurrentSeekPos = WaveLength/(duration+0.0f)*millisecond;
        // Chiều rộng của view là mWidth
        float move;
        // Nếu như SeekPos nhỏ hơn một nửa độ rộng, ta không cần di chuyển
        if(CurrentSeekPos<=mWidth/2)  {
            move = 0;
        } else { // còn nếu SeekPos lớn hơn một nửa độ rộng
            // Ta cần dịch sang trái một đoạn sao cho SeekPos ở chính giữa theo chiều ngang
            move = CurrentSeekPos - mWidth/2;
        }
        // Tiếp theo ta vẽ đoạn visual có thể thấy đc lên view
        // Nghĩa là vẽ đoạn visual từ vị trí move tới vị trí SeekPos + mWidth /2

        // Ta tính xem những Pen nào thì sẽ xuất hiện trong view, và ta chỉ vẽ chúng mà thôi
        int firstPen = (int) (move/PenWidth);
        int midPen = (int) (CurrentSeekPos/PenWidth);
        int endPen = (int) ((move + mWidth)/PenWidth);

        for(int i = firstPen; i <= endPen && i != TotalPens - 1; i++) {
            // càng cách xa midPen, kích thước càng nhỏ
            float runtime_height;
                         // chiều dài thường       (0 -> 1)
            float pen_pos = -move+PenWidth*i+PenWidth/2;
            // 1 <-> 0 <-> 1
            float delta = (float) ((pen_pos<mWidth/2) ?(Math.pow((mWidth/2 - pen_pos)/(mWidth/2),3)) :   (Math.pow((pen_pos- mWidth/2)/(mWidth/2),3)));
          //  float delta = (float) ((pen_pos<mWidth/2) ?(Math.sqrt((mWidth/2 - pen_pos)/(mWidth/2))) :  Math.sqrt((pen_pos- mWidth/2)/(mWidth/2)));

            /*
                runtime_height = (float) (lineHeight
                        - on_seek_animate_value //gia tăng theo animation
                                        *delta // gia tăng theo vị trí pen, càng xa tâm thì càng tiến gần 1
                                        *(lineHeight - min_seek_height)); // hằng số
                                        */
            float min_max_delta = max_seek_height - min_seek_height;
            // ở chính tâm thì có độ lớn là max_seek_height,
            // ở ngoài cùng thì có độ lớn là min_seek_height

            runtime_height = max_seek_height - delta*min_max_delta;
            runtime_height = (runtime_height - lineHeight)*on_seek_animate_value + lineHeight;
            double alpha_delta =1 -  Math.pow(delta,4)*0.85*on_seek_animate_value;
          //  double alpha_delta =1 -  Math.pow(delta,4)*0.85;
            if(alpha_delta>1) alpha_delta = 1;
            mActivePaint.setAlpha((int) (255*alpha_delta));
            mTranslucentActivePaint.setAlpha((int) (255*amount_translucent_paint*alpha_delta));
            mHidePaint.setAlpha((int) (255*amount_hide_paint*alpha_delta));
            mTranslucentHidePaint.setAlpha((int) (255*amount_translucent_hide_paint*alpha_delta));
           if(i<midPen) {

                canvas.drawLine(
                        (float)(-move +PenWidth*i +PenWidth/2),
                        (float)(mSeekBarCenter.Y - oneDp),
                        (float)(-move + PenWidth*i + PenWidth/2),
                        (float)(mSeekBarCenter.Y - oneDp - runtime_height * SmoothedPenGain[i]),
                        mActivePaint);
                canvas.drawLine(
                        (float)(-move +PenWidth*i +PenWidth/2),
                        (float)(mSeekBarCenter.Y + oneDp),
                        (float)(-move + PenWidth*i + PenWidth/2),
                        (float)(mSeekBarCenter.Y + oneDp + runtime_height * SmoothedPenGain[i]),
                        mTranslucentActivePaint);
            } else if(i>midPen) {
                canvas.drawLine(
                        (float)(-move +PenWidth*i +PenWidth/2),
                        (float)(mSeekBarCenter.Y - oneDp),
                        (float)(-move + PenWidth*i +PenWidth/2),
                        (float)(mSeekBarCenter.Y - oneDp - runtime_height * SmoothedPenGain[i]),
                        mHidePaint);
                canvas.drawLine(
                        (float)(-move +PenWidth*i +PenWidth/2),
                        (float)(mSeekBarCenter.Y + oneDp),
                        (float)(-move + PenWidth*i + PenWidth/2),
                        (float)(mSeekBarCenter.Y + oneDp +runtime_height * SmoothedPenGain[i]),
                        mTranslucentHidePaint);
            } else {


                if(midPen*PenWidth +PenWidth-distancePen/2>CurrentSeekPos) {
                    canvas.drawRect(
                            -move + CurrentSeekPos,
                            (float) (mSeekBarCenter.Y - oneDp - runtime_height * SmoothedPenGain[i]),
                            (float) (-move + midPen * PenWidth + PenWidth - distancePen / 2),
                            (float) mSeekBarCenter.Y - oneDp,
                            mHidePaint
                    );
                    canvas.drawRect(
                            -move + CurrentSeekPos,
                            (float) mSeekBarCenter.Y + oneDp,
                            (float) (-move + midPen * PenWidth + PenWidth - distancePen / 2),
                            (float) (mSeekBarCenter.Y +oneDp + runtime_height * SmoothedPenGain[i]),
                            mTranslucentHidePaint
                    );
                }
                if(midPen*PenWidth +distancePen/2<CurrentSeekPos) {
                    canvas.drawRect(
                            (float) (-move + PenWidth * midPen + distancePen / 2),
                            (float) (mSeekBarCenter.Y - oneDp - runtime_height * SmoothedPenGain[i]),
                            (float) (-move + CurrentSeekPos),
                            (float) mSeekBarCenter.Y - oneDp,
                            mActivePaint);
                    canvas.drawRect(
                            (float) (-move + PenWidth * midPen + distancePen / 2),
                            (float) mSeekBarCenter.Y + oneDp,
                            -move + CurrentSeekPos,
                            (float) (mSeekBarCenter.Y + oneDp + runtime_height* SmoothedPenGain[i]),
                            mTranslucentActivePaint);
                }
            }
        }
    }

    public interface OnSeekBarChangeListener {
        void onSeekBarSeekTo(AudioVisualSeekBar seekBar, int i, boolean b);

        void onSeekBarTouchDown(AudioVisualSeekBar seekBar);

        void onSeekBarTouchUp(AudioVisualSeekBar seekBar);
        void onSeekBarSeeking(int seekingValue);
    }

    /**
     * currentWavePos is in range [0;ruler], in pixel unit
     */
    protected float ruler = 0;
    protected float currentTime = 0;
    protected float currentWavePos = 0.0f;
    private float touchDownPos = 0;
    private float currentTouchPos;

    private int touchDownMove = 0;
    private int touchDownFling = 0;
    private int lineFrom = 0, lineTo = 0;
    private int deltaX = 0;
    private int translateX = 0;
    // tỉ lệ về độ cao của line trên so với height/2
    private float top_bottom_ratio = 9 / 15f;
    private float maxLineHeight;
    private float lineHeight;

    public float getCurrentTimeFromPos(float currentPos) {
        return 0;
    }

    /**
     * called by {@link #onDrawVisualizing};
     *
     * @param canvas the canvas of view.
     */

    // Mỗi "chiều cao của một pen" là một SmoothPenGain[i]
    // Có tất cả TotalPenGains
    // Mỗi đường được biểu diễn là một hình chữ nhật
    // có
    // width = strokeWidthPen + distancePen = 4dp =  ( 3 + 0.5. 0.5 )
    // height = lineHeight* SmoothedPenGain[i]
    // ở tâm vẽ một đường thẳng

    /**
     * Vẽ một visual wave nơi nó thể hiện cường độ âm thanh tại thời điểm millisecond
     * @param canvas
     * @param millisecond
     */
    float PenWidth;
    float WaveLength;
    protected void drawVisualWave(Canvas canvas, int millisecond) {
        mActivePaint.setAlpha(255);
        mTranslucentActivePaint.setAlpha((int) (255*amount_translucent_paint));
        mHidePaint.setAlpha((int) (255*amount_hide_paint));
        mTranslucentHidePaint.setAlpha((int) (255*amount_translucent_hide_paint));

        // Tại thời điểm T, tương ứng với đoạn :
        float CurrentSeekPos = WaveLength/(duration+0.0f)*millisecond;
        // Chiều rộng của view là mWidth
        float move;
        // Nếu như SeekPos nhỏ hơn một nửa độ rộng, ta không cần di chuyển
        if(CurrentSeekPos<=mWidth/2)  {
            move = 0;
        } else { // còn nếu SeekPos lớn hơn một nửa độ rộng
            // Ta cần dịch sang trái một đoạn sao cho SeekPos ở chính giữa theo chiều ngang
            move = CurrentSeekPos - mWidth/2;
        }
        // Tiếp theo ta vẽ đoạn visual có thể thấy đc lên view
        // Nghĩa là vẽ đoạn visual từ vị trí move tới vị trí SeekPos + mWidth /2

        // Ta tính xem những Pen nào thì sẽ xuất hiện trong view, và ta chỉ vẽ chúng mà thôi
        int firstPen = (int) (move/PenWidth);
        int midPen = (int) (CurrentSeekPos/PenWidth);
        int endPen = (int) ((move + mWidth)/PenWidth);
        for(int i=firstPen;i<=endPen&&i!=TotalPens-1&&i<SmoothedPenGain.length;i++) {
            if(i<midPen) {
                canvas.drawLine(
                        (float)(-move +PenWidth*i +PenWidth/2),
                        (float)(mSeekBarCenter.Y - oneDp),
                        (float)(-move + PenWidth*i + PenWidth/2),
                        (float)(mSeekBarCenter.Y - oneDp - lineHeight * SmoothedPenGain[i]),
                        mActivePaint);
                canvas.drawLine(
                        (float)(-move +PenWidth*i +PenWidth/2),
                        (float)(mSeekBarCenter.Y + oneDp),
                        (float)(-move + PenWidth*i + PenWidth/2),
                        (float)(mSeekBarCenter.Y + oneDp + lineHeight * SmoothedPenGain[i]),
                        mTranslucentActivePaint);
            } else if(i>midPen) {
                canvas.drawLine(
                        (float)(-move +PenWidth*i +PenWidth/2),
                        (float)(mSeekBarCenter.Y - oneDp),
                        (float)(-move + PenWidth*i +PenWidth/2),
                        (float)(mSeekBarCenter.Y - oneDp - lineHeight * SmoothedPenGain[i]),
                        mHidePaint);
                canvas.drawLine(
                        (float)(-move +PenWidth*i +PenWidth/2),
                        (float)(mSeekBarCenter.Y + oneDp),
                        (float)(-move + PenWidth*i + PenWidth/2),
                        (float)(mSeekBarCenter.Y + oneDp + lineHeight * SmoothedPenGain[i]),
                        mTranslucentHidePaint);
            } else {


                if(midPen*PenWidth +PenWidth-distancePen/2>CurrentSeekPos) {
                    canvas.drawRect(
                            -move + CurrentSeekPos,
                            (float) (mSeekBarCenter.Y - oneDp - lineHeight * SmoothedPenGain[i]),
                            (float) (-move + midPen * PenWidth + PenWidth - distancePen / 2),
                            (float) mSeekBarCenter.Y - oneDp,
                            mHidePaint
                    );
                    canvas.drawRect(
                            -move + CurrentSeekPos,
                            (float) mSeekBarCenter.Y + oneDp,
                            (float) (-move + midPen * PenWidth + PenWidth - distancePen / 2),
                            (float) (mSeekBarCenter.Y +oneDp + lineHeight * SmoothedPenGain[i]),
                            mTranslucentHidePaint
                    );
                }
                if(midPen*PenWidth +distancePen/2<CurrentSeekPos) {
                    canvas.drawRect(
                            (float) (-move + PenWidth * midPen + distancePen / 2),
                            (float) (mSeekBarCenter.Y - oneDp - lineHeight * SmoothedPenGain[i]),
                            (float) (-move + CurrentSeekPos),
                            (float) mSeekBarCenter.Y - oneDp,
                            mActivePaint);
                    canvas.drawRect(
                            (float) (-move + PenWidth * midPen + distancePen / 2),
                            (float) mSeekBarCenter.Y + oneDp,
                            -move + CurrentSeekPos,
                            (float) (mSeekBarCenter.Y + oneDp + lineHeight * SmoothedPenGain[i]),
                            mTranslucentActivePaint);
                }
            }
        }
    }

    OnSeekBarChangeListener listener;

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.listener = listener;
    }
    SwipeDetectorGestureListener swipeListener =  new SwipeDetectorGestureListener() {
        @Override
        public void onUp(MotionEvent e) {
            if(inFling) return;
            restoreState();
            if(listener!=null)
            {
                int seek = currentSeek;
                if(currentSeek<0) seek = 0;
                else if(currentSeek>duration)
                    seek = duration;
                listener.onSeekBarSeekTo(AudioVisualSeekBar.this, seek, true);
                listener.onSeekBarTouchUp(AudioVisualSeekBar.this);
            }
        }
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            //Log.d(TAG,"onSingleTapUp");
            return super.onSingleTapUp(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            //Log.d(TAG,"onLongPress");
            super.onLongPress(e);
        }
        private void saveState() {
            if(STATE!=State.ON_SEEKING) {
                savedState = STATE;
                STATE = State.ON_SEEKING;
            }
        }
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            /*
            Sự kiện kéo
            Ta dịch chuyển thanh ngay tức thì
             */
            if(inFling) {
                flingAnimation.cancel();
                inFling = false;
            }
            saveState();
            float ms_seek = distanceX/WaveLength*(duration+0.0f);
            currentSeek+=ms_seek;
            if(listener!=null) listener.onSeekBarSeeking(currentSeek);
            invalidate();

            return super.onScroll(e1,e2,distanceX,distanceY);
        }
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
            //Log.d(TAG,"onFling");
            boolean d;
            try {
                 d = super.onFling(e1, e2, vx, vy);
            } catch (Exception e) {
                d = false;
            }
            //     restoreState();
            return d;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            //Log.d(TAG,"onShowPress");
        }

        @Override
        public boolean onSwipeRight(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //Log.d(TAG,"onSwipeRight");
            return onSwipeHandler(e1,e2,velocityX,velocityY);
        }

        @Override
        public boolean onSwipeLeft(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //Log.d(TAG,"onSwipeLeft ");
            return onSwipeHandler(e1,e2,velocityX,velocityY);
        }
        boolean inFling = false;
        FlingAnimation flingAnimation;
        private boolean onSwipeHandler(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if(inFling) {
                flingAnimation.cancel();
            }
            inFling = true;

            flingAnimation = new FlingAnimation(new FloatValueHolder(0));
            int saved = currentSeek;
            saveState();
            flingAnimation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
                @Override
                public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                    currentSeek = (int) (saved +  value/WaveLength*(duration+0.0f));
                    if(currentSeek<0) currentSeek = 0;
                    else if(currentSeek>duration) currentSeek = duration - 5000;
                    if(currentSeek<0) currentSeek = 1000;
                    if(listener!=null) listener.onSeekBarSeeking(currentSeek);
                    //    Log.d(TAG, "value = " + value);
                    invalidate();
                }
            });
            flingAnimation.addEndListener(new DynamicAnimation.OnAnimationEndListener() {
                                              @Override
                                              public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                                                  inFling = false;
                                                  restoreState();
                                                  if(listener!=null)
                                                  {
                                                      int seek = currentSeek;
                                                      if(currentSeek<0) seek = 0;
                                                      else if(currentSeek>duration)
                                                          seek = duration;
                                                      listener.onSeekBarSeekTo(AudioVisualSeekBar.this, seek, true);
                                                      listener.onSeekBarTouchUp(AudioVisualSeekBar.this);
                                                  }
                                              }
                                          });
                    flingAnimation
                            .setFriction(2f)
                            .setStartVelocity(-velocityX)
                          //  .setMinValue(-currentWavePos)
                       //     .setMaxValue(WaveLength- currentWavePos)
                            //.setStartValue(currentSeek)
                            .start();

            return true;
        }
        @Override
        public boolean onSwipeTop(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //Log.d(TAG,"onSwipeTop");
            return false;
        }

        @Override
        public boolean onSwipeBottom(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //Log.d(TAG,"onSwipeBottom");
            return false;
        }
        private State savedState;
        @Override
        public boolean onDown(MotionEvent e) {
            if(listener!=null)
                listener.onSeekBarTouchDown(AudioVisualSeekBar.this);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //Log.d(TAG,"onDoubleTap");
            return super.onDoubleTap(e);
        }

        private void restoreState() {
            STATE = savedState;
            invalidate();
        }
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }
    };
    protected void setGesture(Context context) {
        mGestureDetector = new GestureDetector(
                context,
               swipeListener);
    }
    protected GestureDetector mGestureDetector;

        public class SwipeDetectorGestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            public void onUp(MotionEvent e) {

            }
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                if(e1==null||e2==null) return false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                result = onSwipeRight(e1,e2,velocityX,velocityY);
                            } else {
                                result = onSwipeLeft(e1,e2,velocityX,velocityY);
                            }
                        }
                    } else {
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY > 0) {
                                result = onSwipeBottom(e1,e2,velocityX,velocityY);
                            } else {
                                result = onSwipeTop(e1,e2,velocityX,velocityY);
                            }
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }

        public boolean onSwipeRight(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        public boolean onSwipeLeft(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        public boolean onSwipeTop(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        public boolean onSwipeBottom(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        performClick();
/*        if(event.getAction()==MotionEvent.ACTION_DOWN)
        Log.d(TAG, "onTouchEvent DOWN");
        else Log.d(TAG, "onTouchEvent: "+event.getAction());*/
        boolean b = false;
        try {
            b = mGestureDetector.onTouchEvent(event);
        } catch (Exception e) {
            b = false;
        }
        if (event.getAction() == MotionEvent.ACTION_UP)
            swipeListener.onUp(event);
        if(b) return true;

        return super.onTouchEvent(event);

        /*
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                waveformTouchDown(event.getX());
                break;
            case MotionEvent.ACTION_MOVE:
                waveformTouchMove(event.getX());
                break;
            case MotionEvent.ACTION_UP:
                waveformTouchUp();
                break;
        }
        return true;
        */
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    protected void onDrawSwitching(Canvas canvas) {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!isValidProperties())
        initDraw();
        //Log.d(TAG, "onDraw: width = "+ mWidth+", height = "+mHeight);
        try {
            updateScreen(canvas);
        } catch (Exception ignore) {}
    }

    /*
        NOTHING: It means that SeekBar now do not show visualization, it is just a normal seek bar.
        PREPARING: A Sound File is being parsed by its and will show a visualization when it finishes.
        VISUALIZING:  VISUALIZING is being showed
        SWITCHING: Close Effect is being showed to prepare for a new sound file.
         */
    enum State {
        NOTHING,
        PREPARING,
        VISUALIZING,
        ON_SEEKING,
        SWITCHING
    }

    enum Command {
        FILE_SET,
        BEGIN,
        PREPARED_ALREADY
    }

    private State STATE = State.NOTHING;

    private void updateState(Command state) {
        switch (state) {
            case FILE_SET:
                if (STATE == State.NOTHING)
                    onBeginPreparing();
                else if (STATE == State.VISUALIZING)
                    onBeginSwitching();
                break;
            case BEGIN:
                STATE = State.NOTHING;
                break;
            case PREPARED_ALREADY:
                onBeginVisualizing();
                break;
            default:
                break;
        }
    }

    protected double currentFractionComplete = 0;
    protected boolean loadingFalse = false;
    // Only this
    // set a new cheap sound file.
    void onBeginPreparing() {
        mHandler.postDelayed(mTimerRunnable, (long) nextDelayedTime);
        STATE = State.PREPARING;
        reformat();
        loadingFalse = false;
        final CheapSoundFile.ProgressListener listener = new CheapSoundFile.ProgressListener() {
            @Override
            public boolean reportProgress(double fractionComplete) {
                currentFractionComplete = fractionComplete;
             //   Log.d(TAG,"frac = "+currentFractionComplete);
                return mLoadingKeepGoing;
            }
        };
        new AsyncTask < Void, Void, Void > () {
            @Override
            protected Void doInBackground(Void...voids) {
                try {
                    mSoundFile = CheapSoundFile.create(mFile.getAbsolutePath(), listener);
                    calculateSound();
                } catch (final Exception e) {
                    //Log.e(TAG, "Error while loading sound file", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
               // Log.d(TAG, "fraction = " + currentFractionComplete);
                if (currentFractionComplete != 1) {
                    currentFractionComplete = 1;
                    pp_timeOneRound = 350;
                }
            }
        }.execute();
    }

    // Or this
    // Remove current cheap sound file and set a new one.
    void onBeginSwitching() {
        STATE = State.SWITCHING;
        reformat();
        onBeginPreparing();
    }

    protected void onBeginVisualizing() {

        STATE = State.VISUALIZING;
        reformat();
    }

    Handler waitHandler = new Handler();
    Runnable runnable_UpdateState_FileSet = new Runnable() {
        @Override
        public void run() {
            updateState(Command.FILE_SET);
        }
    };
    private int mTempProgress = 0;

    public void visualize(String fileName, long duration, int progress) {
        mFileName = fileName;

        try {
            mFile = new File(mFileName);
            setMax((int) duration);
            if(progress<0)
            mTempProgress = 0;

            else if(progress>duration) mTempProgress = (int) (duration-1);
            else mTempProgress = progress;
            setProgress(mTempProgress);
            updateProperties();
            updateState(Command.FILE_SET);
        } catch (Exception e) {
            mFileName = "";
            duration = 0;
        }
    }

    //protected long mLoadingLastUpdateTime = 0;
    protected boolean mLoadingKeepGoing = true;
    protected String mFileName = "";
    public String getCurrentFileName() {
        return mFileName;
    }
    protected File mFile;
    protected CheapSoundFile mSoundFile;
    protected Handler mHandler;
    protected double mSampleRate;
    protected double mSamplesPerFrame;
    protected double mNumFrames;
    protected double mDuration;
    protected int mIntDuration;
    protected int mMaxGain, mMinGain;
    protected int[] mFrameGain;
    float strokeWidthPen;
    double distancePen;
    int NumberFrameAppearInScreen;
    int NumberPensAppearInScreen;
    int TotalPens;

    double[] SmoothedPenGain;

    protected void calculateSound() {
        if(mWidth==0) mWidth = Tool.getScreenSize(getContext())[0];
        // run in the background
        mNumFrames = mSoundFile.getNumFrames();
        //Log.d(TAG, "calculateSound: "+mNumFrames);
        mSampleRate = mSoundFile.getSampleRate();
        mSamplesPerFrame = mSoundFile.getSamplesPerFrame();
        mDuration = mNumFrames * mSamplesPerFrame / mSampleRate + 0.0f;
        mIntDuration = (int) mDuration;
        mFrameGain = mSoundFile.getFrameGains();
        mMaxGain = 0;
        mMinGain = 255;
        for (int i = 0; i < mNumFrames; i++) {
            if (mMaxGain < mFrameGain[i]) mMaxGain = mFrameGain[i];
            if (mMinGain > mFrameGain[i]) mMinGain = mFrameGain[i];
        }

        //30 s for a screen width
        // how many frames appeared in a screen width ?
        // how many pens appeared in a screen width ?
        // >> how many frame for one pen ?
        // duration = 1.5*Width

        NumberPensAppearInScreen = (int)(((mWidth + distancePen) / (0.0f + oneDp) + 1.0f) / 4.0f);

        float secondsInScreen  = (duration/4f)/1000f;
        //Log.d(TAG, "calculateSound: duration  = "+duration+", sis = "+ secondsInScreen);
        NumberFrameAppearInScreen = (int)(mNumFrames * secondsInScreen / mDuration);
        NumberFrameInAPen = NumberFrameAppearInScreen / NumberPensAppearInScreen;
        double re = (mNumFrames + 0.0f) / NumberFrameInAPen;
        TotalPens = (re == ((int) re)) ? (int) re : ((int) re + 1);

        double[] originalPenGain = new double[TotalPens];
        originalPenGain[0] = 0;
        //  reduce the frame gains array (large data) into the pen gains with smaller data.
        int iPen = 0;
        int pos = 0;
        for (int iFrame = 0; iFrame < mNumFrames; iFrame++) {
           /* if(iPen>=226)
                Log.d(TAG, "calculateSound");*/
            originalPenGain[iPen] += mFrameGain[iFrame];
            pos++;
            if (iFrame == mNumFrames - 1) {
                originalPenGain[iPen] /= pos;
            } else if (pos == NumberFrameInAPen) {
                originalPenGain[iPen] /= NumberFrameInAPen;
                pos = 0;
                iPen++;
            }
        }
        // make pen gains smoothly
       computeDoublesForAllZoomLevels(TotalPens, originalPenGain);
        SmoothedPenGain = new double[TotalPens];
        for (int i_pen = 0; i_pen < TotalPens; i_pen++)
            SmoothedPenGain[i_pen] = getHeight(i_pen, TotalPens, originalPenGain, scaleFactor, minGain, range);

        ruler = (float)(TotalPens * strokeWidthPen + (TotalPens - 1) * distancePen);
        currentWavePos = 0;
        lineFrom = 0;
        lineTo = NumberPensAppearInScreen / 2 + 10;
        translateX = (int)(mSeekBarCenter.X - currentWavePos);
        int x = 0;
    }


    protected double getHeight(int i, int totalPens, double[] penGain, float scaleFactor, float minGain, float range) {
        double value = (getGain(i, totalPens, penGain) * scaleFactor - minGain) / range;
        if (value < 0.0)
            value = 0.0f;
        if (value > 1.0)
            value = 1.0f;
        value = (value + 0.01f) / 1.01f;
        return value;
    }

    /**
     * Called once when a new sound file is added
     */
    protected void computeDoublesForAllZoomLevels(int totalPenGains, double[] originPenGain) {
        // Make sure the range is no more than 0 - 255
        float maxGain = 1.0f;
        for (int i = 0; i < totalPenGains; i++) {
            float gain = (float) getGain(i, totalPenGains, originPenGain);
            if (gain > maxGain) {
                maxGain = gain;
            }
        }
        scaleFactor = 1.0f;
        if (maxGain > 255.0) {
            scaleFactor = 255 / maxGain;
        }

        // Build histogram of 256 bins and figure out the new scaled max
        maxGain = 0;
        int gainHist[] = new int[256];
        for (int i = 0; i < totalPenGains; i++) {
            int smoothedGain = (int)(getGain(i, totalPenGains, originPenGain) * scaleFactor);
            if (smoothedGain < 0)
                smoothedGain = 0;
            if (smoothedGain > 255)
                smoothedGain = 255;

            if (smoothedGain > maxGain)
                maxGain = smoothedGain;

            gainHist[smoothedGain]++;
        }

        // Re-calibrate the min to be 5%
        minGain = 0;
        int sum = 0;
        while (minGain < 255 && sum < totalPenGains / 20) {
            sum += gainHist[(int) minGain];
            minGain++;
        }

        // Re-calibrate the max to be 99%
        sum = 0;
        while (maxGain > 2 && sum < totalPenGains / 100) {
            sum += gainHist[(int) maxGain];
            maxGain--;
        }

        range = maxGain - minGain;

        mInitialized = true;
    }

    protected static double getGain(int i, int totalPens, double[] penGain) {
        // if x > size - 1, x = size -1
        int x = Math.min(i, totalPens - 1);

        // if size < 2, do nothing
        if (totalPens < 2) {
            return penGain[x];
        } else {// else  (size > 2)
            //  if x is the first element
            // x = 1/2 itself + 1/2 next element
            if (x == 0) {
                return penGain[0]*0.7f + penGain[1]*0.3f;
            }
            // else if x is the last one,
            // x = 1/2 itself + 1/2 previous element
            else if (x == totalPens - 1) {
                return penGain[totalPens - 2]*0.3f + penGain[totalPens - 1]*0.7f;
            } else {
                // else
                // x = 1/3 prev + 1/3 itself + 1/3 next
                return penGain[x - 1]*3/13f + penGain[x]*7/13f + penGain[x + 1]*3/13f;
            }
        }
    }

    /*
    protected String secondsToMinutes(String seconds) {

    }
    */
    private final float amount_translucent_paint = 0.588f;
    public void updateProperties() {
        initDraw();
        mActivePaint.setColor(Tool.getBaseColor());
        mTranslucentActivePaint.setColor(Tool.getBaseColor());
        mTranslucentActivePaint.setAlpha((int)(amount_translucent_paint * 255));
        int global = Tool.getMostCommonColor();
        mLinearPaint.setShader(getLinearShader());

        mHidePaint.setColor(Color.WHITE);
        mHidePaint.setAlpha((int)(amount_hide_paint * 255));
        mTranslucentHidePaint.setColor(Color.WHITE);
        mTranslucentHidePaint.setAlpha((int)(amount_translucent_hide_paint * 255));
    }
    private float touchDown_alphaAdd = 0.2f;
    protected void initDraw() {
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        mSeekBarCenter = new MPoint(mWidth / 2, mHeight / 2);
        mRectCenter = new MPoint(mWidth / 2, mHeight / 2);

        lineHeight = maxLineHeight = top_bottom_ratio * (mHeight / 2.0f - oneDp);
        min_seek_height = min_seek_value*(mHeight/2.0f-oneDp);
        max_seek_height = max_seek_value*(mHeight/2.0f - oneDp);
        mLinearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinearPaint.setShader(getLinearShader());

        mLinearPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mLinearPaint.setStrokeWidth(1.5f*oneDp);
    }
}