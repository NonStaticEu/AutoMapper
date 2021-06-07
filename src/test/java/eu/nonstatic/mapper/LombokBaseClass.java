package eu.nonstatic.mapper;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder @Data
public class LombokBaseClass {
    private Character myCharacter;
    private Number myNumber;
    private Integer myInteger;
}
