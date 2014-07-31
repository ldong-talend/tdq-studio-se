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
package org.talend.dataquality.record.linkage.grouping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;
import org.talend.dataquality.record.linkage.attribute.AttributeMatcherFactory;
import org.talend.dataquality.record.linkage.attribute.IAttributeMatcher;
import org.talend.dataquality.record.linkage.constant.AttributeMatcherType;
import org.talend.dataquality.record.linkage.constant.RecordMatcherType;
import org.talend.dataquality.record.linkage.record.CombinedRecordMatcher;
import org.talend.dataquality.record.linkage.record.IRecordMatcher;
import org.talend.dataquality.record.linkage.record.RecordMatcherFactory;
import org.talend.dataquality.record.linkage.utils.CustomAttributeMatcherClassNameConvert;
import org.talend.utils.classloader.TalendURLClassLoader;

/**
 * created by zhao on Jul 19, 2013 <br>
 * Abstract record grouping implementation without hadoop API included.
 * 
 */
public abstract class AbstractRecordGrouping<TYPE> implements IRecordGrouping<TYPE> {

    private List<TYPE[]> masterRecords = new ArrayList<TYPE[]>();

    public static final float DEFAULT_THRESHOLD = 0.95f;

    private float acceptableThreshold = DEFAULT_THRESHOLD;

    // Output distance details or not.
    private boolean isOutputDistDetails = Boolean.FALSE;

    // Allow seperate output give group score and the confidence threshold.
    private boolean isSeperateOutput = Boolean.FALSE;

    private CombinedRecordMatcher combinedRecordMatcher = RecordMatcherFactory.createCombinedRecordMatcher();

    /**
     * @deprecated use {{@link #multiMatchRules}
     */
    @Deprecated
    private List<Map<String, String>> matchingColumns = new ArrayList<Map<String, String>>();

    private List<List<Map<String, String>>> multiMatchRules = new ArrayList<List<Map<String, String>>>();

    protected String columnDelimiter = null;

    private Boolean isLinkToPrevious = Boolean.FALSE;

    private int originalInputColumnSize;

    private Boolean isDisplayAttLabels = Boolean.TRUE;

    private Boolean isGIDStringType = Boolean.TRUE;

    // old tMatchGroup GID using Long type
    AtomicLong atomicLongGID = new AtomicLong();

