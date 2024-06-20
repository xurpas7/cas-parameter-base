/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.cas.parameter.base;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.rmj.appdriver.GCrypt;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.iface.GRecord;
import org.rmj.cas.parameter.pojo.UnitCPFinancer;

/**
 *
 * @author jovanalic
 * since 2021-07-12
 */
public class CP_Financer implements GRecord{

    @Override
    public Object newRecord() {
        UnitCPFinancer loObject = new UnitCPFinancer();
        
        Connection loConn = null;
        loConn = setConnection();       
        
        //assign the primary values
        loObject.setsFnancrID(MiscUtil.getNextCode(loObject.getTable(), "sFnancrID", false, loConn, psBranchCd));
        
        return loObject;
    }

    @Override
    public Object openRecord(String fstransNox) {
        UnitCPFinancer loObject = new UnitCPFinancer();
        
        com.mysql.jdbc.Connection loConn = null;
        loConn = setConnection();   
        
        String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sFnancrID = " + SQLUtil.toSQL(fstransNox));
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        try {
            if (!loRS.next()){
                setMessage("No Record Found");
            }else{
                //load each column to the entity
                for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                    loObject.setValue(lnCol, loRS.getObject(lnCol));
                }
            }              
        } catch (SQLException ex) {
            setErrMsg(ex.getMessage());
        } finally{
            MiscUtil.close(loRS);
            if (!pbWithParent) MiscUtil.close(loConn);
        }
        
        return loObject;
    }

    @Override
    public Object saveRecord(Object foEntity, String fsTransNox) {
        String lsSQL = "";
        UnitCPFinancer loOldEnt = null;
        UnitCPFinancer loNewEnt = null;
        UnitCPFinancer loResult = null;
        
        // Check for the value of foEntity
        if (!(foEntity instanceof UnitCPFinancer)) {
            setErrMsg("Invalid Entity Passed as Parameter");
            return loResult;
        }
        
        // Typecast the Entity to this object
        loNewEnt = (UnitCPFinancer) foEntity;
        
        
        // Test if entry is ok
        if (loNewEnt.getsCompnyNm()== null || loNewEnt.getsCompnyNm().isEmpty()){
            setMessage("UNSET Company Name detected.");
            return loResult;
        }
        

        loNewEnt.setsModified(poCrypt.encrypt(psUserIDxx));
        loNewEnt.setdModified(poGRider.getServerDate());
        
        // Generate the SQL Statement
        if (fsTransNox.equals("")){
            com.mysql.jdbc.Connection loConn = null;
            loConn = setConnection();   
            
            loNewEnt.setsFnancrID(MiscUtil.getNextCode(loNewEnt.getTable(), "sFnancrID", false, loConn, psBranchCd));
            
            if (!pbWithParent) MiscUtil.close(loConn);
            
            //Generate the SQL Statement
            lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);
        }else{
            //Load previous transaction
            loOldEnt = (UnitCPFinancer) openRecord(fsTransNox);
            
            //Generate the Update Statement
            lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt, (GEntity) loOldEnt, "sFnancrID = " + SQLUtil.toSQL(loNewEnt.getValue(1)));
        }
        
        //No changes have been made
        if (lsSQL.equals("")){
            setMessage("Record is not updated");
            return loResult;
        }
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if(poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0){
            if(!poGRider.getErrMsg().isEmpty())
                setErrMsg(poGRider.getErrMsg());
            else
            setMessage("No record updated");
        } else loResult = loNewEnt;
        
        if (!pbWithParent) {
            if (!getErrMsg().isEmpty()){
                poGRider.rollbackTrans();
            } else poGRider.commitTrans();
        }        
        
        return loResult;
    }

    @Override
    public boolean deleteRecord(String fsTransNox) {
        UnitCPFinancer loObject = (UnitCPFinancer) openRecord(fsTransNox);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        String lsSQL = "DELETE FROM " + loObject.getTable() + 
                        " WHERE sBrandCde = " + SQLUtil.toSQL(fsTransNox);
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = true;
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        
        return lbResult;
    }

    @Override
    public boolean deactivateRecord(String fsTransNox) {
        UnitCPFinancer loObject = (UnitCPFinancer) openRecord(fsTransNox);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        if (loObject.getcRecdStat().equalsIgnoreCase(RecordStatus.INACTIVE)){
            setMessage("Current record is inactive...");
            return lbResult;
        }
        
        String lsSQL = "UPDATE " + loObject.getTable() + 
                        " SET  cRecdStat = " + SQLUtil.toSQL(RecordStatus.INACTIVE) + 
                            ", sModified = " + SQLUtil.toSQL(poCrypt.encrypt(psUserIDxx)) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sFnancrID = " + SQLUtil.toSQL(loObject.getsFnancrID());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = true;
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        return lbResult;
    }

    @Override
    public boolean activateRecord(String fsTransNox) {
        UnitCPFinancer loObject = (UnitCPFinancer) openRecord(fsTransNox);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        if (loObject.getcRecdStat().equalsIgnoreCase(RecordStatus.ACTIVE)){
            setMessage("Current record is active...");
            return lbResult;
        }
        
        String lsSQL = "UPDATE " + loObject.getTable() + 
                        " SET  cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE) + 
                            ", sModified = " + SQLUtil.toSQL(poCrypt.encrypt(psUserIDxx)) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sFnancrID = " + SQLUtil.toSQL(loObject.getsFnancrID());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = true;
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        return lbResult;
    }

    @Override
    public String getMessage() {
        return psWarnMsg;
    }

    @Override
    public void setMessage(String fsMessage) {
         this.psWarnMsg = fsMessage;
    }

    @Override
    public String getErrMsg() {
         return psErrMsgx;
    }

    @Override
    public void setErrMsg(String fsErrMsg) {
         this.psErrMsgx = fsErrMsg;
    }

    @Override
    public void setBranch(String foBranchCD) {
        this.psBranchCd = foBranchCD;
    }

    @Override
    public void setWithParent(boolean fbWithParent) {
        this.pbWithParent = fbWithParent;
    }

    @Override
    public String getSQ_Master() {
          return (MiscUtil.makeSelect(new UnitCPFinancer()));
    }
    
    //Added methods
    public void setGRider(GRider foGRider){
        this.poGRider = foGRider;
        this.psUserIDxx = foGRider.getUserID();
        
        if (psBranchCd.isEmpty()) psBranchCd = foGRider.getBranchCode();
    }
    
    public void setUserID(String fsUserID){
        this.psUserIDxx  = fsUserID;
    }
    
    private com.mysql.jdbc.Connection setConnection(){
        com.mysql.jdbc.Connection foConn;
        
        if (pbWithParent){
            foConn = (com.mysql.jdbc.Connection) poGRider.getConnection();
            if (foConn == null) foConn = (com.mysql.jdbc.Connection) poGRider.doConnect();
        }else foConn = (com.mysql.jdbc.Connection) poGRider.doConnect();
        
        return foConn;
    }
    
    //Member Variables
    private GRider poGRider = null;
    private String psUserIDxx = "";
    private String psBranchCd = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private boolean pbWithParent = false;
    private final GCrypt poCrypt = new GCrypt();
    
}
