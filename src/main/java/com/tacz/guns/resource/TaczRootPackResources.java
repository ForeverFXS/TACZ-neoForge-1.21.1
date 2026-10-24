package com.tacz.guns.resource;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 把 tacz 目录下的所有枪包（目录或 zip）聚合成一个资源包，并额外提供 TACZ 自身的
 * pack.png 与 pack metadata。
 * <p>
 * 1.21 移除了 Forge 的 {@code DelegatingPackResources} / {@code PathPackResources}，
 * 原实现依赖的 {@code SecureJar} 统一处理目录与 zip 的方式也不再适用。
 * 现在改为：目录枪包走 {@link net.minecraft.server.packs.PathPackResources}，
 * zip 枪包走 {@link net.minecraft.server.packs.FilePackResources}，
 * 聚合与 pack.png 由本类负责。
 */
public class TaczRootPackResources implements PackResources {
    private final PackLocationInfo location;
    private final List<PackResources> children;
    private final PackMetadataSection metadata;

    public TaczRootPackResources(PackLocationInfo location, PackType packType, List<PackResources> children) {
        this.location = location;
        this.children = children;
        this.metadata = new PackMetadataSection(
                Component.translatable("tacz.resources.modresources"),
                SharedConstants.getCurrentVersion().getPackVersion(packType));
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        if (paths.length == 1 && paths[0].equals("pack.png")) {
            Path logoPath = GunPackLoader.getModIcon("tacz");
            if (logoPath != null) {
                return IoSupplier.create(logoPath);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        for (PackResources child : this.children) {
            IoSupplier<InputStream> resource = child.getResource(type, location);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, PackResources.ResourceOutput output) {
        for (PackResources child : this.children) {
            child.listResources(type, namespace, path, output);
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        Set<String> namespaces = new HashSet<>();
        for (PackResources child : this.children) {
            namespaces.addAll(child.getNamespaces(type));
        }
        return namespaces;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        if (serializer == PackMetadataSection.TYPE) {
            return (T) this.metadata;
        }
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return this.location;
    }

    @Override
    public void close() {
        for (PackResources child : this.children) {
            child.close();
        }
    }
}
