package com.yy.tool.servermonitor;

import com.yy.log.Logger;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.util.*;


/**
 * 监听器。
 *
 * @author Luowen
 * @version 1.0
 * @since 2018-02-06
 */
public class Watcher {

    /**
     * 默认监听采集周期，单位秒。
     */
    private static final int DEFAULT_PERIOD = 5;
    private static int period = DEFAULT_PERIOD;


    private Reportor reportor;

    private SystemInfo instance;
    private HardwareAbstractionLayer hardware;

    private Timer timer;


    /**
     * 构造函数。
     *
     * @param reportor
     */
    public Watcher(Reportor reportor) {

        this.reportor = reportor;

        instance = new SystemInfo();
        hardware = instance.getHardware();
    }


    /**
     * 设置采集周期。
     *
     * @param period
     */
    public static void setPeriod(int period) {

        Watcher.period = period > 0 ? period : DEFAULT_PERIOD;
    }


    /**
     * 获取采集周期。
     *
     * @return
     */
    public static int getPeriod() {

        return period;
    }


    /**
     * 开始采集。
     */
    public void start() {

        int p = period * 1000;


        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    report();
                } catch (Exception e) {
                    Logger.printStackTrace(e);
                }
            }
        }, p, p);
    }


    /**
     * 采集并上报。
     */
    private void report() {

        Map<String, Object> data = new HashMap<>();


        /*
         * 各 CPU 的使用百分比。
         * 如： [0.13046417305092384]
         */
        data.put("cpus", hardware.getProcessor().getProcessorCpuLoadBetweenTicks(null));

        /*
         * CPU 温度。
         * 如：69.8°C
         */
        data.put("cpuTemperature", hardware.getSensors().getCpuTemperature());

        /*
         * 进程数 。
         */
        data.put("processCount", instance.getOperatingSystem().getProcessCount());


        /*
         * 内存。
         * 可用内存、总共内存、交换区已用、交换区大小
         */
        GlobalMemory memory = hardware.getMemory();

        Map<String, Long> memoryData = new HashMap<>();
        memoryData.put("available", memory.getAvailable());
        memoryData.put("total", memory.getTotal());

        data.put("memory", memoryData);


        /*
         * 磁盘。
         * 读、写、总共
         */
        HWDiskStore[] disks = hardware.getDiskStores();
        List<Map<String, Object>> diskData = new ArrayList<>();

        for (HWDiskStore item : disks) {
            Map<String, Object> map = new HashMap<>();
            map.put("model", item.getModel());
            map.put("readBytes", item.getReadBytes());
            map.put("writeBytes", item.getWriteBytes());

            diskData.add(map);
        }
        data.put("disk", diskData);


        /*
         * 网络。
         * 字节发、字节收、包发、包收、速度。
         */
        NetworkIF[] networks = hardware.getNetworkIFs();
        List<Map<String, Object>> networkData = new ArrayList<>();

        for (NetworkIF item : networks) {
            Map<String, Object> map = new HashMap<>();

            map.put("name", item.getName());
            map.put("bytesSend", item.getBytesSent());
            map.put("bytesRecv", item.getBytesRecv());
            map.put("packetsSent", item.getPacketsSent());
            map.put("packetsRecv", item.getPacketsRecv());
            map.put("speed", item.getSpeed());

            networkData.add(map);
        }
        data.put("network", networkData);


        // 开始上报。
        reportor.report(data);
    }
}
