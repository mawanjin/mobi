package com.join.mobi.fragment;

import android.support.v4.app.Fragment;
import android.widget.ListView;
import com.join.android.app.common.R;
import com.join.android.app.common.db.manager.ChapterManager;
import com.join.android.app.common.db.manager.CourseManager;
import com.join.android.app.common.db.manager.LocalCourseManager;
import com.join.android.app.common.db.tables.Chapter;
import com.join.android.app.common.db.tables.LocalCourse;
import com.join.android.app.common.manager.DialogManager;
import com.join.mobi.activity.LiveCourseDetailActivity_;
import com.join.mobi.adapter.LiveCourseChapterAdapter;
import com.join.mobi.dto.ChapterDto;
import com.join.mobi.dto.CourseDetailDto;
import com.join.mobi.enums.Dtype;
import com.php25.PDownload.DownloadApplication;
import com.php25.PDownload.DownloadTool;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: mawanjin@join-cn.com
 * Date: 14-9-10
 * Time: 上午11:41
 */
@EFragment(R.layout.livecourse_chapter_fragment_layout)
public class LiveCourseChapterFragment extends Fragment {

    @ViewById
    ListView listView;
    LiveCourseChapterAdapter liveCourseChapterAdapter;
    CourseDetailDto courseDetailDto;
    String url;
    //学习总时长
    String totalDuration;

    @AfterViews
    void afterViews() {

        courseDetailDto = ((LiveCourseDetailActivity_) getActivity()).getCourseDetail();
        url = ((LiveCourseDetailActivity_) getActivity()).getUrl();
        totalDuration = CourseManager.getInstance().findForId(courseDetailDto.getCourseId()).getTotalDuration();

        liveCourseChapterAdapter = new LiveCourseChapterAdapter(getActivity(),courseDetailDto.getChapters(),new LiveCourseChapterAdapter.Download(){
            @Override
            public void download(ChapterDto chapterDto) {
                doDownload(chapterDto);
            }
        });
        listView.setAdapter(liveCourseChapterAdapter);
        liveCourseChapterAdapter.notifyDataSetChanged();
    }

    void doDownload(ChapterDto chapter) {
        //首先判断本地课程主表记录是否存在
        Map<String,Object> params = new HashMap<String, Object>(0);
        params.put("courseId",courseDetailDto.getCourseId());
        LocalCourse course;
        List<LocalCourse> courseList  = LocalCourseManager.getInstance().findForParams(params);
        if(courseList==null||courseList.size()==0){
            LocalCourse entity = new LocalCourse();
            entity.setCourseId(courseDetailDto.getCourseId());
            entity.setCourseHour(courseDetailDto.getCourseHour());
            entity.setBranch(courseDetailDto.getBranch());
            entity.setCreateTime(courseDetailDto.getCreateTime());
            entity.setDescription(courseDetailDto.getDescription());
            entity.setTitle(courseDetailDto.getName());
            entity.setValidUntil(courseDetailDto.getValidUntil());
            entity.setUrl(url);
            entity.setLearningTimes(Integer.parseInt(totalDuration));
            course = LocalCourseManager.getInstance().saveIfNotExists(entity);
        }else{
            course = courseList.get(0);
        }

        //判断该章节是否已经存在
        Map<String,Object> chapterParams = new HashMap<String, Object>(0);
        chapterParams.put("chapterId",chapter.getChapterId());
        chapterParams.put("localcourse_id",course.getCourseId());

        List<Chapter> chapters = ChapterManager.getInstance().findForParams(chapterParams);
        if(chapters==null||chapters.size()==0){
            Chapter entity = new Chapter();
            entity.setLocalCourse(course);
            entity.setTitle(chapter.getTitle());
            entity.setChapterId(chapter.getChapterId());
            entity.setFilesize(chapter.getFilesize());
            entity.setLearnedTime(chapter.getLearnedTime());
            entity.setChapterDuration(chapter.getChapterDuration());
            entity.setDownloadUrl(chapter.getDownloadUrl());

            ChapterManager.getInstance().save(entity);
            DialogManager.getInstance().makeText(getActivity(),"开始下载",DialogManager.DIALOG_TYPE_OK);
        }else{
            DialogManager.getInstance().makeText(getActivity(),"正在下载,或已下载.",DialogManager.DIALOG_TYPE_OK);
        }

        //下载
        DownloadTool.startDownload((DownloadApplication) getActivity().getApplicationContext(), chapter.getDownloadUrl(), chapter.getTitle(), Dtype.Chapter,  "0");

    }

}