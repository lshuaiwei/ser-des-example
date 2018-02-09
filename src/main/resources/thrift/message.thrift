namespace java com.git.lee.serde.example.thrift.model

enum Type {
	TRANSACTION = 1,
	EVENT = 2	
}

struct ThriftMessage {
	1: i32 id,
	2: Type type,
    3: i64 timestamp,
    4: map<string,string> tags,
    5: list<ThriftMessage> children
}