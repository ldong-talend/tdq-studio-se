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
package org.talend.dq.dbms;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.talend.utils.ProductVersion;
import orgomg.cwm.objectmodel.core.Expression;

/**
 * created by xqliu on Aug 7, 2013 Detailled comment
 * 
 */
public class VerticaDbmsLanguage extends DbmsLanguage {

    public VerticaDbmsLanguage() {
        super(DbmsLanguage.VERTICA);
    }

    public VerticaDbmsLanguage(String dbmsType) {
        super(dbmsType);
    }

    public VerticaDbmsLanguage(String dbmsType, ProductVersion dbVersion) {
        super(dbmsType, dbVersion);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dq.dbms.DbmsLanguage#createStatement(java.sql.Connection)
     */
    @Override
    public Statement createStatement(Connection connection, int fetchSize) throws SQLException {
        Statement statement = connection.createStatement();
        statement.setFetchSize(fetchSize);
        return statement;
    }

    @Override
    public String regexLike(String element, String regex) {
        return surroundWithSpaces(getRegularExpressionFunction() + "(TO_CHAR(" + element + ") , " + regex + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public String regexNotLike(String element, String regex) {
        return surroundWithSpaces(this.not() + getRegularExpressionFunction() + "(TO_CHAR(" + element + ") , " + regex + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dq.dbms.DbmsLanguage#getRegularExpressionFunction()
     */
    @Override
    public String getRegularExpressionFunction() {
        return "REGEXP_LIKE"; //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dq.dbms.DbmsLanguage#getRegularExpressionFunction()
     */
    @Override
    public String extractRegularExpressionFunction(Expression expression) {
        return getRegularExpressionFunction();
    }

    @Override
    protected String getPatternFinderFunction(String expression, String charsToReplace, String replacementChars) {
        assert charsToReplace != null && replacementChars != null && charsToReplace.length() == replacementChars.length();
        return translateUsingPattern(expression, charsToReplace, replacementChars);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dq.dbms.DbmsLanguage#getInvalidClauseBenFord(java.lang.String)
     */
    @Override
    public String getInvalidClauseBenFord(String columnName) {
        return columnName + " is null or " + "not regexp_like(to_char(" + columnName + "),'^[[:digit:]]')";//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dq.dbms.DbmsLanguage#getColumnNameInQueryClause(java.lang.String)
     */
    @Override
    public String castColumnNameToChar(String columnName) {
        return "to_char(" + columnName + ")"; //$NON-NLS-1$//$NON-NLS-2$
    }
}
