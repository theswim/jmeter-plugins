package kg.apc.jmeter.vizualizers;

import kg.apc.charting.AbstractGraphRow;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.jmeter.graphs.AbstractOverTimeVisualizer;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.gui.RateRenderer;

import java.awt.*;
import java.text.DecimalFormatSymbols;

public class TransactionThroughputOverTimeGui
        extends AbstractOverTimeVisualizer {

    /**
     *
     */
    public TransactionThroughputOverTimeGui() {
        super();
        graphPanel.getGraphObject().setyAxisLabelRenderer(new CustomRateRenderer("#.0"));
        graphPanel.getGraphObject().setYAxisLabel("Transaction Throughput /sec");
    }

    private void addThreadGroupRecord(String threadGroupName, int numThreads, long time,
                                      long responseTime) {
        String labelAgg = "Overall Transaction Throughput";
        AbstractGraphRow row = model.get(threadGroupName);
        AbstractGraphRow rowAgg = modelAggregate.get(labelAgg);
        if (row == null) {
            row = getNewRow(model, AbstractGraphRow.ROW_AVERAGES, threadGroupName, AbstractGraphRow.MARKER_SIZE_SMALL, false, false, false, true, true);
        }
        if (rowAgg == null) {
            rowAgg = getNewRow(modelAggregate, AbstractGraphRow.ROW_AVERAGES, labelAgg, AbstractGraphRow.MARKER_SIZE_SMALL, false, false, false, true, Color.RED, true);
        }

        double tps = numThreads * (1000d / responseTime);

        row.add(time, tps);
        rowAgg.add(time, tps);
    }

    public String getLabelResource() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getStaticLabel() {
        return JMeterPluginsUtils.prefixLabel("Transaction Throughput Over Time");
    }

    @Override
    public void add(SampleResult res) {
        if (!isSampleIncluded(res)) {
            return;
        }
        super.add(res);
        addThreadGroupRecord(res.getSampleLabel(),
                res.getGroupThreads(),
                normalizeTime(res.getEndTime()), res.getTime());
        updateGui(null);
    }

    @Override
    protected JSettingsPanel createSettingsPanel() {
        return new JSettingsPanel(this,
                JSettingsPanel.TIMELINE_OPTION
                | JSettingsPanel.GRADIENT_OPTION
                | JSettingsPanel.FINAL_ZEROING_OPTION
                | JSettingsPanel.LIMIT_POINT_OPTION
                | JSettingsPanel.AGGREGATE_OPTION
                | JSettingsPanel.MAXY_OPTION
                | JSettingsPanel.RELATIVE_TIME_OPTION
                | JSettingsPanel.MARKERS_OPTION);
    }

    @Override
    public String getWikiPage() {
        return "TransactionThroughput";
    }

    private static class CustomRateRenderer
            extends RateRenderer
    {

        private String zeroLabel;

        public CustomRateRenderer(String format)
        {
            super(format);
            zeroLabel = "0" + new DecimalFormatSymbols().getDecimalSeparator() + "0/sec";
        }

        @Override
        public void setValue(Object value)
        {
            if (value != null && (value instanceof Double) && ((Double)value).doubleValue() == 0)
            {
                setText(zeroLabel);
            } else
            {
                super.setValue(value);
            }
        }
    }
}
