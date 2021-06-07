package eu.nonstatic.mapper;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder @Data @EqualsAndHashCode(callSuper = true)
public class LombokCustomClass extends LombokBaseClass {
    private boolean myBoolean;
    private long myLong;
    private String myString;
}
