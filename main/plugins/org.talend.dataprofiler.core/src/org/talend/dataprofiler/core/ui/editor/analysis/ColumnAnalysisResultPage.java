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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.Section;
import org.jfree.data.category.CategoryDataset;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.model.ModelElementIndicator;
import org.talend.dataprofiler.core.model.dynamic.DynamicIndicatorModel;
import org.talend.dataprofiler.core.ui.editor.preview.model.dataset.CustomerDefaultBAWDataset;
import org.talend.dataprofiler.core.ui.events.DynamicBAWChartEventReceiver;
import org.talend.dataprofiler.core.ui.events.DynamicChartEventReceiver;
import org.talend.dataprofiler.core.ui.events.EventEnum;
import org.talend.dataprofiler.core.ui.events.EventManager;
import org.talend.dataprofiler.core.ui.events.EventReceiver;
import org.talend.dataprofiler.core.ui.utils.AnalysisUtils;
import org.talend.dataprofiler.core.ui.utils.pagination.UIPagination;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dq.analysis.AnalysisHandler;

/**
 * DOC zqin class global comment. Detailled comment
 */
public class ColumnAnalysisResultPage extends AbstractAnalysisResultPage implements PropertyChangeListener {

    // private static Logger log = Logger.getLogger(ColumnAnalysisResultPage.class);

    private Composite resultComp;

    ColumnMasterDetailsPage masterPage;

    private Section resultSection = null;

    private UIPagination uiPagination = null;

    protected Map<Indicator, EventReceiver> eventReceivers = new IdentityHashMap<Indicator, EventReceiver>();

    private EventReceiver registerDynamicRefreshEvent;

    Composite chartTableComposite = null;

    private Composite chartComposite;

    /**
     * DOC zqin ColumnAnalysisResultPage constructor comment.
     * 
     * @param editor
     * @param id
     * @param title
     */
    public ColumnAnalysisResultPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
        AnalysisEditor analysisEditor = (AnalysisEditor) editor;
        this.masterPage = (ColumnMasterDetailsPage) analysisEditor.getMasterPage();
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        super.createFormContent(managedForm);

