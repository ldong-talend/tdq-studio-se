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

import org.jfree.data.category.DefaultCategoryDataset;
import org.talend.commons.utils.SpecialValueDisplay;
import org.talend.dataprofiler.common.ui.editor.preview.CustomerDefaultCategoryDataset;
import org.talend.dataprofiler.common.ui.editor.preview.ICustomerDataset;
import org.talend.dataprofiler.core.ui.editor.preview.model.ChartWithData;
import org.talend.dataprofiler.core.ui.utils.AnalysisUtils;
import org.talend.dataquality.indicators.BenfordLawFrequencyIndicator;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.IndicatorParameters;
import org.talend.dataquality.indicators.ModeIndicator;
import org.talend.dq.indicators.ext.FrequencyExt;
import org.talend.dq.indicators.preview.table.ChartDataEntity;

/**
 * created by yyin on 2014-7-11 Detailled comment
 * 
 */
public class FrequencyDynamicChartEventReceiver extends DynamicChartEventReceiver {

    @Override
    public boolean handle(Object value) {
        Object indValue = value;
        if (value == null) {
            indValue = 0;
        }
        if (dataset != null) {
            // clear old data
            if (dataset instanceof CustomerDefaultCategoryDataset) {
                ((CustomerDefaultCategoryDataset) dataset).clearAll();
            } else {
                dataset.clear();
            }
            // no sort needed here
            if (indValue instanceof FrequencyExt[]) {
                FrequencyExt[] frequencyExt = (FrequencyExt[]) indValue;
                setFrequecyToDataset(dataset, frequencyExt, indicator);
            }
        }
        if (tableViewer != null) {
            // if (indValue instanceof FrequencyExt[]) {
            ChartWithData input = (ChartWithData) tableViewer.getInput();
            if (input != null) {
                if (this.indicator instanceof ModeIndicator) {
                    ChartDataEntity entity = new ChartDataEntity();
                    entity.setIndicator(indicator);
                    entity.setLabel(this.indicatorName);
                    entity.setValue(String.valueOf(indValue));

                    ((CustomerDefaultCategoryDataset) dataset).addDataEntity(entity);
                }
                input.setEntities(((ICustomerDataset) dataset).getDataEntities());
            }
            if (!tableViewer.getTable().isDisposed()) {
                tableViewer.getTable().clearAll();
                tableViewer.setInput(input);
            }
            // }
        }

        // need to refresh the parent composite of the chart to show the changes
        if (!(indicator instanceof ModeIndicator)) {
            EventManager.getInstance().publish(chartComposite, EventEnum.DQ_DYNAMIC_REFRESH_DYNAMIC_CHART, null);
        }
        return true;
    }

    private void setFrequecyToDataset(DefaultCategoryDataset customerdataset, FrequencyExt[] frequencyExt, Indicator indicator) {

        int numOfShown = frequencyExt.length;
        IndicatorParameters parameters = indicator.getParameters();
        if (parameters != null) {
            if (parameters.getTopN() < numOfShown) {
                numOfShown = parameters.getTopN();
            }
        }
        FrequencyExt[] tempFreq = handleFrequency(frequencyExt);

        boolean withRowCountIndicator = AnalysisUtils.isWithRowCountIndicator(indicator);
        for (int i = 0; i < numOfShown; i++) {
            FrequencyExt freqExt = tempFreq[i];
            String keyLabel = String.valueOf(freqExt.getKey());
            if ("null".equals(keyLabel)) { //$NON-NLS-1$
                keyLabel = SpecialValueDisplay.NULL_FIELD;
            }
            if ("".equals(keyLabel)) { //$NON-NLS-1$
                keyLabel = SpecialValueDisplay.EMPTY_FIELD;
            }

            addValueToDataset(customerdataset, freqExt, keyLabel);

            ChartDataEntity entity = AnalysisUtils.createChartEntity(indicator, freqExt, keyLabel, withRowCountIndicator);

            ((CustomerDefaultCategoryDataset) customerdataset).addDataEntity(entity);
        }
    }

    /**
     * DOC yyin Comment method "handleFrequency".
     * 
     * @param frequencyExt
     * @param indicator
     * @return
     */
    protected FrequencyExt[] handleFrequency(FrequencyExt[] frequencyExt) {
        return frequencyExt;
    }

    /**
     * DOC yyin Comment method "addValueToDataset".
     * 
     * @param customerdataset
     * @param indicator
     * @param freqExt
     * @param keyLabel
     */
    protected void addValueToDataset(DefaultCategoryDataset customerdataset, FrequencyExt freqExt, String keyLabel) {
        if (indicator instanceof BenfordLawFrequencyIndicator) {
            customerdataset.addValue(freqExt.getFrequency(), "1", keyLabel); //$NON-NLS-1$
        } else {
            customerdataset.addValue(freqExt.getValue(), "1", keyLabel); //$NON-NLS-1$
        }
    }
}
