package jake2.qcommon.network.messages.server;

import jake2.qcommon.MSG;
import jake2.qcommon.sizebuf_t;

public class LayoutMessage extends ServerMessage {
    public String layout;

    public LayoutMessage() {
        super(ServerMessageType.svc_layout);
    }

    public LayoutMessage(String layout) {
        this();
        this.layout = layout;
    }

    @Override
    protected void writeProperties(sizebuf_t buffer) {
        MSG.WriteString(buffer, layout);
    }

    @Override
    void parse(sizebuf_t buffer) {
        this.layout = MSG.ReadString(buffer);
    }

    @Override
    int getSize() {
        return 1 + layout.length() + 1;
    }

    @Override
    public String toString() {
        return "LayoutMessage{" +
                "layout='" + layout + '\'' +
                '}';
    }
}
