package com.example;

import com.example.jmx.Hello;
import com.opencsv.CSVWriter;
import com.sun.management.GarbageCollectionNotificationInfo;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.management.ManagementFactory.getGarbageCollectorMXBeans;

public class GcTrackingMainClass {

    private static final int number = 10000;
    private static final int minValue = 1000;
    private static final int maxValue = 1000000;
    private static final String CSV_FILE_NAME = "results/gc-results.csv";

    public static void main(String[] args) throws InterruptedException, MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, IOException {

        System.out.println("App is Running");

        // mbeans setup
        prepareJmxBeans();

        // add gc listener
        addGcListener();

        List<Integer> lst = new LinkedList<>();
        int iteration = 0;

        Supplier<Integer> supplier = () -> ThreadLocalRandom.current().nextInt(minValue, maxValue);

        while (true) {
//            Thread.sleep(100);

            List<Integer> collectList = Stream.generate(supplier).limit(number).collect(Collectors.toList());
            lst.addAll(collectList);

            Iterator<Integer> iterator = lst.iterator();
            for (int i = 0; i < number / 2; i++) {
                iterator.next();
                iterator.remove();

                // adding some local variables to consume stack memory
                int number = 10034;
                double doubleNumber = 234234.12324;
                String stringText = "dfsdgdfgsdfasd";
            }

            iteration++;
            System.out.println("Iteration № " + iteration + ". List Size Is: " + lst.size());
        }
    }

    private static void prepareJmxBeans() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("com.example.mbean:type=HelloMBeanImpl");

        Hello hello = new Hello();
        mbs.registerMBean(hello, name);
    }

    private static void addGcListener() throws IOException {

        File f = new File(CSV_FILE_NAME);
        String[] columnNames = "gcName,gcAction,gcCause,startTime,duration".split(",");

        if (!f.exists()) {
            CSVWriter writer = getConfiguredCsvWriter(f.getAbsolutePath(), false);
            writer.writeNext(columnNames);
            writer.close();
        }

        List<GarbageCollectorMXBean> gcbeans = getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcbean : gcbeans) {
            System.out.println("GC name:" + gcbean.getName());
            NotificationEmitter emitter = (NotificationEmitter) gcbean;
            NotificationListener listener = (notification, handback) -> {
                if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                    GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());

                    String gcName = info.getGcName();
                    String gcAction = info.getGcAction();
                    String gcCause = info.getGcCause();

                    long startTime = info.getGcInfo().getStartTime();
                    long duration = info.getGcInfo().getDuration();

                    System.out.println("start:" + startTime + " Name:" + gcName + ", action:" + gcAction + ", gcCause:" + gcCause + "(" + duration + " ms)");

                    // add data to csv
                    try {
                        CSVWriter writer = getConfiguredCsvWriter(f.getAbsolutePath(), true);
                        writer.writeNext(new String[]{gcName, gcAction, gcCause, String.valueOf(startTime), String.valueOf(duration)});
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            emitter.addNotificationListener(listener, null, null);
        }
    }

    private static CSVWriter getConfiguredCsvWriter(String fullPath, boolean append) throws IOException {
        FileWriter fileWriter;
        if (append) {
            fileWriter = new FileWriter(fullPath, true);
        } else {
            fileWriter = new FileWriter(fullPath);
        }
        return new CSVWriter(fileWriter,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
    }

}