        resultComp = toolkit.createComposite(topComposite);
        resultComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
        resultComp.setLayout(new GridLayout());
        createResultSection(resultComp);
        form.reflow(true);
    }

    @Override
    protected AnalysisHandler getAnalysisHandler() {
        return this.masterPage.getAnalysisHandler();
    }

    @Override
    protected void createResultSection(Composite parent) {
        resultSection = createSection(form, parent,
                DefaultMessagesImpl.getString("ColumnAnalysisResultPage.analysisResult"), null); //$NON-NLS-1$
        chartTableComposite = toolkit.createComposite(resultSection);
        chartTableComposite.setLayout(new GridLayout());
        chartTableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final ModelElementIndicator[] modelElementIndicatores = masterPage.getCurrentModelElementIndicators();

        // ~ MOD mzhao 2009-04-20, Do pagination. Bug 6512.
        uiPagination = new UIPagination(toolkit, chartTableComposite);
        int pageSize = IndicatorPaginationInfo.getPageSize();
        int totalPages = modelElementIndicatores.length / pageSize;
        List<ModelElementIndicator> modelElementIndLs = null;
        for (int index = 0; index < totalPages; index++) {
            modelElementIndLs = new ArrayList<ModelElementIndicator>();
            for (int idx = 0; idx < pageSize; idx++) {
                modelElementIndLs.add(modelElementIndicatores[index * pageSize + idx]);
            }
            IndicatorPaginationInfo pginfo = new ResultPaginationInfo(form, modelElementIndLs, masterPage, uiPagination);
            uiPagination.addPage(pginfo);
        }

        int left = modelElementIndicatores.length % pageSize;
        if (left != 0) {
            modelElementIndLs = new ArrayList<ModelElementIndicator>();
            for (int leftIdx = 0; leftIdx < left; leftIdx++) {
                modelElementIndLs.add(modelElementIndicatores[totalPages * pageSize + leftIdx]);
            }
            IndicatorPaginationInfo pginfo = new ResultPaginationInfo(form, modelElementIndLs, masterPage, uiPagination);
            uiPagination.addPage(pginfo);
        }
        chartComposite = toolkit.createComposite(chartTableComposite);
        chartComposite.setLayout(new GridLayout());
        chartComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        // ~
        resultSection.setClient(chartTableComposite);

        uiPagination.setChartComposite(chartComposite);
        uiPagination.init();
        chartComposite.layout();
        chartComposite.pack();
        form.reflow(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.AbstractFormPage#setDirty(boolean)
     */
    @Override
    public void setDirty(boolean isDirty) {
    }

    /*
     * (non-Javadoc)
     * 
     * @seejava.beans.PropertyChangeListener#propertyChange(java.beans. PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (PluginConstant.ISDIRTY_PROPERTY.equals(evt.getPropertyName())) {
            ((AnalysisEditor) this.getEditor()).firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisResultPage#refresh(org.talend.dataprofiler.core
     * .ui.editor.analysis.AbstractAnalysisMetadataPage)
     */
    @Override
    public void refresh(AbstractAnalysisMetadataPage masterPage) {
        this.masterPage = (ColumnMasterDetailsPage) masterPage;

        disposeComposite();
        masterPage.refresh();
        createFormContent(getManagedForm());
    }

    private void disposeComposite() {
        if (summaryComp != null && !summaryComp.isDisposed()) {
            summaryComp.dispose();
        }

        if (resultComp != null && !resultComp.isDisposed()) {
            resultComp.dispose();
        }
        if (chartComposite != null && !chartComposite.isDisposed()) {
            for (Control control : chartComposite.getChildren()) {
                control.dispose();
            }
        }
    }

    /**
     * Added TDQ-8787 20140613 yyin: create all charts before running, register each chart with its related indicator.
     */
    public void registerDynamicEvent() {
        disposeComposite();
        createFormContent(getManagedForm());

        // get all indicators and datasets
        List<DynamicIndicatorModel> indiAndDatasets = uiPagination.getAllIndcatorAndDatasetOfCurrentPage();

        // register dynamic event,for the indicator (for each column)
        for (DynamicIndicatorModel oneCategoryIndicatorModel : indiAndDatasets) {
            CategoryDataset categoryDataset = oneCategoryIndicatorModel.getDataset();
            TableViewer tableViewer = oneCategoryIndicatorModel.getTableViewer();
            if (categoryDataset instanceof CustomerDefaultBAWDataset) {
                // when all summary indicators are selected
                // when all summary indicators are selected
                DynamicBAWChartEventReceiver bawReceiver = AnalysisUtils.createDynamicBAWChartEventReceiver(
                        oneCategoryIndicatorModel, categoryDataset, eventReceivers);
                bawReceiver.setChartComposite(chartComposite);
                // register the parent baw receiver with one of summary indicator, no need to handle baw actually
                registerIndicatorEvent(oneCategoryIndicatorModel.getIndicatorList().get(0), bawReceiver);
            } else {
                int index = 0;
                for (Indicator oneIndicator : oneCategoryIndicatorModel.getIndicatorList()) {
                    DynamicChartEventReceiver eReceiver = AnalysisUtils.createDynamicChartEventReceiver(categoryDataset, index++,
                            oneIndicator);
                    eReceiver.setChartComposite(chartComposite);

                    registerIndicatorEvent(oneIndicator, eReceiver);
                }
            }
        }
        reLayoutChartComposite();

        registerRefreshDynamicChartEvent();
    }

    private void registerIndicatorEvent(Indicator oneIndicator, DynamicChartEventReceiver eReceiver) {
        eventReceivers.put(oneIndicator, eReceiver);
        EventManager.getInstance().register(oneIndicator, EventEnum.DQ_DYMANIC_CHART, eReceiver);
    }

    public void reLayoutChartComposite() {
        chartComposite.getParent().layout();
        chartComposite.layout();
    }

    /**
     * refresh the composite of the chart, to show the changes on the chart.
     */
    private void registerRefreshDynamicChartEvent() {
        registerDynamicRefreshEvent = new EventReceiver() {

            @Override
            public boolean handle(Object data) {
                reLayoutChartComposite();
                return true;
            }
        };
        EventManager.getInstance().register(chartComposite, EventEnum.DQ_DYNAMIC_REFRESH_DYNAMIC_CHART,
                registerDynamicRefreshEvent);
    }

    /**
     * unregister every dynamic events which registered before executing analysis
     * 
     * @param eventReceivers
     */
    public void unRegisterDynamicEvent() {
        for (Indicator oneIndicator : eventReceivers.keySet()) {
            DynamicChartEventReceiver eventReceiver = (DynamicChartEventReceiver) eventReceivers.get(oneIndicator);
            eventReceiver.clear();
            EventManager.getInstance().unRegister(oneIndicator, EventEnum.DQ_DYMANIC_CHART, eventReceiver);
        }
        eventReceivers.clear();
        EventManager.getInstance().unRegister(chartComposite, EventEnum.DQ_DYNAMIC_REFRESH_DYNAMIC_CHART,
                registerDynamicRefreshEvent);

        uiPagination.clearAllDynamicMapOfCurrentPage();

        masterPage.clearDynamicDatasets();
    }

}
