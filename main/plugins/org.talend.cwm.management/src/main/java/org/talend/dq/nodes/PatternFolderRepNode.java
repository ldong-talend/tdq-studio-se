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
package org.talend.dq.nodes;

import java.util.List;

import org.apache.log4j.Logger;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.data.container.Container;
import org.talend.commons.utils.data.container.RootContainer;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.Folder;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.dq.helper.RepositoryNodeHelper;
import org.talend.repository.model.IRepositoryNode;
import org.talend.repository.model.RepositoryNode;

/**
 * DOC gdbu class global comment. Detailled comment
 */
public class PatternFolderRepNode extends DQRepositoryNode {

    private static Logger log = Logger.getLogger(UserDefIndicatorFolderRepNode.class);

    /**
     * 
     * @param object
     * @param parent
     * @param type
     */
    public PatternFolderRepNode(IRepositoryViewObject object, RepositoryNode parent, ENodeType type) {
        super(object, parent, type);
    }

    @Override
    public List<IRepositoryNode> getChildren() {
        return getChildren(false);
    }

    @Override
    public List<IRepositoryNode> getChildren(boolean withDeleted) {
        try {
            super.getChildren().clear();
            RootContainer<String, IRepositoryViewObject> tdqViewObjects = ProxyRepositoryFactory.getInstance()
                    .getTdqRepositoryViewObjects(getContentType(), RepositoryNodeHelper.getPath(this).toString());
            // sub folders
            for (Container<String, IRepositoryViewObject> container : tdqViewObjects.getSubContainer()) {
                Folder folder = null;
                boolean isSystem = container.getLabel().equals("Regex");
                if (isSystem) {
                    folder = new Folder((Property) container.getProperty(), ERepositoryObjectType.TDQ_PATTERN_REGEX);
                    if (!withDeleted && folder.isDeleted()) {
                        continue;
                    }
                    PatternRegexFolderRepNode patternRegexFolderRepNode = new PatternRegexFolderRepNode(folder, this,
                            ENodeType.SYSTEM_FOLDER);
                    folder.setRepositoryNode(patternRegexFolderRepNode);
                    patternRegexFolderRepNode
                            .setProperties(EProperties.CONTENT_TYPE, ERepositoryObjectType.TDQ_SYSTEM_INDICATORS);
                    patternRegexFolderRepNode.setProperties(EProperties.LABEL, ERepositoryObjectType.TDQ_SYSTEM_INDICATORS);
                    super.getChildren().add(patternRegexFolderRepNode);

                } else {
                    folder = new Folder((Property) container.getProperty(), ERepositoryObjectType.TDQ_PATTERN_SQL);
                    if (!withDeleted && folder.isDeleted()) {
                        continue;
                    }
                    PatternSqlFolderRepNode patternSqlFolderRepNode = new PatternSqlFolderRepNode(folder, this,
                            ENodeType.SYSTEM_FOLDER);
                    folder.setRepositoryNode(patternSqlFolderRepNode);
                    patternSqlFolderRepNode.setProperties(EProperties.CONTENT_TYPE,
                            ERepositoryObjectType.TDQ_USERDEFINE_INDICATORS);
                    patternSqlFolderRepNode.setProperties(EProperties.LABEL, ERepositoryObjectType.TDQ_USERDEFINE_INDICATORS);
                    super.getChildren().add(patternSqlFolderRepNode);
                }

            }

        } catch (PersistenceException e) {
            log.error(e, e);
        }
        // MOD gdbu 2011-6-29 bug : 22204
        return filterResultsIfAny(super.getChildren());
        // ~22204
    }

}
