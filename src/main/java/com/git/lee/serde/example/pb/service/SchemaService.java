package com.git.lee.serde.example.pb.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import io.protostuff.compiler.model.EnumConstant;
import io.protostuff.compiler.model.Field;
import io.protostuff.compiler.model.Message;
import io.protostuff.compiler.parser.*;

import com.git.lee.serde.example.pb.schema.dynamic.DynamicSchema;
import com.git.lee.serde.example.pb.schema.dynamic.EnumDefinition;
import com.git.lee.serde.example.pb.schema.dynamic.MessageDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author lishuaiwei
 *         Date: 16/12/28
 *         Time: 上午11:34
 */
@Singleton
public class SchemaService {

    @Inject
    private Importer importer;
    private final static String SCHEMA_PREFIX = "pb_schema_";
    private final static String MSG_REGEXP = "message(.*)\\{";
    private final static Pattern pattern = Pattern.compile(MSG_REGEXP);
    private final static String PB_TEMP_DIRECTORY = "pb_temp_directory";
    private Map<Long, List<String>> msgTypeOfChannel = new HashMap<>();

    public DynamicSchema parseProto(long messageId, String input) throws Exception {
        DynamicSchema schema = null;
        if (input != null) {
            String protoFile = UUID.randomUUID() + ".proto";
            Path tempDirectory = Files.createTempDirectory(PB_TEMP_DIRECTORY);
            Path file = Files.write(tempDirectory.resolve(protoFile), input.getBytes());

            List<String> msgTypeNames = parseAndGetMsgType(input);
            try {
                DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
                schemaBuilder.setName(SCHEMA_PREFIX + messageId);
                if (msgTypeNames != null) {
                    for (String msgType : msgTypeNames) {
                        schemaBuilder.addMessageDefinition(defineMessage(tempDirectory, protoFile, msgType));
                    }
                    msgTypeOfChannel.put(messageId, msgTypeNames);
                }
                schema = schemaBuilder.build();
            } finally {
                Files.delete(file);
                Files.delete(tempDirectory);
            }
        }
        return schema;
    }

    public Map<String, Map<String, Object>> parseSchema(DynamicSchema schema, long messageId) throws Exception {
        Map<String, Map<String, Object>> fieldMap = new LinkedHashMap<>();
        List<String> msgTypeNames = msgTypeOfChannel.get(messageId);
        if (msgTypeNames != null) {
            for (String msgTypeName : msgTypeNames) {
                Descriptors.Descriptor descriptor = schema.getMessageDescriptor(msgTypeName);
                Map<String, Object> fm = getFieldMap(descriptor);
                if (fm != null) {
                    fieldMap.put(msgTypeName, fm);
                }
                List<Descriptors.Descriptor> nestedDescriptors = descriptor.getNestedTypes();
                if (fm != null) {
                    for (Descriptors.Descriptor ds : nestedDescriptors) {
                        //map
                        fm.put(ds.getName(), getFieldType("map", null));
                    }
                }
            }
        }
        return fieldMap;
    }

    private MessageDefinition defineMessage(Path tempDirectory, String protoFile, String messageTypeName) throws Exception {
        ProtoContext context = importer.importFile(new LocalFileReader(tempDirectory), protoFile);
        MessageDefinition.Builder builder = MessageDefinition.newBuilder(messageTypeName);
        Message message = context.getProto().getMessage(messageTypeName);
        if (message != null) {
            List<Message> messages = message.getMessages();
            List<String> subMsgNames = new ArrayList<>();
            if (messages != null) {
                //map type
                for (Message msg : messages) {
                    String jsonName = getEntryJsonName(msg.getName());
                    msg.setName(jsonName);
                    subMsgNames.add(jsonName);
                    builder.addMessageDefinition(buildMapMessage(msg));
                }
            }
            List<Field> fields = message.getFields();
            if (fields != null && fields.size() > 0) {
                for (Field field : fields) {
                    if (!subMsgNames.contains(field.getName())) {
                        addField(builder, field);
                    }
                }
            }
        }
        return builder.build();
    }

    private MessageDefinition buildMapMessage(Message message) {
        MessageDefinition.Builder builder = MessageDefinition.newBuilder(message.getName());
        if (message.isMapEntry()) {
            addFields(builder, message);
        }
        return builder.build();
    }

    private String getEntryJsonName(String entryName) {
        String jsonName = entryName;
        int inx = entryName.indexOf("_entry");
        if (inx != -1) {
            jsonName = entryName.substring(0, inx);
        }
        return jsonName;
    }

