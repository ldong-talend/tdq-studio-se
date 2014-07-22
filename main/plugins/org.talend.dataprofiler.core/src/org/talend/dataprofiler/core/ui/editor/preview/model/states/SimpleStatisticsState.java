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
package org.talend.dataprofiler.core.ui.editor.preview.model.states;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.talend.dataprofiler.common.ui.editor.preview.CustomerDefaultCategoryDataset;
import org.talend.dataprofiler.common.ui.editor.preview.ICustomerDataset;
import org.talend.dataprofiler.common.ui.editor.preview.chart.TopChartFactory;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.entity.TableStructureEntity;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderClassSet.BaseChartTableLabelProvider;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderClassSet.CommonContenteProvider;
import org.talend.dq.analysis.explore.DataExplorer;
import org.talend.dq.analysis.explore.SimpleStatisticsExplorer;
import org.talend.dq.indicators.preview.table.ChartDataEntity;

/**
 * DOC Zqin class global comment. Detailled comment
 */
public class SimpleStatisticsState extends AbstractChartTypeStates {

    public SimpleStatisticsState(List<IndicatorUnit> units) {
        super(units);
    }

    public JFreeChart getChart() {
        return TopChartFactory.createBarChart(
                DefaultMessagesImpl.getString("SimpleStatisticsState.SimpleStatistics"), getDataset(), false); //$NON-NLS-1$
    }

    @Override
    public JFreeChart getChart(CategoryDataset dataset) {
        return TopChartFactory.createBarChart(
                DefaultMessagesImpl.getString("SimpleStatisticsState.SimpleStatistics"), dataset, false); //$NON-NLS-1$
    }

    public ICustomerDataset getCustomerDataset() {
        CustomerDefaultCategoryDataset customerdataset = new CustomerDefaultCategoryDataset();
        for (IndicatorUnit unit : units) {
            final Object unitValue = unit.getValue();
            double value = unitValue != null ? Double.parseDouble(unitValue.toString()) : Double.NaN;
            String label = unit.getIndicatorName();

            customerdataset.addValue(value, label, label);

            ChartDataEntity entity = new ChartDataEntity();
            entity.setIndicator(unit.getIndicator());
            entity.setLabel(label);
            entity.setValue(String.valueOf(value));
            entity.setPercent(value / unit.getIndicator().getCount());

            customerdataset.addDataEntity(entity);
        }
        return customerdataset;
    }

    public DataExplorer getDataExplorer() {
        // TODO Auto-generated method stub
        return new SimpleStatisticsExplorer();
    }

    public JFreeChart getExampleChart() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected TableStructureEntity getTableStructure() {

        TableStructureEntity entity = new TableStructureEntity();
        entity.setFieldNames(new String[] {
                DefaultMessagesImpl.getString("SimpleStatisticsState.Label"), DefaultMessagesImpl.getString("SimpleStatisticsState.Count"), "%" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entity.setFieldWidths(new Integer[] { 200, 150, 150 });
        return entity;
    }

    @Override
    protected ITableLabelProvider getLabelProvider() {

        return new BaseChartTableLabelProvider();
    }

    @Override
    protected IStructuredContentProvider getContentProvider() {

        return new CommonContenteProvider();
    }

    public String getReferenceLink() {
        // TODO Auto-generated method stub
        return null;
    }
}
