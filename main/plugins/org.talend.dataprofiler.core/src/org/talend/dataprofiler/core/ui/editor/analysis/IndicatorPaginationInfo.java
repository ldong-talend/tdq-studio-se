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
package org.talend.dataprofiler.core.ui.editor.analysis;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.model.ModelElementIndicator;
import org.talend.dataprofiler.core.model.dynamic.DynamicIndicatorModel;
import org.talend.dataprofiler.core.ui.chart.ChartUtils;
import org.talend.dataprofiler.core.ui.editor.preview.IndicatorUnit;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.IChartTypeStates;
import org.talend.dataprofiler.core.ui.pref.EditorPreferencePage;
import org.talend.dataprofiler.core.ui.utils.pagination.PaginationInfo;
import org.talend.dataprofiler.core.ui.utils.pagination.UIPagination;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dq.nodes.indicator.type.IndicatorEnum;

/**
 * 
 * DOC mzhao UIPagination class global comment. Detailled comment
 */
public abstract class IndicatorPaginationInfo extends PaginationInfo {

    private static final int PAGE_SIZE = 5;

    protected List<? extends ModelElementIndicator> modelElementIndicators;

    // Added TDQ-8787 20140617 yyin : store the temp indicator and its related dataset between one running
    protected List<DynamicIndicatorModel> dynamicList = new ArrayList<DynamicIndicatorModel>();

    public IndicatorPaginationInfo(ScrolledForm form, List<? extends ModelElementIndicator> modelElementIndicators,
            UIPagination uiPagination) {
        super(form, modelElementIndicators, uiPagination);
        this.modelElementIndicators = modelElementIndicators;
    }

    protected void addListenerToChartComp(final ChartComposite chartComp, final IChartTypeStates chartTypeState) {
        chartComp.addChartMouseListener(new ChartMouseListener() {

            public void chartMouseClicked(ChartMouseEvent event) {
                final String referenceLink = chartTypeState.getReferenceLink();
                if (event.getTrigger().getButton() == 1 && referenceLink != null) {
                    Menu menu = new Menu(chartComp.getShell(), SWT.POP_UP);
                    chartComp.setMenu(menu);

                    MenuItem item = new MenuItem(menu, SWT.PUSH);
                    item.setText(DefaultMessagesImpl.getString("ColumnMasterDetailsPage.what")); //$NON-NLS-1$
                    item.addSelectionListener(new SelectionAdapter() {

                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            ChartUtils.openReferenceLink(referenceLink);
                        }
                    });

                    menu.setVisible(true);
                }
            }

            public void chartMouseMoved(ChartMouseEvent event) {
                // no need to implement
            }

        });
        chartComp.addDisposeListener(new DisposeListener() {

            public void widgetDisposed(DisposeEvent e) {
                chartComp.dispose();

            }

        });
    }

    @SuppressWarnings("deprecation")
    public static int getPageSize() {
        try {
            String defaultPageSize = ResourcesPlugin.getPlugin().getPluginPreferences()
                    .getString(EditorPreferencePage.ANALYZED_ITEMS_PER_PAGE);
            if (!"".equals(defaultPageSize)) { //$NON-NLS-1$
                return Integer.parseInt(defaultPageSize);
            }
        } catch (NumberFormatException e) {
            ExceptionHandler.process(e);
        }
        return PAGE_SIZE;
    }

    public void setModelElementIndicators(List<? extends ModelElementIndicator> modelElementIndicators) {
        this.modelElementIndicators = modelElementIndicators;
    }

    public List<? extends ModelElementIndicator> getModelElementIndicators() {
        return modelElementIndicators;
    }

    /**
     * get the indicators from the units, filter the range and IQR type, For the chart
     * 
     * @param units
     * @return
     */
    protected List<Indicator> getIndicators(List<IndicatorUnit> units) {
        List<Indicator> indicators = new ArrayList<Indicator>();
        for (IndicatorUnit indicatorunit : units) {
            if (!IndicatorEnum.RangeIndicatorEnum.equals(indicatorunit.getType())
                    && !IndicatorEnum.IQRIndicatorEnum.equals(indicatorunit.getType())) {
                indicators.add(indicatorunit.getIndicator());
            }
        }
        return indicators;
    }

    /**
     * get the indicator for the table, which will show alls, different from the chart
     * 
     * @param units
     * @param filterNull
     * @return
     */
    protected List<Indicator> getIndicatorsForTable(List<IndicatorUnit> units, boolean filterNull) {
        List<Indicator> indicators = new ArrayList<Indicator>();
        for (IndicatorUnit unit : units) {
            if (filterNull) {
                if (unit.getIndicator().getRealValue() != null && "null".equals(unit.getIndicator().getRealValue())) {//$NON-NLS-1$
                    continue;
                }
            }
            indicators.add(unit.getIndicator());
        }
        return indicators;
    }

    public List<DynamicIndicatorModel> getDynamicIndicatorList() {
        return this.dynamicList;
    }

    public void clearDynamicList() {
        for (DynamicIndicatorModel dyModel : dynamicList) {
            dyModel.clear();
        }
        dynamicList.clear();
    }
}
