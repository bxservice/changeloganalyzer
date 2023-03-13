/**********************************************************************
* This file is part of iDempiere ERP Open Source                      *
* http://www.idempiere.org                                            *
*                                                                     *
* Copyright (C) Contributors                                          *
*                                                                     *
* This program is free software; you can redistribute it and/or       *
* modify it under the terms of the GNU General Public License         *
* as published by the Free Software Foundation; either version 2      *
* of the License, or (at your option) any later version.              *
*                                                                     *
* This program is distributed in the hope that it will be useful,     *
* but WITHOUT ANY WARRANTY; without even the implied warranty of      *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
* GNU General Public License for more details.                        *
*                                                                     *
* You should have received a copy of the GNU General Public License   *
* along with this program; if not, write to the Free Software         *
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
* MA 02110-1301, USA.                                                 *
*                                                                     *
* Contributors:                                                       *
* - Carlos Ruiz - globalqss - bxservice                               *
**********************************************************************/
package de.bxservice.process.changeloganalyzer;

import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * 	@author 	Carlos Ruiz - globalqss - bxservice
 */
@org.adempiere.base.annotation.Process
public class DisableChangeLog extends SvrProcess
{

	/* Disable Change Log for Table */
	private Boolean p_IsDisableTableChangeLog = null;
	/* Disable Change Log for Column */
	private Boolean p_IsDisableColumnChangeLog = null;
	/* Delete Change Log Records */
	private Boolean p_IsDeleteChangeLogRecords = null;	/**
	 *  Prepare - e.g., get Parameters.
	 */

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			switch (name) {
			case "IsDisableTableChangeLog":
				p_IsDisableTableChangeLog = para.getParameterAsBoolean();
				break;
			case "IsDisableColumnChangeLog":
				p_IsDisableColumnChangeLog = para.getParameterAsBoolean();
				break;
			case "IsDeleteChangeLogRecords":
				p_IsDeleteChangeLogRecords = para.getParameterAsBoolean();
				break;
			default:
				if (log.isLoggable(Level.INFO))
					log.log(Level.INFO, "Custom Parameter: " + name + "=" + para.getInfo());
				break;
			}
		}
	}

	/**
	 *  Perform process.
	 *  @return Message
	 *  @throws Exception
	 */
	protected String doIt() throws Exception
	{
		if (log.isLoggable(Level.INFO)) log.info("");

		List<List<Object>> viewIds = DB.getSQLArrayObjectsEx(get_TrxName(), "SELECT ViewID FROM T_Selection WHERE AD_PInstance_ID=? ORDER BY ViewID", getAD_PInstance_ID());
		int cnt = 0;
		for (List<Object> viewId : viewIds) {
			for (Object tabColId : viewId) {
				String[] arr = tabColId.toString().split("\\|"); 
				int tabId = Integer.valueOf(arr[0]);
				int colId = Integer.valueOf(arr[1]);
				MTable table = MTable.getCopy(getCtx(), tabId, get_TrxName());
				if (p_IsDisableTableChangeLog && table.isChangeLog()) {
					table.setIsChangeLog(false);
					table.saveEx();
				}
				if (colId > 0) {
					MColumn column = MColumn.getCopy(getCtx(), colId, get_TrxName());
					if (p_IsDisableColumnChangeLog && column.isAllowLogging()) {
						column.setIsAllowLogging(false);
						column.saveEx();
					}
				}
				if (p_IsDeleteChangeLogRecords) {
					StringBuilder delete = new StringBuilder("DELETE FROM AD_ChangeLog WHERE AD_Table_ID=?");
					Object[] params = null;
					if (colId > 0) {
						delete.append(" AND AD_Column_ID=?");
						params = new Object[] {tabId, colId};
					} else {
						params = new Object[] {tabId};
					}
					cnt += DB.executeUpdateEx(delete.toString(), params, get_TrxName());
				}
			}
		}

		return "@OK@ " + cnt;
	}

}	//	DisableChangeLog
