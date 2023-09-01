package jake2.server;

import jake2.qcommon.side.EnvType;
import jake2.qcommon.side.Environment;

import java.util.List;

@Environment(EnvType.SERVER)
public interface ServerUserCommand {
    void execute(List<String> args, GameImportsImpl gameImports, client_t client);
}
