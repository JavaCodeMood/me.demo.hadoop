package com.lt.hbase.orm.exceptions;

public class RowKeyCouldNotBeParsedException extends IllegalArgumentException {
    public RowKeyCouldNotBeParsedException(String s) {
        super(s);
    }

    public RowKeyCouldNotBeParsedException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
