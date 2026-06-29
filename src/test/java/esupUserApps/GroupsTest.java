package esupUserApps;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(hasGroup(group, "{ a: ['foo'] }"), "$eq");
        assertTrue(hasGroup(group, "{ a: ['foo', 'bar'] }"), "$eq: multiple vals");
        assertFalse(hasGroup(group, "{}"), "$eq: no attr");
        assertFalse(hasGroup(group, "{ a: ['bar'] }"), "$eq: not equal");

        group = "{ a: { $ne: 'foo' } }";
        assertFalse(hasGroup(group, "{ a: ['foo'] }"), "$ne");
        assertFalse(hasGroup(group, "{ a: ['foo', 'bar'] }"), "$ne: multiple vals");
        assertTrue(hasGroup(group, "{}"), "$ne: no attr");
        assertTrue(hasGroup(group, "{ a: ['bar'] }"), "$ne: not equal");

        group = "{ a: { $in: [ 'foo' ] } }";
        assertTrue(hasGroup(group, "{ a: ['foo'] }"), "$in");
        group = "{ a: { $in: [ 'foo', 'bar' ] } }";
        assertTrue(hasGroup(group, "{ a: ['foo'] }"), "$in");
        assertTrue(hasGroup(group, "{ a: ['bar', 'zzz'] }"), "$in: multiple vals");
        assertFalse(hasGroup(group, "{}"), "$in: no attr");
        assertFalse(hasGroup(group, "{ a: ['zzz', 'yyy'] }"), "$in: not in");

        group = "{ a: { $nin: [ 'foo' ] } }";
        assertFalse(hasGroup(group, "{ a: ['foo'] }"), "$nin");
        group = "{ a: { $nin: [ 'foo', 'bar' ] } }";
        assertFalse(hasGroup(group, "{ a: ['foo'] }"), "$nin");
        assertFalse(hasGroup(group, "{ a: ['bar', 'zzz'] }"), "$nin: multiple vals");
        assertTrue(hasGroup(group, "{}"), "$nin: no attr");
        assertTrue(hasGroup(group, "{ a: ['zzz', 'yyy'] }"), "$nin: not in");

        group = "{ a: { $in: [ 'foo' ], $eq: 'bar' } }";
        assertTrue(hasGroup(group, "{ a: ['foo', 'bar', 'zzz'] }"), "$in + $eq");
        assertFalse(hasGroup(group, "{}"), "$in + $eq: no attr");
        assertFalse(hasGroup(group, "{ a: ['foo', 'zzz'] }"), "$in + $eq: not both");

        group = "{ $and: [ { a: 'foo' }, { a: 'bar' } ] }";
        assertTrue(hasGroup(group, "{ a: ['foo', 'bar', 'zzz'] }"), "$and $eq");
        assertFalse(hasGroup(group, "{}"), "$and $eq: no attr");
        assertFalse(hasGroup(group, "{ a: ['foo', 'zzz'] }"), "$and $eq: not both");

        group = "{ $or: [ { a: 'foo' }, { b: 'foo' } ] }";
        assertTrue(hasGroup(group, "{ a: ['foo', 'bar', 'zzz'] }"), "$or $eq: one");
        assertTrue(hasGroup(group, "{ a: ['bar'], b: ['foo'] }"), "$or $eq: other one");
        assertFalse(hasGroup(group, "{}"), "$or $eq: no attr");
        assertFalse(hasGroup(group, "{ a: ['bar'], b: ['bar'] }"), "$or $eq: none");

        group = "{ a: { $eq: null } }";
        assertFalse(hasGroup(group, "{ a: ['foo'] }"), "$eq null: no attr");
        assertTrue(hasGroup(group, "{}"), "$eq null: no attr");
        assertTrue(hasGroup(group, "{ a: null }"), "$eq null: null list");
        assertTrue(hasGroup(group, "{ a: [null] }"), "$eq null: null value");

        group = "{ a: 'foo', b: 'bar' }";
        assertTrue(hasGroup(group, "{ a: ['foo'], b: ['bar'] }"), "mutiple attrs $eq");
        assertFalse(hasGroup(group, "{ a: ['foo'] }"), "mutiple attrs $eq");
        assertFalse(hasGroup(group, "{ b: ['bar'] }"), "mutiple attrs $eq");
        assertFalse(hasGroup(group, "{ a: ['foo'], b: ['zzz'] }"), "mutiple attrs $eq");

        assertTrue(hasGroup("{ a: { $regex: 'foo.*' } }", "{ a: ['foobar'] }"), "$regex");
        assertFalse(hasGroup("{ a: { $regex: 'foo.*' } }", "{ a: ['Zfoobar'] }"), "$regex");
        assertFalse(hasGroup("{ a: { $regex: 'foo' } }", "{ a: ['foobar'] }"), "$regex");
        assertFalse(hasGroup("{ a: { $regex: 'foo' } }", "{}"), "$regex: no attr");
    }

}
