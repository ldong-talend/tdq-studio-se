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
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.talend.core.model.metadata.MetadataColumnRepositoryObject;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.cwm.dependencies.DependenciesHandler;
import org.talend.cwm.helper.ColumnHelper;
import org.talend.cwm.helper.ResourceHelper;
import org.talend.cwm.relational.TdColumn;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.ui.editor.composite.AnalysisColumnCompareTreeViewer;
import org.talend.dataquality.domain.Domain;
import org.talend.dataquality.exception.DataprofilerCoreException;
import org.talend.dataquality.helpers.AnalysisHelper;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dataquality.indicators.columnset.ColumnDependencyIndicator;
import org.talend.dataquality.indicators.columnset.ColumnsetFactory;
import org.talend.dataquality.indicators.columnset.ColumnsetPackage;
import org.talend.dq.analysis.AnalysisBuilder;
import org.talend.dq.analysis.AnalysisHandler;
import org.talend.dq.analysis.ColumnDependencyAnalysisHandler;
import org.talend.dq.helper.RepositoryNodeHelper;
import org.talend.dq.indicators.definitions.DefinitionHandler;
import org.talend.dq.nodes.DBColumnRepNode;
import org.talend.dq.writer.impl.ElementWriterFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.utils.sugars.ReturnCode;
import org.talend.utils.sugars.TypedReturnCode;
import orgomg.cwm.objectmodel.core.Dependency;
import orgomg.cwm.resource.relational.ColumnSet;

/**
 * This is a Master Detail Page for <a href="http://www.talendforge.org/bugs/view.php?id=8134">Functional Dependency
 * Analysis</a>.
 * 
 * @author jet
 */
public class ColumnDependencyMasterDetailsPage extends AbstractAnalysisMetadataPage implements PropertyChangeListener {

    private static Logger log = Logger.getLogger(ColumnDependencyMasterDetailsPage.class);

    private Section columnsComparisonSection = null;

    public Section getColumnsComparisonSection() {
        return this.columnsComparisonSection;
    }

    public void setColumnsComparisonSection(Section columnsComparisonSection) {
        this.columnsComparisonSection = columnsComparisonSection;
    }

    List<RepositoryNode> columnListA = null;

    List<RepositoryNode> columnListB = null;

