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
package org.talend.dataprofiler.core.ui.editor.preview.model.states.pattern;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.talend.dataprofiler.common.ui.editor.preview.CustomerDefaultCategoryDataset;
import org.talend.dataprofiler.common.ui.editor.preview.ICustomerDataset;
import org.talend.dataprofiler.common.ui.editor.preview.chart.TopChartFactory;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.entity.TableStructureEntity;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.AbstractChartTypeStates;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderClassSet.CommonContenteProvider;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderClassSet.PatternLabelProvider;
import org.talend.dq.analysis.explore.DataExplorer;
import org.talend.dq.analysis.explore.PatternExplorer;
import org.talend.dq.indicators.ext.PatternMatchingExt;
import org.talend.dq.indicators.preview.table.PatternChartDataEntity;

/**
 * DOC Zqin class global comment. Detailled comment
 */
public class PatternStatisticsState extends AbstractChartTypeStates {

    public PatternStatisticsState(List<IndicatorUnit> units) {
        super(units);
    }

    public JFreeChart getChart() {
        return getChart(getDataset());
    }

    @Override
    public JFreeChart getChart(CategoryDataset dataset) {
        return TopChartFactory.createStackedBarChart(
                DefaultMessagesImpl.getString("PatternStatisticsState.PatternStatistics"), dataset, PlotOrientation.VERTICAL); //$NON-NLS-1$
    }

    public ICustomerDataset getCustomerDataset() {
        CustomerDefaultCategoryDataset customerdataset = new CustomerDefaultCategoryDataset();
        for (IndicatorUnit unit : units) {
            String label = unit.getIndicator().getName();
            PatternMatchingExt patternExt = (PatternMatchingExt) unit.getValue();
            double notMathCount = patternExt == null ? Double.NaN : patternExt.getNotMatchingValueCount();
            double machCount = patternExt == null ? Double.NaN : patternExt.getMatchingValueCount();

            customerdataset.addValue(notMathCount, DefaultMessagesImpl.getString("PatternStatisticsState.NotMatching"), label); //$NON-NLS-1$
            customerdataset.addValue(machCount, DefaultMessagesImpl.getString("PatternStatisticsState.Matching"), label); //$NON-NLS-1$

            PatternChartDataEntity patternEntity = new PatternChartDataEntity();
            patternEntity.setIndicator(unit.getIndicator());
            patternEntity.setLabel(label);
            patternEntity.setNumMatch(String.valueOf(machCount));
            patternEntity.setNumNoMatch(String.valueOf(notMathCount));

            customerdataset.addDataEntity(patternEntity);
        }

        return customerdataset;
    }

    public DataExplorer getDataExplorer() {
        return new PatternExplorer();
    }

    public JFreeChart getExampleChart() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected TableStructureEntity getTableStructure() {
        TableStructureEntity entity = new TableStructureEntity();
        entity.setFieldNames(new String[] {
                DefaultMessagesImpl.getString("PatternStatisticsState.Label"), DefaultMessagesImpl.getString("PatternStatisticsState.Match"), DefaultMessagesImpl.getString("PatternStatisticsState.NoMatch"), DefaultMessagesImpl.getString("PatternStatisticsState.Match_"), DefaultMessagesImpl.getString("PatternStatisticsState.NoMatch_") }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        entity.setFieldWidths(new Integer[] { 200, 75, 75, 75, 75 });
        return entity;
    }

    @Override
    protected ITableLabelProvider getLabelProvider() {
        return new PatternLabelProvider();
    }

    @Override
    protected IStructuredContentProvider getContentProvider() {
        return new CommonContenteProvider();
    }

    public String getReferenceLink() {
        return null;
    }
}
