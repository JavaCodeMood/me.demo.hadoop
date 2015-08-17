package com.lt.hbase.orm.test.mr.lib;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mrunit.mapreduce.MapDriver;

public class TableMapDriver<KEYOUT, VALUEOUT> extends MapDriver<ImmutableBytesWritable, Result, KEYOUT, VALUEOUT> {
    public TableMapDriver() {
        super();
    }

    public TableMapDriver(final Mapper<ImmutableBytesWritable, Result, KEYOUT, VALUEOUT> m) {
        super(m);
    }

    public static <KEYOUT, VALUEOUT> TableMapDriver<KEYOUT, VALUEOUT> newTableMapDriver() {
        return new TableMapDriver<KEYOUT, VALUEOUT>();
    }

    public static <KEYOUT, VALUEOUT> TableMapDriver<KEYOUT, VALUEOUT> newTableMapDriver(final Mapper<ImmutableBytesWritable, Result, KEYOUT, VALUEOUT> m) {
        return new TableMapDriver<KEYOUT, VALUEOUT>(m);
    }
}
