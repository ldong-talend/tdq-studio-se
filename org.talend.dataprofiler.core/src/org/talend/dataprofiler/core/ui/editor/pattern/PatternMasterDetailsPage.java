// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.editor.pattern;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.FileEditorInput;
import org.talend.commons.emf.EMFUtil;
import org.talend.cwm.helper.TaggedValueHelper;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.PluginConstant;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dataprofiler.core.pattern.PatternLanguageType;
import org.talend.dataprofiler.core.ui.editor.AbstractMetadataFormPage;
import org.talend.dataprofiler.core.ui.views.PatternTestView;
import org.talend.dataquality.domain.pattern.ExpressionType;
import org.talend.dataquality.domain.pattern.Pattern;
import org.talend.dataquality.domain.pattern.PatternComponent;
import org.talend.dataquality.domain.pattern.RegularExpression;
import org.talend.dataquality.domain.pattern.impl.RegularExpressionImpl;
import org.talend.dataquality.helpers.BooleanExpressionHelper;
import org.talend.dataquality.helpers.DomainHelper;
import org.talend.dq.helper.resourcehelper.PatternResourceFileHelper;
import orgomg.cwm.objectmodel.core.ModelElement;

/**
 * DOC rli class global comment. Detailled comment
 */
public class PatternMasterDetailsPage extends AbstractMetadataFormPage implements PropertyChangeListener {

    private static final String SQL = "SQL"; //$NON-NLS-1$

    private Pattern pattern;

    private Composite patternDefinitionSectionComp;

    private Composite componentsComp;

    private List<PatternComponent> tempPatternComponents;

    private List<String> allDBTypeList;

    private List<String> remainDBTypeList;

    private ScrolledForm form;

    private Section patternDefinitionSection;

    private String expressionType;

