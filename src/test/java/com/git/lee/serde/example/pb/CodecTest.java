package com.git.lee.serde.example.pb;

import com.git.lee.serde.example.pb.model.Message;
import com.git.lee.serde.example.thrift.model.ThriftMessage;
import com.git.lee.serde.example.thrift.model.Type;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LISHUAIWEI
 * @date 2018/2/9 14:14
 */
public class CodecTest {

    @Test
    public void codec() throws InvalidProtocolBufferException, TException {
        Message.MyMessage message = buildProtoMessage();
        byte[] data = message.toByteArray();

//        Message.MyMessage message1 = Message.MyMessage.parseFrom(data);
//        Assert.assertEquals(message.getId(), message1.getId());

        TSerializer serializer = new TSerializer(new TBinaryProtocol.Factory());
        ThriftMessage thriftMessage = buildThriftMessage();
        byte[] data2 = serializer.serialize(thriftMessage);

        System.out.printf("Protobuf 序列化后的大小为：%s, Thrift 序列化后的大小为：%s \n", data.length, data2.length );

        //warm up
        for (int i = 0; i < 1000; i++) {
            message.toByteArray();
            serializer.serialize(thriftMessage);
        }
        int n = 100000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            message.toByteArray();
        }
        System.out.printf("Protobuf序列化耗时：%s ms\n", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            serializer.serialize(thriftMessage);
        }
        System.out.printf("Thrift序列化耗时：%s ms\n", System.currentTimeMillis() - start);



        //decode
        TDeserializer deserializer = new TDeserializer();
        //warm up
        for (int i = 0; i < 1000; i++) {
            Message.MyMessage.parseFrom(data);
            deserializer.deserialize(thriftMessage, data2);
        }

        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            Message.MyMessage.parseFrom(data);
        }
        System.out.printf("Protobuf反序列化耗时：%s ms\n", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            deserializer.deserialize(thriftMessage, data2);
        }
        System.out.printf("Thrift反序列化耗时：%s ms\n", System.currentTimeMillis() - start);
        
    }

    private static Message.MyMessage buildProtoMessage() {
        Message.MyMessage.Builder builder = Message.MyMessage.newBuilder();
        builder.setId(1);
        builder.setType(Message.Type.TRANSACTION);
        builder.setTimestamp(System.currentTimeMillis());
        builder.putAllTags(buildTags());

        return builder.build();
    }

    private static ThriftMessage buildThriftMessage() {
        ThriftMessage message = new ThriftMessage();
        message.id = 1;
        message.type = Type.TRANSACTION;
        message.timestamp = System.currentTimeMillis();
        message.setTags(buildTags());
        return message;
    }

    private static Map<String, String> buildTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("tag_key1", "tag_value1");
        tags.put("tag_key2", "tag_value2");
        tags.put("tag_key3", "tag_value3");
        tags.put("tag_key4", "tag_value4");
        return tags;
    }
}
