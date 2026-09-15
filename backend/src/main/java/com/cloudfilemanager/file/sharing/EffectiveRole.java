package com.cloudfilemanager.file.sharing;

public enum EffectiveRole {
    OWNER,
    EDITOR,
    VIEWER,
    NONE;

    public boolean atLeast(EffectiveRole minimum) {
        return this.rank() >= minimum.rank();
    }

    private int rank() {
        return switch (this) {
            case NONE -> 0;
            case VIEWER -> 1;
            case EDITOR -> 2;
            case OWNER -> 3;
        };
    }
}
