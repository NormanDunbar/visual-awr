package org.altaprise.vawr.awrdata;

import dai.shared.businessObjs.DBRec;
import dai.shared.businessObjs.DBRecSet;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;

import org.altaprise.vawr.utils.PropertyFile;

public class AWRData {

    
    private List _headerTokens = new ArrayList();
    private LinkedHashMap<String, AWRRecord> _dataRecords = new LinkedHashMap<String, AWRRecord>();

    private static AWRData _theInstance = null;

    private AWRData() {
    }

    public static AWRData getInstance() {
        if (_theInstance == null) {
            _theInstance = new AWRData();
        }
        return _theInstance;
    }

    public void clearAWRData() {
        _headerTokens.clear();
        _dataRecords.clear();
    }
    
    public void dumpData() {

        for (int i = 0; i < getHeaderCount(); i++) {
            System.out.print(getHeaderName(i) + ", ");
        }
        System.out.println();

        for (Map.Entry<String, AWRRecord> entry : _dataRecords.entrySet()) {
            AWRRecord awrRec = entry.getValue();
            for (int j = 0; j < awrRec.getSize(); j++) {
                String headerName = getHeaderName(j);
                String val = awrRec.getVal(headerName);
                System.out.print(val + ", ");
            }
            System.out.println();
        }
    }
    
    public void parseDataRecords(DBRecSet recSet) {
        
        for (int i = 0; i < recSet.getSize(); i++) {
            String rec = "";
            String racInstNum = "";
            String snapId = "";
            DBRec dbRec = recSet.getRec(i);
            AWRRecord awrRec = new AWRRecord();
            for (int j = 0; j < dbRec.size(); j++) {
                String dbFieldVal = dbRec.getAttrib(j).getValue();
                String dbFieldName = dbRec.getAttrib(j).getName();

                this.addHeaderName(dbFieldName);
                
                //Check to see if this is the header.  If so, add another header field for time.
                if (dbFieldName.toUpperCase().equals("END")) {
                    //The END field value has the format: "14/06/28 21:55"
                    //we need to break it up into two fields END and TIME
                    this.addHeaderName("TIME");
                    String endDateS = dbFieldVal.substring(0, 8);
                    String endTimeS = dbFieldVal.substring(9, 14);
                    awrRec.putVal("END", endDateS);
                    awrRec.putVal("TIME", endTimeS);
                } else {
                    awrRec.putVal(dbFieldName, dbFieldVal);
                }
                
                //Build the Key field    
                if (dbFieldName.equals("SNAP")) snapId = dbFieldVal;
                if (dbFieldName.equals("INST")) racInstNum = dbFieldVal;
            }
            _dataRecords.put(snapId+"-"+racInstNum, awrRec);
        }
    }

    public AWRRecord getAWRRecordByKey(String snapId, String racInstNum) {
        AWRRecord awrRec = _dataRecords.get(snapId+"-"+racInstNum);
        return awrRec;    
    }
    
    public LinkedHashSet<String> getUniqueSnapshotIds() {
        LinkedHashSet<String> snapIdSet = new LinkedHashSet<String>();
        
        for (Map.Entry<String, AWRRecord> entry : _dataRecords.entrySet()) {
            AWRRecord awrRec = entry.getValue();
            String snapId = awrRec.getSnapId();
            snapIdSet.add(snapId);
        }
        
        return snapIdSet;
    }
    
    public void parseMemoryDataRecords(DBRecSet recSet) {
/*
        snap_id, " +
                " instance_number, " +
                " MAX (DECODE (stat_name, \'SGA\', stat_value, NULL)) \"SGA\", " +
                " MAX (DECODE (stat_name, \'PGA\', stat_value, NULL)) \"PGA\", " +
                " MAX (DECODE (stat_name, 'SGA', stat_value, NULL)) + MAX (DECODE (stat_name, 'PGA', stat_value, " +
                " NULL)) \"TOTAL\" " +
*/
        for (int i = 0; i < recSet.getSize(); i++) {

            DBRec dbRec = recSet.getRec(i);
            String snapId = dbRec.getAttribVal("SNAP_ID");
            String racInstNum = dbRec.getAttribVal("INSTANCE_NUMBER");
            String sga = dbRec.getAttribVal("SGA");
            String pga = dbRec.getAttribVal("PGA");
            String memTot = dbRec.getAttribVal("SGA_PGA_TOT");
            
            AWRRecord awrRec = _dataRecords.get(snapId+"-"+racInstNum);
            this.addHeaderName("SGA");
            awrRec.putVal("SGA", sga);
            this.addHeaderName("PGA");
            awrRec.putVal("PGA", pga);
            this.addHeaderName("SGA_PGA_TOT");
            awrRec.putVal("SGA_PGA_TOT", memTot);
        }
    }

    
    public String getAWRDataTextString() {
        String ret = "";
        for (int i = 0; i < getHeaderCount(); i++) {
            ret += this.getHeaderName(i) + " ";
        }
        ret += "\n";

        for (Map.Entry<String, AWRRecord> entry : _dataRecords.entrySet()) {
            AWRRecord awrRec = entry.getValue();
            for (int j = 0; j < awrRec.getSize(); j++) {
                String headerName = getHeaderName(j);
                String val = awrRec.getVal(headerName);
                ret += val + " ";
            }
            ret += "\n";
        }
        return ret;
    }
    
    public void parseHeaders(String rec) {
        if (rec.length() > 0) {

            StringTokenizer st = new StringTokenizer(rec);
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                addHeaderName(token);

                //Check to see if this is the header.  If so, add another header field for time.
                if (token.equals("end") || token.equals("END")) {
                    addHeaderName("TIME");
                }
            }
        }
    }
    
    public ArrayList<AWRRecord> getAWRRecordArray() {
        return new ArrayList<AWRRecord>(_dataRecords.values());
    }
    
    public AWRRecord parseDataRecord(String rec) {
        AWRRecord awrRec = new AWRRecord();
        String racInstNum = "";
        String snapId = "";

        int tokenCnt = 0;
        if (rec.length() > 0) {

            StringTokenizer st = new StringTokenizer(rec);
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                String headerName = (String)getHeaderName(tokenCnt);
                awrRec.putVal(headerName.toUpperCase(), tok);
                if (headerName.equals("SNAP")) snapId = tok;
                if (headerName.equals("INST")) racInstNum = tok;
                tokenCnt++;
            }
        }
        _dataRecords.put(snapId+"-"+racInstNum, awrRec);
        
        return awrRec;
    }
    
    public long getAWRDataRecordCount() {
        return _dataRecords.size();
    }
    
    private void addHeaderName(String name) {
        _headerTokens.add(name.toUpperCase());    
    }

    private int getHeaderCount() {
        return _headerTokens.size();    
    }
    
    private String getHeaderName(int i) {
        return (String)_headerTokens.get(i);
    }
    
  
    public boolean awrMetricExists(String metric) {
        return _headerTokens.contains(metric.toUpperCase());
    }

    public int getNumRACInstances() {
        int numRacInst = 0;
        for (Map.Entry<String, AWRRecord> entry : _dataRecords.entrySet()) {
            AWRRecord awrRec = entry.getValue();
            if (awrRec.getInst() > numRacInst) {
                numRacInst = awrRec.getInst();
            }
        }
        return numRacInst;
    }
}
