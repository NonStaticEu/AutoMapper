package eu.nonstatic.mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Builder @Data
@NoArgsConstructor @AllArgsConstructor
public class LombokSimpleClass {
    private short myShort; // not a wrapper here
    private Boolean myBooleanWrapper;
    private Integer myNumber; // Integer on purpose to see if assignment is possible from Number PojoClass#myNumber containing an integer
    private boolean myBoolean;
    private long myLong;
    private Object anything;
    private Collection<Integer> myList; // Collection on purpose to see if assignment is possible from List PojoClass#myList<String> which should work because of the erasure!
    private Object AAA;
    private String myString;

    @Data @Builder
    public static class LombokSubClass {
        private double myDouble;
    }
}
