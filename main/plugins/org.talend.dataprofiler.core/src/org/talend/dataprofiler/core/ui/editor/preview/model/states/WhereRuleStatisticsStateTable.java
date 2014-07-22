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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.talend.cwm.helper.TaggedValueHelper;
import org.talend.dataprofiler.common.ui.editor.preview.CustomerDefaultCategoryDataset;
import org.talend.dataprofiler.common.ui.editor.preview.ICustomerDataset;
import org.talend.dataprofiler.common.ui.editor.preview.chart.ChartDecorator;
import org.talend.dataprofiler.common.ui.editor.preview.chart.TopChartFactory;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.model.TableIndicator;
import org.talend.dataprofiler.core.ui.editor.preview.TableIndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.dataset.CustomerXYSeriesCollection;
import org.talend.dataprofiler.core.ui.editor.preview.model.entity.TableStructureEntity;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderClassSet.BaseChartTableLabelProvider;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderClassSet.CommonContenteProvider;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.ChartTableProviderClassSet.PatternLabelProvider;
import org.talend.dataprofiler.core.ui.pref.EditorPreferencePage;
import org.talend.dataquality.helpers.IndicatorHelper;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.RowCountIndicator;
import org.talend.dataquality.indicators.definition.IndicatorDefinition;
import org.talend.dataquality.indicators.sql.WhereRuleIndicator;
import org.talend.dq.analysis.explore.DQRuleExplorer;
import org.talend.dq.analysis.explore.DataExplorer;
import org.talend.dq.indicators.preview.table.ChartDataEntity;
import org.talend.dq.indicators.preview.table.WhereRuleChartDataEntity;
import org.talend.dq.nodes.indicator.type.IndicatorEnum;
import orgomg.cwm.objectmodel.core.TaggedValue;

/**
 * DOC xqliu class global comment. Detailled comment
 */
public class WhereRuleStatisticsStateTable extends AbstractChartTypeStatesTable {

    public static final String ROW_KEY_PASS = DefaultMessagesImpl.getString("WhereRuleStatisticsStateTable.match"); //$NON-NLS-1$

    public static final String ROW_KEY_NOT_PASS = DefaultMessagesImpl.getString("WhereRuleStatisticsStateTable.notMatch"); //$NON-NLS-1$

    private TableIndicator tableIndicator;

    private Long rowCount;

