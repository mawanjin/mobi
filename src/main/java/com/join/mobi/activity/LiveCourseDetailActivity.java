package com.join.mobi.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import com.join.android.app.common.R;
import com.join.android.app.common.dialog.CommonDialogLoading;
import com.join.android.app.common.utils.DateUtils;
import com.join.mobi.dto.ChapterDto;
import com.join.mobi.dto.CourseDetailDto;
import com.join.mobi.fragment.*;
import com.join.mobi.pref.PrefDef_;
import com.join.mobi.rpc.RPCService;
import org.androidannotations.annotations.*;
import org.androidannotations.annotations.rest.RestService;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Date;

/**
 * User: mawanjin@join-cn.com
 * Date: 14-9-10
 * Time: 上午10:25
 * 在线课程
 */
@EActivity(R.layout.livecourse_detail_activity_layout)
public class LiveCourseDetailActivity extends FragmentActivity implements MediaPlayer.OnInfoListener{

    public static final String EXTRA_COURSE_ID = "myStringExtra";

    @Pref
    PrefDef_ myPref;
    @RestService
    RPCService rpcService;
    @ViewById
    ImageView back;
    @ViewById
    TextView detailTab;
    @ViewById
    TextView title;
    @ViewById
    TextView chapterTab;
    @ViewById
    TextView examTab;
    @ViewById
    TextView referenceTab;
    @ViewById
    View main;
    @ViewById
    ImageView curveMarkerDetail;
    @ViewById
    ImageView curveMarkerChapter;
    @ViewById
    ImageView curveMarkerExam;
    @ViewById
    ImageView curveMarkerReference;
    @ViewById
    View header;
    @ViewById
    View frameContainer;
    @Extra(EXTRA_COURSE_ID)
    String courseId;
    /**
     * 课程海报
     */
    @Extra
    String url;
    @Extra
    String name;
    @Extra
    long seekTo;

    //视频部分
    @ViewById
    RelativeLayout videoContainer;
    @ViewById
    ProgressBar progressBar;
    @ViewById
    SeekBar seekbar;
    @ViewById
    ImageView issrt;
    @ViewById
    ImageView centerPlay;
    @ViewById
    RelativeLayout btm;
    @ViewById
    SurfaceView surface;
    @ViewById
    ImageView fullScreen;
    private int postion = 0;


    private String playUrl;
    LiveCourseDetailFragment_ liveCourseDetailFragment;
    LiveCourseChapterFragment_ liveCourseChapterFragment;
    LiveCourseExamFragment_ liveCourseExamFragment;
    LiveCourseReferenceFragment_ liveCourseReferenceFragment;

    FragmentManager fragmentManager;
    Fragment currentFragment;
    CourseDetailDto courseDetail;
    CommonDialogLoading loading;
    Thread threadUpdateGrogress;
    Thread checkProgress;

