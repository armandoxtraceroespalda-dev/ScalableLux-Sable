package ca.spottedleaf.starlight.common;

import ca.spottedleaf.starlight.common.config.Config;
import net.neoforged.fml.common.Mod;

@Mod("scalablelux")
public class ScalableLuxEntrypoint {
    public ScalableLuxEntrypoint() {
        Config.init();
    }
}
