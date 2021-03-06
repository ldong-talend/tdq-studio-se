/**
 * <copyright> </copyright>
 * 
 * $Id$
 */
package org.talend.dataquality.indicators.impl;

import org.eclipse.emf.ecore.EClass;
import org.talend.dataquality.indicators.IndicatorParameters;
import org.talend.dataquality.indicators.IndicatorsPackage;
import org.talend.dataquality.indicators.MaxLengthIndicator;

/**
 * <!-- begin-user-doc --> An implementation of the model object '<em><b>Max Length Indicator</b></em>'. <!--
 * end-user-doc -->
 * <p>
 * </p>
 *
 * @generated
 */
public class MaxLengthIndicatorImpl extends LengthIndicatorImpl implements MaxLengthIndicator {

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @generated
     */
    protected MaxLengthIndicatorImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @generated
     */
    @Override
    protected EClass eStaticClass() {
        return IndicatorsPackage.Literals.MAX_LENGTH_INDICATOR;
    }

    /*
     * (non-Javadoc) ADDED scorreia 2008-04-08 compute max length
     * 
     * @see org.talend.dataquality.indicators.impl.IndicatorImpl#handle(java.lang.Object)
     */
    @Override
    public boolean handle(Object data) {
        this.mustStoreRow = true;
        boolean ok = super.handle(data);
        if (data != null) {
            String str = (String) data;
            if (str.length() > 0 && (length == null || length.intValue() < str.length())) {
                length = Long.valueOf(str.length());
            }
        }
        return ok;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.indicators.impl.LengthIndicatorImpl#getParameters()
     */
    @Override
    public IndicatorParameters getParameters() {
        parameters = super.getParameters();
        parameters.getTextParameter().setUseNulls(false);
        parameters.getTextParameter().setUseBlank(false);
        return parameters;
    }

} // MaxLengthIndicatorImpl
