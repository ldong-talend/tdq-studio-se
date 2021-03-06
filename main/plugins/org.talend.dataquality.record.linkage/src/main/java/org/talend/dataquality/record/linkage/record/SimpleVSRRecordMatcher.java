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
package org.talend.dataquality.record.linkage.record;

import org.talend.dataquality.record.linkage.attribute.IAttributeMatcher;

/**
 * @author scorreia
 * 
 * This class does not use the group of attributes to match. It implements a kind of Vector Space Retrieval method (VSR)
 * to compare the records, but no weight is computed depending on the frequency of the data.
 */
public class SimpleVSRRecordMatcher extends AbstractRecordMatcher {

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.matching.record.IRecordMatcher#getMatchingWeight(java.lang.String[],
     * java.lang.String[])
     */
    public double getMatchingWeight(String[] record1, String[] record2) {
        double result = 0;
        // first loop on blocking variables indices
        if (blockedIndices != null) {
            for (int i = 0; i < super.blockedIndices.length; i++) {
                int blockedIdx = blockedIndices[i];
                double pa = computeMatchingWeight(blockedIdx, record1, record2);
                if (pa < super.blockingThreshold) {
                    return 0; // we break the computation here because it is considered as a non-match.
                }
                // else compute weight
                result += pa * attributeWeights[blockedIdx];
            }
        }

        // loop on other indices
        for (int j = 0; j < getUsedIndicesNotblocked().length; j++) {
            int usedIdx = usedIndicesNotblocked[j];
            double pa = computeMatchingWeight(usedIdx, record1, record2);
            result += pa * attributeWeights[usedIdx];
        }

        return result;
    }

    private double computeMatchingWeight(int blockedIdx, String[] record1, String[] record2) {
        final IAttributeMatcher match = attributeMatchers[blockedIdx];
        double pa = match.getMatchingWeight(record1[blockedIdx], record2[blockedIdx]);
        // store matching weight of this attribute
        this.attributeMatchingWeights[blockedIdx] = pa;
        return pa;
    }

}
