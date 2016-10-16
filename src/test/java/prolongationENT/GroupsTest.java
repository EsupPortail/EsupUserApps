package prolongationENT;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import static org.junit.Assert.*;
import static prolongationENT.Utils.*;

public class GroupsTest {
    Map<String, Map<String, Object>> parseGroups(String json) {
        return new Gson().fromJson(json, new TypeToken<Map<String, Map<String, Object>>>(){}.getType());
    }

    Map<String, List<String>> parsePerson(String json) {
        return new Gson().fromJson(json, new TypeToken<Map<String, List<String>>>(){}.getType());
    }

    boolean hasGroup(String group, String person) {
        Groups groups = new Groups(parseGroups("{ g: " + group + " }"));
        return groups.hasGroup(parsePerson(person), "g");
    }
    
    @Test
    public void test() {
        String group;
        group = "{ a: 'foo' }";
        assertTrue("$eq", hasGroup(group, "{ a: ['foo'] }"));
        assertTrue("$eq: multiple vals", hasGroup(group, "{ a: ['foo', 'bar'] }"));
        assertFalse("$eq: no attr", hasGroup(group, "{}"));
        assertFalse("$eq: not equal", hasGroup(group, "{ a: ['bar'] }"));

        group = "{ a: { $ne: 'foo' } }";
        assertFalse("$ne", hasGroup(group, "{ a: ['foo'] }"));
        assertFalse("$ne: multiple vals", hasGroup(group, "{ a: ['foo', 'bar'] }"));
        assertTrue("$ne: no attr", hasGroup(group, "{}"));
        assertTrue("$ne: not equal", hasGroup(group, "{ a: ['bar'] }"));

        group = "{ a: { $in: [ 'foo' ] } }";
        assertTrue("$in", hasGroup(group, "{ a: ['foo'] }"));
        group = "{ a: { $in: [ 'foo', 'bar' ] } }";
        assertTrue("$in", hasGroup(group, "{ a: ['foo'] }"));
        assertTrue("$in: multiple vals", hasGroup(group, "{ a: ['bar', 'zzz'] }"));
        assertFalse("$in: no attr", hasGroup(group, "{}"));
        assertFalse("$in: not in", hasGroup(group, "{ a: ['zzz', 'yyy'] }"));

        group = "{ a: { $nin: [ 'foo' ] } }";
        assertFalse("$nin", hasGroup(group, "{ a: ['foo'] }"));
        group = "{ a: { $nin: [ 'foo', 'bar' ] } }";
        assertFalse("$nin", hasGroup(group, "{ a: ['foo'] }"));
        assertFalse("$nin: multiple vals", hasGroup(group, "{ a: ['bar', 'zzz'] }"));
        assertTrue("$nin: no attr", hasGroup(group, "{}"));
        assertTrue("$nin: not in", hasGroup(group, "{ a: ['zzz', 'yyy'] }"));

        group = "{ a: { $in: [ 'foo' ], $eq: 'bar' } }";
        assertTrue("$in + $eq", hasGroup(group, "{ a: ['foo', 'bar', 'zzz'] }"));
        assertFalse("$in + $eq: no attr", hasGroup(group, "{}"));
        assertFalse("$in + $eq: not both", hasGroup(group, "{ a: ['foo', 'zzz'] }"));

        group = "{ $and: [ { a: 'foo' }, { a: 'bar' } ] }";
        assertTrue("$and $eq", hasGroup(group, "{ a: ['foo', 'bar', 'zzz'] }"));
        assertFalse("$and $eq: no attr", hasGroup(group, "{}"));
        assertFalse("$and $eq: not both", hasGroup(group, "{ a: ['foo', 'zzz'] }"));

        group = "{ $or: [ { a: 'foo' }, { b: 'foo' } ] }";
        assertTrue("$or $eq: one", hasGroup(group, "{ a: ['foo', 'bar', 'zzz'] }"));
        assertTrue("$or $eq: other one", hasGroup(group, "{ a: ['bar'], b: ['foo'] }"));
        assertFalse("$or $eq: no attr", hasGroup(group, "{}"));
        assertFalse("$or $eq: none", hasGroup(group, "{ a: ['bar'], b: ['bar'] }"));

        group = "{ a: { $eq: null } }";
        assertFalse("$eq null: no attr", hasGroup(group, "{ a: ['foo'] }"));
        assertTrue("$eq null: no attr", hasGroup(group, "{}"));
        assertTrue("$eq null: null list", hasGroup(group, "{ a: null }"));
        assertTrue("$eq null: null value", hasGroup(group, "{ a: [null] }"));

        group = "{ a: 'foo', b: 'bar' }";
        assertTrue("mutiple attrs $eq", hasGroup(group, "{ a: ['foo'], b: ['bar'] }"));
        assertFalse("mutiple attrs $eq", hasGroup(group, "{ a: ['foo'] }"));
        assertFalse("mutiple attrs $eq", hasGroup(group, "{ b: ['bar'] }"));
        assertFalse("mutiple attrs $eq", hasGroup(group, "{ a: ['foo'], b: ['zzz'] }"));

        assertTrue("$regex", hasGroup("{ a: { $regex: 'foo.*' } }", "{ a: ['foobar'] }"));
        assertFalse("$regex", hasGroup("{ a: { $regex: 'foo.*' } }", "{ a: ['Zfoobar'] }"));
        assertFalse("$regex", hasGroup("{ a: { $regex: 'foo' } }", "{ a: ['foobar'] }"));
        assertFalse("$regex: no attr", hasGroup("{ a: { $regex: 'foo' } }", "{}"));
    }

}
