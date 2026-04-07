package org.aliceincradle.pxlsloader;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * PxlsLoader 的全局不可变配置类。
 * 通过 Builder 模式构建，确保线程安全与参数合法性。
 */
public final class Settings {
    
    private final LoadFromPngFunction loadFromPngFunction;
    
    private Settings(Builder builder) {
        this.loadFromPngFunction = builder.loadFromPngFunction;
    }
    
    public LoadFromPngFunction getLoadFromPngFunction() {
        return loadFromPngFunction;
    }
    
    @FunctionalInterface
    public interface LoadFromPngFunction extends IntFunction<byte[]> {
        @Override
        @Nullable byte[] apply(int value);
    }
    
    public static class Builder {
        private LoadFromPngFunction loadFromPngFunction;
        
        public Builder() {
            loadFromPngFunction = i -> {
                throw new NotImplementedException("no loadFromPngFunction implemented");
            };
        }
        
        public Builder setLoadFromPngFunction(LoadFromPngFunction function) {
            this.loadFromPngFunction = function;
            return this;
        }
        
        public Settings build() {
            return new Settings(this);
        }
    }
}