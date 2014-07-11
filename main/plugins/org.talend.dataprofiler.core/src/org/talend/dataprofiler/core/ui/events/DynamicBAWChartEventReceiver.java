// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.events;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.JFreeChart;
import org.talend.dataprofiler.common.ui.editor.preview.chart.ChartDecorator;
import org.talend.dataprofiler.core.ui.chart.TalendChartComposite;
import org.talend.dataprofiler.core.ui.editor.preview.ColumnIndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.dataset.CustomerDefaultBAWDataset;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.SummaryStatisticsState;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dq.indicators.IndicatorCommonUtil;
import org.talend.dq.nodes.indicator.type.IndicatorEnum;

/**
 * For Summary statistics indicators: when select all
 */
public class DynamicBAWChartEventReceiver extends DynamicChartEventReceiver {

    private CustomerDefaultBAWDataset bawDataset;

    private TalendChartComposite BAWparentComposite = null;

    private List<IndicatorUnit> indicators = new ArrayList<IndicatorUnit>();

    private DynamicChartEventReceiver IRQIndicatorEvent = null;

    private Indicator IRQIndicator = null;

    private DynamicChartEventReceiver rangeIndicatorEvent = null;

    private Indicator rangeIndicator = null;

    Map<IndicatorEnum, Double> summaryValues = new HashMap<IndicatorEnum, Double>();

    public DynamicChartEventReceiver createEventReceiver(IndicatorEnum type, Indicator oneIndicator) {
        // the receiver for each summary indicator, just need to put the indicator name and its related value into the
        // map, and call the method which judge the total
        DynamicChartEventReceiver eReceiver = new DynamicChartEventReceiver() {

            @Override
            public boolean handle(Object value) {
                if (isIntact()) {
                    addToSummaryMap(getIndicatorType(), value);
                    if (this.getTableViewer() != null) {
                        String str = value == null ? String.valueOf(Double.NaN) : String.valueOf(value);
                        this.refreshTable(str);
                    }
                } else {
                    super.handle(value);
                    updateValueOfIRQAndRange();
                }
                return true;
            }
        };
        eReceiver.setIndicatorType(type);
        eReceiver.setIndicatorName(oneIndicator.getName());
        indicators.add(new ColumnIndicatorUnit(type, oneIndicator, null));

        if (IndicatorEnum.IQRIndicatorEnum.equals(type)) {
            this.IRQIndicator = oneIndicator;
            this.IRQIndicatorEvent = eReceiver;
        } else if (IndicatorEnum.RangeIndicatorEnum.equals(type)) {
            this.rangeIndicator = oneIndicator;
            this.rangeIndicatorEvent = eReceiver;
        }
        return eReceiver;
    }

    private void addToSummaryMap(IndicatorEnum indicatorType, Object value) {
        Object indValue = value;
        if (value == null) {
            indValue = 0;
        }
        summaryValues.put(indicatorType, Double.parseDouble(String.valueOf(indValue)));

        refreshChart();
    }

    private boolean isIntact() {
        return indicators.size() == SummaryStatisticsState.FULL_FLAG;
    }

    // judge if all summary indicator has its result, if yes, create a item and refresh the BAW chart
    private void refreshChart() {
        // when the user didnot select all summary indicators
        if (isIntact()) {
            updateValueOfIRQAndRange();
        } else {

            // when the user select all summary indicators
            if (summaryValues.size() == SummaryStatisticsState.FULL_CHART) {
                // The BAW chart doesnot support dynamic, so only can create a new one after all finished.
                SummaryStatisticsState state = new SummaryStatisticsState(indicators);
                state.setSqltype(Types.DOUBLE);
                JFreeChart chart = state.getChart();
                ChartDecorator.decorate(chart, null);
                if (BAWparentComposite != null) {
                    BAWparentComposite.setChart(chart);
                    BAWparentComposite.forceRedraw();
                }

                EventManager.getInstance().publish(chartComposite, EventEnum.DQ_DYNAMIC_REFRESH_DYNAMIC_CHART, null);

                // 6 indicators will refresh the chart, IQR and range indicator need to refresh their values in the
                // table viewer
                updateValueOfIRQAndRange();

                // reset the values
                summaryValues.clear();
                indicators.clear();
            }
        }
    }

    /**
     * DOC yyin Comment method "updateValueOfIRQAndRange".
     */
    private void updateValueOfIRQAndRange() {
        if (this.getTableViewer() != null) {
            Object indicatorValue = IndicatorCommonUtil.getIndicatorValue(IRQIndicator);
            String str = indicatorValue == null ? String.valueOf(Double.NaN) : String.valueOf(indicatorValue);
            IRQIndicatorEvent.refreshTable(str);
            indicatorValue = IndicatorCommonUtil.getIndicatorValue(rangeIndicator);
            str = indicatorValue == null ? String.valueOf(Double.NaN) : String.valueOf(indicatorValue);
            rangeIndicatorEvent.refreshTable(str);
        }
    }

    @Override
    public boolean handle(Object value) {
        return true;
    }

    @Override
    public void clearValue() {
        if (bawDataset != null && bawDataset.getRowCount() == 7) {
            for (int i = 0; i < 7; i++) {
                bawDataset.remove(String.valueOf(i), "");//$NON-NLS-1$ 
            }
        }
    }

    public CustomerDefaultBAWDataset getBawDataset() {
        return bawDataset;
    }

    public void setBawDataset(CustomerDefaultBAWDataset bawDataset) {
        this.bawDataset = bawDataset;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.events.DynamicChartEventReceiver#clear()
     */
    @Override
    public void clear() {
        super.clear();
        // reset the values
        summaryValues.clear();
        indicators.clear();
        BAWparentComposite = null;
        bawDataset = null;
    }

    /**
     * Sets the bAWparentComposite.
     * 
     * @param bAWparentComposite the bAWparentComposite to set
     */
    public void setBAWparentComposite(TalendChartComposite bAWparentComposite) {
        this.BAWparentComposite = bAWparentComposite;
    }
}
