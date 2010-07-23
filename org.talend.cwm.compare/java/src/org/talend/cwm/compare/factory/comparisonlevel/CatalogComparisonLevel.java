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
package org.talend.cwm.compare.factory.comparisonlevel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeLeftTarget;
import org.eclipse.emf.compare.diff.metamodel.ModelElementChangeRightTarget;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.cwm.compare.DQStructureComparer;
import org.talend.cwm.compare.exception.ReloadCompareException;
import org.talend.cwm.helper.CatalogHelper;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.cwm.helper.DataProviderHelper;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.dq.writer.EMFSharedResources;
import orgomg.cwm.objectmodel.core.Package;
import orgomg.cwm.resource.relational.Catalog;
import orgomg.cwm.resource.relational.Schema;

/**
 * 
 * DOC mzhao class global comment. Detailled comment
 */
public class CatalogComparisonLevel extends AbstractComparisonLevel {

    public CatalogComparisonLevel(Catalog selObj) {
        super(selObj);
    }

    @Override
    protected Connection findDataProvider() {
        Connection provider = ConnectionHelper.getTdDataProvider((Package) selectedObj);
        return provider;
    }

    @Override
    protected Resource getLeftResource() throws ReloadCompareException {
        Package selectedPackage = (Package) selectedObj;
        Package findMatchPackage = DQStructureComparer.findMatchedPackage(selectedPackage, copyedDataProvider);
        List<Schema> schemas = new ArrayList<Schema>();
        schemas.addAll(CatalogHelper.getSchemas((Catalog) findMatchPackage));
        Resource leftResource = copyedDataProvider.eResource();
        leftResource.getContents().clear();
        for (Schema schema : schemas) {
            DQStructureComparer.clearSubNode(schema);
            leftResource.getContents().add(schema);
        }
        EMFSharedResources.getInstance().saveResource(leftResource);
        return leftResource;
    }

    @Override
    protected Resource getRightResource() throws ReloadCompareException {
        Package selectedPackage = (Package) selectedObj;
        Package toReloadObj = DQStructureComparer.findMatchedPackage(selectedPackage, tempReloadProvider);
        List<Schema> schemas = reloadElementOfPackage(toReloadObj);
        Resource rightResource = null;
        rightResource = tempReloadProvider.eResource();
        rightResource.getContents().clear();
        for (Schema schema : schemas) {
            DQStructureComparer.clearSubNode(schema);
            rightResource.getContents().add(schema);
        }
        EMFSharedResources.getInstance().saveResource(rightResource);
        return rightResource;
    }

    private List<Schema> reloadElementOfPackage(Package toReloadObj) throws ReloadCompareException {
        List<Schema> schemas = new ArrayList<Schema>();
        Catalog catalogObj = SwitchHelpers.CATALOG_SWITCH.doSwitch(toReloadObj);
        if (catalogObj != null) {
            schemas = CatalogHelper.getSchemas(catalogObj);
        }
        return schemas;
    }

    @Override
    protected EObject getSavedReloadObject() throws ReloadCompareException {
        Package selectedPackage = (Package) selectedObj;
        Package findMatchPackage = DQStructureComparer.findMatchedPackage(selectedPackage, tempReloadProvider);
        reloadElementOfPackage(findMatchPackage);
        return findMatchPackage;
    }

    @Override
    protected void handleAddElement(ModelElementChangeRightTarget addElement) {
        EObject rightElement = addElement.getRightElement();
        Schema schema = SwitchHelpers.SCHEMA_SWITCH.doSwitch(rightElement);
        if (schema != null) {
            DataProviderHelper.addSchema(schema, oldDataProvider);
        }
    }

    @Override
    protected void handleRemoveElement(ModelElementChangeLeftTarget removeElement) {
        Schema removedSchema = SwitchHelpers.SCHEMA_SWITCH.doSwitch(removeElement.getLeftElement());
        if (removedSchema == null) {
            return;
        }
        popRemoveElementConfirm();
        ((Package) selectedObj).getOwnedElement().remove(removedSchema);
        // CatalogHelper.(removedSchema, (Package) selectedObj);
    }

}
