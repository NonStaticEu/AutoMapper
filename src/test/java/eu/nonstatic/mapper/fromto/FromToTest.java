package eu.nonstatic.mapper.fromto;

import eu.nonstatic.mapper.FromToMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FromToTest {

    static final String foo = "SomeString";
    static final long wut = 42L;
    static final int zii = 69;
    static final FromPojo from = new FromPojo(foo, wut, zii);

    @Test
    public void should_still_auto_map() {
        FromToMapper mapper = new FromToMapper();
        ToPojo to = mapper.mapToInstance(from, ToPojo.class);

        assertSame(foo, to.getFoo());
        assertEquals(wut, to.getWut());
        assertEquals(0, to.getPii());
    }

    @Test
    public void should_map_to_instance() {
        FromToMapper mapper = new FromToMapper();
        mapper.registerMapping(FromPojo.class, "foo", ToPojo.class, "bar");

        ToPojo to = mapper.mapToInstance(from, ToPojo.class);

        assertNull(to.getFoo());
        assertSame(foo, to.getBar());
        assertEquals(0, to.getPii());



        mapper.registerMapping(FromPojo.class, "zii", ToPojo.class, "pii");
        mapper.map(from, to);

        assertNull(to.getFoo());
        assertSame(foo, to.getBar());
        assertEquals(zii, to.getPii());
    }

    @Test
    public void should_map_to_builder() {
        FromToMapper mapper = new FromToMapper();
        mapper.registerMapping(FromPojo.class, "foo", ToPojo.class, "bar");
        mapper.registerMapping(FromPojo.class, "zii", ToPojo.class, "pii");

        ToPojo.ToPojoBuilder builder = mapper.mapToBuilder(from, ToPojo.class, "wut");
        ToPojo to = builder.build();

        assertNull(to.getFoo());
        assertSame(foo, to.getBar());
        assertEquals(zii, to.getPii());
    }

    @Test
    public void should_map_shadowing_prop() {
        FromToMapper mapper = new FromToMapper();
        mapper.registerMapping(FromPojo.class, "foo", ToPojoShadow.class, "shadow");

        ToPojoShadow to = mapper.mapToInstance(from, ToPojoShadow.class);

        assertSame(foo, to.shadow);
        assertNull(((ToPojo)to).shadow);
    }
}