    // The exthended column size.
    int extSize;

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.hadoop.group.IRecordGrouping#setColumnDelimiter(java.lang.String)
     */
    @Override
    public void setColumnDelimiter(String columnDelimiter) {
        this.columnDelimiter = columnDelimiter;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.hadoop.group.IRecordGrouping#SetIsLinkToPrevious(java.lang.Boolean)
     */
    @Override
    public void setIsLinkToPrevious(Boolean isLinkToPrevious) {
        this.isLinkToPrevious = isLinkToPrevious;
    }

    /**
     * @deprecated {@link IRecordMatcher#setRecordMatchThreshold(double)}
     */
    @Deprecated
    @Override
    public void setAcceptableThreshold(float acceptableThreshold) {
        this.acceptableThreshold = acceptableThreshold;
    }

    @Override
    public void setIsOutputDistDetails(boolean isOutputDistDetails) {
        this.isOutputDistDetails = isOutputDistDetails;
    }

    /**
     * Set original input column size.except GID,MASTER,SCORE,GRP_SIZE,GRP_QUALITY,MATCHING_DISTANCES.
     * 
     * @param prevOrginalColumnSize the prevOrginalColumnSize to set
     */
    public void setOrginalInputColumnSize(int originalInputColumnSize) {
        this.originalInputColumnSize = originalInputColumnSize;
    }

    /**
     * Sets the isSeperateOutput.
     * 
     * @param isSeperateOutput the isSeperateOutput to set
     */
    @Override
    public void setSeperateOutput(boolean isSeperateOutput) {
        this.isSeperateOutput = isSeperateOutput;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.record.linkage.grouping.IRecordGrouping#setIsDisplayAttLabels(java.lang.Boolean)
     */
    @Override
    public void setIsDisplayAttLabels(Boolean isDisplayAttLabels) {
        this.isDisplayAttLabels = isDisplayAttLabels;

    }

    /*
     * (non-Javadoc)
     * 
     * Set GID data type. if it is import form old version,the data type is Long. or else it is String .
     */
    public void setIsGIDStringType(Boolean isGIDStringType) {
        this.isGIDStringType = isGIDStringType;

    }

    @Override
    public void doGroup(TYPE[] inputRow) throws IOException, InterruptedException {

        extSize = isOutputDistDetails ? 5 : 4;
        // extSize + 1 when isSeperateOutput is enabled.
        extSize = isSeperateOutput ? extSize + 1 : extSize;

        // if the inputRow size less than original column size,should set 'isLinkToPrevious' to false and work on
        // none-multi-pass.
        if (isLinkToPrevious && inputRow.length <= originalInputColumnSize) {
            isLinkToPrevious = false;
        }
        // In case of current component is linked to previous, and the record is NOT master, just put it to the output
        // and continue;
        if (isLinkToPrevious && !isMaster(inputRow[originalInputColumnSize + 2])) {
            TYPE[] inputRowWithExtColumns = createNewInputRowForMultPass(inputRow, originalInputColumnSize + extSize);
            outputRow(inputRowWithExtColumns);
            return;
        }

        if (multiMatchRules.size() == 0) {
            // No rule defined.
            return;
        }
        // temporary array to store attributes to match
        List<Map<String, String>> matchingRule = multiMatchRules.get(0);
        String[] lookupDataArray = new String[matchingRule.size()];

        for (int idx = 0; idx < lookupDataArray.length; idx++) {
            lookupDataArray[idx] = String.valueOf(inputRow[Integer
                    .parseInt(matchingRule.get(idx).get(IRecordGrouping.COLUMN_IDX))]);
        }
        boolean isSimilar = false;
        for (TYPE[] masterRecord : masterRecords) {
            if (isLinkToPrevious) {
                int masterGRPSize = Integer.valueOf(String.valueOf(masterRecord[originalInputColumnSize + 1]));
                int inputGRPSize = Integer.valueOf(String.valueOf(inputRow[originalInputColumnSize + 1]));
                // Don't compare the records whose GRP_SIZE both > 1.
                if (masterGRPSize > 1 && inputGRPSize > 1) {
                    continue;
                }
            }

            String[] masterMatchRecord = new String[lookupDataArray.length];
            // Find the match record from master record.
            for (int idx = 0; idx < lookupDataArray.length; idx++) {
                masterMatchRecord[idx] = String.valueOf(masterRecord[Integer.parseInt(matchingRule.get(idx).get(
                        IRecordGrouping.COLUMN_IDX))]);
            }
            double matchingProba = combinedRecordMatcher.getMatchingWeight(masterMatchRecord, lookupDataArray);
            // Similar
            if (matchingProba >= combinedRecordMatcher.getRecordMatchThreshold()) {
                String distanceDetails = computeOutputDetails();
                isSimilar = true;
                // Master GRP_SIZE ++
                if (isLinkToPrevious) {
                    int masterGRPSize = Integer.valueOf(String.valueOf(masterRecord[originalInputColumnSize + 1]));
                    if (masterGRPSize == 1) {
                        inputRow[originalInputColumnSize + 1] = modifyGroupSize(inputRow[originalInputColumnSize + 1]);
                        TYPE[] inputRowWithExtColumns = createNewInputRowForMultPass(inputRow, originalInputColumnSize + extSize);
                        // since the 'masterRecord' will be output as a duplicate,need to set masterRecord GRP_QUALITY
                        // to 'inputRow_with_extColumns'. then compare it with 'matchingProba' later.
                        if (isSeperateOutput) {
                            inputRowWithExtColumns[originalInputColumnSize + 4] = masterRecord[originalInputColumnSize + 4];
                        }
                        updateWithExtendedColumn(masterRecord, inputRowWithExtColumns, matchingProba, distanceDetails,
                                columnDelimiter);
                        // Update master record from the temporary master list.
                        masterRecords.remove(masterRecord);
                        masterRecords.add(inputRowWithExtColumns);
                        break;
                    }
                }
                masterRecord[masterRecord.length - extSize + 1] = modifyGroupSize(masterRecord[masterRecord.length - extSize + 1]);
                // Duplicated record
                updateWithExtendedColumn(inputRow, masterRecord, matchingProba, distanceDetails, columnDelimiter);
                break;
            }

        }
        if (!isSimilar) {
            // For the passes that linked to previous, the extension size (e.g: GID,GRP_SIZE) had already been set
            // before.

            // Master record
            int inputColumnLenth = isLinkToPrevious ? originalInputColumnSize : inputRow.length;
            TYPE[] masterRow = createTYPEArray(inputColumnLenth + extSize);
            for (int idx = 0; idx < inputRow.length; idx++) {
                masterRow[idx] = inputRow[idx];
            }
            int extIdx = 0;
            if (!isLinkToPrevious) {
                // GID
                if (this.isGIDStringType) {
                    masterRow[masterRow.length - extSize] = getTYPEFromObject(UUID.randomUUID().toString());
                } else {
                    masterRow[masterRow.length - extSize] = getTYPEFromObject(atomicLongGID.incrementAndGet());
                }
                // Group size
                masterRow[masterRow.length - extSize + 1] = getTYPEFromObject(1);
                // Master
                masterRow[masterRow.length - extSize + 2] = getTYPEFromObject(true);
                // Score
                masterRow[masterRow.length - extSize + 3] = getTYPEFromObject(1.0);

            }
            if (isSeperateOutput) {
                // Group quality for multiple pass
                extIdx++;
                masterRow[inputColumnLenth + 4] = getTYPEFromObject(1.0);
            }
            if (isOutputDistDetails) {
                // Match distance details for multiple pass
                masterRow[inputColumnLenth + 4 + extIdx] = getTYPEFromObject(EMPTY_STR);
            }

            masterRecords.add(masterRow);
        }
    }

    /**
     * This method is for 2nd tMatchGroup in multi-pass only.add the external columns into input schema.
     * 
     * @param inputRow the output of 1st tMatchGroup. Then will be the input of 2nd tMatchGroup.
     * @param extSizeWithLinkPrev
     * @return
     */
    private TYPE[] createNewInputRowForMultPass(TYPE[] inputRow, int newLength) {
        TYPE[] inputRowWithExtColumn = createTYPEArray(newLength);
        for (int idx = 0; idx < inputRow.length; idx++) {
            inputRowWithExtColumn[idx] = inputRow[idx];
        }

        TYPE prevDistanceTMP = null;
        // In case of multi-pass, the value of index "originalInputColumnSize + 4" is the 1st tMatchGroup's distance
        // details. Note no seperate output for 1st tMatchGroup in multi-pass.
        if (extSize > 4 && inputRowWithExtColumn[originalInputColumnSize + 4] != null) {
            prevDistanceTMP = inputRowWithExtColumn[originalInputColumnSize + 4];
        }
        int extInd = 0;
        if (isSeperateOutput) {
            inputRowWithExtColumn[originalInputColumnSize + 4] = getTYPEFromObject(0.0);
            extInd++;
        }
        if (isOutputDistDetails) {
            // propagate previous Match distance to next when the previous distance is not null.
            if (prevDistanceTMP != null) {
                inputRowWithExtColumn[originalInputColumnSize + 4 + extInd] = prevDistanceTMP;
            } else {
                inputRowWithExtColumn[originalInputColumnSize + 4 + extInd] = getTYPEFromObject(EMPTY_STR);
            }
        }
        return inputRowWithExtColumn;
    }

    /**
     * DOC zhao Comment method "computeGroupQuality".
     * 
     * @param matchingProba
     * @return
     */
    private double computeGroupQuality(TYPE[] masterRecord, double matchingProba, int idx) {
        double groupQuality = Double.valueOf(String.valueOf(masterRecord[masterRecord.length - extSize + idx]));
        if (matchingProba < groupQuality) {
            // Use the minimal match distance as the group score.
            groupQuality = matchingProba;
        }
        return groupQuality;
    }

    /**
     * DOC zhao Comment method "computeOutputDetails".
     * 
     * @param distanceDetails
     * @return
     */
    private String computeOutputDetails() {
        String distanceDetails = StringUtils.EMPTY;
        if (isOutputDistDetails) {
            combinedRecordMatcher.setDisplayLabels(isDisplayAttLabels);
            distanceDetails = combinedRecordMatcher.getLabeledAttributeMatchWeights();
        }
        return distanceDetails;
    }

    @Override
    public void end() throws IOException, InterruptedException {
        // output the masters
        for (TYPE[] mst : masterRecords) {
            outputRow(mst);
        }
    }

    private void updateWithExtendedColumn(TYPE[] inputRow, TYPE[] masterRecord, double matchingProba, String distanceDetails,
            String delimiter) throws IOException, InterruptedException {
        // String[] duplicateRecord = new String[masterRecord.length];
        TYPE[] duplicateRecord = createTYPEArray(masterRecord.length);
        for (int idx = 0; idx < inputRow.length; idx++) {
            duplicateRecord[idx] = inputRow[idx];
        }
        // GID
        duplicateRecord[duplicateRecord.length - extSize] = masterRecord[masterRecord.length - extSize];
        // Group size
        duplicateRecord[duplicateRecord.length - extSize + 1] = getTYPEFromObject(0);
        // Master
        duplicateRecord[duplicateRecord.length - extSize + 2] = getTYPEFromObject(false);
        // Score
        duplicateRecord[duplicateRecord.length - extSize + 3] = getTYPEFromObject(matchingProba);

        int extIdx = 3;
        // Group quality
        if (isSeperateOutput) {
            extIdx++;
            double groupQuality = computeGroupQuality(masterRecord, matchingProba, extIdx);
            masterRecord[duplicateRecord.length - extSize + extIdx] = getTYPEFromObject(groupQuality);
            // change the duplicate group quality to 0.0 .
            duplicateRecord[duplicateRecord.length - extSize + extIdx] = getTYPEFromObject(0.0);
        }
        if (isOutputDistDetails) {
            extIdx++;
            // Match distance details
            duplicateRecord[duplicateRecord.length - extSize + extIdx] = getTYPEFromObject(distanceDetails);
        }
        // output the duplicate record
        outputRow(duplicateRecord);
    }

    /**
     * @deprecated use {{@link #addMatchRule}
     */
    @Deprecated
    @Override
    public void add(Map<String, String> matchingColumn) {
        matchingColumns.add(matchingColumn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.hadoop.group.IRecordGrouping#addMatchRule(java.util.List)
     */
    @Override
    public void addMatchRule(List<Map<String, String>> matchRule) {
        multiMatchRules.add(matchRule);
    }

    public List<List<Map<String, String>>> getMultiMatchRules() {
        return multiMatchRules;
    }

    /**
     * 
     * Output one row
     * 
     * @param row
     */
    protected abstract void outputRow(TYPE[] row);

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.dataquality.hadoop.group.IRecordGrouping#initialize()
     */
    @Override
    public void initialize() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        masterRecords.clear();
        for (List<Map<String, String>> matchRule : multiMatchRules) {
            createSimpleRecordMatcher(matchRule);
        }
    }

    private void createSimpleRecordMatcher(List<Map<String, String>> matchRule) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        final int recordSize = matchRule.size();
        double[] arrAttrWeights = new double[recordSize];
        String[] attributeNames = new String[recordSize];
        String[][] algorithmName = new String[recordSize][2];
        String[] arrMatchHandleNull = new String[recordSize];
        String[] customizedJarPath = new String[recordSize];
        double recordMatchThreshold = acceptableThreshold;// keep compatibility to older version.
        int recordIdx = 0;
        for (Map<String, String> recordMap : matchRule) {
            algorithmName[recordIdx][0] = recordMap.get(IRecordGrouping.MATCHING_TYPE);
            if (AttributeMatcherType.DUMMY.name().equals(algorithmName[recordIdx][0])) {
                recordIdx++;
                continue;
            }
            arrAttrWeights[recordIdx] = Double.parseDouble(recordMap.get(IRecordGrouping.CONFIDENCE_WEIGHT));
            algorithmName[recordIdx][1] = recordMap.get(IRecordGrouping.CUSTOMER_MATCH_CLASS);
            attributeNames[recordIdx] = recordMap.get(IRecordGrouping.ATTRIBUTE_NAME);
            arrMatchHandleNull[recordIdx] = recordMap.get(IRecordGrouping.HANDLE_NULL);
            String rcdMathThresholdEach = recordMap.get(IRecordGrouping.RECORD_MATCH_THRESHOLD);
            customizedJarPath[recordIdx] = recordMap.get(IRecordGrouping.JAR_PATH);
            if (!StringUtils.isEmpty(rcdMathThresholdEach)) {
                recordMatchThreshold = Double.valueOf(rcdMathThresholdEach);

            }
            recordIdx++;
        }
        IAttributeMatcher[] attributeMatcher = new IAttributeMatcher[recordSize];

        for (int indx = 0; indx < recordSize; indx++) {
            AttributeMatcherType attrMatcherType = AttributeMatcherType.get(algorithmName[indx][0]);

            if (attrMatcherType == AttributeMatcherType.CUSTOM && customizedJarPath[indx] != null) {
                // Put the jar into class path so that the class can be loaded.
                TalendURLClassLoader cl = new TalendURLClassLoader(
                        CustomAttributeMatcherClassNameConvert.changeJarPathToURLArray(customizedJarPath[indx]));
                attributeMatcher[indx] = AttributeMatcherFactory.createMatcher(attrMatcherType,
                        CustomAttributeMatcherClassNameConvert.getClassName(algorithmName[indx][1]), cl);
            } else {
                // Use the default class loader to load the class.
                attributeMatcher[indx] = AttributeMatcherFactory.createMatcher(attrMatcherType, algorithmName[indx][1]);
            }
            attributeMatcher[indx].setNullOption(arrMatchHandleNull[indx]);
            attributeMatcher[indx].setAttributeName(attributeNames[indx]);

        }
        IRecordMatcher simpleRecordMatcher = RecordMatcherFactory.createMatcher(RecordMatcherType.simpleVSRMatcher);
        simpleRecordMatcher.setRecordSize(recordSize);
        simpleRecordMatcher.setAttributeWeights(arrAttrWeights);
        simpleRecordMatcher.setAttributeMatchers(attributeMatcher);
        simpleRecordMatcher.setRecordMatchThreshold(recordMatchThreshold);
        combinedRecordMatcher.add(simpleRecordMatcher);
    }

    protected abstract boolean isMaster(TYPE col);

    protected abstract TYPE modifyGroupSize(TYPE oldGroupSize);

    protected abstract TYPE[] createTYPEArray(int size);

    protected abstract TYPE getTYPEFromObject(Object objectValue);

}
