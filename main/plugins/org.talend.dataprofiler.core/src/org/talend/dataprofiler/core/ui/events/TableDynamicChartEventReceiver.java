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
package org.talend.dataprofiler.core.ui.events;

import org.talend.dataprofiler.core.ui.editor.preview.model.ChartWithData;
import org.talend.dataprofiler.core.ui.editor.preview.model.states.WhereRuleStatisticsStateTable;
import org.talend.dataquality.indicators.Indicator;
import org.talend.dq.indicators.preview.table.ChartDataEntity;
import org.talend.dq.indicators.preview.table.WhereRuleChartDataEntity;
import org.talend.dq.nodes.indicator.type.IndicatorEnum;

/**
 * created by yyin on 2014-7-9 Detailled comment
 * 
 */
public class TableDynamicChartEventReceiver extends DynamicChartEventReceiver {

    @Override
    public boolean handle(Object value) {
        if (value == null) {
            return false;
        }
        if (IndicatorEnum.RowCountIndicatorEnum.equals(this.getIndicatorType())) {
            super.handle(value);
        } else {
            double valueD = Double.parseDouble(value.toString());
            double valueM = getIndicator().getCount() - valueD;
            if (this.getDataset() != null) {
                this.getDataset().setValue(valueM, WhereRuleStatisticsStateTable.ROW_KEY_NOT_PASS, this.getIndicatorName());
                this.getDataset().setValue(valueD, WhereRuleStatisticsStateTable.ROW_KEY_PASS, this.getIndicatorName());
            }
            if (this.getTableViewer() != null) {
                ChartWithData input = (ChartWithData) getTableViewer().getInput();
                if (input != null) {
                    ChartDataEntity[] dataEntities = input.getEnity();

                    ((WhereRuleChartDataEntity) dataEntities[this.getEntityIndex()]).setNumMatch(String.valueOf(valueD));
                    ((WhereRuleChartDataEntity) dataEntities[this.getEntityIndex()]).setNumNoMatch(String.valueOf(valueM));

                    getTableViewer().getTable().clearAll();
                    getTableViewer().setInput(input);
                }
            }
        }
        return true;
    }

    // every indicator need to be remembered in table analysis
    @Override
    public void setIndicator(Indicator indicator) {
        this.indicator = indicator;
    }
}
