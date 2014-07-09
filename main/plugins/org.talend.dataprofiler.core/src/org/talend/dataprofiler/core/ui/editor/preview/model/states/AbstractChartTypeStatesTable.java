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

import java.awt.Font;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.talend.dataprofiler.common.ui.editor.preview.ICustomerDataset;
import org.talend.dataprofiler.core.ui.editor.preview.TableIndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.entity.TableStructureEntity;
import org.talend.dq.indicators.preview.table.ChartDataEntity;

/**
 * DOC xqliu class global comment. Detailled comment
 */
public abstract class AbstractChartTypeStatesTable implements IChartTypeStates {

    protected List<TableIndicatorUnit> units = new ArrayList<TableIndicatorUnit>();

    public AbstractChartTypeStatesTable(List<TableIndicatorUnit> units) {
        Iterator<TableIndicatorUnit> it = units.iterator();
        // remove if executed check, TDQ-8787, for dynamic chart
        if (units != null) {
            this.units.addAll(units);
        }
    }

    public ChartDataEntity[] getDataEntity() {
        if (getCustomerDataset() != null) {
            return getCustomerDataset().getDataEntities();
        }
        return null;
    }

    public CategoryDataset getDataset() {
        if (getCustomerDataset() != null) {
            return (CategoryDataset) getCustomerDataset();
        }
        return null;
    }

    public JFreeChart getFeatChart() {
        JFreeChart chart = getChart();
        if (chart != null) {
            Font font = null;
            CategoryPlot plot = chart.getCategoryPlot();
            CategoryItemRenderer render = plot.getRenderer();
            CategoryAxis domainAxis = plot.getDomainAxis();
            ValueAxis valueAxis = plot.getRangeAxis();

            font = new Font("Arail", Font.BOLD, 12); //$NON-NLS-1$

            render.setBaseItemLabelFont(font);

            font = new Font("Verdana", Font.BOLD, 12); //$NON-NLS-1$
            domainAxis.setLabelFont(font);
            valueAxis.setLabelFont(font);

            font = new Font("Verdana", Font.PLAIN, 10); //$NON-NLS-1$
            domainAxis.setTickLabelFont(font);
            valueAxis.setTickLabelFont(font);

            font = new Font("Verdana", Font.BOLD, 10); //$NON-NLS-1$
            LegendTitle legend = chart.getLegend();
            if (legend != null) {
                legend.setItemFont(font);
            }

            font = null;
        }

        return chart;
    }

    public TableViewer getTableForm(Composite parent) {
        TableViewer tbViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);

        Table table = tbViewer.getTable();

        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gd = new GridData();
        gd.heightHint = 220;
        gd.widthHint = 500;
        gd.verticalAlignment = SWT.BEGINNING;
        table.setLayoutData(gd);

        createTableColumnStructure(getTableStructure(), table);

        tbViewer.setLabelProvider(getLabelProvider());

        tbViewer.setContentProvider(getContentProvider());

        return tbViewer;
    }

    protected abstract TableStructureEntity getTableStructure();

    protected abstract ITableLabelProvider getLabelProvider();

    protected abstract IStructuredContentProvider getContentProvider();

    protected void createTableColumnStructure(TableStructureEntity entity, Table table) {
        if (entity.isValid()) {
            for (int i = 0; i < entity.getColumnCount(); i++) {
                TableColumn column = new TableColumn(table, SWT.NONE);
                column.setText(entity.getFieldNames()[i]);
                column.setWidth(entity.getFieldWidths()[i]);
            }
        }
    }

    public XYDataset getXYDataset() {
        if (getCustomerXYDataset() != null) {
            return (XYDataset) getCustomerXYDataset();
        }

        return null;
    }

    public ICustomerDataset getCustomerXYDataset() {
        return null;
    }

    public List<JFreeChart> getChartList() {
        return null;
    }

    public JFreeChart getChart() {
        return null;
    }
}
