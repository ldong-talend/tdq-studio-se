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
package org.talend.dataprofiler.core.model;

import org.talend.dataquality.indicators.Indicator;
import org.talend.repository.model.IRepositoryNode;

/**
 * DOC klliu  class global comment. Detailled comment
 */
public class OverviewIndUIElement {

    private Indicator overviewIndicator = null;

    private IRepositoryNode node = null;

    public void setOverviewIndicator(Indicator overviewIndicator) {
        this.overviewIndicator = overviewIndicator;
    }

    public Indicator getOverviewIndicator() {
        return overviewIndicator;
    }

    public void setNode(IRepositoryNode node) {
        this.node = node;
    }

    public IRepositoryNode getNode() {
        return node;
    }
}
