package eu.nonstatic.mapper.fromto;

public class  FromPojo extends FromSuperPojo {

    private String foo;
    private long wut;
    private Integer zii;

    public FromPojo() {
    }

    public FromPojo(String foo, long wut, Integer zii) {
        this.foo = foo;
        this.wut = wut;
        this.zii = zii;
    }

    @Override
    public String getFoo() {
        return foo;
    }

    @Override
    public void setFoo(String foo) {
        this.foo = foo;
    }

    public long getWut() {
        return wut;
    }

    public void setWut(long wut) {
        this.wut = wut;
    }

    public Integer getZii() {
        return zii;
    }

    public void setZii(Integer zii) {
        this.zii = zii;
    }
}
