/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;
import javax.microedition.rms.InvalidRecordIDException;

/**
 *
 * @author christoph
 */
public class Queue {

    private final String name;
    private final long maxSize;
    private static final int maxRecord = 4 + 255 + 255;

    private RecordStore recordStore;
    private int recordID;

    Queue(long maxSize, String name) {
        this.name = name;
        this.maxSize = maxSize;
        shrink();
    }
    
    private void shrink() {
        recordID = 1;
        try {
            this.recordStore = RecordStore.openRecordStore(name, false);
            int numRecords = this.recordStore.getNumRecords();
            SLog.log(SLog.Informational, "Queue", "openRecordStore " + name + " " + numRecords);
            if (numRecords < 1  // < 0 should never happen
                    || Settings.getInstance().getSetting("killQueue", false)) {
                SLog.log(SLog.Informational, "Queue", "deleteRecordStore " + name);
                if (Settings.getInstance().getSetting("killQueue", false)) {
                    SLog.log(SLog.Informational, "Queue", "killedQueue");
                    Settings.getInstance().setSetting("killQueue", null);
                }
                this.recordStore.closeRecordStore();
                RecordStore.deleteRecordStore(name);
                this.recordStore = RecordStore.openRecordStore(name, true);
                SLog.log(SLog.Informational, "Queue", "openRecordStore (create) " + name);
            }
        } catch (RecordStoreNotFoundException rsnfe) {
            try {
                this.recordStore = RecordStore.openRecordStore(name, true);
                SLog.log(SLog.Informational, "Queue", "openRecordStore (create) " + name);
            } catch (RecordStoreFullException rsfe) {
                SLog.log(SLog.Error, "Queue", "RecordStoreFullException " + name);
            } catch (RecordStoreException rse) {
                SLog.log(SLog.Error, "Queue", "RecordStoreException " + name);

            }
        } catch (RecordStoreFullException rsfe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreFullException " + name);

        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException " + name);
        }
    }

    public synchronized byte[] get() {
        byte[] bytes = null;
        try {
            SLog.log(SLog.Debug, "Queue", "getNumRechords " + recordStore.getNumRecords()
                    + " size " + recordStore.getSize()
                    + "/" + recordStore.getSizeAvailable()
                    + "/" + maxSize);
            if (recordStore.getNumRecords() < 1) { // < 0 should never happen
                if (recordStore.getSize() + maxRecord > maxSize
                        || maxRecord > recordStore.getSizeAvailable()) {
                    recordStore.closeRecordStore();
                    shrink();
                }
                return null;
            } else {
                boolean gotRecord = false;
                do {
                    try {
                        SLog.log(SLog.Debug, "Queue", "getRecord " + recordID);
                        bytes = recordStore.getRecord(recordID);
                        gotRecord = true;
                    } catch (InvalidRecordIDException irie) {
                        SLog.log(SLog.Informational, "Queue", "InvalidRecordIDException " + recordID);
                        if (recordID < recordStore.getNextRecordID() - 1) {
                                recordID++;
                        } else {
                            gotRecord = true; // should never happen
                        }
                    }
                } while (!gotRecord);
            }
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException getRecord " + recordID);
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException getRecord " + recordID);
        }
        return bytes;
    }

    public synchronized void consume() {
        try {
            SLog.log(SLog.Debug, "Queue", "deleteRecord " + recordID);
            recordStore.deleteRecord(recordID);
            recordID++;
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException deleteRecord " + recordID);
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException deleteRecord " + recordID);
        }
    }

    public synchronized boolean put(byte[] bytes) {
        try {
            SLog.log(SLog.Debug, "Queue", "addRecord " + recordStore.getNextRecordID()
                    + " size " + recordStore.getSize()
                    + "/" + recordStore.getSizeAvailable()
                    + "/" + maxSize);
            if (recordStore.getSize() + maxRecord > maxSize
                    || maxRecord > recordStore.getSizeAvailable()) {
                SLog.log(SLog.Warning, "Queue", "maxSize limit reached");
                return false;
            }
            int newRecordId = recordStore.addRecord(bytes, 0, bytes.length);
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException addRecord");
            return false;
        } catch (RecordStoreException rse) {
            SLog.log(SLog.Error, "Queue", "RecordStoreException addRecord");
            return false;
        }
        return true;
    }

    public synchronized int size() {
        try {
            return recordStore.getNumRecords();
        } catch (RecordStoreNotOpenException rsnoe) {
            SLog.log(SLog.Error, "Queue", "RecordStoreNotOpenException getNumRecords");
            return 0;
        }
    }
}
