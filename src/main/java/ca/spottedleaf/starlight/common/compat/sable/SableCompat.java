package ca.spottedleaf.starlight.common.compat.sable;

import net.neoforged.fml.ModList;

/**
 * Compatibility shim for the Sable physics/sub-level mod.
 * <p>
 * Sable creates one extra {@link net.minecraft.world.level.lighting.LevelLightEngine} per "plot"
 * (sub-level). Each plot engine is a plain {@code LevelLightEngine} (not {@code ThreadedLevelLightEngine})
 * and lights only the small set of chunks belonging to that sub-level. Those chunks are
 * <b>not</b> registered in the parent world's {@code ServerChunkCache} / {@code ChunkMap}, so
 * ScalableLux's {@link ca.spottedleaf.starlight.common.light.StarLightInterface} cannot resolve
 * neighbour chunks for them and its scheduling hooks have nothing to attach to.
 * <p>
 * Additionally, Sable reads the vanilla {@code blockEngine} / {@code skyEngine} fields of the
 * plot's light engine to decide whether the plot has block/sky light enabled (see
 * {@code ServerLevelPlot} constructor and chunk-loading code). ScalableLux nulls those fields out
 * in its {@code LevelLightEngineMixin}, which would make Sable believe plots have no lighting
 * at all and produce pitch-black sub-levels.
 * <p>
 * The fix is to leave Sable's plot light engines alone: when one is being constructed, we set
 * a thread-local marker so that {@code LevelLightEngineMixin#construct} skips its initialisation
 * for that engine and leaves the vanilla light engines in place. Every overwritten method in
 * {@code LevelLightEngineMixin} then falls back to the vanilla implementation when no
 * {@code StarLightInterface} is attached. The main-world {@code ThreadedLevelLightEngine}
 * continues to receive the full ScalableLux treatment, so all performance gains are preserved
 * for the dimensions where they actually matter.
 */
public final class SableCompat {

    private SableCompat() {}

    /**
     * Lazily-evaluated and cached. We check the loaded mod list once.
     */
    private static final boolean SABLE_LOADED;

    static {
        boolean loaded = false;
        try {
            loaded = ModList.get() != null && ModList.get().isLoaded("sable");
        } catch (final Throwable ignored) {
            // ModList may not be initialised yet during early bootstrap; we'll just assume Sable
            // is absent in that case. The thread-local marker is the authoritative signal anyway.
        }
        SABLE_LOADED = loaded;
    }

    /**
     * Set while a Sable {@code ServerLevelPlot} constructor is creating its plot
     * {@link net.minecraft.world.level.lighting.LevelLightEngine}. Read by
     * {@code LevelLightEngineMixin#construct} to decide whether to skip ScalableLux init.
     */
    private static final ThreadLocal<Boolean> CONSTRUCTING_PLOT_ENGINE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static boolean isSableLoaded() {
        return SABLE_LOADED;
    }

    public static void markPlotEngineConstruction() {
        CONSTRUCTING_PLOT_ENGINE.set(Boolean.TRUE);
    }

    public static void clearPlotEngineConstruction() {
        CONSTRUCTING_PLOT_ENGINE.set(Boolean.FALSE);
    }

    public static boolean isConstructingPlotEngine() {
        return CONSTRUCTING_PLOT_ENGINE.get();
    }
}
