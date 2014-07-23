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

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.talend.dataprofiler.common.ui.editor.preview.ICustomerDataset;
import org.talend.dq.analysis.explore.DataExplorer;
import org.talend.dq.indicators.preview.table.ChartDataEntity;

/**
 * DOC Zqin class global comment. Detailled comment
 */
public interface IChartTypeStates {

    /**
     * DOC Zqin Comment method "getDataset".
     * 
     * @return the dataset of the specified chart.
     */
    public CategoryDataset getDataset();

    /**
     * DOC Zqin Comment method "getDataEntity".
     * 
     * @return the data entity of the specified chart, this is to create table viewer.
     */
    public ChartDataEntity[] getDataEntity();

    /**
     * DOC Zqin Comment method "getCustomerDataset".
     * 
     * @return the customer dataset of the specified chart, the dataset own both basic dataset and data entity.
     */
    public ICustomerDataset getCustomerDataset();

    /**
     * DOC Zqin Comment method "getChart".
     * 
     * @return the specified chart.
     */
    public JFreeChart getChart();

    /**
     * DOC Zqin Comment method "getExampleChart".
     * 
     * @return the specified chart with example data.
     */
    public JFreeChart getExampleChart();

    /**
     * DOC Zqin Comment method "getDataExplorer".
     * 
     * @return the specified data explorer for kinds of chart.
     */
    public DataExplorer getDataExplorer();

    /**
     * DOC Zqin Comment method "getTableForm".
     * 
     * @param parent
     * @return the table form of the specified chart data.
     */
    public TableViewer getTableForm(Composite parent);

    /**
     * DOC Administrator Comment method "getReferenceLink".
     * 
     * @return
     */
    public String getReferenceLink();

    /**
     * DOC xqliu Comment method "getXYDataset".
     * 
     * @return
     */
    public XYDataset getXYDataset();

    /**
     * DOC xqliu Comment method "getCustomerXYDataset".
     * 
     * @return
     */
    public ICustomerDataset getCustomerXYDataset();

    /**
     * DOC xqliu Comment method "getChartList".
     * 
     * @return
     */
    public List<JFreeChart> getChartList();

    /**
     * use an existed dataset to create a chart
     * 
     * @param dataset
     * @return
     */
    public JFreeChart getChart(CategoryDataset dataset);

    public List<JFreeChart> getChartList(List<DefaultCategoryDataset> datasets);
}