    private void addFields(MessageDefinition.Builder builder, Message message) {
        List<Field> fields = message.getFields();
        if (fields != null && fields.size() > 0) {
            for (Field field : fields) {
                addField(builder, field);
            }
        }
    }

    private void addField(MessageDefinition.Builder builder, Field field) {
        if (field.getType().isEnum()) {
            io.protostuff.compiler.model.Enum e = (io.protostuff.compiler.model.Enum) field.getType();
            List<EnumConstant> constants = e.getConstants();
            builder.addEnumDefinition(defineEnum(field.getTypeName(), constants));
        } else {
            builder.addField(field.getModifier().name().toLowerCase(), field.getType().getName(), field.getName(), field.getIndex());
        }
    }

    private Map<String, Object> getFieldMap(Descriptors.Descriptor descriptor) {
        Map<String, Object> fieldMap = null;
        if (descriptor != null) {
            List<Descriptors.EnumDescriptor> enumDescriptors = descriptor.getEnumTypes();
            List<Descriptors.FieldDescriptor> fieldDescriptors = descriptor.getFields();
            Map<String, Object> enumFields = enumField(enumDescriptors);
            Map<String, Object> fields = field(fieldDescriptors);
            fieldMap = getOrCreateMap(fieldMap);
            if (enumFields != null) {
                fieldMap.putAll(enumFields);
            }
            if (fields != null) {
                fieldMap.putAll(fields);
            }
        }
        return fieldMap;
    }

    private Map<String, Object> enumField(List<Descriptors.EnumDescriptor> enumDescriptors) {
        Map<String, Object> fieldMap = null;
        if (enumDescriptors != null) {
            for (Descriptors.EnumDescriptor ed : enumDescriptors) {
                fieldMap = getOrCreateMap(fieldMap);
                fieldMap.put(changeEnumFieldName(ed.getName()), getFieldType("string", null));
            }
        }
        return fieldMap;
    }

    private Map<String, Object> field(List<Descriptors.FieldDescriptor> fieldDescriptors) {
        Map<String, Object> fieldMap = null;
        if (fieldDescriptors != null) {
            for (Descriptors.FieldDescriptor fd : fieldDescriptors) {
                fieldMap = getOrCreateMap(fieldMap);
                fieldMap.put(fd.getJsonName(), getFieldType(fd.getJavaType().name(), fd.toProto().getTypeName()));
            }
        }
        return fieldMap;
    }

    private Map<String, Object> getOrCreateMap(Map<String, Object> map) {
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        return map;
    }

    private EnumDefinition defineEnum(String typeName, List<EnumConstant> constants) {
        EnumDefinition.Builder builder = EnumDefinition.newBuilder(typeName);
        if (constants != null) {
            for (EnumConstant c : constants) {
                builder.addValue(c.getName(), c.getValue());
            }
        }
        return builder.build();
    }

    private String changeEnumFieldName(String enumName) {
        String changedName = enumName;
        int inx = enumName.indexOf(".");
        if (inx != -1) {
            String en = enumName.substring(inx + 1);
            changedName = enumName.substring(0, inx + 1) + en.substring(0, 1).toLowerCase() + en.substring(1);
        } else {
            changedName = enumName.substring(0, 1).toLowerCase() + enumName.substring(1);
        }
        return changedName;
    }

    private Object getFieldType(String type, String defaultType) {
        if ("string".equalsIgnoreCase(type)) {
            return String.class;
        } else if ("map".equalsIgnoreCase(type)) {
            return Map.class;
        } else if ("double".equalsIgnoreCase(type)) {
            return Double.class;
        } else if ("long".equalsIgnoreCase(type)) {
            return Long.class;
        } else if ("int".equalsIgnoreCase(type)) {
            return Integer.class;
        } else if ("bool".equalsIgnoreCase(type) || "boolean".equalsIgnoreCase(type)) {
            return Boolean.class;
        } else if ("BYTE_STRING".equalsIgnoreCase(type)) {
            return ByteString.class;
        } else if (defaultType != null) {
            return getFieldType(defaultType, null);
        }

        return type;
    }

    private List<String> parseAndGetMsgType(String input) {
        List<String> msgTypeNames = null;
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            if (msgTypeNames == null) {
                msgTypeNames = new ArrayList<>();
            }
            msgTypeNames.add(matcher.group(1).trim());
        }
        return msgTypeNames;
    }

}
