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
package org.talend.dataprofiler.core.ui.action.actions;

import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.connection.DelimitedFileConnection;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.cwm.compare.exception.ReloadCompareException;
import org.talend.cwm.compare.factory.ComparisonLevelFactory;
import org.talend.cwm.db.connection.ConnectionUtils;
import org.talend.cwm.helper.TaggedValueHelper;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.exception.ExceptionFactory;
import org.talend.dataprofiler.core.exception.ExceptionHandler;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.IRuningStatusListener;
import org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisMetadataPage;
import org.talend.dataprofiler.core.ui.editor.analysis.AnalysisEditor;
import org.talend.dataprofiler.core.ui.editor.analysis.AnalysisItemEditorInput;
import org.talend.dataprofiler.core.ui.editor.analysis.ColumnAnalysisResultPage;
import org.talend.dataprofiler.core.ui.editor.analysis.DynamicAnalysisMasterPage;
import org.talend.dataprofiler.core.ui.editor.analysis.TableAnalysisResultPage;
import org.talend.dataprofiler.core.ui.events.EventEnum;
import org.talend.dataprofiler.core.ui.events.EventManager;
import org.talend.dataquality.analysis.AnalysisType;
import org.talend.dataquality.analysis.ExecutionLanguage;
import org.talend.dataquality.helpers.AnalysisHelper;
import org.talend.dataquality.properties.TDQAnalysisItem;
import org.talend.dq.analysis.AnalysisExecutorSelector;
import org.talend.dq.analysis.connpool.TdqAnalysisConnectionPool;
import org.talend.dq.helper.ProxyRepositoryManager;
import org.talend.dq.helper.RepositoryNodeHelper;
import org.talend.dq.nodes.AnalysisRepNode;
import org.talend.repository.model.RepositoryNode;
import org.talend.utils.sugars.ReturnCode;
import orgomg.cwm.foundation.softwaredeployment.DataManager;
import orgomg.cwm.objectmodel.core.TaggedValue;

/**
 * Run Analysis Action.
 * 
 */
public class RunAnalysisAction extends Action implements ICheatSheetAction {

    private static Logger log = Logger.getLogger(RunAnalysisAction.class);

    public static final String ID = "org.talend.common.runTalendElement"; //$NON-NLS-1$

    private static final DecimalFormat FORMAT_SECONDS = new DecimalFormat("0.00"); //$NON-NLS-1$

    private IRuningStatusListener listener;

    @Deprecated
    private IFile selectionFile;// no one used

    private TDQAnalysisItem item;

    private boolean isNeedUnlock = false;

    @SuppressWarnings("unused")
    @Deprecated
    private AnalysisRepNode node;// no need to used

    @Deprecated
    public IFile getSelectionFile() {
        return selectionFile;
    }

    @Deprecated
    public void setSelectionFile(IFile selectionFile) {
        this.selectionFile = selectionFile;
    }

    @Deprecated
    public void setSelectionNode(AnalysisRepNode node) {
        this.node = node;
    }

    /**
     * DOC yyin Comment method "setAnalysisItem".
     * 
     * @param item2
     */
    public void setAnalysisItem(TDQAnalysisItem item) {
        this.item = item;
    }

    public void setListener(IRuningStatusListener listener) {
        this.listener = listener;
    }

