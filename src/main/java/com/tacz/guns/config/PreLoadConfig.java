package com.tacz.guns.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import java.nio.file.Path;
import java.util.List;

/**
 * 枪械包覆盖开关配置，存放于 {@code <gameDir>/tacz/tacz-pre.toml}。
 * <p>
 * 它刻意不走 FML 的配置生命周期：{@link com.tacz.guns.resource.GunPackLoader#discoverExtensions()}
 * 在资源包发现阶段就要读到这个开关，而那时标准的 mod 配置文件尚未加载（见该方法内的注释）。
 * <p>
 * <b>迁移说明（Forge 1.20.1 → NeoForge 1.21.1）</b>：
 * 旧实现通过继承 {@code ModConfig}（子类 {@code PreLoadModConfig}）伪造一个配置对象，
 * 再把它从 {@code ConfigTracker} 里摘出来以防重复加载。这条路在 NeoForge 1.21.1 已被完全封死：
 * <ul>
 *   <li>{@code net.neoforged.fml.config.ModConfig} 变为 {@code public final}，且构造器为包私有 → 无法继承；</li>
 *   <li>{@code IConfigSpec.ILoadedConfig} 是 {@code sealed} 接口，唯一许可子类
 *       {@code net.neoforged.fml.config.LoadedConfig} 也是包私有 record → 无法自实现；</li>
 *   <li>{@code ConfigTracker.configSets()/fileMap()} 变为包私有字段，
 *       替代品 {@code ModConfigs.getConfigSet()/getFileMap()} 返回
 *       {@code Collections.unmodifiableSet/Map} → 无法再做“从 tracker 摘除”的手术；</li>
 *   <li>{@code IConfigEvent} 与 {@code ModConfig#getHandler()} 已移除。</li>
 * </ul>
 * 由于 {@code ModConfigSpec.ConfigValue#get()} 在 {@code spec.loadedConfig == null} 时会直接抛异常，
 * 而这个配置又拿不到合法的 {@code ILoadedConfig}，因此改为直接持有
 * {@link CommentedFileConfig} 读写。对外契约保持不变：
 * {@link #override}{@code .get() / .set(boolean)}，5 处调用点无需修改。
 * <p>
 * 旧实现在加载后会派发一个 config loading 事件，现已不再派发：
 * 全库确认没有任何监听者（{@code LoadingConfigEvent} 只监听 {@code tacz-server.toml}）。
 */
public class PreLoadConfig {
    public static final String FILE_NAME = "tacz-pre.toml";

    private static final List<String> OVERRIDE_PATH = List.of("gunpack", "DefaultPackDebug");
    private static final String OVERRIDE_COMMENT =
            "When enabled, the mod will not try to overwrite the default pack under .minecraft/tacz\n"
                    + "Since 1.0.4, the overwriting will only run when you start client or a dedicated server";
    private static final boolean OVERRIDE_DEFAULT = false;

    public static final PreLoadBooleanValue override = new PreLoadBooleanValue();

    /**
     * 由 {@link #load(Path)} 赋值后刻意不关闭：
     * {@link PreLoadBooleanValue#get()} 需要在整个运行期反复读取，
     * 提前关闭会让后续读取作用在已释放的资源上。
     */
    private static CommentedFileConfig configData;

    public static void load(Path configBasePath) {
        if (configData != null) {
            return;
        }
        configData = CommentedFileConfig.builder(configBasePath.resolve(FILE_NAME)).sync().build();
        // 文件不存在时 nightconfig 默认创建空文件，此处补齐注释后写盘，
        // 与旧实现（builder.comment(...) 后 save()）的产物一致。
        configData.setComment(OVERRIDE_PATH, OVERRIDE_COMMENT);
        configData.save();
    }

    /**
     * 保持旧 {@code ModConfigSpec.BooleanValue} 的调用形态（{@code get()} / {@code set(boolean)}），
     * 使 {@code OverwriteCommand}、{@code OtherClothConfig}、{@code GunPackLoader} 无需改动。
     * {@code set} 同时可作为 {@code Consumer<Boolean>} 方法引用使用。
     */
    public static final class PreLoadBooleanValue {
        private PreLoadBooleanValue() {
        }

        /**
         * 配置尚未加载时返回默认值而非抛异常——
         * 旧实现在此场景会抛 "Cannot get config value before config is loaded"，
         * 但本配置的读取点（命令、配置界面、资源包发现）都发生在 {@link #load(Path)} 之后，
         * 返回默认值只会在异常路径上生效，且语义与“未开启覆盖”一致。
         */
        public boolean get() {
            if (configData == null) {
                return OVERRIDE_DEFAULT;
            }
            return configData.getOrElse(OVERRIDE_PATH, () -> OVERRIDE_DEFAULT);
        }

        public void set(boolean value) {
            if (configData == null) {
                throw new IllegalStateException(
                        "Cannot set PreLoadConfig.override before PreLoadConfig.load() has been called");
            }
            configData.set(OVERRIDE_PATH, value);
            configData.save();
        }

        public boolean getDefault() {
            return OVERRIDE_DEFAULT;
        }
    }
}
