package com;

import android.app.Activity;
import com.join.android.app.common.db.DatabaseHelper;
import com.join.android.app.common.db.manager.*;
import com.join.android.app.common.db.tables.Course;
import com.join.android.app.common.db.tables.Live;
import com.join.android.app.common.db.tables.Notice;
import com.join.android.app.common.db.tables.ResourceShare;
import com.join.android.app.common.utils.BeanUtils;
import com.join.mobi.dto.*;

import java.util.List;

/**
 * User: mawanjin@join-cn.com
 * Date: 14-2-10
 * Time: 下午3:20
 */
public class BaseActivity extends Activity {

    private DatabaseHelper databaseHelper = null;

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
        List<LiveCourseDto> liveCourseDtos = mainContent.getCourses();
        List<ResourceShareDto> resourceShareDtos = mainContent.getResourceShares();
        List<NoticeDto> noticeDtos = mainContent.getNotices();

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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

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

}
