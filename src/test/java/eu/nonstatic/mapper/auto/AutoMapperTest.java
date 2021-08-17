package eu.nonstatic.mapper.auto;

import eu.nonstatic.mapper.AutoMapper;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class AutoMapperTest {

    AutoMapper mapper = new AutoMapper();

    final static PojoClass POJO_FOOBAR;
    final static PojoClass POJO_CANONICAL;

    static {
        POJO_FOOBAR = new PojoClass();
        POJO_FOOBAR.setMyString("FooBar");

        try {
            POJO_CANONICAL = new PojoClass(true, false, 456, 123L, (short)77, "Hello World", new CompletableFuture<>(), Arrays.asList("foo", "bar"), InetAddress.getLocalHost(), "shadow");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void should_not_map_from_enum() {
        assertThrows(IllegalArgumentException.class, () -> mapper.map(SomeEnum.FOO, new Object()));
    }

    @Test
    public void should_not_map_with_private_constructor() {
        try {
            mapper.mapToInstance(POJO_CANONICAL, PojoPrivate.class);
        } catch(RuntimeException e) {
            assertTrue(e.getCause() instanceof IllegalAccessException);
        }
    }

    @Test
    public void should_not_map_to_primitive() {
        try {
            mapper.mapToInstance(POJO_FOOBAR, int.class);
        } catch(RuntimeException e) {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    @Test
    public void testTshould_not_map_to_annotation() {
        try {
            mapper.mapToInstance(POJO_FOOBAR, Override.class);
        } catch(RuntimeException e) {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    @Test
    public void should_not_map_to_enum() {
        try {
            mapper.mapToInstance(POJO_FOOBAR, SomeEnum.class);
        } catch(RuntimeException e) {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    @Test
    public void should_not_map_to_interface() {
        try {
            mapper.mapToInstance(POJO_FOOBAR, SomeInterface.class);
        } catch(RuntimeException e) {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    @Test
    public void should_not_map_to_abstract_class() {
        try {
            mapper.mapToInstance(POJO_FOOBAR, SomeAbstractClass.class);
        } catch(RuntimeException e) {
            assertTrue(e.getCause() instanceof InstantiationException);
        }
    }

    @Test
    public void should_not_map_to_array() {
        try {
            mapper.mapToInstance(POJO_FOOBAR, Object[].class);
        } catch(RuntimeException e) {
            assertTrue(e.getCause() instanceof NoSuchMethodException);
        }
    }

    @Test
    public void should_map_to_object() {
        Object result = mapper.mapToInstance(POJO_FOOBAR, Object.class);// just to check useless things do work
        assertTrue(Object.class == result.getClass());
    }

    @Test
    public PojoClass should_map_to_same_class() {
        PojoClass pojo2 = mapper.mapToInstance(POJO_FOOBAR, PojoClass.class);
        assertNotSame(POJO_FOOBAR, pojo2);
        assertEquals(POJO_FOOBAR, pojo2);
        return pojo2;
    }

    @Test
    public void should_map_to_itself() {
        PojoClass pojoFooBarCopy = mapper.mapToInstance(POJO_FOOBAR, PojoClass.class);
        PojoClass pojoFooBarItself = mapper.map(POJO_FOOBAR, POJO_FOOBAR); //overwrite itself
        assertSame(POJO_FOOBAR, pojoFooBarItself);
        assertEquals("FooBar", pojoFooBarItself.getMyString());
        assertEquals(POJO_FOOBAR, pojoFooBarCopy);
    }



    @Test
    public void should_map_pojos() {
        PojoClass pojo1 = POJO_CANONICAL;
        PojoClass pojo2 = new PojoClass();
        PojoClass pojo3 = mapper.map(pojo1, pojo2);

        assertSame(pojo2, pojo3);
        assertNotSame(pojo1, pojo2);
        assertEquals(pojo1, pojo2);

        PojoClass pojo4 = mapper.mapToInstance(pojo1, PojoClass.class);
        assertNotSame(pojo1, pojo4);
        assertNotSame(pojo2, pojo4);
        assertEquals(pojo1, pojo4);
    }

    @Test
    public void should_map_shadowed_prop() {
        PojoShadow pojoShadow = mapper.mapToInstance(POJO_CANONICAL, PojoShadow.class);

        assertEquals("shadow", pojoShadow.getMyShadowedString());
        assertEquals(123L, pojoShadow.getMyLong());

        assertNull(((PojoClass)pojoShadow).myShadowedString);
    }

    @Test
    public void should_map_to_lombok_builder() {
        PojoClass pojo = POJO_CANONICAL;

        LombokSimpleClass.LombokSimpleClassBuilder builder = mapper.mapToBuilder(pojo, LombokSimpleClass.class);
        LombokSimpleClass lombok = builder.build();

        assertEquals(pojo.isMyBoolean(), lombok.isMyBoolean());
        assertEquals(pojo.getMyBooleanWrapper(), lombok.getMyBooleanWrapper());
        assertEquals(pojo.getMyNumber(), lombok.getMyNumber());
        assertEquals(pojo.getMyLong(), lombok.getMyLong());
        assertEquals(pojo.getMyString(), lombok.getMyString());
        assertEquals(pojo.getAAA(), lombok.getAAA());
        assertEquals(pojo.getMyList(), lombok.getMyList());
        assertThrows(ClassCastException.class, () -> lombok.getMyList().iterator().next()); // as expected, String => Integer !
        assertNull(lombok.getAnything());
    }

    @Test
    public void should_map_assignable_props() {
        PojoClass pojo = new PojoClass();
        pojo.setMyNumber(987L); // can't be assigned to an Integer
        List<String> myList = singletonList("FooBar");
        pojo.setMyList(myList);
        pojo.setMyBooleanWrapper(false);

        assertThrows(IllegalArgumentException.class, () -> mapper.map(pojo, new LombokSimpleClass())); // myShort won't be unboxed to null

        LombokSimpleClass lombok = mapper.map(pojo, new LombokSimpleClass(), "myShort", "myBooleanWrapper");
        assertSame(myList, lombok.getMyList());
        List<String> myListCopy = singletonList("FooBar");
        assertEquals(myListCopy, lombok.getMyList());
        assertNull(lombok.getMyNumber()); // Long => Integer mismatch
        assertNull(lombok.getMyBooleanWrapper());

        assertThrows(IllegalArgumentException.class, () -> mapper.map(pojo, new LombokSimpleClass(), "XXX")); // prop XXX is unknown

        pojo.setMyNumber(111);
        pojo.setMyShort((short)77); // now unboxing will work
        mapper.map(pojo, lombok);
        assertEquals(111, lombok.getMyNumber());
        assertEquals((short)77, lombok.getMyShort());
        assertSame(myList, lombok.getMyList());

        lombok.setMyList(new HashSet<>()); // works since this is a collection
        mapper.map(lombok, pojo);
        assertSame(myList, pojo.getMyList()); // assignment of the HashSet didn't happen Set => List mismatch
    }

    @Test
    public void should_map_to_super_builder() {
        PojoClass pojo = POJO_CANONICAL;
        mapper.<LombokBaseClass.LombokBaseClassBuilder>mapToBuilder(pojo, LombokBaseClass.class);
    }
}
