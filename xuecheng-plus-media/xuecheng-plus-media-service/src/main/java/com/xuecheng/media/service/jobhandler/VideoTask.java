package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 视频任务
 *
 * @author 张杨
 * @date 2024/07/19
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VideoTask {

    private final MediaFileService mediaFileService;
    private final MediaFileProcessService mediaFileProcessService;
    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegPath;

    /**
     * 分片广播任务
     */
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        //获取任务列表
        List<MediaProcess> mediaFileProcessList;
        int size;
        try {
            int processors = Runtime.getRuntime().availableProcessors();
            mediaFileProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
            size = mediaFileProcessList.size();
            log.debug("取出待处理视频任务{}条", size);
            if (size == 0) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //开启线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaFileProcessList.forEach(mediaProcess -> {
            threadPool.execute(() -> {
                try {
                    Long taskId = mediaProcess.getId();
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if (!b) {
                        return;
                    }
                    log.debug("开始执行任务:{}", mediaProcess);

                    //下载待处理文件
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();
                    String fileId = mediaProcess.getFileId();
                    File originalFile = mediaFileService.downloadFileFromMinIO(bucket, objectName);
                    if (originalFile == null) {
                        log.error("下载待处理文件失败,originalFile:{}", bucket.concat(objectName));
                        mediaFileProcessService.saveProcessFinishStatus(
                                taskId, "3", fileId, null, "下载待处理文件失败");
                        return;
                    }

                    //转码视频文件
                    File mp4;
                    try {
                        mp4 = File.createTempFile("mp4", ".mp4");
                    } catch (IOException e) {
                        log.error("创建mp4临时文件失败");
                        mediaFileProcessService.saveProcessFinishStatus(
                                taskId, "3", fileId, null, "创建mp4临时文件失败");
                        return;
                    }
                    String mp4Path = mp4.getAbsolutePath();
                    String result = "";
                    String videoPath = "";
                    try {
                        videoPath = originalFile.getAbsolutePath();
                        Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegPath, videoPath, mp4.getName(), mp4Path);
                        result = videoUtil.generateMp4();
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("处理视频文件:{},出错:{}", videoPath, e.getMessage());
                    }
                    if (!result.equals("success")) {
                        log.error("处理视频失败,视频地址:{},错误信息:{}", videoPath, result);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", fileId, null, result);
                        return;
                    }

                    //上传MP4到minio
                    String mp4ObjectName = getFilePath(fileId, ".mp4");
                    String url = "/" + bucket + "/" + mp4ObjectName;
                    try {
                        mediaFileService.addMediaFilesToMinIO(mp4Path, "video/mp4", bucket, mp4ObjectName);
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, null);
                    } catch (Exception e) {
                        log.error("上传视频或入库失败,视频地址:{},错误信息:{}", mp4Path, e.getMessage());
                        mediaFileProcessService.saveProcessFinishStatus(
                                taskId, "3", fileId, null, "上传视频或入库失败");
                    }

                } finally {
                    //每处理一个任务,计数器-1
                    countDownLatch.countDown();
                }
            });
        });
        //计数器等待30分钟或者计数器为零,该任务列表结束
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    /**
     * 获取文件路径
     *
     * @param fileMd5 文件md5
     * @param fileExt 文件ext
     * @return 字符串
     */
    private String getFilePath(String fileMd5, String fileExt) {
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
}