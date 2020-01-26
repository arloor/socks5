package protobuf;
public class Test {
    public static void main(String[] args) {

        Message.Request request=Message.Request.newBuilder()
                .setAck("a")
                .setMsgType(1)
                .setMsgTime(10000)
                .build();
        System.out.println(new String(request.toByteArray()));
    }
}
