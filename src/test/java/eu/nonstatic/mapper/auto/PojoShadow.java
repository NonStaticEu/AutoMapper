package eu.nonstatic.mapper.auto;

public class PojoShadow extends PojoClass {

    private String myShadowedString; // shadowing PojoClass' prop with the same name

    public PojoShadow() {}

    public PojoShadow(String myShadowedString) {
        this.myShadowedString = myShadowedString;
    }

    public String getMyShadowedString() {
        return myShadowedString;
    }

    public void setMyShadowedString(String myShadowedString) {
        this.myShadowedString = myShadowedString;
    }
}