    public TableIndicator getTableIndicator() {
        return tableIndicator;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public WhereRuleStatisticsStateTable(List<TableIndicatorUnit> units, TableIndicator tableIndicator) {
        super(units);
        this.tableIndicator = tableIndicator;
        this.rowCount = initRowCount(tableIndicator);
    }

    @Override
    public JFreeChart getChart() {
        List<JFreeChart> chartList = getChartList();
        if (chartList != null && chartList.size() > 0) {
            return chartList.get(0);
        }
        return null;
    }

    private Long initRowCount(TableIndicator tableIndicator1) {
        Long result = 0L;
        if (tableIndicator1 != null) {
            TableIndicatorUnit[] tius = tableIndicator1.getIndicatorUnits();
            for (TableIndicatorUnit tiu : tius) {
                if (tiu.getIndicator() instanceof RowCountIndicator) {
                    RowCountIndicator rci = (RowCountIndicator) tiu.getIndicator();
                    result = rci.getCount();
                    break;
                }
            }
        }
        return result;
    }

    public ICustomerDataset getCustomerDataset() {
        // MOD xqliu 2010-03-10 feature 10834
        CustomerDefaultCategoryDataset customerDataset = new CustomerDefaultCategoryDataset();
        for (TableIndicatorUnit unit : units) {
            addDataEntity2CustomerDataset(customerDataset, unit);
        }
        return customerDataset;
        // ~10834
    }

    /**
     * DOC xqliu Comment method "getCustomerDatasetRownCount".
     * 
     * @return
     */
    public ICustomerDataset getCustomerDatasetRownCount() {
        CustomerDefaultCategoryDataset customerDataset = new CustomerDefaultCategoryDataset();
        TableIndicatorUnit rownCountUnit = getRownCountUnit(units);
        if (rownCountUnit != null) {
            addRownCountDataEntity2CustomerDataset(customerDataset, rownCountUnit);
        }
        return customerDataset;
    }

    /**
     * DOC xqliu Comment method "getUnitToolTip". ADD xqliu 2010-03-10 feature 10834
     * 
     * @param unit
     * @return
     */
    private String getUnitToolTip(TableIndicatorUnit unit) {
        if (unit != null) {
            if (unit.getIndicator() != null && unit.getIndicator().getIndicatorDefinition() != null) {
                IndicatorDefinition indicatorDefinition = unit.getIndicator().getIndicatorDefinition();
                TaggedValue taggedValue = TaggedValueHelper.getTaggedValue(TaggedValueHelper.DESCRIPTION,
                        indicatorDefinition.getTaggedValue());
                if (taggedValue != null) {
                    return taggedValue.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public ICustomerDataset getCustomerXYDataset() {
        final CustomerXYSeriesCollection dataset = new CustomerXYSeriesCollection();
        final XYSeries series = new XYSeries("Rules"); //$NON-NLS-1$
        int x = 0;
        for (TableIndicatorUnit unit : units) {
            x++;
            double y = 100 * (1 - (Double.parseDouble(unit.getValue().toString()) / getRowCount()));
            series.add(x, y);
            dataset.addSeries(series);
        }
        return dataset;
    }

    public DataExplorer getDataExplorer() {
        return new DQRuleExplorer();
    }

    public JFreeChart getExampleChart() {
        return null;
    }

    @Override
    protected TableStructureEntity getTableStructure() {
        TableStructureEntity entity = new TableStructureEntity();
        entity.setFieldNames(new String[] {
                DefaultMessagesImpl.getString("WhereRuleStatisticsStateTable.Label"), DefaultMessagesImpl.getString("WhereRuleStatisticsStateTable.Match"), DefaultMessagesImpl.getString("WhereRuleStatisticsStateTable.NoMatch"), DefaultMessagesImpl.getString("WhereRuleStatisticsStateTable.Match_"), DefaultMessagesImpl.getString("WhereRuleStatisticsStateTable.NoMatch_") }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        entity.setFieldWidths(new Integer[] { 200, 75, 75, 75, 75 });
        return entity;
    }

    protected TableStructureEntity getTableStructureRowCount() {
        TableStructureEntity entity = new TableStructureEntity();
        entity.setFieldNames(new String[] {
                DefaultMessagesImpl.getString("SimpleStatisticsStateTable.Label"), DefaultMessagesImpl.getString("SimpleStatisticsStateTable.Count"), "%" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        entity.setFieldWidths(new Integer[] { 200, 150, 150 });
        return entity;
    }

    @Override
    protected ITableLabelProvider getLabelProvider() {
        return new WhereRuleTableLabelProvider(this.getRowCount());
    }

    @Override
    protected IStructuredContentProvider getContentProvider() {
        return new CommonContenteProvider();
    }

    public String getReferenceLink() {
        return null;
    }

    @Override
    public List<JFreeChart> getChartList() {
        // MOD xqliu 2010-03-17 feature 10834
        List<JFreeChart> ret = new ArrayList<JFreeChart>();
        List<CategoryDataset> optimizeShowDataset = getOptimizeShowDataset();
        // MOD xqliu 2012-04-23 TDQ-5057
        int i = 0;
        for (CategoryDataset dataset : optimizeShowDataset) {
            if (i < 1) {
                JFreeChart barChart = TopChartFactory.createBarChart(
                        DefaultMessagesImpl.getString("SimpleStatisticsState.SimpleStatistics"), dataset, false); //$NON-NLS-1$
                ChartDecorator.decorate(barChart, null);
                ret.add(barChart);
            } else {
                JFreeChart stackChart = TopChartFactory.createStackedBarChart(
                        DefaultMessagesImpl.getString("WhereRuleStatisticsStateTable.WhereRuleStatistics"), dataset, true); //$NON-NLS-1$
                ChartDecorator.decorate(stackChart, null);
                ret.add(stackChart);
            }
            i++;
        }
        return ret;
    }

    /**
     * DOC xqliu Comment method "getOptimizeShowDataset". ADD xqliu 2010-03-10 feature 10834
     * 
     * @return
     */
    private List<CategoryDataset> getOptimizeShowDataset() {
        List<CategoryDataset> result = new ArrayList<CategoryDataset>();
        // get the page size
        int size = getSizeOfDQRulePerChart();

        // Add RowCountIndicator dataset
        CustomerDefaultCategoryDataset customerDatasetRownCount = new CustomerDefaultCategoryDataset();
        // MOD msjian TDQ-5119: fix a NPE
        if (units != null && units.size() > 0) {
            addRownCountDataEntity2CustomerDataset(customerDatasetRownCount, getRownCountUnit(units));
            result.add(customerDatasetRownCount);

            // MOD xqliu 2012-04-23 TDQ-5057: don't include RowCountUnit
            List<TableIndicatorUnit> whereRuleUnits = removeRowCountUnit(units);
            int totalNum = whereRuleUnits.size();
            int pageNum = totalNum % size == 0 ? totalNum / size : totalNum / size + 1;
            for (int i = 0; i < pageNum; i++) {
                CustomerDefaultCategoryDataset customerDataset = new CustomerDefaultCategoryDataset();
                for (int j = 0; j < size; ++j) {
                    int index = i * size + j;
                    if (index < totalNum) {
                        addDataEntity2CustomerDataset(customerDataset, whereRuleUnits.get(index));
                    } else {
                        break;
                    }
                }
                result.add(customerDataset);
            }
        }
        // TDQ-5119~

        return result;
    }

    /**
     * DOC yyin Comment method "getSizeOfDQRule".
     * 
     * @return
     */
    public static int getSizeOfDQRulePerChart() {
        String dqruleSize = EditorPreferencePage.getDQRuleSize();
        int maxSize = 999999;
        int size = maxSize;
        try {
            size = Integer.parseInt(dqruleSize);
            if (size < 1) {
                size = maxSize;
            }
        } catch (NumberFormatException e) {
            size = maxSize;
        }
        return size;
    }

    public List<List<Indicator>> getPagedIndicators() {
        int size = getSizeOfDQRulePerChart();

        List<List<Indicator>> indicatorList = new ArrayList<List<Indicator>>();
        // first , add row count indicator
        List<Indicator> rowInd = new ArrayList<Indicator>();
        rowInd.add(getRownCountUnit(units).getIndicator());
        indicatorList.add(rowInd);

        // then, add all where rules(one chart <--> one list)
        List<TableIndicatorUnit> whereRuleUnits = removeRowCountUnit(units);
        int totalNum = whereRuleUnits.size();
        int pageNum = totalNum % size == 0 ? totalNum / size : totalNum / size + 1;
        for (int i = 0; i < pageNum; i++) {
            List<Indicator> rules = new ArrayList<Indicator>();
            for (int j = 0; j < size; ++j) {
                int index = i * size + j;
                if (index < totalNum) {
                    rules.add(whereRuleUnits.get(index).getIndicator());
                } else {
                    break;
                }
            }
            indicatorList.add(rules);
        }
        return indicatorList;
    }

    /**
     * DOC xqliu Comment method "removeRowCountUnit".
     * 
     * @param units1
     * @return
     */
    public List<TableIndicatorUnit> removeRowCountUnit(List<TableIndicatorUnit> units1) {
        List<TableIndicatorUnit> result = new ArrayList<TableIndicatorUnit>();
        for (TableIndicatorUnit tiu : units1) {
            if (!IndicatorEnum.RowCountIndicatorEnum.equals(tiu.getType())) {
                result.add(tiu);
            }
        }
        return result;
    }

    /**
     * DOC xqliu Comment method "getRownCountUnit".
     * 
     * @param units1
     * @return
     */
    public TableIndicatorUnit getRownCountUnit(List<TableIndicatorUnit> units1) {
        for (TableIndicatorUnit tiu : units1) {
            if (IndicatorEnum.RowCountIndicatorEnum.equals(tiu.getType())) {
                return tiu;
            }
        }
        return null;
    }

    /**
     * DOC xqliu Comment method "addDataEntity2CustomerDataset". ADD xqliu 2010-03-10 feature 10834
     * 
     * @param customerDataset
     * @param unit
     */
    private void addDataEntity2CustomerDataset(CustomerDefaultCategoryDataset customerDataset, TableIndicatorUnit unit) {
        if (IndicatorEnum.WhereRuleIndicatorEnum.equals(unit.getType())) {
            // Added TDQ-7547 20140107 yyin: when the value is null, no need to proceed
            // if (unit.getValue() == null) {
            // return;
            // }// ~
            String columnKey = unit.getIndicatorName();
            double value = unit.getValue() == null ? Double.NaN : Double.parseDouble(unit.getValue().toString());
            double valueNotM = unit.getValue() == null ? Double.NaN : unit.geIndicatorCount() - value;
            customerDataset.addValue(valueNotM, ROW_KEY_NOT_PASS, columnKey);
            customerDataset.addValue(value, ROW_KEY_PASS, columnKey);

            WhereRuleChartDataEntity entity = new WhereRuleChartDataEntity();
            entity.setIndicator(unit.getIndicator());
            entity.setLabel(columnKey);
            entity.setNumMatch(String.valueOf(value));
            entity.setNumNoMatch(String.valueOf(valueNotM));
            // ADD xqliu 2010-03-10 feature 10834
            entity.setToolTip(getUnitToolTip(unit));
            // ~

            customerDataset.addDataEntity(entity);
        }
    }

    /**
     * DOC xqliu Comment method "addRownCountDataEntity2CustomerDataset".
     * 
     * @param customerDataset
     * @param unit
     */
    private void addRownCountDataEntity2CustomerDataset(CustomerDefaultCategoryDataset customerDataset, TableIndicatorUnit unit) {
        if (IndicatorEnum.RowCountIndicatorEnum.equals(unit.getType())) {
            final Object unitValue = unit.getValue();
            double valueCount = unitValue != null ? Double.parseDouble(unitValue.toString()) : Double.NaN;
            String label = unit.getIndicatorName();

            customerDataset.addValue(valueCount, label, label);

            ChartDataEntity entityCount = new ChartDataEntity();
            entityCount.setIndicator(unit.getIndicator());
            entityCount.setLabel(label);
            entityCount.setValue(String.valueOf(valueCount));
            entityCount.setPercent(valueCount / unit.getIndicator().getCount());

            customerDataset.addDataEntity(entityCount);
        }
    }

    /**
     * DOC xqliu Comment method "getDataEntityRowCount".
     * 
     * @return
     */
    public ChartDataEntity[] getDataEntityRowCount() {
        return getCustomerDatasetRownCount().getDataEntities();
    }

    /**
     * DOC xqliu Comment method "getTableFormRowCount".
     * 
     * @param composite
     * @return
     */
    public TableViewer getTableFormRowCount(Composite composite) {
        TableViewer tbViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);

        Table table = tbViewer.getTable();

        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gd = new GridData();
        gd.heightHint = 220;
        gd.widthHint = 500;
        gd.verticalAlignment = SWT.BEGINNING;
        table.setLayoutData(gd);

        createTableColumnStructure(getTableStructureRowCount(), table);

        tbViewer.setLabelProvider(getLabelProviderRowCount());

        tbViewer.setContentProvider(getContentProvider());

        return tbViewer;
    }

    /**
     * DOC xqliu Comment method "getLabelProviderRowCount".
     * 
     * @return
     */
    private IBaseLabelProvider getLabelProviderRowCount() {
        return new BaseChartTableLabelProvider();
    }

    /**
     * DOC xqliu WhereRuleStatisticsStateTable class global comment. Detailled comment
     */
    public static class WhereRuleTableLabelProvider extends PatternLabelProvider {

        private Long rowCount = 0L;

        public Long getRowCount() {
            return this.rowCount;
        }

        public void setRowCount(Long rowCount) {
            this.rowCount = rowCount;
        }

        public WhereRuleTableLabelProvider(Long rowCount) {
            setRowCount(rowCount);
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            Image result = super.getColumnImage(element, columnIndex);

            if (result == null) {
                boolean largeThanRowCount = false;

                Indicator indicator = ((WhereRuleChartDataEntity) element).getIndicator();

                if (!Double.isNaN(Double.parseDouble(((WhereRuleChartDataEntity) element).getNumMatch()))) {
                    if (IndicatorHelper.isWhereRuleIndicatorNotAide(indicator)) {
                        largeThanRowCount = getRowCount() < ((WhereRuleIndicator) indicator).getUserCount();
                    }
                }

                if (3 == columnIndex && largeThanRowCount) {
                    result = ImageLib.getImage(ImageLib.LEVEL_WARNING);
                }
            }

            return result;
        }

        @Override
        public Color getForeground(Object element, int columnIndex) {
            Color result = super.getForeground(element, columnIndex);

            if (result == null) {
                boolean largeThanRowCount = false;

                Indicator indicator = ((WhereRuleChartDataEntity) element).getIndicator();

                if (!Double.isNaN(Double.parseDouble(((WhereRuleChartDataEntity) element).getNumMatch()))) {
                    // MOD yyin 20121031 TDQ-6194, when: match+no match>row count, highlight
                    if (IndicatorHelper.isWhereRuleIndicatorNotAide(indicator)) {
                        largeThanRowCount = getRowCount() < ((WhereRuleIndicator) indicator).getCount();
                    }
                }

                if ((3 == columnIndex || 4 == columnIndex) && largeThanRowCount) {
                    result = Display.getDefault().getSystemColor(SWT.COLOR_RED);
                }
            }

            return result;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.dataprofiler.core.ui.editor.preview.model.states.IChartTypeStates#getChart(org.jfree.data.category
     * .CategoryDataset)
     */
    public JFreeChart getChart(CategoryDataset dataset) {
        // TODO Auto-generated method stub
        return null;
    }
}
