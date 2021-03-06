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
package org.talend.dq.analysis.explore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.talend.cwm.relational.TdColumn;
import org.talend.dq.dbms.DbmsLanguageFactory;
import orgomg.cwm.objectmodel.core.Expression;

/**
 * DOC mzhao class global comment. Detailled comment
 */
public class SoundexFrequencyExplorer extends FrequencyStatisticsExplorer {

    private static final String REGEX = "SELECT.*\\s*MAX\\((.*)\\)\\s*, (SOUNDEX|NYSIIS)\\(.*\\)\\s*, COUNT\\(\\*\\)\\s*(AS|as)?\\s*\\w*\\s*, COUNT\\(DISTINCT .*\\)\\s*(AS|as)?\\s*\\w*\\s* FROM"; //$NON-NLS-1$

    @Override
    protected String getFreqRowsStatement() {
        TdColumn column = (TdColumn) indicator.getAnalyzedElement();
        // MOD zshen 11005: SQL syntax error for all analysis on Informix databases in Talend Open Profiler

        String resultSql = null;
        // MOD klliu 0013242: set soundex indicator for null field,drill down will get NPE
        if (entity.getKey() != null) {
            resultSql = dbmsLanguage.getFreqRowsStatement(this.columnName, getFullyQualifiedTableName(column), entity.getKey()
                    .toString());
            if (resultSql != null) {
                return resultSql;
            }
        }

        String clause = getInstantiatedClause();
        return "SELECT * FROM " + getFullyQualifiedTableName(column) + dbmsLanguage.where() + inBrackets(clause) //$NON-NLS-1$
                + andDataFilterClause();
    }

    @Override
    protected String getInstantiatedClause() {
        // get function which convert data into a pattern
        String function = getFunction();

        // MOD zshen bug 11005 sometimes(when instead of soundex() with some sql),the Variable named "function" is not
        // is
        // colName.
        if (function != null
                && (DbmsLanguageFactory.isInfomix(this.dbmsLanguage.getDbmsName()) || DbmsLanguageFactory
                        .isOracle(this.dbmsLanguage.getDbmsName()))) {
            function = columnName;
        }
        // ~11005

        // MOD mzhao bug 9740 2009-11-10
        String clause = entity.isLabelNull() || function == null ? columnName + dbmsLanguage.isNull() : dbmsLanguage
                .getSoundexPrefix() + "("//$NON-NLS-1$ 
                + function + ")" + dbmsLanguage.equal() + dbmsLanguage.getSoundexPrefix() + "('" + entity.getKey() + "')"; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
        return clause;
    }

    private String getFunction() {
        Expression instantiatedExpression = dbmsLanguage.getInstantiatedExpression(indicator);
        final String body = instantiatedExpression.getBody();
        Pattern p = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(body);
        matcher.find();
        String group = matcher.group(1);
        return group;
    }
}
