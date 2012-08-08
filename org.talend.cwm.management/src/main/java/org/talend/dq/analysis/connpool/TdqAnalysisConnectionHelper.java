package org.talend.dq.analysis.connpool;

import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.cwm.helper.SwitchHelpers;
import org.talend.dataquality.analysis.Analysis;
import org.talend.dq.analysis.connpool.TdqAnalysisConnectionPoolMap;
import org.talend.dq.helper.EObjectHelper;
import orgomg.cwm.foundation.softwaredeployment.DataManager;

/**
 * common methods for analysis connection.
 * @author msjian
 *
 */
public class TdqAnalysisConnectionHelper {

    /**
     * close the connection pool which belong to the analysis
     * (whether use pooled connection to execute the analysis or
     * not, can call this method safely).
     * 
     * @param ana
     */
    public static void closeConnectionPool(Analysis ana) {
        TdqAnalysisConnectionPoolMap instance = TdqAnalysisConnectionPoolMap.getInstance(ana);
        Connection analysisDataProvider = TdqAnalysisConnectionHelper.getAnalysisDataProvider(ana);
        if (analysisDataProvider != null) {
            instance.closePool(analysisDataProvider);
        }
        instance.closePools();
    }
    
    /**
     * get Analysis Data Provider.
     * @param analysis
     * @return Connection
     */
    public static Connection getAnalysisDataProvider(Analysis analysis) {
        DataManager datamanager = analysis.getContext().getConnection();
        if (datamanager != null && datamanager.eIsProxy()) {
            datamanager = (DataManager) EObjectHelper.resolveObject(datamanager);
        }
        return SwitchHelpers.CONNECTION_SWITCH.doSwitch(datamanager);
    }
}
