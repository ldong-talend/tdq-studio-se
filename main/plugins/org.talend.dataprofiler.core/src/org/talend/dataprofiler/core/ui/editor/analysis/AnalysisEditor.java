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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.FileEditorInput;
import org.talend.commons.emf.FactoriesUtil;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.repository.model.IRepositoryFactory;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.repository.model.RepositoryFactoryProvider;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.IRuningStatusListener;
import org.talend.dataprofiler.core.ui.action.actions.DefaultSaveAction;
import org.talend.dataprofiler.core.ui.action.actions.RefreshChartAction;
import org.talend.dataprofiler.core.ui.action.actions.RunAnalysisAction;
import org.talend.dataprofiler.core.ui.editor.CommonFormEditor;
import org.talend.dataprofiler.core.ui.editor.TdEditorToolBar;
import org.talend.dataprofiler.core.ui.events.EventEnum;
import org.talend.dataprofiler.core.ui.events.EventManager;
import org.talend.dataprofiler.core.ui.events.EventReceiver;
import org.talend.dataprofiler.core.ui.utils.WorkbenchUtils;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dataquality.analysis.AnalysisType;
import org.talend.dataquality.analysis.ExecutionLanguage;
import org.talend.dataquality.properties.TDQAnalysisItem;
import org.talend.dq.helper.resourcehelper.AnaResourceFileHelper;
import org.talend.repository.model.RepositoryConstants;
import org.talend.utils.sugars.ReturnCode;

/**
 * @author rli
 * 
 */
public class AnalysisEditor extends CommonFormEditor {

    private static Logger log = Logger.getLogger(AnalysisEditor.class);

    public static final String RESULT_PAGE = "SecondPage";//$NON-NLS-1$

    public static final String MASTER_PAGE = "MasterPage";//$NON-NLS-1$

    private static final String ANALYSIS_RESULTS = DefaultMessagesImpl.getString("AnalysisEditor.analysisResult"); //$NON-NLS-1$

    private static final String ANALYSIS_SETTINGS = DefaultMessagesImpl.getString("AnalysisEditor.analysisSettings"); //$NON-NLS-1$

    private static final int RESULT_PAGE_INDEX = 1;

    private AbstractAnalysisMetadataPage masterPage;

    private AbstractAnalysisResultPage resultPage;

    private AnalysisType analysisType;

    private RunAnalysisAction runAction;

    private RefreshChartAction refreshAction;

    // MOD xqliu 2009-07-02 bug 7687
    private DefaultSaveAction saveAction;

    // ~

    // Added 20130725 TDQ-7639 yyin
    private EventReceiver refreshReceiver = null;

    private EventReceiver checkBeforeRunReceiver = null;

    // Added 20140411 TDQ-8360 yyin
    private EventReceiver refreshDataProvider = null;

    private EventReceiver refresh2ShowMatchIndicator = null;

    private EventReceiver reopenEditor = null;

    // Added TDQ-8787 2014-06-16 yyin
    private EventReceiver registerDynamicEvent = null;

    private EventReceiver unRegisterDynamicEvent = null;

    private boolean isRefreshResultPage = false;

    /**
     * 
     */
    public AnalysisEditor() {

    }

