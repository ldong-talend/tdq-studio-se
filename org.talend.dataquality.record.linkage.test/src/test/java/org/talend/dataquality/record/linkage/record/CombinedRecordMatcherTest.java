// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.talend.dataquality.record.linkage.constant.RecordMatcherType;

/**
 * created by scorreia on Jan 17, 2013 Detailled comment
 * 
 */
public class CombinedRecordMatcherTest {

    private static final boolean DEBUG = false;

    private static final String[][] MAINRECORDS = { { "seb", "talend", "suresnes", "data not used in record matching" },
            { "seb", "talend", "suresns", null }, { "seb", "tlend", "sursnes", null }, { "sebas", "taland", "suresnes", null },
            { "seb", "tlend", "sursnes", null }, { "sebas", "taland", "suresnes", null },
            { "babass", "Atlend", "sursene", null }, { "Sebastião", "talènd", "Suresnes", "comment" }, };

    // the algorithms selected by the user for each of the 3 match keys
    private static final String[] ATTRIBUTEMATCHERALGORITHMS = { "Exact", "Double Metaphone", "Levenshtein", "soundex" };

    // the weights given by the user to each of the 3 match key.
    private static final double[] ATTRIBUTEWEIGHTS_1 = { 30, 10, 0, 0 };

    private static final double[] ATTRIBUTEWEIGHTS_2 = { 1, 1, 30, 0 };

    /**
     * Test method for
     * {@link org.talend.dataquality.record.linkage.record.CombinedRecordMatcher#getMatchingWeight(java.lang.String[], java.lang.String[])}
     * .
     */
    @Test
    public void testGetMatchingWeight() {
        // create a first matcher
        IRecordMatcher recMatcher1 = RecordMatcherFactory.createMatcher(RecordMatcherType.simpleVSRMatcher,
                ATTRIBUTEMATCHERALGORITHMS, ATTRIBUTEWEIGHTS_1);
        List<List<Double>> matcherWeigths = computeWeights(recMatcher1);

        // check that the combined matcher with only one matcher is identical to the matcher
        CombinedRecordMatcher combMatcher1 = RecordMatcherFactory.createCombinedRecordMatcher();
        Assert.assertTrue(combMatcher1.add(recMatcher1));
        List<List<Double>> combinedMatcherWeigths = computeWeights(combMatcher1);
        System.out.println(combMatcher1);
        compare(matcherWeigths, combinedMatcherWeigths);
        int nbMatch1 = countMatches(matcherWeigths);

        // create a second simple matcher and do the same
        IRecordMatcher recMatcher2 = RecordMatcherFactory.createMatcher(RecordMatcherType.simpleVSRMatcher,
                ATTRIBUTEMATCHERALGORITHMS, ATTRIBUTEWEIGHTS_2);
        matcherWeigths = computeWeights(recMatcher2);

        CombinedRecordMatcher combMatcher2 = RecordMatcherFactory.createCombinedRecordMatcher();
        Assert.assertTrue(combMatcher2.add(recMatcher2));
        System.out.println(combMatcher2);
        compare(matcherWeigths, computeWeights(combMatcher2));

        // compare the number of matches for each matcher
        int nbMatch2 = countMatches(matcherWeigths);
        Assert.assertNotSame("The two matchers should not have the same match count", nbMatch1, nbMatch2);

        // create a third simple matcher and do the same
        IRecordMatcher recMatcher3 = RecordMatcherFactory.createMatcher(RecordMatcherType.simpleVSRMatcher, new String[] {
                "dummy", "dummy", "dummy", "exact" }, new double[] { 0, 0, 0, 1 });
        matcherWeigths = computeWeights(recMatcher3);

        CombinedRecordMatcher combMatcher3 = RecordMatcherFactory.createCombinedRecordMatcher();
        Assert.assertTrue(combMatcher3.add(recMatcher3));
        System.out.println(combMatcher3);
        compare(matcherWeigths, computeWeights(combMatcher3));

        // compare the number of matches for each matcher
        int nbMatch3 = countMatches(matcherWeigths);
        Assert.assertNotSame("The two matchers should not have the same match count. " + nbMatch1, nbMatch1, nbMatch3);
        Assert.assertNotSame("The two matchers should not have the same match count. " + nbMatch2, nbMatch2, nbMatch3);

        // combine them
        CombinedRecordMatcher combMatcher = RecordMatcherFactory.createCombinedRecordMatcher();
        Assert.assertTrue(combMatcher.add(recMatcher1));
        Assert.assertTrue(combMatcher.add(recMatcher2));
        Assert.assertTrue(combMatcher.add(recMatcher3));
        System.out.println(combMatcher);

        int nbMatch = countMatches(combMatcher);
        Assert.assertEquals(true, nbMatch >= nbMatch1);
        Assert.assertEquals(true, nbMatch >= nbMatch2);
        Assert.assertEquals(true, nbMatch >= nbMatch3);

        // Test order of matcher. Results should be different.
        // combine them
        CombinedRecordMatcher reverseCombMatcher = RecordMatcherFactory.createCombinedRecordMatcher();
        Assert.assertTrue(reverseCombMatcher.add(recMatcher3));
        Assert.assertTrue(reverseCombMatcher.add(recMatcher2));
        Assert.assertTrue(reverseCombMatcher.add(recMatcher1));
        System.out.println(reverseCombMatcher);

        int nbMatchReverse = countMatches(combMatcher);
        Assert.assertEquals("The order of matcher should not change the number of matches", nbMatch, nbMatchReverse);

    }