    @AfterViews
    void afterViews() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        title.setText(name);
        loading = new CommonDialogLoading(this);
        loading.show();
        //加载数据
        retrieveDataFromServer();
    }

    private void startUpdateLearningTime() {
//        //开始播放了,更新学习时间
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    while (mediaPlayer != null) {
                        try{
                            while (mediaPlayer.isPlaying()) {
                                //更新学习时间
                                Intent intent = new Intent("org.androidannotations.updateLearningTime");
                                sendBroadcast(intent);
                                Thread.sleep(1000);
                            }
                        }catch (java.lang.IllegalStateException e){

                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    private void initPlayer() {
        if(update==null)
        update = new upDateSeekBar(); // 创建更新进度条对象
        mediaPlayer = new MediaPlayer();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int mSurfaceViewWidth = dm.widthPixels;
        int mSurfaceViewHeight = dm.heightPixels;
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        lp.width = mSurfaceViewWidth;
//        lp.height = mSurfaceViewHeight * 1 / 3;
        surface.setLayoutParams(lp);
        surface.getHolder().setFixedSize(lp.width, lp.height);
        surface.getHolder().setKeepScreenOn(true);
        surface.getHolder().addCallback(new SurfaceViewLis());
        surface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                progressBar.setVisibility(View.GONE);
            }
        });

        mediaPlayer.setOnInfoListener(this);
        seekbar.setOnSeekBarChangeListener(new surfaceSeekBar());
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final MediaPlayer mediaPlayer) {
                mediaPlayer.start();

//                if(checkProgress!=null&&!checkProgress.isAlive()){
//                    checkProgress.start();
//                }
//                checkProgress = new Thread(){
//                    @Override
//                    public void run() {
//                        super.run();
//                        while(true){
//                            try {
//                                Thread.sleep(1000);
//                                if(mediaPlayer.isPlaying()){
//                                   //todo xxx  progressBar.setVisibility(View.GONE);
////                                    mHandler.sendEmptyMessage(0);
//                                }
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                };

            }

        });
        mediaPlayer.setOnCompletionListener(new MyOnCompletionListener());


    }

    @UiThread
    public void play(String url){
        mediaPlayer.setDisplay(surface.getHolder());
        progressBar.setVisibility(View.VISIBLE);
        mediaPlayer.reset();
        playUrl = url;
        if(StringUtils.isEmpty(playUrl))return;
        try {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(playUrl);
//            mediaPlayer.setDisplay(surface.getHolder());
            play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Background
    void retrieveDataFromServer() {
        courseDetail = rpcService.getCourseDetail(myPref.rpcUserId().get(),courseId);
        afterRetrieveDataFromServer();

    }

    @UiThread
    void afterRetrieveDataFromServer() {
        fragmentManager = getSupportFragmentManager();
        liveCourseDetailFragment = new LiveCourseDetailFragment_();
        liveCourseChapterFragment = new LiveCourseChapterFragment_();
        liveCourseExamFragment = new LiveCourseExamFragment_();
        liveCourseReferenceFragment = new LiveCourseReferenceFragment_();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.liveCourseFragment, liveCourseDetailFragment);
        transaction.add(R.id.liveCourseFragment, liveCourseChapterFragment);
        transaction.add(R.id.liveCourseFragment, liveCourseExamFragment);
        transaction.add(R.id.liveCourseFragment, liveCourseReferenceFragment);
        transaction.hide(liveCourseReferenceFragment);
        transaction.hide(liveCourseExamFragment);
        transaction.hide(liveCourseChapterFragment);
        transaction.show(liveCourseDetailFragment);
        currentFragment = liveCourseDetailFragment;
        transaction.commit();
        loading.dismiss();


        //播放器
        if(courseDetail.getChapter()!=null&&courseDetail.getChapter().size()>0){
            ChapterDto chapter = courseDetail.getChapter().get(0);
            if(chapter.getChapter()!=null&&chapter.getChapter().size()>0&&StringUtils.isEmpty(chapter.getDownloadUrl())){
//                play(chapter.getChapter().get(0).getDownloadUrl());
                playUrl = chapter.getChapter().get(0).getDownloadUrl();
            }
            else{
//                play(chapter.getDownloadUrl());
                playUrl = chapter.getDownloadUrl();
            }

        }

        initPlayer();
        startUpdateLearningTime();
        play(playUrl);
    }

    @Click
    void detailTabClicked() {
        switchFragment(liveCourseDetailFragment);
        invisibleAll();
        curveMarkerDetail.setVisibility(View.VISIBLE);
    }

    @Click
    void chapterTabClicked() {
        switchFragment(liveCourseChapterFragment);
        invisibleAll();
        curveMarkerChapter.setVisibility(View.VISIBLE);
    }

    @Click
    void examTabClicked() {
        switchFragment(liveCourseExamFragment);
        invisibleAll();
        curveMarkerExam.setVisibility(View.VISIBLE);
    }

    @Click
    void referenceTabClicked() {
        switchFragment(liveCourseReferenceFragment);
        invisibleAll();
        curveMarkerReference.setVisibility(View.VISIBLE);
    }

    @Click
    void backClicked() {
        finish();
    }

    @Click
    void videoContainerClicked(){
        if (centerPlay.getVisibility() == View.VISIBLE) {
            try {
                centerPlay.setVisibility(View.GONE);
                play();
                issrt.setImageDrawable(getResources().getDrawable(R.drawable.pause));
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            if (btm.getVisibility() == View.VISIBLE)
                btm.setVisibility(View.GONE);
            else
                btm.setVisibility(View.VISIBLE);
        }
    }


    @Click
    void fullScreenClicked(){
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Click
    void issrtClicked(){

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            issrt.setImageDrawable(getResources().getDrawable(R.drawable.player));
        } else {
            mediaPlayer.start();
            issrt.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        }
    }

    void invisibleAll() {
        curveMarkerDetail.setVisibility(View.INVISIBLE);
        curveMarkerChapter.setVisibility(View.INVISIBLE);
        curveMarkerExam.setVisibility(View.INVISIBLE);
        curveMarkerReference.setVisibility(View.INVISIBLE);
    }

    void switchFragment(Fragment to) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.hide(currentFragment).show(to).commit();
        currentFragment = to;
    }

    public CourseDetailDto getCourseDetail() {
        return courseDetail;
    }

    public String getUrl() {
        return url;
    }

    @Override
    protected void onStop() {
        checkProgress=null;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        threadUpdateGrogress = null;

//        updateLearningTime();
        super.onStop();
    }

//    void updateLearningTime() {
//        CourseDetailDto courseDetailDto = liveCourseChapterFragment.getCourseDetailDto();
//        if (courseDetailDto != null) {
//            //更新到数据库中
//            for (ChapterDto chapterDto : courseDetailDto.getChapter()) {
//
//            }
//        }
//    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        commitLearningLog();
        Intent i = new Intent("org.androidannotations.updateLearningTimeAfterCommitLog");
        sendBroadcast(i);
        super.onDestroy();
    }

    @Background
    void commitLearningLog(){
        try{
            //提交学习记录
            rpcService.commitLearningLog(myPref.rpcUserId().get(), DateUtils.ConvertDateToNormalString(new Date()),liveCourseDetailFragment.getDuration()+"",courseId,liveCourseChapterFragment.getLastChapterId()+"","0");

        }catch (Exception e){
            e.printStackTrace();
        }
    }



    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {

        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                issrt.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                progressBar.setVisibility(View.VISIBLE);
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                progressBar.setVisibility(View.GONE);
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                progressBar.setVisibility(View.GONE);
                break;
            default:
                progressBar.setVisibility(View.VISIBLE);
                break;
        }
        if(mediaPlayer.isPlaying()){

            progressBar.setVisibility(View.GONE);
            issrt.setImageDrawable(getResources().getDrawable(R.drawable.pause));
            return true;
        }
        return true;
    }

    private final class surfaceSeekBar implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            seekBar.setProgress(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
            int value = seekbar.getProgress() * mediaPlayer.getDuration() // 计算进度条需要前进的位置数据大小
                    / seekbar.getMax();
            mediaPlayer.seekTo(value);
            progressBar.setVisibility(View.VISIBLE);
        }

    }

    public String getPlayUrl() {
        return playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }

    /**
     * 切换播放源
     */
    @Receiver(actions = "org.androidannotations.play", registerAt = Receiver.RegisterAt.OnCreateOnDestroy)
    public void play(Intent intent){
        if(intent==null)return;
        playUrl = intent.getExtras().getString("playUrl");
        if(StringUtils.isEmpty(playUrl))return;
        try {
            mediaPlayer.pause();

            play(playUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }



    @Override
    protected void onResume() {
        super.onResume();
//        if(mediaPlayer==null){
//            try {
//                initPlayer();
//                play(playUrl);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }else{
//            try{
//                mediaPlayer.prepareAsync();
//            }catch (Exception e){
//                initPlayer();
//            }
//
//        }

    }

    @Receiver(actions = "org.androidannotations.downloadCompelte", registerAt = Receiver.RegisterAt.OnResumeOnPause)
    public void downLoadComplete(Intent i) {
        showDownLoadHint(main,i.getExtras().getString("name"));
    }

    private PopupWindow downLoadCompleteHint;

    public void showDownLoadHint(View anchor,String title) {
        View view = LayoutInflater.from(this).inflate(R.layout.pop_download_complete, null);
        ((TextView) view.findViewById(R.id.title)).setText(title);

        downLoadCompleteHint = new PopupWindow(view, getWindow().getWindowManager().getDefaultDisplay().getWidth(), 150, false);
        downLoadCompleteHint.setOutsideTouchable(true);
        downLoadCompleteHint.showAtLocation(anchor, Gravity.NO_GRAVITY,0,0);
        dismissPopUp();
    }

    @UiThread(delay = 2000)
    void dismissPopUp(){
        if (downLoadCompleteHint != null && downLoadCompleteHint.isShowing()) {
            try{
                downLoadCompleteHint.dismiss();
            }catch (Exception e){

            }
        }
    }

    public long getSeekTo() {
        return seekTo;
    }

    public void setSeekTo(long seekTo) {
        this.seekTo = seekTo;
    }


    //视频总部代码
    private MediaPlayer mediaPlayer;
    private upDateSeekBar update; // 更新进度条用
    private boolean isplayingFlag = true; // 用于判断视频是否在播放中


    class upDateSeekBar implements Runnable {

        @Override
        public void run() {
            mHandler.sendMessage(Message.obtain());
            if (mediaPlayer.isPlaying()) {
                mHandler.postDelayed(update, 1000);
            }
        }
    }

    /**
     * 更新进度条
     */
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mediaPlayer == null) {
                isplayingFlag = false;
            } else if (mediaPlayer.isPlaying()) {
                issrt.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                progressBar.setVisibility(View.GONE);
                isplayingFlag = true;

                int position = mediaPlayer.getCurrentPosition();
                int mMax = mediaPlayer.getDuration();
                int sMax = seekbar.getMax();
                if(mMax==0)return;
                seekbar.setProgress(position * sMax / mMax);
            } else {
                progressBar.setVisibility(View.VISIBLE);
                issrt.setImageDrawable(getResources().getDrawable(R.drawable.player));
                isplayingFlag = false;
                return;
            }
        }
    };

    public void play() throws IllegalArgumentException, SecurityException,
            IllegalStateException, IOException {
        seekbar.setProgress(0);
        if(threadUpdateGrogress==null)
        threadUpdateGrogress = new Thread(update);
        try{
            if(!threadUpdateGrogress.isAlive())
            threadUpdateGrogress.start();
        }catch (java.lang.IllegalThreadStateException e){}

        progressBar.setVisibility(View.VISIBLE);
        mediaPlayer.prepareAsync();
    }

    private final class MyOnCompletionListener implements MediaPlayer.OnCompletionListener{

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            progressBar.setVisibility(View.GONE);
//            mediaPlayer.seekTo(0);
            seekbar.setProgress(0);
            issrt.setImageDrawable(getResources().getDrawable(R.drawable.player));
        }
    }

    private class SurfaceViewLis implements SurfaceHolder.Callback {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (postion == 0) {
                try {

                    // 把视频输出到SurfaceView上
                    mediaPlayer.setDisplay(surface.getHolder());
                    play(playUrl);

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }

            }

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        // 检测屏幕的方向：纵向或横向
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 当前为横屏， 在此处添加额外的处理代码
            btm.setVisibility(View.GONE);
            frameContainer.setVisibility(View.GONE);

            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            int mSurfaceViewWidth = dm.widthPixels;
            int mSurfaceViewHeight = dm.heightPixels;
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            lp.width = mSurfaceViewWidth;
            lp.height = mSurfaceViewHeight;

            surface.setLayoutParams(lp);
            surface.getHolder().setFixedSize(mSurfaceViewWidth,
                    mSurfaceViewHeight);

            videoContainer.setLayoutParams(lp);


        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 当前为竖屏， 在此处添加额外的处理代码
            header.setVisibility(View.VISIBLE);

            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            int mSurfaceViewWidth = dm.widthPixels;
            int mSurfaceViewHeight = dm.heightPixels;
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.FILL_PARENT);

            lp.width = mSurfaceViewWidth;
            lp.height = mSurfaceViewHeight * 1 / 3;
            lp.addRule(RelativeLayout.BELOW,R.id.header);
            videoContainer.setLayoutParams(lp);
            frameContainer.setVisibility(View.VISIBLE);

        }
        super.onConfigurationChanged(newConfig);
    }
}
