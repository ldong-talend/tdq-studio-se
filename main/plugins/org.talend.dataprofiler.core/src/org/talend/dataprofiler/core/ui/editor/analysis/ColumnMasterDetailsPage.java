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
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.talend.core.model.metadata.MetadataColumnRepositoryObject;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.MetadataColumn;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.repositoryObject.MetadataXmlElementTypeRepositoryObject;
import org.talend.cwm.helper.ModelElementHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.cwm.helper.TaggedValueHelper;
import org.talend.cwm.relational.TdColumn;
import org.talend.cwm.xml.TdXmlElementType;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.helper.ModelElementIndicatorHelper;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.model.ModelElementIndicator;
import org.talend.dataprofiler.core.model.dynamic.DynamicIndicatorModel;
import org.talend.dataprofiler.core.ui.dialog.ColumnsSelectionDialog;
import org.talend.dataprofiler.core.ui.editor.composite.AbstractColumnDropTree;
import org.talend.dataprofiler.core.ui.editor.composite.AnalysisColumnTreeViewer;
import org.talend.dataprofiler.core.ui.editor.composite.DataFilterComp;
import org.talend.dataprofiler.core.ui.pref.EditorPreferencePage;
import org.talend.dataprofiler.core.ui.utils.pagination.UIPagination;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.analysis.ExecutionLanguage;
import org.talend.dataquality.domain.Domain;
import org.talend.dataquality.exception.DataprofilerCoreException;
import org.talend.dataquality.helpers.MetadataHelper;
import org.talend.dataquality.indicators.DataminingType;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.sql.UserDefIndicator;
import org.talend.dq.analysis.ModelElementAnalysisHandler;
import org.talend.dq.analysis.connpool.TdqAnalysisConnectionPool;
import org.talend.dq.helper.EObjectHelper;
import org.talend.dq.helper.RepositoryNodeHelper;
import org.talend.dq.helper.UDIHelper;
import org.talend.dq.nodes.indicator.type.IndicatorEnum;
import org.talend.dq.writer.impl.ElementWriterFactory;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.RepositoryNode;
import org.talend.utils.sugars.ReturnCode;
import orgomg.cwm.objectmodel.core.ModelElement;

/**
 * @author rli
 */
public class ColumnMasterDetailsPage extends DynamicAnalysisMasterPage implements PropertyChangeListener {

    private Composite tree;

    private Composite navigationComposite;

    AnalysisColumnTreeViewer treeViewer;

    ModelElementAnalysisHandler analysisHandler;

    private ModelElementIndicator[] currentModelElementIndicators;

    private String stringDataFilter;

    private static final int TREE_MAX_LENGTH = 400;

    private ExpandableComposite[] previewChartCompsites;

    private Section analysisColumnSection = null;

    final private List<ExpandableComposite> previewChartList = new ArrayList<ExpandableComposite>();

    private UIPagination uiPagination;

