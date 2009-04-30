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
package org.talend.dataprofiler.core.migration.impl;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.talend.dataprofiler.core.CorePlugin;
import org.talend.dataprofiler.core.ResourceManager;
import org.talend.dataprofiler.core.manager.DQStructureManager;
import org.talend.dataprofiler.core.migration.AbstractMigrationTask;

/**
 * DOC xqliu class global comment. Detailled comment
 */
public class CreateDQRulesFolderTask extends AbstractMigrationTask {

    protected static Logger log = Logger.getLogger(CreateDQRulesFolderTask.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.migration.IWorkspaceMigrationTask#execute()
     */
    public boolean execute() {
        try {
            DQStructureManager manager = DQStructureManager.getInstance();
            IFolder createNewFoler = manager.createNewFoler(ResourceManager.getLibrariesFolder(), DQStructureManager.DQ_RULES);
            createNewFoler.setPersistentProperty(DQStructureManager.FOLDER_CLASSIFY_KEY,
                    DQStructureManager.DQRULES_FOLDER_PROPERTY);
            manager.copyFilesToFolder(CorePlugin.getDefault(), DQStructureManager.DQ_RULES_PATH, true, createNewFoler, null);
        } catch (Exception e) {
            log.error(e, e);
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataprofiler.core.migration.IWorkspaceMigrationTask#getOrder()
     */
    public Date getOrder() {
        Calendar calender = Calendar.getInstance();
        calender.set(2009, 2, 13);
        return calender.getTime();
    }

}
