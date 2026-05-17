package ca.spottedleaf.starlight.mixin.compat.sable;

import ca.spottedleaf.starlight.common.compat.sable.SableCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Conditional mixin into Sable's {@code dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot}.
 * <p>
 * Sable creates a private {@link net.minecraft.world.level.lighting.LevelLightEngine} inside the
 * constructor of {@code ServerLevelPlot} (a "plot" is one sub-level worth of chunks). That engine
 * is a plain {@code LevelLightEngine}, not the {@code ThreadedLevelLightEngine} used by main
 * worlds, and its chunks live outside the regular {@code ChunkMap}. ScalableLux's normal
 * {@code StarLightInterface} hook cannot resolve those chunks, so we want to leave that one
 * engine on the vanilla light path.
 * <p>
 * We do this by setting a thread-local marker for the duration of {@code ServerLevelPlot}'s
 * constructor. When the {@code new LevelLightEngine(...)} call inside runs, ScalableLux's
 * {@code LevelLightEngineMixin#construct} sees the marker and skips StarLightInterface
 * attachment for that engine.
 * <p>
 * The mixin is declared {@link Pseudo} so it does not error if Sable is absent: the target class
 * simply does not exist and the mixin becomes a no-op. It is also listed in a Sable-only mixin
 * config (loaded conditionally on Sable being present), giving us belt-and-suspenders safety.
 * <p>
 * <b>Note on staticness:</b> {@code @Inject(method = "<init>", at = @At("HEAD"))} runs <i>before</i>
 * the constructor's call to {@code super()}. At that point {@code this} is not yet a valid object
 * instance, so Mixin requires the handler to be {@code static}. The {@code @At("RETURN")} handler
 * could in principle be a non-static method, but we keep it static for symmetry and because the
 * handler only touches a {@link ThreadLocal} on {@code SableCompat} — it never reads {@code this}.
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot", remap = false)
public abstract class ServerLevelPlotMixin {

    @Inject(method = "<init>", at = @At("HEAD"))
    private static void scalablelux$markPlotEngineStart(final CallbackInfo ci) {
        SableCompat.markPlotEngineConstruction();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private static void scalablelux$markPlotEngineEnd(final CallbackInfo ci) {
        SableCompat.clearPlotEngineConstruction();
    }
}