    /**
     * DOC scorreia Comment method "compare".
     * 
     * @param m1
     * @param m2
     */
    private void compare(List<List<Double>> m1, List<List<Double>> m2) {
        for (int i = 0; i < m1.size(); i++) {
            List<Double> mi1 = m1.get(i);
            for (int j = 0; j < mi1.size(); j++) {
                Assert.assertEquals(mi1.get(j), m2.get(i).get(j));
            }
        }

    }

    private int countMatches(IRecordMatcher combMatcher) {
        List<List<Double>> allWeights = computeWeights(combMatcher);
        return countMatches(allWeights);
    }

    private int countMatches(List<List<Double>> allWeights) {
        // count number of matches
        final double MATCH_THRESHOLD = 0.9;
        int nbMatch = 0;
        for (int i = 0; i < allWeights.size(); i++) {
            List<Double> list = allWeights.get(i);
            for (int j = 0; j < list.size(); j++) {
                Double value = list.get(j);
                if (value > MATCH_THRESHOLD) {
                    nbMatch++;
                }

                System.out.print(NumberFormat.getNumberInstance().format(value));
                System.out.print("\t");
                if (i == j) {
                    Assert.assertEquals("Diagonal weights should be one", 1.0d, value);
                }
            }
            System.out.println();
        }
        System.out.println("nb match = " + nbMatch + " over " + MAINRECORDS.length * MAINRECORDS.length + " comparisons");
        System.out.println();
        return nbMatch;
    }

    private List<List<Double>> computeWeights(IRecordMatcher combMatcher) {
        // compare all records together
        List<List<Double>> allWeights = new ArrayList<List<Double>>();
        for (String[] record1 : MAINRECORDS) {
            List<Double> allWeightInRow = new ArrayList<Double>();
            for (String[] record2 : MAINRECORDS) {
                double matchingWeight = combMatcher.getMatchingWeight(record1, record2);
                allWeightInRow.add(matchingWeight);

                if (DEBUG) {
                    // print details
                    StringBuffer buf = new StringBuffer();
                    buf.append(SimpleVSRRecordMatcherTest.printRecord(record1));
                    buf.append(" ~ ");
                    buf.append(SimpleVSRRecordMatcherTest.printRecord(record2));
                    buf.append(" ; \tweight= " + matchingWeight);
                    System.out.println(buf.toString());
                }
            }
            allWeights.add(allWeightInRow);
            if (DEBUG) {
                System.out.println();
            }
        }
        return allWeights;
    }

