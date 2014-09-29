package com;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.join.android.app.common.R;
import com.join.android.app.common.db.DatabaseHelper;
import com.join.android.app.common.db.manager.*;
import com.join.android.app.common.db.tables.*;
import com.join.android.app.common.dialog.CommonDialogLoading;
import com.join.android.app.common.dialog.CommonDialogWithLetterLoading;
import com.join.android.app.common.utils.BeanUtils;
import com.join.mobi.dto.*;
import com.join.mobi.pref.PrefDef_;
import com.join.mobi.rpc.RPCService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: mawanjin@join-cn.com
 * Date: 14-2-10
 * Time: 下午3:20
 */
public class BaseActivity extends Activity {

    private PrefDef_ pref;
    private RPCService webService;
    private DatabaseHelper databaseHelper = null;
    private CommonDialogLoading loading;
    private CommonDialogWithLetterLoading letterLoading;
    private PopupWindow downLoadCompleteHint;


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        DBManager.getInstance(this).onDestroy();
    }

    public DatabaseHelper getHelper() {
        if (databaseHelper == null)
            databaseHelper = DBManager.getInstance(this).getHelper();

        return databaseHelper;
    }

    /**
     * db stuff
     * when needs multiple databases we need create database manually.
     * for example ,after login
     *
     * @param dbName
     */
    public void createDB(String dbName) {
        DBManager.getInstance(this).createDB(dbName);
    }

    /**
     * 更新live、course、resource_share、notice这四张表
     */
    public void updateMainContent(MainContentDto mainContent) {
        if (mainContent == null) return;
        LiveManager.getInstance().deleteAll();
        List<LiveDto> liveDtos = mainContent.getLives();
        List<LiveCourseDto> liveCourseDtos = mainContent.getCourse();
        List<ResourceShareDto> resourceShareDtos = mainContent.getResourceShare();
        List<NoticeDto> noticeDtos = mainContent.getNotice();

        if (liveDtos != null) {
            for (LiveDto liveDto : liveDtos) {
                Live live = new Live();
                try {
                    BeanUtils.copyProperties(live, liveDto);
                    LiveManager.getInstance().save(live);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        CourseManager.getInstance().deleteAll();
        if (liveCourseDtos != null) {
            for (LiveCourseDto liveCourseDto : liveCourseDtos) {
                Course course = new Course();
                try {
                    BeanUtils.copyProperties(course, liveCourseDto);
                    CourseManager.getInstance().save(course);

                    //判断是否某课程已下载到本地
                    Map<String, Object> params = new HashMap<String, Object>(0);
                    params.put("courseId", course.getCourseId());
                    List<LocalCourse> localCourses = LocalCourseManager.getInstance().findForParams(params);
                    if (localCourses != null && localCourses.size() > 0) {
                        //更新学习总时长
                        localCourses.get(0).setLearningTimes(Integer.parseInt(course.getTotalDuration()));
                        LocalCourseManager.getInstance().saveOrUpdate(localCourses.get(0));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        ResourceShareManager.getInstance().deleteAll();
        if (resourceShareDtos != null) {
            for (ResourceShareDto resourceShareDto : resourceShareDtos) {
                ResourceShare resourceShare = new ResourceShare();
                try {
                    BeanUtils.copyProperties(resourceShare, resourceShareDto);
                    ResourceShareManager.getInstance().save(resourceShare);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        NoticeManager.getInstance().deleteAll();
        if (noticeDtos != null) {
            for (NoticeDto noticeDto : noticeDtos) {
                Notice notice = new Notice();
                try {
                    BeanUtils.copyProperties(notice, noticeDto);
                    NoticeManager.getInstance().save(notice);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }


    }

    public void showLetterLoading() {
        letterLoading = new CommonDialogWithLetterLoading(this, getString(R.string.saving_exam_result));
        letterLoading.show();
    }

    public void dismissLetterLoading() {
        if (letterLoading == null) return;
        try {
            letterLoading.dismiss();
        } catch (Exception e) {
        }

    }

    public void showLoading() {
        loading = new CommonDialogLoading(this);
        loading.show();
    }

    public void dismissLoading() {
        if (loading == null) return;
        try {
            loading.dismiss();
        } catch (Exception e) {
        }

    }


    public MainContentDto refreshMainData() {
        MainContentDto mainContent;
        try {
//            mainContent = RPCTestData.getMainContentDto();
            mainContent = webService.getMainContent(pref.rpcUserId().get());

        } catch (Throwable e) {
            try {
                rpcException(e);
                e.printStackTrace();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            return null;
        }

        updateMainContent(mainContent);
        return mainContent;
    }

    public void rpcException(Throwable e) throws Throwable {
        throw e;
    }


    public RPCService getWebService() {
        return webService;
    }

    public void setWebService(RPCService webService) {
        this.webService = webService;
    }

    public PrefDef_ getPref() {
        return pref;
    }

    public void setPref(PrefDef_ pref) {
        this.pref = pref;
    }

    public void showDownLoadHint(View anchor,String title) {
        View view = LayoutInflater.from(this).inflate(R.layout.pop_download_complete, null);
        ((TextView) view.findViewById(R.id.title)).setText(title);

        downLoadCompleteHint = new PopupWindow(view, getWindow().getWindowManager().getDefaultDisplay().getWidth(), 150, false);
        downLoadCompleteHint.setOutsideTouchable(true);
        mHandler.sendMessageDelayed(new Message(),2000);

        downLoadCompleteHint.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, 0);
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (downLoadCompleteHint != null && downLoadCompleteHint.isShowing()) {
                downLoadCompleteHint.dismiss();
            }
        }
    };
}
