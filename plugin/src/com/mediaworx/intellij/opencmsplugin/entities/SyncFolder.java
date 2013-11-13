package com.mediaworx.intellij.opencmsplugin.entities;

import java.util.List;

public class SyncFolder extends SyncEntity {
    private List<SyncEntity> children;

    public List<SyncEntity> getChildren() {
        return children;
    }

    public void setChildren(List<SyncEntity> children) {
        this.children = children;
    }
}