    /**
     * Test method for
     * {@link org.talend.dataquality.record.linkage.record.CombinedRecordMatcher#add(org.talend.dataquality.record.linkage.record.IRecordMatcher)}
     * .
     */
    @Test
    public void testAdd() {
        // create a first matcher
        IRecordMatcher recMatcher1 = RecordMatcherFactory.createMatcher(RecordMatcherType.simpleVSRMatcher,
                ATTRIBUTEMATCHERALGORITHMS, ATTRIBUTEWEIGHTS_1);

        // check that the combined matcher with only one matcher is identical to the matcher
        CombinedRecordMatcher combMatcher1 = RecordMatcherFactory.createCombinedRecordMatcher();
        Assert.assertTrue(combMatcher1.add(recMatcher1));

        IRecordMatcher recMatcher2 = RecordMatcherFactory.createMatcher(RecordMatcherType.simpleVSRMatcher,
                ATTRIBUTEMATCHERALGORITHMS, new double[] { 1.0, 3.0 });
        Assert.assertNull("cannot create a matcher like this", recMatcher2);
        recMatcher2 = RecordMatcherFactory.createMatcher(RecordMatcherType.simpleVSRMatcher, new String[] { "exact", "exact" },
                new double[] { 1.0, 3.0 });
        Assert.assertFalse("cannot add a matcher with a different size", combMatcher1.add(recMatcher2));

        recMatcher2 = RecordMatcherFactory.createMatcher(RecordMatcherType.simpleVSRMatcher, new String[] { "exact", "exact",
                "exact", "exact" }, ATTRIBUTEWEIGHTS_1);
        Assert.assertTrue(combMatcher1.add(recMatcher2));

    }

    /**
     * Test method for
     * {@link org.talend.dataquality.record.linkage.record.AbstractRecordMatcher#setAttributeMatchers(org.talend.dataquality.record.linkage.attribute.IAttributeMatcher[])}
     * .
     */
    @Test
    public void testSetAttributeMatchers() {
        for (RecordMatcherType type : RecordMatcherType.values()) {
            IRecordMatcher recMatcher = RecordMatcherFactory.createMatcher(type, ATTRIBUTEMATCHERALGORITHMS, ATTRIBUTEWEIGHTS_1);
            checkAttributeMatching(1.0d, recMatcher);
            Assert.assertTrue(recMatcher.setAttributeWeights(new double[] { 3, 2, 0, 3 }));
            CombinedRecordMatcher combMatcher = RecordMatcherFactory.createCombinedRecordMatcher();
            checkAttributeMatching(0.0d, combMatcher);
            Assert.assertFalse("the attribute weights of a combined matcher cannot be modified",
                    combMatcher.setAttributeWeights(new double[] { 3, 2, 0, 3 }));
            combMatcher.add(recMatcher);
            Assert.assertFalse("the attribute weights of a combined matcher cannot be modified",
                    combMatcher.setAttributeWeights(new double[] { 3, 2, 0, 3 }));
        }

    }

    private void checkAttributeMatching(double expectedValue, IRecordMatcher recMatcher) {
        Assert.assertFalse(recMatcher.setAttributeWeights(new double[] { 3, 2, 0 }));

        String[][] records = { { "a", "a", "a", "a" }, { "a", "a", null, "a" }, { "a", "a", "", "a" }, { "a", "a", "b", "a" } };
        for (String[] rec1 : records) {
            for (String[] rec2 : records) {
                double matchingWeight = recMatcher.getMatchingWeight(rec1, rec2);
                Assert.assertEquals(expectedValue, matchingWeight);
            }
        }
    }

}