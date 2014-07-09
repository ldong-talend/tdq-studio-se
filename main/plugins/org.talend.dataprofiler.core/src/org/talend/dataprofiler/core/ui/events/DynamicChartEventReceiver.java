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

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.talend.dataprofiler.common.ui.editor.preview.CustomerDefaultCategoryDataset;
import org.talend.dataprofiler.common.ui.editor.preview.ICustomerDataset;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartWithData;
import org.talend.dataprofiler.core.ui.utils.AnalysisUtils;
import org.talend.dataquality.indicators.FrequencyIndicator;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.ModeIndicator;
import org.talend.dq.helper.UDIHelper;
import org.talend.dq.indicators.ext.FrequencyExt;
import org.talend.dq.indicators.ext.PatternMatchingExt;
import org.talend.dq.indicators.preview.table.ChartDataEntity;
import org.talend.dq.nodes.indicator.type.IndicatorEnum;
import org.talend.utils.format.StringFormatUtil;

/**
 * DOC yyin class global comment. Detailled comment
 */
public class DynamicChartEventReceiver extends EventReceiver {

    private DefaultCategoryDataset dataset;

    protected Indicator indicator;

    private int entityIndex;

    protected String indicatorName;

    // mainly used for the summary indicators
    private IndicatorEnum indicatorType = null;

    private TableViewer tableViewer = null;

    protected Composite chartComposite;

    public void setDataset(CategoryDataset categoryDataset) {
        this.dataset = (DefaultCategoryDataset) categoryDataset;
    }

    public int getIndexInDataset() {
        return this.entityIndex;
    }

    public void setIndexInDataset(int row) {
        this.entityIndex = row;
    }

    // only Frequency indicator need to remember itself
    public void setIndicator(Indicator indicator) {
        if (indicator instanceof FrequencyIndicator || UDIHelper.isFrequency(indicator)) {
            this.indicator = indicator;
        }
    }

    /**
     * Getter for dataset.
     * 
     * @return the dataset
     */
    public DefaultCategoryDataset getDataset() {
        return this.dataset;
    }

    /**
     * Sets the dataset.
     * 
     * @param dataset the dataset to set
     */
    public void setDataset(DefaultCategoryDataset dataset) {
        this.dataset = dataset;
    }

    /**
     * Getter for entityIndex.
     * 
     * @return the entityIndex
     */
    public int getEntityIndex() {
        return this.entityIndex;
    }

    /**
     * Sets the entityIndex.
     * 
     * @param entityIndex the entityIndex to set
     */
    public void setEntityIndex(int entityIndex) {
        this.entityIndex = entityIndex;
    }

    /**
     * Getter for indicator.
     * 
     * @return the indicator
     */
    public Indicator getIndicator() {
        return this.indicator;
    }

    @Override
    public boolean handle(Object value) {
        Object indValue = value;
        if (value == null) {
            indValue = 0;
        }
        if (dataset != null) {
            if (indValue instanceof Number) {
                ((CustomerDefaultCategoryDataset) dataset).setValue((Number) indValue, indicatorName, indicatorName);
            } else if (indValue instanceof String) {
                if (!(indicator instanceof ModeIndicator)) {
                    ((CustomerDefaultCategoryDataset) dataset).setValue(Double.parseDouble((String) indValue), indicatorName,
                            indicatorName);
                }
            } else if (indValue instanceof FrequencyExt[]) {
                // clear old data
                if (dataset instanceof CustomerDefaultCategoryDataset) {
                    ((CustomerDefaultCategoryDataset) dataset).clearAll();
                } else {
                    dataset.clear();
                }
                // no sort needed here
                FrequencyExt[] frequencyExt = (FrequencyExt[]) indValue;
                AnalysisUtils.setFrequecyToDataset(dataset, frequencyExt, indicator);
            } else if (indValue instanceof PatternMatchingExt) {
                PatternMatchingExt patternExt = (PatternMatchingExt) indValue;
                ((CustomerDefaultCategoryDataset) dataset).setValue(patternExt.getNotMatchingValueCount(),
                        DefaultMessagesImpl.getString("PatternStatisticsState.NotMatching"), this.indicatorName);//$NON-NLS-1$
                ((CustomerDefaultCategoryDataset) dataset).setValue(patternExt.getMatchingValueCount(),
                        DefaultMessagesImpl.getString("PatternStatisticsState.Matching"), this.indicatorName);//$NON-NLS-1$
            } else {
                ((CustomerDefaultCategoryDataset) dataset).setValue(
                        (Number) StringFormatUtil.format(indValue, StringFormatUtil.NUMBER), indicatorName, indicatorName);
            }
        }
        if (tableViewer != null) {
            if (indValue instanceof FrequencyExt[]) {
                ChartWithData input = (ChartWithData) tableViewer.getInput();
                if (input != null) {
                    input.setEntities(((ICustomerDataset) dataset).getDataEntities());
                }
                tableViewer.getTable().clearAll();
                tableViewer.setInput(input);
            } else {
                refreshTable(String.valueOf(indValue));
            }
        }

        // need to refresh the parent composite of the chart to show the changes
        EventManager.getInstance().publish(chartComposite, EventEnum.DQ_DYNAMIC_REFRESH_DYNAMIC_CHART, null);

        return true;
    }

    public void clearValue() {
        if (dataset != null) {
            dataset.setValue(0.0, indicatorName, indicatorName);
        }
        if (tableViewer != null) {
            refreshTable("0.0");//$NON-NLS-1$
        }
    }

    public void refreshTable(String value) {
        ChartWithData input = (ChartWithData) tableViewer.getInput();
        if (input != null) {
            ChartDataEntity[] dataEntities = input.getEnity();
            if (dataEntities != null && dataEntities.length > entityIndex) {
                dataEntities[entityIndex].setValue(value);
                tableViewer.getTable().clearAll();
                tableViewer.setInput(input);
            }
        }
    }

    public String getIndicatorName() {
        return indicatorName;
    }

    public void setIndicatorName(String indicatorName) {
        this.indicatorName = indicatorName;
    }

    public Composite getChartComposite() {
        return chartComposite;
    }

    public void setChartComposite(Composite chartComposite) {
        this.chartComposite = chartComposite;
    }

    public IndicatorEnum getIndicatorType() {
        return indicatorType;
    }

    public void setIndicatorType(IndicatorEnum indicatorType) {
        this.indicatorType = indicatorType;
    }

    public TableViewer getTableViewer() {
        return tableViewer;
    }

    public void setTableViewer(TableViewer tableViewer) {
        this.tableViewer = tableViewer;
    }

    public void clear() {
        this.chartComposite = null;
        this.dataset = null;
        this.tableViewer = null;
        this.indicatorType = null;
        this.indicator = null;
    }
}
