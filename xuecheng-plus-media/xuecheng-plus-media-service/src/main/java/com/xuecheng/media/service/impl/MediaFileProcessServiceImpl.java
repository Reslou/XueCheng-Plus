package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MediaFileProcessServiceImpl implements MediaFileProcessService {

    private static final Logger log = LoggerFactory.getLogger(MediaFileProcessServiceImpl.class);
    private final MediaFilesMapper mediaFilesMapper;
    private final MediaProcessMapper mediaProcessMapper;
    private final MediaProcessHistoryMapper mediaProcessHistoryMapper;

    /**
     * 获取待处理媒体文件列表
     *
     * @param shardIndex 碎片索引
     * @param shardTotal 碎片总
     * @param count      数
     * @return 列表<媒体进程>
     */
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
    }

    /**
     * 开始任务
     *
     * @param id 任务id
     * @return boolean 是否成功
     */
    @Override
    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        String m = "m";
        if (result <= 0) {
            return false;
        }
        return true;
    }

    /**
     * 保存任务完成状态
     *
     * @param taskId   任务id
     * @param status   状态
     * @param fileId   文件标识
     * @param url      url
     * @param errorMsg
     */
    @Transactional
    @Override
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        //尝试获取任务
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if (mediaProcess == null) {
            return;
        }
        //任务处理失败
        LambdaQueryWrapper<MediaProcess> wrapper = new LambdaQueryWrapper<MediaProcess>()
                .eq(MediaProcess::getId, taskId);
        if (status.equals("3")) {
            MediaProcess mp = new MediaProcess();
            mp.setStatus("3");
            mp.setErrormsg(errorMsg);
            mp.setFailCount(mediaProcess.getFailCount() + 1);
            mediaProcessMapper.update(mp, wrapper);
            log.error("更新任务处理状态为失败，任务信息:{}", mp);
            return;
        }
        //任务处理成功
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        if (mediaFiles != null) {
            mediaFiles.setUrl(url);
            mediaFilesMapper.updateById(mediaFiles);
        }

        mediaProcess.setUrl(url);
        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcessMapper.updateById(mediaProcess);

        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess, mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        mediaProcessMapper.deleteById(mediaProcess.getId());
    }
}
