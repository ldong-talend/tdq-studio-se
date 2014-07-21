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

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.jfree.chart.JFreeChart;
import org.talend.dataprofiler.common.ui.editor.preview.CustomerDefaultCategoryDataset;
import org.talend.dataprofiler.common.ui.editor.preview.ICustomerDataset;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.entity.TableStructureEntity;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderClassSet.CommonContenteProvider;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderClassSet.ModeLabelProvider;
import org.talend.dq.analysis.explore.DataExplorer;
import org.talend.dq.indicators.preview.table.ChartDataEntity;

/**
 * DOC Zqin class global comment. Detailled comment
 */
public class ModeStatisticsState extends AbstractChartTypeStates {

    public ModeStatisticsState(List<IndicatorUnit> units) {
        super(units);
    }

    public JFreeChart getChart() {
        // TODO Auto-generated method stub
        return null;
    }

    public ICustomerDataset getCustomerDataset() {
        CustomerDefaultCategoryDataset customerdataset = new CustomerDefaultCategoryDataset();
        for (IndicatorUnit unit : units) {
            String label = unit.getIndicatorName();

            ChartDataEntity entity = new ChartDataEntity();
            entity.setIndicator(unit.getIndicator());
            entity.setLabel(label);
            Object value = unit.getValue();
            entity.setValue(String.valueOf(value == null ? StringUtils.EMPTY : value));

            customerdataset.addDataEntity(entity);
        }
        return customerdataset;
    }

    public DataExplorer getDataExplorer() {
        // TODO Auto-generated method stub
        return null;
    }

    public JFreeChart getExampleChart() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected TableStructureEntity getTableStructure() {
        TableStructureEntity entity = new TableStructureEntity();
        entity.setFieldNames(new String[] { DefaultMessagesImpl.getString("ModeStatisticsState.Mode") }); //$NON-NLS-1$
        entity.setFieldWidths(new Integer[] { 500 });
        return entity;
    }

    @Override
    protected ITableLabelProvider getLabelProvider() {
        // TODO Auto-generated method stub
        return new ModeLabelProvider();
    }

    @Override
    protected IStructuredContentProvider getContentProvider() {
        // TODO Auto-generated method stub
        return new CommonContenteProvider();
    }

    public String getReferenceLink() {
        // TODO Auto-generated method stub
        return null;
    }
}
