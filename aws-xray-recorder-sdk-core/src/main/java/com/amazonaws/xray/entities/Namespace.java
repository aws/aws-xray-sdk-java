package com.amazonaws.xray.entities;

public enum Namespace {
    REMOTE("remote"), AWS("aws");

    private String value;

    private Namespace(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
