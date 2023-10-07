package com.mmg;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @Author: fan
 * @Date: 2023/10/7
 * @Description:
 */
@Slf4j
public class Application {
    public static void main(String... args) throws IOException {
        log.info("推送数据...");
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        FFmpegLogCallback.set();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(Files.newInputStream(Paths.get("C:\\temp\\TVC.mp4")), 0)) {
            grabber.setOption("nobuffer", "1");
            grabber.setVideoCodecName("h264_qsv");
            grabber.start();
            if (grabber.getFormatContext() == null || grabber.getFormatContext().nb_streams() < 1) {
                log.error("本地视频中没有流数据");
                return;
            }
            log.info("grabber.getVideoCodecName(); {}", grabber.getVideoCodecName());
            log.info("grabber.getVideoCodec(); {}", grabber.getVideoCodec());
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder("rtmp://localhost:1935/live/test",
                    grabber.getImageWidth(), grabber.getImageHeight())) {
                //视频编码格式编码器 intel核显使用h264_qsv（像素格式使用NV12）， Nvidia显卡使用hevc_nvenc 更多编码器和对应的像素格式使用ffmpeg -h encoder=h264查看
                recorder.setVideoCodecName("h264_qsv");
                recorder.setPixelFormat(avutil.AV_PIX_FMT_NV12); // 像素格式
                recorder.setFormat("flv");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC); // 音频编码格式
                recorder.setAudioBitrate(grabber.getAudioBitrate());
                recorder.setAudioChannels(grabber.getAudioChannels());
                recorder.setSampleRate(grabber.getSampleRate());
                try {
                    recorder.start();
                } catch (FFmpegFrameRecorder.Exception e) {
                    throw new RuntimeException(e);
                }
                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    recorder.record(frame);
                    //log.info("推送成功...{}", playHandle);
                }
            } catch (FrameRecorder.Exception e) {
                log.error("推送失败...{}", e.getMessage());
            }
        } catch (FrameGrabber.Exception e) {
            log.error("帧捕获失败...{}", e.getMessage());
        } finally {
            log.error("推送结束...");
        }
    }
}
