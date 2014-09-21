package com.join.mobi.fragment;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.join.android.app.common.R;
import com.join.mobi.activity.MyVideoViewBufferFullScreen_;
import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

/**
 * User: mawanjin@join-cn.com
 * Date: 14-9-21
 * Time: 下午11:15
 */
@EFragment(R.layout.video_fragment_layout)
public class VideoFragment extends Fragment implements MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener {

    private String path = "http://192.168.1.104/apple.mp4";
    private Uri uri;
    @ViewById(resName = "buffer")
    VideoView mVideoView;
    @ViewById(resName = "probar")
    ProgressBar pb;
    @ViewById(resName = "download_rate")
    TextView downloadRateView;
    @ViewById(resName = "load_rate")
    TextView loadRateView;

    @AfterViews
    void afterViews() {

        if (!LibsChecker.checkVitamioLibs(getActivity()))
            return;
        initVideo();
    }


    void initVideo() {
//        uri = Uri.parse(path);

//      mVideoView.setVideoURI(uri);

        mVideoView.setVideoPath(path);
        MediaController mediaController = new MediaController(getActivity()) {

        };

        mediaController.setMediaPlayerControlFullScreen(new MediaController.MediaPlayerControlFullScreen() {
            @Override
            public void onFullScreen() {
                MyVideoViewBufferFullScreen_.intent(getActivity()).flags(Intent.FLAG_ACTIVITY_NEW_TASK).seekTo(mVideoView.getCurrentPosition()).start();
                mVideoView.stopPlayback();
            }


        });

        mVideoView.setMediaController(mediaController);
        mVideoView.requestFocus();
        mVideoView.setOnInfoListener(this);
        mVideoView.setOnBufferingUpdateListener(this);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // optional need Vitamio 4.0
                mediaPlayer.setPlaybackSpeed(1.0f);
//                mediaPlayer.seekTo(seekTo);
            }
        });
    }


    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    pb.setVisibility(View.VISIBLE);
                    downloadRateView.setText("");
                    loadRateView.setText("");
                    downloadRateView.setVisibility(View.VISIBLE);
                    loadRateView.setVisibility(View.VISIBLE);

                }
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                mVideoView.start();
                pb.setVisibility(View.GONE);
                downloadRateView.setVisibility(View.GONE);
                loadRateView.setVisibility(View.GONE);
                startUpdateLearningTime();
                break;
            case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
                downloadRateView.setText("" + extra + "kb/s" + "  ");
                break;
        }
        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        loadRateView.setText(percent + "%");
    }

    private void startUpdateLearningTime() {
        //开始播放了,更新学习时间
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    while (mVideoView != null) {
                        while (mVideoView.isPlaying()) {
                            //更新学习时间
                            Intent intent = new Intent("org.androidannotations.updateLearningTime");
                            getActivity().sendBroadcast(intent);
                            Thread.sleep(1000);
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

}
