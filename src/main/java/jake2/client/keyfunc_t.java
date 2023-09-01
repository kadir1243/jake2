package jake2.client;

import jake2.qcommon.side.EnvType;
import jake2.qcommon.side.Environment;

/**
 * Menu
 */

@Environment(EnvType.CLIENT)
abstract class keyfunc_t {
    abstract String execute(int key);
}
