package com.mediaworx.intellij.opencmsplugin.entities;

import java.util.Date;

public class SyncFile extends SyncEntity {

    private Date lastChangeDate;

    public Date getLastChangeDate() {
        return lastChangeDate;
    }

    public void setLastChangeDate(Date lastChangeDate) {
        this.lastChangeDate = lastChangeDate;
    }
}
