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
package org.talend.dq.analysis.explore;

import java.util.HashMap;
import java.util.Map;

import org.talend.dataquality.indicators.sql.WhereRuleIndicator;
import org.talend.dataquality.rules.WhereRule;
import orgomg.cwm.resource.relational.Table;

/**
 * DOC xqliu class global comment. Detailled comment
 */
public class DQRuleExplorer extends DataExplorer {

    public Map<String, String> getQueryMap() {
        Map<String, String> map = new HashMap<String, String>();

        switch (this.indicatorEnum) {
        case WhereRuleIndicatorEnum:
            map.put(MENU_VIEW_INVALID_ROWS, getInvalidRowsStatement());
            map.put(MENU_VIEW_VALID_ROWS, getRowsStatement());
            break;
        default:
        }

        return map;
    }

    private String getInvalidRowsStatement() {
        Table table = (Table) indicator.getAnalyzedElement();
        String whereClause = ((WhereRule) ((WhereRuleIndicator) indicator).getIndicatorDefinition()).getWhereExpression();
        return "SELECT * FROM " + getFullyQualifiedTableName(table) + dbmsLanguage.where() + "!(" + whereClause + ")";
    }

}
