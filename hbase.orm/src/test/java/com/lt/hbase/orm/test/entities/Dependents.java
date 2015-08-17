package com.lt.hbase.orm.test.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString
@EqualsAndHashCode
public class Dependents {
    @JsonProperty
    private Integer uidWife;
    @JsonProperty
    private List<Integer> uidChildren;

    public Dependents() {
    }

    public Dependents(Integer uidWife, List<Integer> uidChildren) {
        this.uidWife = uidWife;
        this.uidChildren = uidChildren;
    }
}
