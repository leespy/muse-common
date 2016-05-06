package com.leespy.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Date: 16/5/6
 * Time: 下午3:47
 *
 * @author i@leespy.com
 */
public class Jsoner {

    private static Logger logger = LoggerFactory.getLogger(Jsoner.class);

    /**
     * 忽略对象中值为NULL或""的属性
     */
    public static final Jsoner EXCLUDE_EMPTY = new Jsoner(JsonInclude.Include.NON_EMPTY);

    /**
     * 忽略对象中值为默认值的属性
     */
    public static final Jsoner EXCLUDE_DEFAULT = new Jsoner(JsonInclude.Include.NON_DEFAULT);

    /**
     * 默认不排除任何属性
     */
    public static final Jsoner DEFAULT = new Jsoner();

    private ObjectMapper mapper;

    private Jsoner() {
        mapper = new ObjectMapper();
        // ignore attributes exists in json string, but not in java object when deserialization
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.registerModule(new GuavaModule());
    }

    private Jsoner(JsonInclude.Include include) {
        mapper = new ObjectMapper();
        // set serialization feature
        mapper.setSerializationInclusion(include);
        // ignore attributes exists in json string, but not in java object when deserialization
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.registerModule(new GuavaModule());
    }

    /**
     * return a jsoner, only output attributes, not empty or null
     */
    public static Jsoner nonEmptyJsoner() {
        return EXCLUDE_EMPTY;
    }

    /**
     * return a jsoner, only output attributes, have not default value
     */
    public static Jsoner nonDefaultJsoner() {
        return EXCLUDE_DEFAULT;
    }

    /**
     * convert an object(POJO, Collection, ...) to json string
     *
     * @param target target object
     * @return json string
     */
    public String toJson(Object target) {

        try {
            return mapper.writeValueAsString(target);
        } catch (IOException e) {
            logger.error("write to json string error:" + target, e);
            return null;
        }
    }

    /**
     * deserialize a json to target class object
     *
     * @param json   json string
     * @param target target class
     * @param <T>
     * @return target object
     */
    public <T> T fromJson(String json, Class<T> target) {
        if (Strings.isNullOrEmpty(json)) {
            return null;
        }
        try {
            return mapper.readValue(json, target);
        } catch (IOException e) {
            logger.warn("parse json string error:" + json, e);
            return null;
        }
    }

    /**
     * 反序列化复杂Collection如List<Bean>, 先使用函數createCollectionType构造类型,然后调用本函数.
     *
     * @see #createCollectionType(Class, Class...)
     */
    @SuppressWarnings("unchecked")
    public <T> T fromJson(String jsonString, JavaType javaType) {
        if (Strings.isNullOrEmpty(jsonString)) {
            return null;
        }
        try {
            return (T) mapper.readValue(jsonString, javaType);
        } catch (Exception e) {
            logger.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * read a json to JsonNode Tree
     *
     * @param json source json string
     * @return JsonNode Tree
     * @throws java.io.IOException
     */
    public JsonNode treeFromJson(String json) throws IOException {
        return mapper.readTree(json);
    }

    /**
     * convert a JsonNode to target class object
     *
     * @param node   source node
     * @param target target class
     * @param <T>
     * @return target class object
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     */
    public <T> T treeToValue(JsonNode node, Class<T> target) throws JsonProcessingException {
        return mapper.treeToValue(node, target);
    }

    /**
     * construct collection type
     *
     * @param collectionClass collection class, such as ArrayList, HashMap, ...
     * @param elementClasses  element class
     *                        ArrayList<T>:
     *                        createCollectionType(ArrayList.class, T.class)
     *                        HashMap<String, T>:
     *                        createCollectionType(HashMap.class, String.class, T.class)
     * @return JavaType
     */
    public JavaType createCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return mapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    /**
     * update a target object's attributes from json
     *
     * @param json   source json string
     * @param target target object
     * @param <T>
     * @return updated target object
     */
    @SuppressWarnings("unchecked")
    public <T> T update(String json, T target) {
        try {
            return (T) mapper.readerForUpdating(target).readValue(json);
        } catch (JsonProcessingException e) {
            logger.warn("update json string:" + json + " to object:" + target + " error.", e);
        } catch (IOException e) {
            logger.warn("update json string:" + json + " to object:" + target + " error.", e);
        }
        return null;
    }

    /**
     * output JSONP style string
     */
    public String toJsonP(String functionName, Object object) {
        return toJson(new JSONPObject(functionName, object));
    }

    /**
     * enable enumable, make enum attribute read or write as string
     */
    public void enumable() {
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }

    /**
     * return a common json mapper
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * deserialize a json to a complex object
     *
     * @param json      json string
     * @param reference complex type like Object contains List object
     * @param <T>       object
     * @return object
     */
    public <T> T fromJson(String json, TypeReference reference) {

        if (Strings.isNullOrEmpty(json)) {
            return null;
        }
        try {
            return mapper.readValue(json, reference);
        } catch (IOException e) {
            logger.warn("parse json string error:" + json, e);
            return null;
        }
    }
}