    private AnalysisColumnCompareTreeViewer anaColumnCompareViewer;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.talend.dataprofiler.core.ui.editor.AbstractMetadataFormPage#initialize(org.eclipse.ui.forms.editor.FormEditor
     * )
     */
    @Override
    public void initialize(FormEditor editor) {
        super.initialize(editor);
        stringDataFilter = AnalysisHelper.getStringDataFilter(analysisItem.getAnalysis());
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        super.createFormContent(managedForm);
        form = managedForm.getForm();
        form.setText(DefaultMessagesImpl.getString("ColumnDependencyMasterDetailsPage.ColumnDependencyAnalysis")); //$NON-NLS-1$

        this.metadataSection.setText(DefaultMessagesImpl.getString("ColumnDependencyMasterDetailsPage.analysisMeta")); //$NON-NLS-1$

        this.metadataSection.setDescription(DefaultMessagesImpl
                .getString("ColumnsComparisonMasterDetailsPage.setAnalysisProperties")); //$NON-NLS-1$

        anaColumnCompareViewer = new AnalysisColumnCompareTreeViewer(this, topComp, getColumnLeftSet(), getColumnRightSet(),
                DefaultMessagesImpl.getString("FunctionalDependencyMasterDetailsPage.Title"), DefaultMessagesImpl //$NON-NLS-1$
                        .getString("FunctionalDependencyMasterDetailsPage.Description"), false, true); //$NON-NLS-1$

        anaColumnCompareViewer.addPropertyChangeListener(this);

        createDataFilterSection(form, topComp);
        dataFilterComp.addPropertyChangeListener(this);
        dataFilterComp.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                AnalysisHelper.setStringDataFilter(analysisItem.getAnalysis(), dataFilterComp.getDataFilterString());
            }
        });

        createAnalysisParamSection(form, topComp);

        createContextGroupSection(form, topComp);

        columnListA = anaColumnCompareViewer.getColumnListA();
        columnListB = anaColumnCompareViewer.getColumnListB();

        columnsComparisonSection = anaColumnCompareViewer.getColumnsComparisonSection();
        anaColumnCompareViewer.addPropertyChangeListener(this);

        form.reflow(true);
    }

    @Override
    public ScrolledForm getScrolledForm() {
        return form;
    }

    /**
     * @param editor
     * @param id
     * @param title
     */
    public ColumnDependencyMasterDetailsPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisMetadataPage#canRun()
     */
    @Override
    protected ReturnCode canRun() {
        return columnListA.size() > 0 ? new ReturnCode(true) : new ReturnCode(
                DefaultMessagesImpl.getString("ColumnDependencyMasterDetailsPage.columnsBlankRunMessage"), false); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisMetadataPage#canSave()
     */
    @Override
    public ReturnCode canSave() {
        // do some validator.
        return validator(columnListA, columnListB);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisMetadataPage#refresh()
     */
    @Override
    public void refresh() {
        switchToResultPage();
    }

    public AnalysisHandler getAnalysisHandler() {
        ColumnDependencyAnalysisHandler analysisHandler = new ColumnDependencyAnalysisHandler();
        analysisHandler.setAnalysis(this.analysisItem.getAnalysis());
        return analysisHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisMetadataPage#saveAnalysis()
     */
    @Override
    protected void saveAnalysis() throws DataprofilerCoreException {
        // ADD gdbu 2011-3-3 bug 19179
        // remove the space from analysis name
        //        analysis.setName(analysis.getName().replace(" ", ""));//$NON-NLS-1$ //$NON-NLS-2$
        for (Domain domain : this.analysisItem.getAnalysis().getParameters().getDataFilter()) {
            domain.setName(this.analysisItem.getAnalysis().getName());
        }
        // ~

        IRepositoryViewObject reposObject = null;
        getAnalysisHandler().clearAnalysis();

        List<RepositoryNode> columnListAANode = anaColumnCompareViewer.getColumnListA();
        List<RepositoryNode> columnListBBNode = anaColumnCompareViewer.getColumnListB();

        AnalysisBuilder anaBuilder = new AnalysisBuilder();
        anaBuilder.setAnalysis(this.analysisItem.getAnalysis());
        Connection tdDataProvider = null;

        for (int i = 0; i < columnListAANode.size(); i++) {
            if (columnListBBNode.size() > i) {
                ColumnDependencyIndicator indicator = ColumnsetFactory.eINSTANCE.createColumnDependencyIndicator();
                TdColumn columnA = (TdColumn) ((MetadataColumnRepositoryObject) columnListAANode.get(i).getObject())
                        .getTdColumn();
                TdColumn columnB = (TdColumn) ((MetadataColumnRepositoryObject) columnListBBNode.get(i).getObject())
                        .getTdColumn();
                indicator.setColumnA(columnA);
                indicator.setColumnB(columnB);
                indicator.setIndicatorDefinition(DefinitionHandler.getInstance().getFDRuleDefaultIndicatorDefinition());
                analysisItem.getAnalysis().getResults().getIndicators().add(indicator);
                anaBuilder.addElementToAnalyze(columnA, indicator);
                // ADD this line qiongli 2010-6-8
                anaBuilder.addElementToAnalyze(columnB, indicator);
            }
        }

        if (columnListAANode.size() > 0) {
            reposObject = columnListAANode.get(0).getObject();
            tdDataProvider = ((ConnectionItem) reposObject.getProperty().getItem()).getConnection();
            // MOD qiongli bug 14437:Add dependency
            analysisItem.getAnalysis().getContext().setConnection(tdDataProvider);
            TypedReturnCode<Dependency> rc = DependenciesHandler.getInstance().setDependencyOn(analysisItem.getAnalysis(),
                    tdDataProvider);
            if (!rc.isOk()) {
                log.info("fail to save dependency analysis:" + analysisItem.getAnalysis().getFileName());//$NON-NLS-1$
            }
        } else {
            analysisItem.getAnalysis().getContext().setConnection(null);
        }

        // save the number of connections per analysis
        this.saveNumberOfConnectionsPerAnalysis();

        // 2011.1.12 MOD by zhsne to unify anlysis and connection id when saving.
        ReturnCode saved = new ReturnCode(false);
        this.nameText.setText(analysisItem.getAnalysis().getName());
        // MOD yyi 2012-02-08 TDQ-4621:Explicitly set true for updating dependencies.
        saved = ElementWriterFactory.getInstance().createAnalysisWrite().save(analysisItem, true);
        // MOD yyi 2012-02-03 TDQ-3602:Avoid to rewriting all analyzes after saving, no reason to update all analyzes
        // which is depended in the referred connection.
        // Extract saving log function.
        // @see org.talend.dataprofiler.core.ui.editor.analysis.AbstractAnalysisMetadataPage#logSaved(ReturnCode)
        logSaved(saved);
        // ADD xqliu 2012-04-19 TDQ-5005
        anaColumnCompareViewer.setDirty(false);
        dataFilterComp.setDirty(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (PluginConstant.ISDIRTY_PROPERTY.equals(evt.getPropertyName())) {
            ((AnalysisEditor) currentEditor).firePropertyChange(IEditorPart.PROP_DIRTY);
            ((AnalysisEditor) currentEditor).setRefreshResultPage(true);
        } else if (PluginConstant.DATAFILTER_PROPERTY.equals(evt.getPropertyName())) {
            this.setDirty(true);
        }
    }

    private List<RepositoryNode> getColumnLeftSet() {
        return getColumnSet(ColumnsetPackage.Literals.COLUMN_DEPENDENCY_INDICATOR__COLUMN_A);
    }

    private List<RepositoryNode> getColumnRightSet() {
        return getColumnSet(ColumnsetPackage.Literals.COLUMN_DEPENDENCY_INDICATOR__COLUMN_B);
    }

    private List<RepositoryNode> getColumnSet(EReference reference) {
        List<RepositoryNode> columns = new ArrayList<RepositoryNode>();
        EList<Indicator> indicators = analysisItem.getAnalysis().getResults().getIndicators();
        for (Indicator indicator : indicators) {
            TdColumn findColumn = (TdColumn) indicator.eGet(reference);
            RepositoryNode recursiveFind = RepositoryNodeHelper.recursiveFind(findColumn);
            if (recursiveFind == null) {
                recursiveFind = RepositoryNodeHelper.createRepositoryNode(findColumn);
            }
            columns.add(recursiveFind);
        }
        return columns;
    }

    private ReturnCode validator(List<RepositoryNode> columnASet, List<RepositoryNode> columnBSet) {
        // MOD by gdbu 2011-3-21 bug 19179
        ReturnCode canModRetCode = super.canSave();
        if (!canModRetCode.isOk()) {
            return canModRetCode;
        }
        // ~19179

        if (columnASet.size() == 0 || columnBSet.size() == 0) {
            return new ReturnCode(DefaultMessagesImpl.getString("ColumnDependencyMasterDetailsPage.columnsBlankMessag"), false); //$NON-NLS-1$
        }
        if (columnASet.size() != columnBSet.size()) {
            // two set must have equal number.
            return new ReturnCode(DefaultMessagesImpl.getString("ColumnsComparisonMasterDetailsPage.columnsSameMessage"), false); //$NON-NLS-1$
        }

        for (int i = 0; i < columnASet.size(); i++) {
            RepositoryNode columnANode = columnASet.get(i);
            RepositoryNode columnBNode = columnBSet.get(i);
            TdColumn tdColumnA = ((DBColumnRepNode) columnANode).getTdColumn();
            TdColumn tdColumnB = ((DBColumnRepNode) columnBNode).getTdColumn();
            ColumnSet ownerA = ColumnHelper.getColumnOwnerAsColumnSet(tdColumnA);
            ColumnSet ownerB = ColumnHelper.getColumnOwnerAsColumnSet(tdColumnB);
            String uuidA = ResourceHelper.getUUID(ownerA);
            String uuidB = ResourceHelper.getUUID(ownerB);
            if (!uuidA.equals(uuidB)) {
                // must come from one table
                return new ReturnCode(DefaultMessagesImpl.getString("ColumnDependencyMasterDetailsPage.tableMessage"), false); //$NON-NLS-1$
            }
        }

        return new ReturnCode(true);
    }

    /**
     * 
     * DOC zshen DOC mzhao Open column selection dialog for left column set. this method is intended to use from cheat
     * sheets.
     */
    public void openColumnsSetASelectionDialog() {
        anaColumnCompareViewer.openColumnsSetASelectionDialog();
    }

    /**
     * 
     * DOC zshen DOC mzhao Open column selection dialog for right column set. this method is intended to use from cheat
     * sheets.
     */
    public void openColumnsSetBSelectionDialog() {
        anaColumnCompareViewer.openColumnsSetBSelectionDialog();
    }

    /**
     * ADD xqliu 2012-04-19 TDQ-5005.
     */
    @Override
    public boolean isDirty() {
        if (anaColumnCompareViewer == null ? false : anaColumnCompareViewer.isDirty()) {
            this.setDirty(anaColumnCompareViewer.isDirty());
        }
        if (dataFilterComp == null ? false : dataFilterComp.isDirty()) {
            this.setDirty(dataFilterComp.isDirty());
        }
        return super.isDirty();
    }
}