    @Override
    protected void addPages() {

        TdEditorToolBar toolbar = getToolBar();
        if (toolbar != null) {
            saveAction = new DefaultSaveAction(this);
            runAction = new RunAnalysisAction();
            // do not use the refresh on match analysis
            if (analysisType.equals(AnalysisType.MATCH_ANALYSIS)) {
                toolbar.addActions(saveAction, runAction);
            } else {
                refreshAction = new RefreshChartAction();
                toolbar.addActions(saveAction, runAction, refreshAction);
            }
        }

        switch (analysisType) {

        case COLUMN_CORRELATION:
            masterPage = new ColumnCorrelationNominalAndIntervalMasterPage(this, MASTER_PAGE, ANALYSIS_SETTINGS);
            resultPage = new ColumnCorrelationNominalIntervalResultPage(this, RESULT_PAGE, ANALYSIS_RESULTS);
            break;
        case MULTIPLE_COLUMN:
            masterPage = new ColumnMasterDetailsPage(this, MASTER_PAGE, ANALYSIS_SETTINGS);
            resultPage = new ColumnAnalysisResultPage(this, RESULT_PAGE, ANALYSIS_RESULTS);
            break;
        case CONNECTION:
            masterPage = new ConnectionMasterDetailsPage(this, MASTER_PAGE, ANALYSIS_SETTINGS);
            break;
        case CATALOG:
            masterPage = new CatalogMasterDetailsPage(this, MASTER_PAGE, ANALYSIS_SETTINGS);
            break;
        case SCHEMA:
            masterPage = new SchemaAnalysisMasterDetailsPage(this, MASTER_PAGE, ANALYSIS_SETTINGS);
            break;
        case COLUMNS_COMPARISON:
            masterPage = new ColumnsComparisonMasterDetailsPage(this, MASTER_PAGE, ANALYSIS_SETTINGS);
            resultPage = new ColumnsComparisonAnalysisResultPage(this, RESULT_PAGE, ANALYSIS_RESULTS);
            break;
        case TABLE:
            masterPage = new TableMasterDetailsPage(this, MASTER_PAGE, ANALYSIS_SETTINGS);
            resultPage = new TableAnalysisResultPage(this, RESULT_PAGE, ANALYSIS_RESULTS);
            break;
        case TABLE_FUNCTIONAL_DEPENDENCY:
            masterPage = new ColumnDependencyMasterDetailsPage(this, MASTER_PAGE, ANALYSIS_SETTINGS);
            resultPage = new ColumnDependencyResultPage(this, RESULT_PAGE, ANALYSIS_RESULTS);
            break;
        case COLUMN_SET:
            masterPage = new ColumnSetMasterPage(this, MASTER_PAGE, ANALYSIS_SETTINGS);
            resultPage = new ColumnSetResultPage(this, RESULT_PAGE, ANALYSIS_RESULTS);
            break;
        case MATCH_ANALYSIS:// Added 20130724 TDQ-7504
            masterPage = new MatchMasterDetailsPage(this, MASTER_PAGE, ANALYSIS_SETTINGS);
            resultPage = new MatchAnalysisResultPage(this, RESULT_PAGE, ANALYSIS_RESULTS);
            break;
        default:

        }
        try {
            if (masterPage != null) {
                addPage(masterPage);
                setPartName(masterPage.getIntactElemenetName());
                // Added 20130930 TDQ-8117, yyin
                // init the run analysis action, to give it the analysis item and listener
                TDQAnalysisItem item = (TDQAnalysisItem) masterPage.getAnalysisRepNode().getObject().getProperty().getItem();
                this.runAction.setAnalysisItem(item);
            }

            if (resultPage != null) {
                addPage(resultPage);
            } else {
                setRefreshActionButtonState(false);
            }
        } catch (PartInitException e) {
            ExceptionHandler.process(e, Level.ERROR);
        }
        // Added 20130725 TDQ-7639 yyin : register the run analysis event, which need to refresh the pages
        registerUpdateExecutionEvent();

    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        if (masterPage != null && masterPage.isDirty()) {
            masterPage.doSave(monitor);
            setPartName(masterPage.getIntactElemenetName());
        }
        setEditorObject(masterPage.getAnalysisRepNode());
        super.doSave(monitor);

    }

    @Override
    protected void firePropertyChange(final int propertyId) {
        if (masterPage.isActive()) {
            // setRunActionButtonState(!isDirty() && masterPage.canRun().isOk());
            setRunActionButtonState(true);
        }
        // MOD klliu 2011-04-08 if masterPage is dirty,then button of SaveAction can been used.
        setSaveActionButtonState(masterPage.isDirty());
        super.firePropertyChange(propertyId);
    }

