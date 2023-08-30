package jake2.qcommon.network.messages.client;

import jake2.qcommon.network.messages.NetworkMessage;
import jake2.qcommon.sizebuf_t;

public abstract class ClientMessage implements NetworkMessage {
    public final ClientMessageType type;

    protected ClientMessage(ClientMessageType type) {
        this.type = type;
    }

    public final void writeTo(sizebuf_t buffer) {
        buffer.writeByte(type.value);
        writeProperties(buffer);
    }

    protected abstract void writeProperties(sizebuf_t buffer);

    public static ClientMessage parseFromBuffer(sizebuf_t buffer, int incomingSequence) {
        ClientMessageType type = ClientMessageType.fromByte((byte) buffer.readByte());
        final ClientMessage msg = switch (type) {
            case CLC_BAD -> new EndOfClientPacketMessage();
            case CLC_NOP ->
                // fixme: never sent by client directly
                    new NoopMessage();
            case CLC_USERINFO -> new UserInfoMessage();
            case CLC_STRINGCMD -> new StringCmdMessage();
            case CLC_MOVE -> new MoveMessage(incomingSequence);
        };
        msg.parse(buffer);
        return msg;
    }

}
