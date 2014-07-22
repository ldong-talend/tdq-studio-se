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
package org.talend.dataprofiler.core.ui.editor.preview.model.states.freq;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.talend.commons.utils.SpecialValueDisplay;
import org.talend.dataprofiler.common.ui.editor.preview.CustomerDefaultCategoryDataset;
import org.talend.dataprofiler.common.ui.editor.preview.ICustomerDataset;
import org.talend.dataprofiler.common.ui.editor.preview.chart.TopChartFactory;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.entity.TableStructureEntity;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.AbstractChartTypeStates;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderClassSet.CommonContenteProvider;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderClassSet.FrequencyLabelProvider;
import org.talend.dataprofiler.core.ui.utils.AnalysisUtils;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.IndicatorParameters;
import org.talend.dq.indicators.ext.FrequencyExt;
import org.talend.dq.indicators.preview.table.ChartDataEntity;

/**
 * DOC Zqin class global comment. Detailled comment
 */
public abstract class FrequencyTypeStates extends AbstractChartTypeStates {

    public FrequencyTypeStates(List<IndicatorUnit> units) {
        super(units);
    }

    public JFreeChart getChart() {
        return getChart(getDataset());
    }

    @Override
    public JFreeChart getChart(CategoryDataset dataset) {
        return TopChartFactory.createBarChart(DefaultMessagesImpl.getString("TopChartFactory.count"), dataset); //$NON-NLS-1$
    }

    public ICustomerDataset getCustomerDataset() {
        CustomerDefaultCategoryDataset customerdataset = new CustomerDefaultCategoryDataset();
        boolean withRowCountIndicator = isWithRowCountIndicator();

        for (IndicatorUnit unit : units) {
            if (unit.isExcuted()) {
                FrequencyExt[] frequencyExt = (FrequencyExt[]) unit.getValue();

                sortIndicator(frequencyExt);

                int numOfShown = frequencyExt.length;
                IndicatorParameters parameters = unit.getIndicator().getParameters();
                if (parameters != null) {
                    if (parameters.getTopN() < numOfShown) {
                        numOfShown = parameters.getTopN();
                    }
                }

                for (int i = 0; i < numOfShown; i++) {
                    FrequencyExt freqExt = frequencyExt[i];
                    String keyLabel = String.valueOf(freqExt.getKey());
                    if ("null".equals(keyLabel)) { //$NON-NLS-1$
                        keyLabel = SpecialValueDisplay.NULL_FIELD;
                    }
                    if ("".equals(keyLabel)) { //$NON-NLS-1$
                        keyLabel = SpecialValueDisplay.EMPTY_FIELD;
                    }

                    setValueToDataset(customerdataset, freqExt, keyLabel);

                    ChartDataEntity entity = new ChartDataEntity();
                    entity.setIndicator(unit.getIndicator());
                    // MOD mzhao feature:6307 display soundex distinct count and real count.
                    entity.setKey(freqExt.getKey());
                    entity.setLabelNull(freqExt.getKey() == null);
                    entity.setLabel(keyLabel);
                    entity.setValue(String.valueOf(freqExt.getValue()));

                    Double percent = withRowCountIndicator ? freqExt.getFrequency() : Double.NaN;
                    entity.setPercent(percent);

                    customerdataset.addDataEntity(entity);
                }
            } else {
                ChartDataEntity entity = AnalysisUtils.createChartEntity(unit.getIndicator(), null,
                        SpecialValueDisplay.EMPTY_FIELD, false);
                FrequencyExt fre = new FrequencyExt();
                fre.setFrequency(0.0);
                setValueToDataset(customerdataset, fre, unit.getIndicatorName());

                customerdataset.addDataEntity(entity);
            }
        }
        return customerdataset;
    }

    /**
     * extract the method for change the addvalue parameter for benford law
     * 
     * @param customerdataset
     * @param freqExt
     * @param keyLabel
     */
    protected void setValueToDataset(CustomerDefaultCategoryDataset customerdataset, FrequencyExt freqExt, String keyLabel) {
        customerdataset.addValue(freqExt.getValue(), "1", keyLabel); //$NON-NLS-1$
    }

    public JFreeChart getExampleChart() {
        return null;
    }

    public String getReferenceLink() {
        return null;
    }

    @Override
    protected TableStructureEntity getTableStructure() {
        TableStructureEntity entity = new TableStructureEntity();
        entity.setFieldNames(new String[] {
                DefaultMessagesImpl.getString("FrequencyTypeStates.value"), DefaultMessagesImpl.getString("FrequencyTypeStates.count"), "%" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entity.setFieldWidths(new Integer[] { 200, 150, 150 });
        return entity;
    }

    @Override
    protected ITableLabelProvider getLabelProvider() {
        return new FrequencyLabelProvider();
    }

    @Override
    protected IStructuredContentProvider getContentProvider() {
        return new CommonContenteProvider();
    }

    /**
     * DOC bZhou Comment method "isWithRowCountIndicator".
     * 
     * If have RowCountIndicator in the indicator list, return true, otherwise, return false.
     * 
     * @return
     */
    protected boolean isWithRowCountIndicator() {
        if (!units.isEmpty()) {
            Indicator indicator = units.get(0).getIndicator();
            return AnalysisUtils.isWithRowCountIndicator(indicator);
        }
        return false;
    }

    protected abstract void sortIndicator(FrequencyExt[] frequencyExt);

    protected abstract String getTitle();

}
