// ============================================================================
//
// Copyright (C) 2006-2013 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataquality.record.linkage.ui.composite.chart;

import java.awt.Color;
import java.awt.Paint;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.talend.dataquality.record.linkage.ui.composite.utils.MatchRuleColorRegistry;
import org.talend.dataquality.record.linkage.ui.i18n.internal.DefaultMessagesImpl;

/**
 * DOC yyi class global comment. Detailled comment
 */
public class MatchRuleDataChart extends Composite {

    private static Logger log = Logger.getLogger(MatchRuleDataChart.class);

    public static final Color[] COLOR_LIST = MatchRuleColorRegistry.getColorsForAwt();

    private List<String[]> previewData;

    private ChartComposite jfreeChartComp;

    private int gidColumn, grpSizeColumn, masterColumn, scoreColumn;

    private int times = 0;

    private String[] viewColumn = null;

    /**
     * DOC yyi DataChart constructor comment.
     * 
     * @param parent
     * @param style
     */
    public MatchRuleDataChart(Composite parent, List<String[]> viewData, String[] viewColumn) {
        super(parent, SWT.NONE);

        this.setLayout(new FillLayout());
        // make the size of the chart full fill the area
        if (this.getParent().getLayout() instanceof GridLayout) {
            GridData data = new GridData(GridData.FILL_BOTH);
            this.setLayoutData(data);
        }

        this.viewColumn = viewColumn;
        initChartData(viewData);
    }

    public boolean initChartData(List<String[]> viewData) {
        if (viewData != null && this.jfreeChartComp == null) {

            this.previewData = viewData;

            // MOD yyi 2012-01-11 TDQ-4362:fix the bug when checking the distance detail option
            List<String> header = java.util.Arrays.asList(viewColumn);
            this.gidColumn = header.indexOf("GID"); //$NON-NLS-1$
            this.grpSizeColumn = header.indexOf("GRP_SIZE"); //$NON-NLS-1$
            this.masterColumn = header.indexOf("MASTER"); //$NON-NLS-1$
            this.scoreColumn = header.indexOf("SCORE"); //$NON-NLS-1$
            createChart();
            return true;
        }
        return false;
    }

    /**
     * DOC yyi Comment method "createChart".
     */
    private void createChart() {
        Composite composite = new Composite(this, SWT.NONE);
        composite.setLayout(new FillLayout());
        JFreeChart jfreechart = createChart(createDataset());
        this.jfreeChartComp = new ChartComposite(composite, SWT.NONE, jfreechart, true);

    }

    private JFreeChart createChart(CategoryDataset categorydataset) {
        JFreeChart localJFreeChart = ChartFactory.createBarChart(null,
                DefaultMessagesImpl.getString("DataChart.0"), "#group", categorydataset, PlotOrientation.VERTICAL, //$NON-NLS-1$ //$NON-NLS-2$
                false, true, false);

        localJFreeChart.addSubtitle(new TextTitle(DefaultMessagesImpl.getString(
                "DataChart.title", sumItemCount(categorydataset), sumGroupCount(categorydataset)))); //$NON-NLS-1$
        CategoryPlot plot = (CategoryPlot) localJFreeChart.getPlot();

        CustomRenderer customrenderer = new CustomRenderer(COLOR_LIST);
        customrenderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        customrenderer.setBaseItemLabelsVisible(true);
        plot.setRenderer(customrenderer);

        CategoryAxis localCategoryAxis = plot.getDomainAxis();
        localCategoryAxis.setCategoryMargin(0.25D);
        localCategoryAxis.setUpperMargin(0.02D);
        localCategoryAxis.setLowerMargin(0.02D);

        NumberAxis localNumberAxis = (NumberAxis) plot.getRangeAxis();
        localNumberAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        localNumberAxis.setUpperMargin(0.1D);
        return localJFreeChart;
    }

    protected double sumItemCount(CategoryDataset categorydataset) {
        double itemCount = 0;
        for (int i = 0; i < categorydataset.getColumnCount(); i++) {
            int columnKey = Integer.valueOf(categorydataset.getColumnKey(i).toString());
            itemCount += categorydataset.getValue(0, i).intValue() * columnKey;
        }
        return itemCount;
    }

    protected double sumGroupCount(CategoryDataset categorydataset) {
        double groupCount = 0.0;
        for (int i = 0; i < categorydataset.getColumnCount(); i++) {
            groupCount += categorydataset.getValue(0, i).doubleValue();
        }
        return groupCount;
    }

    private CategoryDataset createDataset() {
        String s = DefaultMessagesImpl.getString("DataChart.4"); //$NON-NLS-1$
        DefaultCategoryDataset defaultcategorydataset = new DefaultCategoryDataset();

        // grp count, itmes
        final Map<String, Integer> groupCounts = new HashMap<String, Integer>();

        for (String[] values : this.previewData) {
            if (values.length <= masterColumn) {
                // TODO zshen check this exceptional case
                log.error("Array size " + values.length + " less than " + masterColumn);
            }
            if (Boolean.valueOf(values[masterColumn])) {
                if (null == groupCounts.get(values[grpSizeColumn])) {
                    groupCounts.put(values[grpSizeColumn], 1);
                } else {
                    int i = groupCounts.get(values[grpSizeColumn]) + 1;
                    groupCounts.put(values[grpSizeColumn], i);
                }
            }
        }

        // sort dataset
        String[] array = groupCounts.keySet().toArray(new String[0]);
        List<String> groups = Arrays.asList(array);
        Collections.sort(groups, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                // return groupCounts.get(o2).compareTo(groupCounts.get(o1));
                return Integer.parseInt(o1) - Integer.parseInt(o2);
            }
        });
        for (String count : groups) {
            if (Integer.parseInt(count) > times - 1) {
                defaultcategorydataset.addValue(groupCounts.get(count), s, count);
            }
        }
        return defaultcategorydataset;
    }

    public ChartComposite getChartComp() {
        return jfreeChartComp;
    }

    public void refresh(List<String[]> viewData) {
        this.previewData = viewData;
        initChartData(viewData);
        jfreeChartComp.setChart(createChart(createDataset()));
        jfreeChartComp.forceRedraw();
    }

    /**
     * DOC set spinner value
     * 
     * @param times
     */
    public void setTimes(int times) {
        this.times = times;
    }

    public int getTimes() {
        return times;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        if (jfreeChartComp != null) {
            jfreeChartComp.dispose();
        }
    }

    /**
     * DOC yyi DataChart class global comment. Detailled comment
     */
    class CustomRenderer extends BarRenderer {

        private Paint[] colors;

        public CustomRenderer(Paint[] apaint) {
            colors = apaint;
        }

        @Override
        public Paint getItemPaint(int i, int j) {
            CategoryDataset categorydataset = getPlot().getDataset();
            int m = Integer.parseInt(categorydataset.getColumnKeys().get(j).toString());
            return colors[m % colors.length];
        }
    }

}