    /**
     * constructor.
     */
    public RunAnalysisAction() {
        super(DefaultMessagesImpl.getString("RunAnalysisAction.Run")); //$NON-NLS-1$
        setImageDescriptor(ImageLib.getImageDescriptor(ImageLib.REFRESH_IMAGE));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        try {
            if (item == null) {
                log.error("Analysis item is null"); //$NON-NLS-1$
                return;
            }
            validateAnalysis();
            if (ifLockByOthers()) {
                return;
            }

            // check if the analysis need to be saved or can run before real running by the event
            if (!EventManager.getInstance().publish(item.getAnalysis(), EventEnum.DQ_ANALYSIS_CHECK_BEFORERUN, null)) {
                // if the analysis need save but can not be saved, return without continue;
                // or the analysis can not run, return without continue
                return;
            }

            if (item == null || !isConnectedAvailable()) {
                return;
            }

            if (log.isInfoEnabled()) {
                addTaggedVaLueIntoConnection();
            }

            AnalysisType analysisType = item.getAnalysis().getParameters().getAnalysisType();
            if (AnalysisType.COLUMNS_COMPARISON.equals(analysisType)) {
                // If the analysis type is column comparison, ask user to continue to run or not.
                if (!isContinueRun()) {
                    return;
                }
            } else if (AnalysisType.CONNECTION.equals(analysisType)) {
                // If the analysis type is overview analysis, reload the database.
                // TODO check here the needed of reloading database
                reloadConnection();
            }

            // lock the analysis before running it if it is not locked yet.(whenever the editor is not opened)
            // when the run comes from the button in editor, the listener is not null;
            // when the run comes from context menu, the listener is null
            if (this.listener == null && !ProxyRepositoryManager.getInstance().isLocked(item)) {
                if (!ProxyRepositoryFactory.getInstance().isEditableAndLockIfPossible(item)) {
                    // if the analysis is not editable , return without running.
                    isNeedUnlock = false;
                    return;
                } else {// it is locked here, means that it need the unlock too.
                    isNeedUnlock = true;
                }
            } else {
                isNeedUnlock = false;
            }

            final WorkspaceJob job = new WorkspaceJob("Run Analysis") { //$NON-NLS-1$

                @Override
                public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

                    monitor.beginTask(
                            DefaultMessagesImpl.getString("RunAnalysisAction.running", item.getAnalysis().getName()), IProgressMonitor.UNKNOWN); //$NON-NLS-1$ 

                    Display.getDefault().syncExec(new Runnable() {

                        public void run() {
                            if (listener != null) {
                                listener.fireRuningItemChanged(false);
                            }
                            // register dynamic event for who supported dynamic chart
                            if (isSupportDynamicChart()) {
                                EventManager.getInstance().publish(item.getAnalysis(),
                                        EventEnum.DQ_DYNAMIC_REGISTER_DYNAMIC_CHART, null);
                            }
                        }

                    });

                    ReturnCode executed = null;
                    try {
                        executed = AnalysisExecutorSelector.executeAnalysis(item, monitor);

                        if (monitor.isCanceled()) {
                            TdqAnalysisConnectionPool.closeConnectionPool(item.getAnalysis());
                            executed = new ReturnCode(DefaultMessagesImpl.getString("RunAnalysisAction.TaskCancel"), false); //$NON-NLS-1$
                            monitor.done();
                            if (isNeedUnlock) {
                                unlockAnalysis();
                            }
                            return Status.CANCEL_STATUS;
                        }

                        monitor.done();
                        if (isNeedUnlock) {
                            unlockAnalysis();
                        }
                    } finally {// if any exception, still need to unregister dynamic events.
                        Display.getDefault().syncExec(new Runnable() {

                            public void run() {
                                // Added TDQ-8787 20140616 yyin: unregister all dynamic chart events after executing
                                // the analysis
                                if (isSupportDynamicChart()) {
                                    EventManager.getInstance().publish(item.getAnalysis(),
                                            EventEnum.DQ_DYNAMIC_UNREGISTER_DYNAMIC_CHART, null);
                                }
                                if (listener != null) {
                                    listener.fireRuningItemChanged(true);
                                } else {
                                    // TODO yyin publish the event from listener.
                                    EventManager.getInstance().publish(item.getAnalysis(), EventEnum.DQ_ANALYSIS_RUN_FROM_MENU,
                                            null);
                                }

                            }

                        });
                    }
                    displayResultStatus(executed);

                    return Status.OK_STATUS;
                }

            };

            job.setUser(true);
            job.schedule();
        } catch (BusinessException e) {
            ExceptionHandler.process(e, Level.FATAL);
        }
    }

    // only sql mode, and column, table, dependency analysis support dynamic chart now
    private boolean isSupportDynamicChart() {
        ExecutionLanguage executionEngine = AnalysisHelper.getExecutionEngine(this.item.getAnalysis());
        if (ExecutionLanguage.SQL.equals(executionEngine)) {
            if (listener == null) {// when run from context menu.
                if (AnalysisType.MULTIPLE_COLUMN.equals(item.getAnalysis().getParameters().getAnalysisType())
                        || AnalysisType.BUSINESS_RULE.equals(item.getAnalysis().getParameters().getAnalysisType())) {
                    return true;
                }
                return false;
            } else {// run from the run button in the editor
                return listener instanceof DynamicAnalysisMasterPage || listener instanceof ColumnAnalysisResultPage
                        || listener instanceof TableAnalysisResultPage;
            }
        } else {
            return false;
        }
    }

    private void addTaggedVaLueIntoConnection() {
        DataManager datamanager = item.getAnalysis().getContext().getConnection();
        if (datamanager instanceof DatabaseConnection) {
            TaggedValue productName = TaggedValueHelper.getTaggedValue(TaggedValueHelper.DB_PRODUCT_NAME,
                    datamanager.getTaggedValue());
            TaggedValue productVersion = TaggedValueHelper.getTaggedValue(TaggedValueHelper.DB_PRODUCT_VERSION,
                    datamanager.getTaggedValue());
            log.info("DB Product Name: " + productName.getValue()); //$NON-NLS-1$
            log.info("DB Product Version: " + productVersion.getValue()); //$NON-NLS-1$
        } else if (datamanager instanceof DelimitedFileConnection) {
            log.info("File Connection path: " + ((DelimitedFileConnection) datamanager).getFilePath()); //$NON-NLS-1$
        }
    }

    /**
     * unlock analysis.
     */
    private void unlockAnalysis() {
        try {
            ProxyRepositoryFactory.getInstance().unlock(this.item);
        } catch (PersistenceException e) {
            log.error(e, e);
        } catch (LoginException e) {
            log.error(e, e);
        }
    }

    /**
     * popup dialog isContinueRun.
     * 
     * @return
     */
    private Boolean isContinueRun() {
        boolean isContinueRun = Boolean.TRUE;
        if (!MessageDialogWithToggle.openConfirm(null, DefaultMessagesImpl.getString("RunAnalysisAction.confirmTitle"), //$NON-NLS-1$
                DefaultMessagesImpl.getString("RunAnalysisAction.confirmMSG"))) { //$NON-NLS-1$
            isContinueRun = Boolean.FALSE;
        }
        return isContinueRun;
    }

    /**
     * reload analysis connection.
     */
    private void reloadConnection() {
        if (AnalysisHelper.getReloadDatabases(item.getAnalysis())) {
            Connection conntion = (Connection) item.getAnalysis().getContext().getConnection();
            if (conntion != null) {
                try {
                    RepositoryNode connectionNode = RepositoryNodeHelper.recursiveFind(conntion);
                    ComparisonLevelFactory.creatComparisonLevel(connectionNode).reloadCurrentLevelElement();
                } catch (ReloadCompareException e) {
                    log.error(e, e);
                }
            }
        }
    }

    /**
     * check whether the connection of analysis is available.
     * 
     * @return true when the connection is well connected
     */
    private boolean isConnectedAvailable() {
        DataManager datamanager = item.getAnalysis().getContext().getConnection();
        return ConnectionUtils.checkConnection(datamanager);
    }

    /**
     * check whether the item is locked by Others.
     * 
     * @return
     */
    private boolean ifLockByOthers() {
        // MOD sizhaoliu TDQ-5452 verify the lock status before running an analysis
        if (ProxyRepositoryManager.getInstance().isLockByOthers(item)) {
            RepositoryNode node1 = RepositoryNodeHelper.recursiveFind(this.item.getProperty());
            if (node1 != null) {
                CorePlugin.getDefault().refreshDQView(node1.getParent());
            }
            MessageDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
                    DefaultMessagesImpl.getString("RunAnalysisAction.runAnalysis"), //$NON-NLS-1$
                    DefaultMessagesImpl.getString("RunAnalysisAction.error.lockByOthers")); //$NON-NLS-1$
            return true;
        } // ~ TDQ-5452
        return false;
    }

    /**
     * validate analysis.
     * 
     * @throws BusinessException
     */
    private void validateAnalysis() throws BusinessException {
        if (item != null) {
            if (item.getAnalysis() == null || item.getAnalysis().getParameters() == null) {
                BusinessException createBusinessException = ExceptionFactory.getInstance().createBusinessException(
                        item.getFilename());
                throw createBusinessException;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.cheatsheets.ICheatSheetAction#run(java.lang.String[],
     * org.eclipse.ui.cheatsheets.ICheatSheetManager)
     */
    public void run(String[] params, ICheatSheetManager manager) {
        // ADD mzhao 2009-02-03 If there is no active editor opened, run
        // analysis action from cheat sheets will do nothing.
        IEditorPart activateEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (activateEditor == null || !(activateEditor instanceof AnalysisEditor)) {
            return;
        }
        // MOD TDQ-8117 20131008 yyin: find the analysis item from the editor
        IEditorInput editorInput = activateEditor.getEditorInput();
        if (editorInput instanceof AnalysisItemEditorInput) {
            this.item = ((TDQAnalysisItem) ((AnalysisItemEditorInput) editorInput).getItem());
        }
        AbstractAnalysisMetadataPage masterPage = ((AnalysisEditor) activateEditor).getMasterPage();
        listener = masterPage;
        // ~
        run();
    }

    /**
     * display Result Status.
     * 
     * @param executed
     */
    private void displayResultStatus(final ReturnCode executed) {
        if (log.isInfoEnabled()) {
            int executionDuration = item.getAnalysis().getResults().getResultMetadata().getExecutionDuration();
            log.info(DefaultMessagesImpl
                    .getString(
                            "RunAnalysisAction.displayInformation", new Object[] { item.getAnalysis().getName(), executed, FORMAT_SECONDS.format(Double.valueOf(executionDuration) / 1000) })); //$NON-NLS-1$ 

        }

        if (!StringUtils.isBlank(executed.getMessage())) {
            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    // use the ActiveWorkbenchWindow's shell, don't use the default shell, otherwize the error dialog
                    // will be cloed when the "Run Analysis" dialog (if user don't check "Always run in background", it
                    // will always show this dialog when run an analysis) close, just like don't show the error dialog
                    Shell shell = null;
                    if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
                        shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                    }
                    MessageDialogWithToggle.openError(
                            shell,
                            DefaultMessagesImpl.getString("RunAnalysisAction.runAnalysis"), DefaultMessagesImpl.getString("RunAnalysisAction.failRunAnalysis",//$NON-NLS-1$ //$NON-NLS-2$ 
                                            item.getAnalysis().getName(), executed.getMessage()));
                }
            });
        }
    }
}
