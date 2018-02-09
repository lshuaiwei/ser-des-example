package com.git.lee.serde.example.pb;

import com.git.lee.serde.example.guice.GuiceInstanceFactory;
import com.git.lee.serde.example.pb.schema.dynamic.DynamicSchema;
import com.git.lee.serde.example.pb.service.SchemaService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * @author LISHUAIWEI
 * @date 2018/2/9 9:57
 */
public class DynamicSchemaTest {

    @Before
    public void initModule() {
        GuiceInstanceFactory.getInjector();
    }

    @Test
    public void parseSchema() throws Exception {
        int messageId = 1;
        SchemaService schemaService = GuiceInstanceFactory.getInstance(SchemaService.class);
        DynamicSchema schema = schemaService.parseProto(messageId, protoContent());
        Assert.assertTrue(schema.getMessageTypes().contains("MyMessage"));
        System.out.println(schema);

        Map<String, Map<String, Object>> messageDesc = schemaService.parseSchema(schema, messageId);
        Map<String, Object> fieldDesc = messageDesc.get("MyMessage");
        Assert.assertEquals(fieldDesc.get("id"), Integer.class);
        Assert.assertEquals(fieldDesc.get("type"), String.class);
        Assert.assertEquals(fieldDesc.get("timestamp"), Long.class);
        Assert.assertEquals(fieldDesc.get("tags"), Map.class);
        Assert.assertEquals(fieldDesc.get("children"), "MyMessage");
    }

    private String protoContent() {
        /**
         *
         syntax = "proto3";

         enum Type {
             TRANSACTION = 0;
             EVENT = 1;
         }

         message MyMessage {
             int32 id = 1;
             Type type = 2;
             int64 timestamp = 3;
             map<string,string> tags = 4;
             repeated MyMessage children = 5;
         }
         */
        return  "syntax = \"proto3\";\n" +
                "\n" +
                "         enum Type {\n" +
                "             TRANSACTION = 0;\n" +
                "             EVENT = 1;\n" +
                "         }\n" +
                "\n" +
                "         message MyMessage {\n" +
                "             int32 id = 1;\n" +
                "             Type type = 2;\n" +
                "             int64 timestamp = 3;\n" +
                "             map<string,string> tags = 4;\n" +
                "             repeated MyMessage children = 5;\n" +
                "         }";
    }
}