    @Override
    protected void translateInput(IEditorInput input) {
        // MOD klliu 2010-12-10
        Analysis findAnalysis = null;
        String label = "";//$NON-NLS-1$
        if (input instanceof AnalysisItemEditorInput) {
            AnalysisItemEditorInput fileEditorInput = (AnalysisItemEditorInput) input;
            TDQAnalysisItem tdqAnalysisItem = fileEditorInput.getTDQAnalysisItem();
            findAnalysis = tdqAnalysisItem.getAnalysis();
            label = tdqAnalysisItem.getProperty().getLabel();
        } else if (input instanceof FileEditorInput) {
            FileEditorInput fileEditorInput = (FileEditorInput) input;
            IFile file = fileEditorInput.getFile();
            label = file.getFullPath().toString();
            if (FactoriesUtil.isAnalysisFile(file.getFileExtension())) {
                findAnalysis = AnaResourceFileHelper.getInstance().findAnalysis(file);
            }
        }
        if (findAnalysis != null) {
            analysisType = findAnalysis.getParameters().getAnalysisType();
        } else {
            log.error("Could not find an analysis in file: " + label);//$NON-NLS-1$
        }
    }

    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
        if (newPageIndex == RESULT_PAGE_INDEX) {
            if (masterPage.isDirty()) {
                masterPage.doSave(null);
            }
            setSaveActionButtonState(false);
        }

