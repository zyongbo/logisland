package com.hurence.connect.spooldir;

import com.google.common.io.PatternFilenameFilter;
import com.hurence.logisland.utils.SynchronizedFileLister;
import com.sun.management.UnixOperatingSystemMXBean;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.regex.Pattern;

public class SynchronizedFileListerTest {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizedFileListerTest.class);
    @Test
    public void test1() throws Exception {
        logger.info("ton cul");
        logger.debug("ton cul debug");
        logger.trace("ton cul trace");
        final String processingSuffix = ".PROCESSING";
        final String inputPath = getClass().getResource("/spooldir").getFile();
        final File inputDir = new File(inputPath);
        PatternFilenameFilter inputFilenameFilter = new PatternFilenameFilter(Pattern.compile("^.*.csv$"));
        SynchronizedFileLister fileLsiter = SynchronizedFileLister.getInstance(
                inputDir, inputFilenameFilter, 0, processingSuffix
        );
        Assert.assertNull(fileLsiter.take());
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        fileLsiter.updateList();
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        fileLsiter.updateList();
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        fileLsiter.updateList();
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        fileLsiter.updateList();
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        fileLsiter.updateList();
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        fileLsiter.updateList();
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        fileLsiter.updateList();
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        fileLsiter.updateList();
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        fileLsiter.updateList();
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        fileLsiter.updateList();
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        File firstFileInQueue = fileLsiter.take();

        Assert.assertNotNull(firstFileInQueue);

        final File outputDir = new File(inputDir.getParent(), "spooldir-finished");
        final File firstFileInQueueProcessingFile = new File(firstFileInQueue.getParent(), firstFileInQueue.getName() + processingSuffix);
        Assert.assertTrue(firstFileInQueue.exists());
        Assert.assertTrue(firstFileInQueueProcessingFile.exists());
        fileLsiter.moveTo(firstFileInQueue, inputDir, outputDir);
        logger.info("Number of open file descriptors is {}", ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
        Assert.assertFalse(firstFileInQueue.exists());
        Assert.assertFalse(firstFileInQueueProcessingFile.exists());
    }

}