    public ColumnMasterDetailsPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
        currentEditor = (AnalysisEditor) editor;
    }

    @Override
    public void initialize(FormEditor editor) {
        super.initialize(editor);
        recomputeIndicators();
    }

    public void recomputeIndicators() {
        analysisHandler = new ModelElementAnalysisHandler();
        analysisHandler.setAnalysis((Analysis) this.currentModelElement);
        // Handle JUDIs
        UDIHelper.updateJUDIsForAnalysis(analysisItem.getAnalysis());

        stringDataFilter = analysisHandler.getStringDataFilter();
        EList<ModelElement> analyzedColumns = analysisHandler.getAnalyzedColumns();
        List<ModelElementIndicator> meIndicatorList = new ArrayList<ModelElementIndicator>();
        ModelElementIndicator currentIndicator = null;
        for (ModelElement element : analyzedColumns) {
            TdColumn tdColumn = SwitchHelpers.COLUMN_SWITCH.doSwitch(element);
            TdXmlElementType xmlElement = SwitchHelpers.XMLELEMENTTYPE_SWITCH.doSwitch(element);

            // MOD qiongli 2011-1-10,16796:delimitefile
            MetadataColumn mdColumn = SwitchHelpers.METADATA_COLUMN_SWITCH.doSwitch(element);
            if (tdColumn == null && xmlElement == null && mdColumn == null) {
                continue;
            }
            // MOD qiongli TDQ-7052 if the node is filtered ,it will be return null,so should create a new node.
            RepositoryNode repNode = RepositoryNodeHelper.recursiveFind(element);
            if (repNode == null) {
                repNode = RepositoryNodeHelper.createRepositoryNode(element);
            }
            // MOD mzhao feature 15750, The column is recompute from the file, here create a new repository view object.
            if (tdColumn == null && mdColumn != null) {
                currentIndicator = ModelElementIndicatorHelper.createDFColumnIndicator(repNode);
            } else if (tdColumn != null) {
                currentIndicator = ModelElementIndicatorHelper.createModelElementIndicator(repNode);
            } else if (xmlElement != null) {
                currentIndicator = ModelElementIndicatorHelper.createModelElementIndicator(repNode);
            }

            DataminingType dataminingType = DataminingType.get(analysisHandler.getDatamingType(element));
            MetadataHelper.setDataminingType(dataminingType == null ? DataminingType.NOMINAL : dataminingType, element);
            Collection<Indicator> indicatorList = analysisHandler.getIndicators(element);
            if (currentIndicator != null) {
                currentIndicator.setIndicators(indicatorList.toArray(new Indicator[indicatorList.size()]));
                meIndicatorList.add(currentIndicator);
            }
        }
        currentModelElementIndicators = meIndicatorList.toArray(new ModelElementIndicator[meIndicatorList.size()]);
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        this.form = managedForm.getForm();
        Composite body = form.getBody();
        body.setLayout(new GridLayout());
        sForm = new SashForm(body, SWT.NULL);
        sForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        topComp = toolkit.createComposite(sForm);
        topComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        topComp.setLayout(new GridLayout());
        metadataSection = creatMetadataSection(form, topComp);
        form.setText(DefaultMessagesImpl.getString("ColumnMasterDetailsPage.columnAna")); //$NON-NLS-1$
        metadataSection.setText(DefaultMessagesImpl.getString("ColumnMasterDetailsPage.analysisMeta")); //$NON-NLS-1$
        metadataSection.setDescription(DefaultMessagesImpl.getString("ColumnMasterDetailsPage.setPropOfAnalysis")); //$NON-NLS-1$

        createAnalysisColumnsSection(form, topComp);

        createDataFilterSection(form, topComp);

        // MOD xqliu 2009-07-01 bug 7068
        createExecuteEngineSection(form, topComp, analysisHandler.getAnalyzedColumns(), analysisHandler.getAnalysis()
                .getParameters());
        // ~
        if (!EditorPreferencePage.isHideGraphics()) {
            createPreviewComposite();

            createPreviewSection(form, previewComp);
        }
    }

    void createAnalysisColumnsSection(final ScrolledForm form1, Composite anasisDataComp) {
        analysisColumnSection = createSection(form1, anasisDataComp,
                DefaultMessagesImpl.getString("ColumnMasterDetailsPage.analyzeColumn"), null); //$NON-NLS-1$

        Composite topComp1 = toolkit.createComposite(analysisColumnSection, SWT.NONE);
        topComp1.setLayout(new GridLayout());
        // ~ MOD mzhao 2009-05-05,Bug 6587.
        createConnBindWidget(topComp1);
        // ~

        Hyperlink clmnBtn = toolkit.createHyperlink(topComp1,
                DefaultMessagesImpl.getString("ColumnMasterDetailsPage.selectColumn"), SWT.NONE); //$NON-NLS-1$
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(clmnBtn);
        clmnBtn.addHyperlinkListener(new HyperlinkAdapter() {

            @Override
            public void linkActivated(HyperlinkEvent e) {
                openColumnsSelectionDialog();
            }

        });

        Hyperlink indcBtn = toolkit.createHyperlink(topComp1,
                DefaultMessagesImpl.getString("ColumnMasterDetailsPage.selectIndicator"), SWT.NONE); //$NON-NLS-1$
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(indcBtn);
        indcBtn.addHyperlinkListener(new HyperlinkAdapter() {

            @Override
            public void linkActivated(HyperlinkEvent e) {
                ModelElementIndicator[] result = treeViewer.openIndicatorSelectDialog(ColumnMasterDetailsPage.this.getSite()
                        .getShell());
                refreshCurrentTreeViewer(result);
            }

        });

        Composite actionBarComp = toolkit.createComposite(topComp1, SWT.NONE);
        GridLayout gdLayout = new GridLayout();
        gdLayout.numColumns = 3;
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        actionBarComp.setLayoutData(data);
        actionBarComp.setLayout(gdLayout);

        ImageHyperlink collapseAllImageLink = toolkit.createImageHyperlink(actionBarComp, SWT.NONE);
        collapseAllImageLink.setToolTipText(DefaultMessagesImpl.getString("CollapseAllColumns")); //$NON-NLS-1$
        collapseAllImageLink.setImage(ImageLib.getImage(ImageLib.COLLAPSE_ALL));
        collapseAllImageLink.addHyperlinkListener(new HyperlinkAdapter() {

            @Override
            public void linkActivated(HyperlinkEvent e) {
                TreeItem[] items = treeViewer.getTree().getItems();
                expandTreeItems(items, false);
                packOtherColumns();
            }
        });

        ImageHyperlink expandAllImageLink = toolkit.createImageHyperlink(actionBarComp, SWT.NONE);
        expandAllImageLink.setToolTipText(DefaultMessagesImpl.getString("ExpandAllColumns")); //$NON-NLS-1$
        expandAllImageLink.setImage(ImageLib.getImage(ImageLib.EXPAND_ALL));
        expandAllImageLink.addHyperlinkListener(new HyperlinkAdapter() {

            @Override
            public void linkActivated(HyperlinkEvent e) {
                TreeItem[] items = treeViewer.getTree().getItems();
                expandTreeItems(items, true);
                packOtherColumns();
            }
        });
        navigationComposite = toolkit.createComposite(actionBarComp, SWT.NONE);

        navigationComposite.setLayoutData(data);
        navigationComposite.setLayout(new GridLayout());

        createPaginationTree(topComp1);
        analysisColumnSection.setClient(topComp1);
    }

    /**
     * DOC zshen Comment method "createPaginationTree".
     * 
     * @param topComp1
     */
    private void createPaginationTree(Composite topComp1) {
        tree = toolkit.createComposite(topComp1, SWT.NONE);

        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tree);
        tree.setLayout(new GridLayout());
        ((GridData) tree.getLayoutData()).heightHint = TREE_MAX_LENGTH;

        treeViewer = new AnalysisColumnTreeViewer(tree, this);
        treeViewer.setDirty(false);
        treeViewer.addPropertyChangeListener(this);

        // pagination compoent
        computePagination();
        // ~
    }

    /**
     * Go to last of the page.
     */
    public void goLastPage() {
        uiPagination.goToLastPage();
    }

    /**
     * DOC zshen Comment method "computePagination".
     */
    private void computePagination() {
        disposeChartComposite();

        if (uiPagination == null) {
            uiPagination = new UIPagination(toolkit);
            uiPagination.setComposite(navigationComposite);
        } else {
            uiPagination.reset();
        }

        final ModelElementIndicator[] modelElementIndicatorArrary = this.getCurrentModelElementIndicators();
        int pageSize = IndicatorPaginationInfo.getPageSize();
        int totalPages = modelElementIndicatorArrary.length / pageSize;
        List<ModelElementIndicator> modelElementIndicatorList = null;

        for (int index = 0; index < totalPages; index++) {
            modelElementIndicatorList = new ArrayList<ModelElementIndicator>();
            for (int idx = 0; idx < pageSize; idx++) {
                modelElementIndicatorList.add(modelElementIndicatorArrary[index * pageSize + idx]);
            }
            IndicatorPaginationInfo pginfo = new MasterPaginationInfo(form, previewChartList, modelElementIndicatorList,
                    uiPagination, treeViewer);
            uiPagination.addPage(pginfo);
        }

        int left = modelElementIndicatorArrary.length % pageSize;
        if (left != 0 || totalPages == 0) {
            modelElementIndicatorList = new ArrayList<ModelElementIndicator>();
            for (int leftIdx = 0; leftIdx < left; leftIdx++) {
                modelElementIndicatorList.add(modelElementIndicatorArrary[totalPages * pageSize + leftIdx]);
            }
            IndicatorPaginationInfo pginfo = new MasterPaginationInfo(form, previewChartList, modelElementIndicatorList,
                    uiPagination, treeViewer);
            uiPagination.addPage(pginfo);
            // FIXME totalPages won't used anymore.
            totalPages++;
        }
        uiPagination.init();
    }

    /**
     * dispose ChartComposite.
     */
    @Override
    public void disposeChartComposite() {
        if (chartComposite != null && !chartComposite.isDisposed()) {
            for (Control control : chartComposite.getChildren()) {
                control.dispose();
            }
        }
    }

    /**
     * DOC mzhao Comment method "packOtherColumns".
     */
    private void packOtherColumns() {
        TreeColumn[] columns = treeViewer.getTree().getColumns();
        for (TreeColumn column : columns) {
            column.pack();
        }
        treeViewer.getTree().pack();
        treeViewer.getTree().getParent().layout();
    }

    /**
     * 
     * DOC mzhao Comment method "expandTreeItems".
     * 
     * @param items
     * @param expandOrCollapse
     */
    private void expandTreeItems(TreeItem[] items, boolean expandOrCollapse) {
        for (TreeItem item : items) {
            item.setExpanded(expandOrCollapse);
            TreeItem[] its = item.getItems();
            if (its != null && its.length > 0) {
                expandTreeItems(its, expandOrCollapse);
            }
        }
    }

    /**
     * open the column selection dialog.
     */
    public void openColumnsSelectionDialog() {
        RepositoryNode connNode = (RepositoryNode) getConnCombo().getData(String.valueOf(getConnCombo().getSelectionIndex()));
        ModelElementIndicator[] modelElementIndicators = this.getCurrentModelElementIndicators();
        List<IRepositoryNode> reposViewObjList = new ArrayList<IRepositoryNode>();
        for (ModelElementIndicator modelElementIndicator : modelElementIndicators) {
            reposViewObjList.add(modelElementIndicator.getModelElementRepositoryNode());
        }
        ColumnsSelectionDialog dialog = new ColumnsSelectionDialog(
                this,
                null,
                DefaultMessagesImpl.getString("ColumnMasterDetailsPage.columnSelection"), reposViewObjList, connNode, DefaultMessagesImpl //$NON-NLS-1$
                        .getString("ColumnMasterDetailsPage.columnSelections")); //$NON-NLS-1$
        if (dialog.open() == Window.OK) {
            Object[] modelElements = dialog.getResult();
            setTreeViewInput(modelElements);
        }
    }

    public void setTreeViewInput(Object[] modelElements) {
        // MOD yyi 2012-02-29 TDQ-3605 Empty column table.
        treeViewer.filterInputData(modelElements);
        refreshTheTree(treeViewer.getModelElementIndicator());
        this.setDirty(true);
    }

    public void refreshTheTree(ModelElementIndicator[] modelElements) {
        this.currentModelElementIndicators = modelElements;
        this.computePagination();
    }

    @Override
    public void createPreviewCharts(final ScrolledForm form1, final Composite composite) {
        uiPagination.setChartComposite(composite);
        uiPagination.init();

        for (ExpandableComposite comp : previewChartList) {
            registerSection(comp);
        }

        composite.layout();
        composite.pack();
        form1.reflow(true);
    }

    @Override
    public void refresh() {
        if (EditorPreferencePage.isHideGraphics()) {
            if (sForm.getChildren().length > 1) {
                if (null != sForm.getChildren()[1] && !sForm.getChildren()[1].isDisposed()) {
                    sForm.getChildren()[1].dispose();
                }
                topComp.getParent().layout();
                topComp.layout();
            }

        } else {
            if (chartComposite != null && !chartComposite.isDisposed()) {
                disposeChartComposite();
                createPreviewCharts(form, chartComposite);
                reLayoutChartComposite();
            } else {
                createPreviewComposite();
                createPreviewSection(form, previewComp);
                createPreviewCharts(form, chartComposite);
            }
        }
    }

    /**
     * @param form1
     * @param toolkit
     * @param anasisDataComp
     */
    void createDataFilterSection(final ScrolledForm form1, Composite anasisDataComp) {
        dataFilterSection = createSection(
                form1,
                anasisDataComp,
                DefaultMessagesImpl.getString("ColumnMasterDetailsPage.dataFilter"), DefaultMessagesImpl.getString("ColumnMasterDetailsPage.editDataFilter")); //$NON-NLS-1$ //$NON-NLS-2$

        Composite sectionClient = toolkit.createComposite(dataFilterSection);
        dataFilterComp = new DataFilterComp(sectionClient, stringDataFilter);
        dataFilterComp.addPropertyChangeListener(this);
        // ADD yyi 2011-05-31 16158:add whitespace check for text fields.
        addWhitespaceValidate(dataFilterComp.getDataFilterText());
        dataFilterSection.setClient(sectionClient);
    }

    /**
     * ADD xqliu 2009-08-24 bug 8776.
     * 
     * @return
     */
    protected boolean includeUDI() {
        ModelElementIndicator[] modelElementIndicators = this.getTreeViewer().getModelElementIndicator();
        for (ModelElementIndicator modelElementIndicator : modelElementIndicators) {
            Indicator[] indicators = modelElementIndicator.getIndicators();
            for (Indicator indicator : indicators) {
                if (indicator instanceof UserDefIndicator) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param outputFolder
     * @throws DataprofilerCoreException
     */
    @Override
    public void saveAnalysis() throws DataprofilerCoreException {

        IRepositoryViewObject reposObject = null;
        analysisHandler.clearAnalysis();
        ModelElementIndicator[] modelElementIndicators = this.getCurrentModelElementIndicators();
        // List<TdDataProvider> providerList = new ArrayList<TdDataProvider>();
        Connection tdProvider = null;
        Analysis analysis = analysisHandler.getAnalysis();

        // ADD gdbu 2011-3-3 bug 19179

        // remove the space from analysis name
        //        this.analysis.setName(this.analysis.getName().replace(" ", ""));//$NON-NLS-1$ //$NON-NLS-2$

        for (Domain domain : this.analysisItem.getAnalysis().getParameters().getDataFilter()) {
            domain.setName(this.analysisItem.getAnalysis().getName());
        }

        // ~

        analysis.getParameters().setExecutionLanguage(ExecutionLanguage.get(execLang));

        try {
            // check whether the field has integer value
            Integer.valueOf(numberOfConnectionsPerAnalysisText.getText());
        } catch (NumberFormatException nfe) {
            MessageDialogWithToggle.openError(null, DefaultMessagesImpl.getString("AbstractAnalysisMetadataPage.SaveAnalysis"), //$NON-NLS-1$
                    DefaultMessagesImpl.getString("ColumnMasterDetailsPage.emptyField", //$NON-NLS-1$
                            DefaultMessagesImpl.getString("AnalysisTuningPreferencePage.NumberOfConnectionsPerAnalysis"))); //$NON-NLS-1$ 

            String originalValue = TaggedValueHelper.getTaggedValue(TdqAnalysisConnectionPool.NUMBER_OF_CONNECTIONS_PER_ANALYSIS,
                    this.analysisItem.getAnalysis().getTaggedValue()).getValue();
            numberOfConnectionsPerAnalysisText.setText(originalValue);
        }
        // save the number of connections per analysis
        this.saveNumberOfConnectionsPerAnalysis();

        try {
            // MOD zshen feature 12919 to save analysisParameter.
            analysis.getParameters().setMaxNumberRows(Integer.parseInt(maxNumText.getText()));
        } catch (NumberFormatException nfe) {
            MessageDialogWithToggle.openError(null, DefaultMessagesImpl.getString("AbstractAnalysisMetadataPage.SaveAnalysis"), //$NON-NLS-1$
                    DefaultMessagesImpl.getString("ColumnMasterDetailsPage.emptyField", //$NON-NLS-1$
                            DefaultMessagesImpl.getString("ColumnMasterDetailsPage.maxNumberLabel"))); //$NON-NLS-1$ 
            maxNumText.setText(String.valueOf(analysis.getParameters().getMaxNumberRows()));
        }
        analysis.getParameters().setMaxNumberRows(analysis.getParameters().getMaxNumberRows());
        analysis.getParameters().setStoreData(drillDownCheck.getSelection());
        // ~12919

        if (modelElementIndicators != null && modelElementIndicators.length != 0) {
            tdProvider = ModelElementIndicatorHelper.getTdDataProvider(modelElementIndicators[0]);
            if (tdProvider.eIsProxy()) {
                // Resolve the connection again
                tdProvider = (Connection) EObjectHelper.resolveObject(tdProvider);
            }
            analysis.getContext().setConnection(tdProvider);

            for (ModelElementIndicator modelElementIndicator : modelElementIndicators) {
                reposObject = modelElementIndicator.getModelElementRepositoryNode().getObject();
                ModelElement modelEle = null;
                if (reposObject instanceof MetadataColumnRepositoryObject) {
                    modelEle = ((MetadataColumnRepositoryObject) reposObject).getTdColumn();
                } else if (reposObject instanceof MetadataXmlElementTypeRepositoryObject) {
                    modelEle = ((MetadataXmlElementTypeRepositoryObject) reposObject).getTdXmlElementType();
                }
                analysisHandler.addIndicator(modelEle, modelElementIndicator.getIndicators());
                DataminingType type = MetadataHelper.getDataminingType(modelEle);
                if (type == null) {
                    type = MetadataHelper.getDefaultDataminingType(modelElementIndicator.getJavaType());
                }
                analysisHandler.setDatamingType(type.getLiteral(), modelEle);
            }
        } else {
            analysis.getContext().setConnection(null);
        }
        analysisHandler.setStringDataFilter(dataFilterComp.getDataFilterString());
        // 2011.1.12 MOD by zhsne to unify anlysis and connection id when saving.

        ReturnCode saved = new ReturnCode(false);
        this.nameText.setText(analysisHandler.getName());
        // TDQ-5581,if has removed emlements(patten/udi),should remove dependency each other before saving.
        // MOD yyi 2012-02-08 TDQ-4621:Explicitly set true for updating dependencies.
        saved = ElementWriterFactory.getInstance().createAnalysisWrite().save(analysisItem, true);
        // MOD yyi 2012-02-03 TDQ-3602:Avoid to rewriting all analyzes after saving, no reason to update all analyzes
        // which is depended in the referred connection.
        // Extract saving log function.
        // @see org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisMetadataPage#logSaved(ReturnCode)
        logSaved(saved);

        treeViewer.setDirty(false);
        dataFilterComp.setDirty(false);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (PluginConstant.ISDIRTY_PROPERTY.equals(evt.getPropertyName())) {
            currentEditor.firePropertyChange(IEditorPart.PROP_DIRTY);
            currentEditor.setRefreshResultPage(true);
            // synNagivatorStat();
        } else if (PluginConstant.DATAFILTER_PROPERTY.equals(evt.getPropertyName())) {
            this.analysisHandler.setStringDataFilter((String) evt.getNewValue());
        } else if (PluginConstant.EXPAND_TREE.equals(evt.getPropertyName())) {

            ModelElementIndicator indicator = (ModelElementIndicator) ((Widget) evt.getNewValue())
                    .getData(AbstractColumnDropTree.MODELELEMENT_INDICATOR_KEY);
            expandChart(indicator);
        }
    }

    /**
     * DOC zshen Comment method "synNagivatorStat".
     */
    public void synNagivatorStat() {
        if (this.uiPagination != null) {
            this.uiPagination.synNagivatorState(this.treeViewer.getModelElementIndicator());
            this.currentModelElementIndicators = this.uiPagination.getAllTheModelElementIndicator();
        }
    }

    /**
     * DOC yyi 2011-06-02 16929:expand the selected column in the graphical chart.
     * 
     * @param indicator
     */
    protected void expandChart(ModelElementIndicator indicator) {
        // MOD klliu bug 22407 check the uiPagination is null.
        if (uiPagination == null) {
            return;
        }

        ExpandableComposite[] previewChartCompsitesa = getPreviewChartCompsites();
        if (previewChartCompsitesa != null) {
            for (ExpandableComposite comp : previewChartCompsitesa) {
                if (comp.getData() != indicator) {
                    comp.setExpanded(false);
                    comp.getParent().pack();
                }
            }
        }
    }

    /**
     * DOC yyi 2011-06-02 16929:get index of the column selected in the tree viewer.
     */
    protected int indexOfSelectedItem(ModelElementIndicator data) {
        int index = -1;
        for (int i = 0; i < treeViewer.getModelElementIndicator().length; i++) {
            if (data == treeViewer.getModelElementIndicator()[i]) {
                index = i + 1;
                break;
            }
        }
        return index;
    }

    @Override
    public boolean isDirty() {
        if (treeViewer == null ? false : treeViewer.isDirty()) {
            this.setDirty(treeViewer.isDirty());
        }
        if (dataFilterComp == null ? false : dataFilterComp.isDirty()) {
            this.setDirty(dataFilterComp.isDirty());
        }
        return super.isDirty();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (this.treeViewer != null) {
            this.treeViewer.removePropertyChangeListener(this);
        }
        if (dataFilterComp != null) {
            this.dataFilterComp.removePropertyChangeListener(this);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisMetadataPage#getTreeViewer()
     */
    @Override
    public AbstractColumnDropTree getTreeViewer() {
        return this.treeViewer;
    }

    @Override
    public ModelElementAnalysisHandler getAnalysisHandler() {
        return analysisHandler;
    }

    public ModelElementIndicator[] getCurrentModelElementIndicators() {
        return this.currentModelElementIndicators;
    }

    public ExpandableComposite[] getPreviewChartCompsites() {
        if (previewChartList != null && !previewChartList.isEmpty()) {
            // ADD msjian TDQ-6213 2012-12-18: filter the disposed composite
            List<ExpandableComposite> withOutDisposed = new ArrayList<ExpandableComposite>();
            for (ExpandableComposite com : previewChartList) {
                if (!com.isDisposed()) {
                    withOutDisposed.add(com);
                }
            }
            // TDQ-6213~
            this.previewChartCompsites = withOutDisposed.toArray(new ExpandableComposite[withOutDisposed.size()]);
        }
        return previewChartCompsites;
    }

    @Override
    public Composite getChartComposite() {
        return chartComposite;
    }

    @Override
    protected ReturnCode canRun() {
        // ADD xqliu 2010-01-22 bug 11200
        ReturnCode checkMdmExecutionEngine = checkMdmExecutionEngine();
        if (!checkMdmExecutionEngine.isOk()) {
            return checkMdmExecutionEngine;
        }

        if (checkJUDIUpdate()) {
            recomputeIndicators();
            computePagination();
        }
        // ~
        ModelElementIndicator[] modelElementIndicators = treeViewer.getModelElementIndicator();
        if (modelElementIndicators == null || modelElementIndicators.length == 0) {
            return new ReturnCode(DefaultMessagesImpl.getString("ColumnMasterDetailsPage.NoColumnAssigned"), false); //$NON-NLS-1$
        }
        for (ModelElementIndicator modelElementIndicator : modelElementIndicators) {
            if (modelElementIndicator.getIndicators().length == 0) {
                return new ReturnCode(DefaultMessagesImpl.getString("ColumnMasterDetailsPage.NoIndicatorAssigned"), false); //$NON-NLS-1$
            }
        }

        return new ReturnCode(true);
    }

    /**
     * check whether JUDI need to update
     * 
     * @return
     */
    private boolean checkJUDIUpdate() {
        ModelElementIndicator[] modelElementIndicators = this.getTreeViewer().getModelElementIndicator();
        for (ModelElementIndicator modelElementIndicator : modelElementIndicators) {
            Indicator[] indicators = modelElementIndicator.getIndicators();
            for (Indicator indicator : indicators) {
                if (UDIHelper.isJavaUDI(indicator) && UDIHelper.needUpdateJUDI(indicator)) {
                    return true;
                }

            }
        }
        return false;
    }

    @Override
    public ReturnCode canSave() {
        // MOD by gdbu 2011-3-21 bug 19179
        ReturnCode canModRetCode = super.canSave();
        if (!canModRetCode.isOk()) {
            return canModRetCode;
        }
        // ~19179

        // MOD klliu 2011-04-18 code cleaning.
        ModelElementIndicator[] modelElementIndicators = treeViewer.getModelElementIndicator();
        if (modelElementIndicators != null && modelElementIndicators.length != 0) {
            analysisItem.getAnalysis().getContext()
                    .setConnection(ModelElementIndicatorHelper.getTdDataProvider(modelElementIndicators[0]));
        }
        // ~
        List<ModelElement> analyzedElement = new ArrayList<ModelElement>();

        for (ModelElementIndicator modelElementIndicator : treeViewer.getModelElementIndicator()) {
            IRepositoryViewObject modelElementRepositoryObj = modelElementIndicator.getModelElementRepositoryNode().getObject();
            if (modelElementRepositoryObj instanceof MetadataColumnRepositoryObject) {
                analyzedElement.add(((MetadataColumnRepositoryObject) modelElementRepositoryObj).getTdColumn());
            }
        }

        if (!analyzedElement.isEmpty()) {
            if (!ModelElementHelper.isFromSameTable(analyzedElement) && !"".equals(dataFilterComp.getDataFilterString())) { //$NON-NLS-1$
                return new ReturnCode(DefaultMessagesImpl.getString("ColumnMasterDetailsPage.CannotCreatAnalysis"), false); //$NON-NLS-1$
            }
        }

        return new ReturnCode(true);
    }

    /**
     * DOC xqliu Comment method "checkMdmExecutionEngine".
     */
    private ReturnCode checkMdmExecutionEngine() {
        ModelElementIndicator[] modelElementIndicators = treeViewer.getModelElementIndicator();
        if (modelElementIndicators != null && modelElementIndicators.length != 0) {
            analysisItem.getAnalysis().getContext()
                    .setConnection(ModelElementIndicatorHelper.getTdDataProvider(modelElementIndicators[0]));
        }
        return new ReturnCode(true);
    }

    /**
     * DOC zshen Comment method "getExecCombo".
     * 
     * @return the Combo for executeLanguage
     */
    public CCombo getExecCombo() {
        return execCombo;
    }

    /**
     * DOC zshen Comment method "includeDatePatternFreqIndicator".
     * 
     * @return whether have a datePatternFreqIndicator in the "analyzed Columns"
     */
    @Override
    protected boolean includeDatePatternFreqIndicator() {
        for (ModelElementIndicator modelElementIndicator : this.treeViewer.getModelElementIndicator()) {
            if (modelElementIndicator.contains(IndicatorEnum.DatePatternFreqIndicatorEnum)) {
                return true;
            }
        }
        return false;
    }

    /**
     * DOC xqliu Comment method "changeExecuteLanguageToSql".
     * 
     * @param enabled
     */
    public void changeExecuteLanguageToSql(boolean enabled) {
        if (this.execCombo == null) {
            return;
        }

        if (!(ExecutionLanguage.SQL.getLiteral().equals(this.execLang))) {
            int i = 0;
            for (ExecutionLanguage language : ExecutionLanguage.VALUES) {
                if (language.compareTo(ExecutionLanguage.SQL) == 0) {
                    this.execCombo.select(i);
                } else {
                    i++;
                }
            }
        }

        execCombo.setEnabled(enabled);
    }

    /**
     * DOC qiongli Comment method "getDataFilterComp".
     * 
     * @return
     */
    public DataFilterComp getDataFilterComp() {
        return this.dataFilterComp;
    }

    /**
     * DOC qiongli Comment method "setWhereClauseDisabled".
     */
    public void setWhereClauseDisabled() {
        if (dataFilterComp != null) {
            dataFilterComp.getDataFilterText().setEnabled(false);
        }
    }

    /**
     * DOC xqliu Comment method "setWhereClauseEnable".
     */
    public void setWhereClauseEnable() {
        if (dataFilterComp != null) {
            dataFilterComp.getDataFilterText().setEnabled(true);
        }
    }

    @Override
    public ExecutionLanguage getUIExecuteEngin() {
        return ExecutionLanguage.get(execCombo.getText());
    }

    /**
     * set the Language To TreeViewer.
     * 
     * @param executionLanguage
     */
    @Override
    protected void setLanguageToTreeViewer(ExecutionLanguage executionLanguage) {
        treeViewer.setLanguage(executionLanguage);

    }

    /**
     * 
     * refresh current Tree Viewer
     * 
     * @param result
     */
    public void refreshCurrentTreeViewer(ModelElementIndicator[] result) {
        if (result.length > 0) {
            int lastTimePageNumber = uiPagination.getCurrentPage();
            refreshTheTree(result);
            // the number of current page is from 0 to n so need to add one when we use it.
            uiPagination.goToPage(lastTimePageNumber + 1);
            treeViewer.setElements(treeViewer.getModelElementIndicator(), true, true);
        }

    }

    @Override
    List<ExpandableComposite> getPreviewChartList() {
        return this.previewChartList;
    }

    @Override
    String getExpandString() {
        return DefaultMessagesImpl.getString("ExpandAllColumns"); //$NON-NLS-1$
    }

    @Override
    String getCollapseAllString() {
        return DefaultMessagesImpl.getString("CollapseAllColumns"); //$NON-NLS-1$
    }

    /**
     * return all created charts for the current running, from the current pagination. The charts which are not on the
     * current page no need to return. TODO check if can use IndicatorUnit
     */
    @Override
    public List<DynamicIndicatorModel> getDynamicDatasets() {
        return uiPagination.getAllIndcatorAndDatasetOfCurrentPage();
    }

    /**
     * clear all related maps which store the dynamic state and charts.
     */
    @Override
    public void clearDynamicDatasets() {
        uiPagination.clearAllDynamicMapOfCurrentPage();
        super.clearDynamicDatasets();
    }

}
