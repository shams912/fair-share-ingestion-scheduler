package com.example.scheduler.metrics;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MetricsChartGenerator {

    private MetricsChartGenerator() {
    }

    public static void generateCharts(String csvFile, String group) throws IOException {

        List<Metric> metrics = readMetrics(csvFile);

        generateQueueDepthChart(metrics, group);
    }

    private static List<Metric> readMetrics(String csvFile) throws IOException {

        List<String> lines = Files.readAllLines(Path.of(csvFile));

        List<Metric> metrics = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {

            String[] cols = lines.get(i).split(",");

            metrics.add(new Metric(
                    Long.parseLong(cols[0]),
                    cols[1],
                    cols[2],
                    Integer.parseInt(cols[3]),
                    Long.parseLong(cols[4]),
                    Long.parseLong(cols[5]),
                    Integer.parseInt(cols[6])
            ));
        }

        return metrics;
    }

    private static void generateQueueDepthChart(List<Metric> metrics, String group)
            throws IOException {

        XYChart chart = new XYChartBuilder()
                .width(1200)
                .height(700)
                .title(group + " Queue Depth vs Timestamp")
                .xAxisTitle("Timestamp")
                .yAxisTitle("Queue Depth")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setMarkerSize(5);

        metrics.stream()
                .map(Metric::tenant)
                .distinct()
                .forEach(tenant -> {

                    List<Long> timestamps = new ArrayList<>();
                    List<Integer> queueDepths = new ArrayList<>();

                    metrics.stream()
                            .filter(m -> m.tenant().equals(tenant))
                            .forEach(m -> {
                                timestamps.add(m.timestamp());
                                queueDepths.add(m.queueDepth());
                            });

                    chart.addSeries(
                            tenant,
                            timestamps,
                            queueDepths);
                });

        Files.createDirectories(Path.of("charts"));

        BitmapEncoder.saveBitmap(
                chart,
                "charts/queue-depth-vs-time-" + group,
                BitmapEncoder.BitmapFormat.PNG);

        System.out.println("Generated charts/queue-depth-vs-time-"+group+".png");
    }

    record Metric(
            long timestamp,
            String tier,
            String tenant,
            int sequence,
            long waitTimeMs,
            long execTimeMs,
            int queueDepth) {
    }
}