/**
 * <copyright> </copyright>
 * 
 * $Id$
 */
package org.talend.dataquality.indicators.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.talend.dataquality.helpers.IndicatorHelper;
import org.talend.dataquality.indicators.IndicatorsPackage;
import org.talend.dataquality.indicators.UniqueCountIndicator;

/**
 * <!-- begin-user-doc --> An implementation of the model object '<em><b>Unique Count Indicator</b></em>'. <!--
 * end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.talend.dataquality.indicators.impl.UniqueCountIndicatorImpl#getUniqueValueCount <em>Unique Value Count</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class UniqueCountIndicatorImpl extends IndicatorImpl implements UniqueCountIndicator {

    /**
     * The default value of the '{@link #getUniqueValueCount() <em>Unique Value Count</em>}' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see #getUniqueValueCount()
     * @generated
     * @ordered
     */
    protected static final Long UNIQUE_VALUE_COUNT_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getUniqueValueCount() <em>Unique Value Count</em>}' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see #getUniqueValueCount()
     * @generated
     * @ordered
     */
    protected Long uniqueValueCount = UNIQUE_VALUE_COUNT_EDEFAULT;

    private Set<Object> uniqueObjects = new HashSet<Object>();

    private Set<Object> duplicateObjects = new HashSet<Object>();

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @generated
     */
    protected UniqueCountIndicatorImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @generated
     */
    @Override
    protected EClass eStaticClass() {
        return IndicatorsPackage.Literals.UNIQUE_COUNT_INDICATOR;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @generated
     */
    public Set<Object> getUniqueValues() {
        // TODO: implement this method
        // Ensure that you remove @generated or mark it @generated NOT
        throw new UnsupportedOperationException();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @generated
     */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case IndicatorsPackage.UNIQUE_COUNT_INDICATOR__UNIQUE_VALUE_COUNT:
                return getUniqueValueCount();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @generated
     */
    @Override
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case IndicatorsPackage.UNIQUE_COUNT_INDICATOR__UNIQUE_VALUE_COUNT:
                setUniqueValueCount((Long)newValue);
                return;
        }
        super.eSet(featureID, newValue);
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @generated
     */
    @Override
    public void eUnset(int featureID) {
        switch (featureID) {
            case IndicatorsPackage.UNIQUE_COUNT_INDICATOR__UNIQUE_VALUE_COUNT:
                setUniqueValueCount(UNIQUE_VALUE_COUNT_EDEFAULT);
                return;
        }
        super.eUnset(featureID);
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @generated
     */
    @Override
    public boolean eIsSet(int featureID) {
        switch (featureID) {
            case IndicatorsPackage.UNIQUE_COUNT_INDICATOR__UNIQUE_VALUE_COUNT:
                return UNIQUE_VALUE_COUNT_EDEFAULT == null ? uniqueValueCount != null : !UNIQUE_VALUE_COUNT_EDEFAULT.equals(uniqueValueCount);
        }
        return super.eIsSet(featureID);
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated NOT
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer(this.getName());
        result.append("= ");
        result.append(uniqueValueCount);
        return result.toString();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @generated
     */
    public Long getUniqueValueCount() {
        return uniqueValueCount;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * @generated
     */
    public void setUniqueValueCount(Long newUniqueValueCount) {
        Long oldUniqueValueCount = uniqueValueCount;
        uniqueValueCount = newUniqueValueCount;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, IndicatorsPackage.UNIQUE_COUNT_INDICATOR__UNIQUE_VALUE_COUNT, oldUniqueValueCount, uniqueValueCount));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.indicators.impl.IndicatorImpl#storeSqlResults(java.lang.Object[])
     * 
     * ADDED scorreia 2008-04-30 storeSqlResults(List<Object[]> objects)
     */
    @Override
    public boolean storeSqlResults(List<Object[]> objects) {
        if (!checkResults(objects, 1)) {
            return false;
        }

        // MOD gdbu 2011-4-14 bug : 18975
        this.setUniqueValueCount(IndicatorHelper.getLongFromObject(objects.get(0)[0]));
        // ~18975

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.indicators.impl.IndicatorImpl#getIntegerValue()
     * 
     * ADDED scorreia 2008-05-12 getIntegerValue()
     */
    @Override
    public Long getIntegerValue() {
        return this.getUniqueValueCount();
    }

    @Override
    public boolean finalizeComputation() {
        uniqueObjects.removeAll(duplicateObjects);
        this.setUniqueValueCount(Long.valueOf(uniqueObjects.size()));
        return super.finalizeComputation();
    }

    @Override
    public boolean handle(Object data) {
        mustStoreRow = false;
        super.handle(data);
        if (data != null) {
            if (!this.uniqueObjects.add(data)) {
                // store duplicate objects
                duplicateObjects.add(data);
            } else {
                mustStoreRow = true;
            }
        }
        return true;
    }

    @Override
    public boolean reset() {
        this.uniqueValueCount = UNIQUE_VALUE_COUNT_EDEFAULT;
        this.uniqueObjects.clear();
        this.duplicateObjects.clear();
        return super.reset();
    }

} // UniqueCountIndicatorImpl
