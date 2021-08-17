package eu.nonstatic.mapper.auto;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PojoClass {
    private boolean myBoolean;
    private Boolean myBooleanWrapper;
    private Number myNumber;
    private long myLong;
    private Short myShort;
    private String myString;
    private Object AAA;
    private List<String> myList;
    private Object unused;
    String myShadowedString;


    public PojoClass() {
    }

    public PojoClass(boolean myBoolean, Boolean myBooleanWrapper, Number myNumber, long myLong, Short myShort, String myString, Object AAA, List<String> myList, Object unused, String myShadowedString) {
        this.myBoolean = myBoolean;
        this.myBooleanWrapper = myBooleanWrapper;
        this.myNumber = myNumber;
        this.myLong = myLong;
        this.myShort = myShort;
        this.myString = myString;
        this.AAA = AAA;
        this.myList = myList;
        this.unused = unused;
        this.myShadowedString = myShadowedString;
    }

    public boolean isMyBoolean() {
        return myBoolean;
    }

    public void setMyBoolean(boolean myBoolean) {
        this.myBoolean = myBoolean;
    }

    public Boolean getMyBooleanWrapper() {
        return myBooleanWrapper;
    }

    public void setMyBooleanWrapper(Boolean myBooleanWrapper) {
        this.myBooleanWrapper = myBooleanWrapper;
    }

    public Number getMyNumber() {
        return myNumber;
    }

    public void setMyNumber(Number myNumber) {
        this.myNumber = myNumber;
    }

    public long getMyLong() {
        return myLong;
    }

    public void setMyLong(long myLong) {
        this.myLong = myLong;
    }

    public Short getMyShort() {
        return myShort;
    }

    public void setMyShort(Short myShort) {
        this.myShort = myShort;
    }

    public String getMyString() {
        return myString;
    }

    public void setMyString(String myString) {
        this.myString = myString;
    }

    public Object getAAA() {
        return AAA;
    }

    public void setAAA(Object AAA) {
        this.AAA = AAA;
    }

    public List<String> getMyList() {
        return myList;
    }

    public void setMyList(List<String> myList) {
        this.myList = myList;
    }

    public Object getUnused() {
        return unused;
    }

    public void setUnused(Object unused) {
        this.unused = unused;
    }

    public String getMyShadowedString() {
        return myShadowedString;
    }

    public void setMyShadowedString(String myShadowedString) {
        this.myShadowedString = myShadowedString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PojoClass pojoClass = (PojoClass) o;
        return myBoolean == pojoClass.myBoolean && myLong == pojoClass.myLong && Objects.equals(myBooleanWrapper, pojoClass.myBooleanWrapper) && Objects.equals(myNumber, pojoClass.myNumber) && Objects.equals(myShort, pojoClass.myShort) && Objects.equals(myString, pojoClass.myString) && Objects.equals(AAA, pojoClass.AAA) && Objects.equals(myList, pojoClass.myList) && Objects.equals(unused, pojoClass.unused) && Objects.equals(myShadowedString, pojoClass.myShadowedString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myBoolean, myBooleanWrapper, myNumber, myLong, myShort, myString, AAA, myList, unused, myShadowedString);
    }

    public static final class PojoSubClass {
        private Float myFloat;
        private Map<Integer, String> myMap;

        public PojoSubClass() {
        }

        public PojoSubClass(Float myFloat, Map<Integer, String> myMap) {
            this.myFloat = myFloat;
            this.myMap = myMap;
        }

        public Float getMyFloat() {
            return myFloat;
        }

        public void setMyFloat(Float myFloat) {
            this.myFloat = myFloat;
        }

        public Map<Integer, String> getMyMap() {
            return myMap;
        }

        public void setMyMap(Map<Integer, String> myMap) {
            this.myMap = myMap;
        }
    }
}
