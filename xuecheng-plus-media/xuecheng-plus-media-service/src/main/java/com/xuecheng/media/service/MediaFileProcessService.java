package com.xuecheng.media.service;

import com.xuecheng.media.model.po.MediaProcess;

import java.util.List;

/**
 * 待处理媒体文件业务层
 *
 * @author 张杨
 * @date 2024/07/18
 */
public interface MediaFileProcessService {

    /**
     * 获取待处理媒体文件列表
     *
     * @param shardIndex 碎片索引
     * @param shardTotal 碎片总
     * @param count      数
     * @return 列表<媒体进程>
     */
    List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count);

    /**
     * 开始任务
     *
     * @param id 任务id
     * @return boolean 是否成功
     */
    boolean startTask(long id);

    /**
     * @description 保存任务结果
     * @param taskId  任务id
     * @param status 任务状态
     * @param fileId  文件id
     * @param url url
     * @param errorMsg 错误信息
     * @return void
     * @author Mr.M
     * @date 2022/10/15 11:29
     */
    void saveProcessFinishStatus(Long taskId,String status,String fileId,String url,String errorMsg);
}
