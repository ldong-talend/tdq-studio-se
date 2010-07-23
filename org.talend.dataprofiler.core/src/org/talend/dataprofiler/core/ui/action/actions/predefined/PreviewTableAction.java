// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprofiler.core.ui.action.actions.predefined;

import org.eclipse.jface.action.Action;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.cwm.helper.DataProviderHelper;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dataprofiler.core.ImageLib;
import org.talend.dataprofiler.core.i18n.internal.DefaultMessagesImpl;
import org.talend.dq.helper.ColumnSetNameHelper;
import orgomg.cwm.resource.relational.NamedColumnSet;

/**
 * DOC qzhang class global comment. Detailled comment <br/>
 * 
 * $Id: talend.epf 1 2006-09-29 17:06:40Z nrousseau $
 * 
 */
public class PreviewTableAction extends Action {

    private NamedColumnSet set;

    /**
     * DOC qzhang PreviewTableAction constructor comment.
     * 
     * @param table
     */
    public PreviewTableAction(NamedColumnSet set) {
        super(DefaultMessagesImpl.getString("PreviewTableAction.previewTable")); //$NON-NLS-1$
        setImageDescriptor(ImageLib.getImageDescriptor(ImageLib.EXPLORE_IMAGE));
        this.set = set;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        // MOD scorreia 2008-12-12 do not switch perspective for preview
        // new ChangePerspectiveAction(PluginConstant.SE_ID).run();

        Connection tdDataProvider = DataProviderHelper.getDataProvider(set);
        String qualifiedName = ColumnSetNameHelper.getColumnSetQualifiedName(tdDataProvider, set);
        String query = "select * from " + qualifiedName; //$NON-NLS-1$
        CorePlugin.getDefault().runInDQViewer(tdDataProvider, query, qualifiedName);
    }
}
