/*
 * Copyright (C) 2006-2014 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package org.talend.dataquality.matchmerge.mfb;

import org.talend.dataquality.record.linkage.constant.AttributeMatcherType;

import java.util.ArrayList;
import java.util.List;

public class MatchResult {

    private final List<Score> scores;

    private final List<Float> thresholds;

    private double normalizedConfidence;

    private double minConfidenceValue;

    public MatchResult(int size) {
        scores = new ArrayList<Score>(size + 1);
        thresholds = new ArrayList<Float>(size + 1);
    }

    public static class Score {
        public AttributeMatcherType algorithm;
        public double score;
        public String[] values = new String[2];
    }

    public void setConfidence(double normalizedConfidence, double minConfidenceValue) {
        this.normalizedConfidence = normalizedConfidence;
        this.minConfidenceValue = minConfidenceValue;
    }

    public List<Score> getScores() {
        return scores;
    }

    public List<Float> getThresholds() {
        return thresholds;
    }

    public double getNormalizedConfidence() {
        return normalizedConfidence;
    }

    public double getMinConfidenceValue() {
        return minConfidenceValue;
    }

    public void setScore(int index, AttributeMatcherType algorithm, double score, String value1, String value2) {
        while (index >= scores.size()) {
            scores.add(new Score());
        }
        Score currentScore = scores.get(index);
        currentScore.algorithm = algorithm;
        currentScore.score = score;
        currentScore.values[0] = value1;
        currentScore.values[1] = value2;
    }

    public void setThreshold(int index, float threshold) {
        while (index >= thresholds.size()) {
            thresholds.add(0f);
        }
        thresholds.set(index, threshold);
    }

    public boolean isMatch() {
        int i = 0;
        for (Score score : scores) {
            if (score.score < thresholds.get(i++)) {
                return false;
            }
        }
        return true;
    }
}