    public PatternMasterDetailsPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
    }

    public void initialize(FormEditor editor) {
        super.initialize(editor);
        reset();
        this.expressionType = DomainHelper.getExpressionType((Pattern) currentModelElement);
    }

    /**
     * DOC rli Comment method "reset".
     */
    private void reset() {
        String[] supportTypes = PatternLanguageType.getAllLanguageTypes();
        allDBTypeList = new ArrayList<String>();
        allDBTypeList.addAll(Arrays.asList(supportTypes));
        if (tempPatternComponents == null) {
            tempPatternComponents = new ArrayList<PatternComponent>();
        } else {
            tempPatternComponents.clear();
        }
        // tempPatternComponents.addAll(pattern.getComponents());
        remainDBTypeList = new ArrayList<String>();
        remainDBTypeList.addAll(allDBTypeList);
    }

    @Override
    protected ModelElement getCurrentModelElement(FormEditor editor) {
        FileEditorInput input = (FileEditorInput) editor.getEditorInput();

        this.pattern = PatternResourceFileHelper.getInstance().findPattern(input.getFile());

        this.currentModelElement = pattern;

        return pattern;
    }

    protected void createFormContent(IManagedForm managedForm) {
        super.createFormContent(managedForm);
        form = managedForm.getForm();

        form.setText(DefaultMessagesImpl.getString("PatternMasterDetailsPage.patternSettings")); //$NON-NLS-1$
        metadataSection.setText(DefaultMessagesImpl.getString("PatternMasterDetailsPage.patternMetadata")); //$NON-NLS-1$
        metadataSection.setDescription(DefaultMessagesImpl.getString("PatternMasterDetailsPage.setProperties")); //$NON-NLS-1$
        creatPatternDefinitionSection(topComp);
    }

    private void creatPatternDefinitionSection(Composite topCmp) {
        patternDefinitionSection = createSection(form, topCmp, DefaultMessagesImpl
                .getString("PatternMasterDetailsPage.patternDefinition"), null); //$NON-NLS-1$

        Label label = new Label(patternDefinitionSection, SWT.WRAP);
        label.setText(DefaultMessagesImpl.getString("PatternMasterDetailsPage.text")); //$NON-NLS-1$
        patternDefinitionSection.setDescriptionControl(label);

        patternDefinitionSectionComp = createPatternDefinitionComp();

    }

    public void updatePatternDefinitonSection() {
        patternDefinitionSectionComp.dispose();
        reset();
        patternDefinitionSectionComp = createPatternDefinitionComp();
        patternDefinitionSection.layout();
        // patternDefinitionSectionComp.layout();
        form.reflow(true);
    }

    /**
     * DOC rli Comment method "ceatePatternDefinitionComp".
     * 
     * @param form
     * @param section
     */
    private Composite createPatternDefinitionComp() {
        Composite newComp = toolkit.createComposite(patternDefinitionSection);
        newComp.setLayout(new GridLayout());

        componentsComp = new Composite(newComp, SWT.NONE);
        componentsComp.setLayout(new GridLayout());

        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(componentsComp);
        EList<PatternComponent> components = this.pattern.getComponents();
        for (int i = 0; i < components.size(); i++) {
            RegularExpression regularExpress = (RegularExpression) components.get(i);
            // RegularExpressionImpl newRegularExpress = (RegularExpressionImpl)
            // PatternFactory.eINSTANCE.createRegularExpression();
            // Expression newExpression = CoreFactory.eINSTANCE.createExpression();
            // newExpression.setBody(regularExpress.getExpression().getBody());
            // newExpression.setLanguage(regularExpress.getExpression().getLanguage());
            // newRegularExpress.setExpression(newExpression);
            tempPatternComponents.add(regularExpress);
            creatNewExpressLine(regularExpress);
        }
        createAddButton(newComp);

        patternDefinitionSection.setClient(newComp);
        return newComp;
    }

    private void createAddButton(Composite parent) {
        final Button addButton = new Button(parent, SWT.NONE);
        addButton.setImage(ImageLib.getImage(ImageLib.ADD_ACTION));
        addButton.setToolTipText(DefaultMessagesImpl.getString("PatternMasterDetailsPage.add")); //$NON-NLS-1$
        GridData labelGd = new GridData();
        labelGd.horizontalAlignment = SWT.CENTER;
        labelGd.widthHint = 65;
        addButton.setLayoutData(labelGd);
        addButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                remainDBTypeList.clear();
                remainDBTypeList.addAll(allDBTypeList);
                for (PatternComponent patternComponent : tempPatternComponents) {
                    String language = ((RegularExpressionImpl) patternComponent).getExpression().getLanguage();
                    String languageName = PatternLanguageType.findNameByLanguage(language);
                    remainDBTypeList.remove(languageName);
                }
                if (remainDBTypeList.size() == 0) {
                    MessageDialog
                            .openWarning(
                                    null,
                                    DefaultMessagesImpl.getString("PatternMasterDetailsPage.warning"), DefaultMessagesImpl.getString("PatternMasterDetailsPage.patternExpression")); //$NON-NLS-1$ //$NON-NLS-2$
                    return;
                }

                String language = PatternLanguageType.findLanguageByName(remainDBTypeList.get(0));
                RegularExpression newRegularExpress = BooleanExpressionHelper.createRegularExpression(language, null);
                newRegularExpress.setExpressionType(expressionType);
                creatNewExpressLine(newRegularExpress);
                tempPatternComponents.add(newRegularExpress);
                patternDefinitionSection.setExpanded(true);
                setDirty(true);
            }
        });
    }

    private void creatNewExpressLine(RegularExpression regularExpress) {
        final Composite expressComp = new Composite(componentsComp, SWT.NONE);
        expressComp.setLayout(new GridLayout(10, false));
        final CCombo combo = new CCombo(expressComp, SWT.BORDER);
        combo.setEditable(false);
        combo.setItems(remainDBTypeList.toArray(new String[remainDBTypeList.size()]));
        final RegularExpression finalRegExpress = regularExpress;
        String language = regularExpress.getExpression().getLanguage();
        String body = regularExpress.getExpression().getBody();

        if (language == null) {
            combo.setText(remainDBTypeList.get(0));
        } else {
            combo.setText(PatternLanguageType.findNameByLanguage(language));
        }

        GridDataFactory.fillDefaults().span(2, 1).grab(false, false).applyTo(combo);

        combo.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                String lang = combo.getText();
                finalRegExpress.getExpression().setLanguage(PatternLanguageType.findLanguageByName(lang));
                setDirty(true);
            }
        });
        final Text patternText = new Text(expressComp, SWT.BORDER);
        patternText.setText(body == null ? PluginConstant.EMPTY_STRING : body);
        GridDataFactory.fillDefaults().span(6, 1).grab(true, true).applyTo(patternText);
        patternText.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                finalRegExpress.getExpression().setBody(patternText.getText());
                setDirty(true);
            }

        });
        Button delButton = new Button(expressComp, SWT.NONE);
        delButton.setImage(ImageLib.getImage(ImageLib.DELETE_ACTION));
        delButton.setToolTipText(DefaultMessagesImpl.getString("PatternMasterDetailsPage.delete")); //$NON-NLS-1$
        GridDataFactory.fillDefaults().span(1, 1).grab(false, false).applyTo(delButton);
        delButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                tempPatternComponents.remove(finalRegExpress);
                expressComp.dispose();
                patternDefinitionSection.setExpanded(true);
                setDirty(true);
            }
        });

        // MOD scorreia 2008-12-15 do not display button when pattern is "SQL Like"
        if (!ExpressionType.SQL_LIKE.getLiteral().equals(expressionType)) {
            Button testPatternButton = new Button(expressComp, SWT.NONE);
            // testPatternButton.setImage(ImageLib.getImage(ImageLib.));
            testPatternButton.setText(DefaultMessagesImpl.getString("PatternMasterDetailsPage.test")); //$NON-NLS-1$
            testPatternButton.setToolTipText(DefaultMessagesImpl.getString("PatternMasterDetailsPage.patternTest")); //$NON-NLS-1$
            GridDataFactory.fillDefaults().span(1, 1).grab(false, false).applyTo(testPatternButton);
            testPatternButton.addSelectionListener(new SelectionAdapter() {

                public void widgetSelected(SelectionEvent e) {
                    // Open test pattern viewer
                    PatternTestView patternTestView = (PatternTestView) CorePlugin.getDefault().findView(PatternTestView.ID);
                    patternTestView.setPatternExpression(PatternMasterDetailsPage.this, pattern, finalRegExpress);
                }
            });
        }

        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(expressComp);
    }

    @Override
    public void setDirty(boolean isDirty) {
        if (this.isDirty != isDirty) {
            this.isDirty = isDirty;
            ((PatternEditor) this.getEditor()).firePropertyChange(IEditorPart.PROP_DIRTY);
            this.firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (PluginConstant.ISDIRTY_PROPERTY.equals(evt.getPropertyName())) {
            ((PatternEditor) this.getEditor()).firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        super.doSave(monitor);
        if (savePattern()) {
            this.isDirty = false;
        }
    }

    private boolean savePattern() {
        if (tempPatternComponents.size() == 0) {
            MessageDialog
                    .openError(
                            null,
                            DefaultMessagesImpl.getString("PatternMasterDetailsPage.error"), DefaultMessagesImpl.getString("PatternMasterDetailsPage.cannotSave", pattern.getName())); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
        this.pattern.getComponents().clear();
        this.pattern.getComponents().addAll(tempPatternComponents);

        // PTODO fixed bug 4296: set the Pattern is valid
        TaggedValueHelper.setValidStatus(true, pattern);
        EList<PatternComponent> components = this.pattern.getComponents();
        List<String> existLanguage = new ArrayList<String>();
        for (int i = 0; i < components.size(); i++) {
            RegularExpressionImpl regularExpress = (RegularExpressionImpl) components.get(i);
            String language = regularExpress.getExpression().getLanguage();
            if ((regularExpress.getExpression().getBody() == null) || (!regularExpress.getExpression().getBody().matches("'.*'"))) { //$NON-NLS-1$
                MessageDialog
                        .openWarning(
                                null,
                                DefaultMessagesImpl.getString("PatternMasterDetailsPage.warning"), DefaultMessagesImpl.getString("PatternMasterDetailsPage.patternExpressions", language)); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }
            if (existLanguage.contains(language)) {
                MessageDialog
                        .openError(
                                null,
                                DefaultMessagesImpl.getString("PatternMasterDetailsPage.error"), DefaultMessagesImpl.getString("PatternMasterDetailsPage.languageType", language)); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            } else {
                existLanguage.add(language);
            }
        }
        EMFUtil.saveSingleResource(pattern.eResource());
        return true;

    }

}