        if (isRefreshResultPage) {
            resultPage.refresh(masterPage);
            isRefreshResultPage = false;
        } else {
            EventManager.getInstance().publish(masterPage.analysisItem.getAnalysis(),
                    EventEnum.DQ_DYNAMIC_SWITCH_MASTER_RESULT_PAGE, null);
        }
    }

    /**
     * Getter for masterPage.
     * 
     * @return the masterPage
     */
    public AbstractAnalysisMetadataPage getMasterPage() {
        return this.masterPage;
    }

    /**
     * Getter for resultPage.
     * 
     * @return the resultPage
     */
    public AbstractAnalysisResultPage getResultPage() {
        return resultPage;
    }

    public void performGlobalAction(String id) {
        if (id.equals(RunAnalysisAction.ID)) {
            runAction.run();
            return;
        }
        if (analysisType == AnalysisType.MULTIPLE_COLUMN) {
            ((ColumnMasterDetailsPage) masterPage).performGlobalAction(id);
        }
        if (analysisType == AnalysisType.TABLE) {
            ((TableMasterDetailsPage) masterPage).performGlobalAction(id);
        }
    }

    public void setRefreshResultPage(boolean isRefreshRP) {
        this.isRefreshResultPage = isRefreshRP;
    }

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    /**
     * DOC bZhou Comment method "setRunActionButtonState".
     * 
     * @param state
     */
    public void setRunActionButtonState(boolean state) {
        if (runAction != null) {
            runAction.setEnabled(state);
        }
    }

    /**
     * DOC bZhou Comment method "setSaveActionButtonState".
     * 
     * @param state
     */
    public void setSaveActionButtonState(boolean state) {
        if (saveAction != null) {
            saveAction.setEnabled(state);
        }
    }

    /**
     * DOC bZhou Comment method "setRefreshActionButtonState".
     * 
     * @param state
     */
    public void setRefreshActionButtonState(boolean state) {
        if (refreshAction != null) {
            refreshAction.setEnabled(state);
        }
    }

    /**
     * 
     * DOC mzhao Comment method "getRunAnalysisAction".
     * 
     * @return
     */
    public Action getRunAnalysisAction() {
        return runAction;
    }

    /**
     * DOC yyi Comment method "canRun".
     * 
     * @return
     */
    public ReturnCode canRun() {
        return masterPage.canRun();
    }

    @Override
    public void setFocus() {
        super.setFocus();
        // don't invoke this method here, invoke it in IPartListener.partBroughtToTop()
        // WorkbenchUtils.autoChange2DataProfilerPerspective();
    }

    public ExecutionLanguage getUIExecuteEngin() {
        return this.getMasterPage().getUIExecuteEngin();
    }

    IFormPage activePageInstance;

    private void changeListener() {
        activePageInstance = getActivePageInstance();
        if (activePageInstance instanceof IRuningStatusListener) {
            runAction.setListener((IRuningStatusListener) activePageInstance);
        }

    }

    /**
     * currently will not open the editor of the analysis when running from menu, so, if the editor is opened and not
     * the current active one, the page will not know that the result is changed. so we need to add the event/listener
     * to them to handle this. Added 20130725 TDQ-7639
     * 
     * @param analysis
     */
    private void registerUpdateExecutionEvent() {
        // register: check if the analysis need to be saved or if it can run before running it(from menu's RUN)
        checkBeforeRunReceiver = new EventReceiver() {

            @Override
            public boolean handle(Object data) {
                if (isDirty()) {
                    ReturnCode canSave = masterPage.canSave();
                    if (canSave.isOk()) {
                        // save the analysis before running

                        // MOD msjian TDQ-8225 : This save action won't invoke any remote repository action such as svn
                        // commit. TDQ-7508
                        IRepositoryFactory localRepository = RepositoryFactoryProvider
                                .getRepositoriyById(RepositoryConstants.REPOSITORY_LOCAL_ID);
                        IRepositoryFactory oldRepository = ProxyRepositoryFactory.getInstance()
                                .getRepositoryFactoryFromProvider();
                        ProxyRepositoryFactory.getInstance().setRepositoryFactoryFromProvider(localRepository);
                        try {
                            doSave(null);
                        } catch (Exception e) {
                            log.error(e, e);
                        } finally {
                            ProxyRepositoryFactory.getInstance().setRepositoryFactoryFromProvider(oldRepository);
                        }
                        // TDQ-8225~

                        setDirty(false);
                    } else {
                        if (canSave.getMessage() != null && !canSave.getMessage().equals(StringUtils.EMPTY)) {
                            MessageDialogWithToggle
                                    .openError(
                                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                                            DefaultMessagesImpl.getString("AbstractAnalysisMetadataPage.SaveAnalysis"), canSave.getMessage()); //$NON-NLS-1$
                        }
                        return false;
                    }
                }
                ReturnCode canRun = canRun();
                if (!canRun.isOk()) {
                    MessageDialogWithToggle.openError(null,
                            DefaultMessagesImpl.getString("RunAnalysisAction.runAnalysis"), canRun//$NON-NLS-1$
                                    .getMessage());
                    return false;
                }
                // TDQ-8220 change the listener every time( master page or result page)
                changeListener();
                return true;
            }
        };
        EventManager.getInstance().register(masterPage.getAnalysis(), EventEnum.DQ_ANALYSIS_CHECK_BEFORERUN,
                checkBeforeRunReceiver);

        // register: refresh the result page after running it from menu
        refreshReceiver = new EventReceiver() {

            @Override
            public boolean handle(Object data) {
                // MOD TDQ-7816: when the result page are not created, no need to refresh, only refresh master page is
                // enough;TDQ-8270 resultpage is null for overview type
                if (resultPage != null && resultPage.getManagedForm() != null) {
                    resultPage.refresh(masterPage);
                    return true;
                } else {
                    masterPage.refresh();
                    return true;
                }
            }
        };
        EventManager.getInstance().register(masterPage.getAnalysis(), EventEnum.DQ_ANALYSIS_RUN_FROM_MENU, refreshReceiver);

        // register: refresh the dataprovider combobox when the name of the data provider is changed.
        refreshDataProvider = new EventReceiver() {

            @Override
            public boolean handle(Object data) {
                masterPage.reloadDataproviderAndFillConnCombo();
                return true;
            }
        };
        EventManager.getInstance().register(masterPage.getAnalysis(), EventEnum.DQ_ANALYSIS_REFRESH_DATAPROVIDER_LIST,
                refreshDataProvider);

        // register: reopen this editor after reload its depended connection
        reopenEditor = new EventReceiver() {

            @Override
            public boolean handle(Object data) {
                Display.getDefault().asyncExec(new Runnable() {

                    public void run() {

                        WorkbenchUtils.refreshCurrentAnalysisEditor(masterPage.getAnalysis().getName());
                    }

                });

                return true;
            }
        };
        EventManager.getInstance()
                .register(masterPage.getAnalysis().getName(), EventEnum.DQ_ANALYSIS_REOPEN_EDITOR, reopenEditor);

        // ADD msjian TDQ-8860 2014-4-30:only for column set analysis, when there have pattern(s) when java engine,show
        // all match indicator in the
        // Indicators section.
        if (analysisType.equals(AnalysisType.COLUMN_SET)) {
            // register: refresh the dataprovider combobox when the name of the data provider is changed.
            refresh2ShowMatchIndicator = new EventReceiver() {

                @Override
                public boolean handle(Object data) {
                    ((ColumnSetMasterPage) masterPage).refreshIndicatorsSection();
                    return true;
                }
            };
            EventManager.getInstance().register(masterPage.getAnalysis(), EventEnum.DQ_COLUMNSET_SHOW_MATCH_INDICATORS,
                    refresh2ShowMatchIndicator);
        }
        // TDQ-8860~

        // Added TDQ8787 2014-06-16 yyin: for dynamic chart, create all charts before execute the analysis
        if (masterPage instanceof DynamicAnalysisMasterPage) {
            registerDynamicEvent = new EventReceiver() {

                @Override
                public boolean handle(Object data) {
                    if (masterPage.equals(activePageInstance)) {
                        ((DynamicAnalysisMasterPage) masterPage).registerDynamicEvent();
                    } else {
                        // register result page
                        if (resultPage != null) {
                            if (resultPage instanceof ColumnAnalysisResultPage) {
                                ((ColumnAnalysisResultPage) resultPage).registerDynamicEvent();
                            } else if (resultPage instanceof TableAnalysisResultPage) {
                                ((TableAnalysisResultPage) resultPage).registerDynamicEvent();
                            }
                        }
                    }
                    return true;
                }
            };
            EventManager.getInstance().register(masterPage.getAnalysis(), EventEnum.DQ_DYNAMIC_REGISTER_DYNAMIC_CHART,
                    registerDynamicEvent);

            unRegisterDynamicEvent = new EventReceiver() {

                @Override
                public boolean handle(Object data) {
                    if (masterPage.equals(activePageInstance)) {
                        ((DynamicAnalysisMasterPage) masterPage).unRegisterDynamicEvent();
                    } else {
                        // register result page
                        if (resultPage != null) {
                            if (resultPage instanceof ColumnAnalysisResultPage) {
                                ((ColumnAnalysisResultPage) resultPage).unRegisterDynamicEvent();
                            } else if (resultPage instanceof TableAnalysisResultPage) {
                                ((TableAnalysisResultPage) resultPage).unRegisterDynamicEvent();
                            }
                        }
                    }
                    return true;
                }
            };
            EventManager.getInstance().register(masterPage.getAnalysis(), EventEnum.DQ_DYNAMIC_UNREGISTER_DYNAMIC_CHART,
                    unRegisterDynamicEvent);

        }
    }

    /**
     * unregister the update execution event Added 20130725 TDQ-7639
     */
    @Override
    public void dispose() {
        EventManager.getInstance().unRegister(masterPage.getAnalysis(), EventEnum.DQ_ANALYSIS_CHECK_BEFORERUN,
                checkBeforeRunReceiver);
        EventManager.getInstance().unRegister(masterPage.getAnalysis(), EventEnum.DQ_ANALYSIS_RUN_FROM_MENU, refreshReceiver);
        EventManager.getInstance().unRegister(masterPage.getAnalysis(), EventEnum.DQ_ANALYSIS_REFRESH_DATAPROVIDER_LIST,
                refreshDataProvider);
        EventManager.getInstance().unRegister(masterPage.getAnalysis().getName(), EventEnum.DQ_ANALYSIS_REOPEN_EDITOR,
                reopenEditor);

        // ADD msjian TDQ-8860 2014-4-30:only for column set analysis, when there have pattern(s) when java engine,show
        // all match indicator in the Indicators section.
        if (analysisType.equals(AnalysisType.COLUMN_SET)) {
            EventManager.getInstance().unRegister(masterPage.getAnalysis(), EventEnum.DQ_COLUMNSET_SHOW_MATCH_INDICATORS,
                    refresh2ShowMatchIndicator);
        }// TDQ-8860~

        // Added TDQ8787 2014-06-16 yyin: for dynamic chart, unregister the create all chart event
        if (masterPage instanceof DynamicAnalysisMasterPage) {
            EventManager.getInstance().unRegister(masterPage.getAnalysis(), EventEnum.DQ_DYNAMIC_REGISTER_DYNAMIC_CHART,
                    registerDynamicEvent);
            EventManager.getInstance().unRegister(masterPage.getAnalysis(), EventEnum.DQ_DYNAMIC_REGISTER_DYNAMIC_CHART,
                    unRegisterDynamicEvent);
        }// ~

        super.dispose();
    }

}
