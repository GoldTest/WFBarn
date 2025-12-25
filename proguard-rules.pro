# 禁用耗时的优化和混淆，只保留压缩（Shrinking），这能极大地加快构建速度
-dontoptimize
-dontobfuscate

# 基础规则
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 保留入口类
-keep class MainKt { *; }

# 保留 Compose 关键组件
-keep class androidx.compose.ui.platform.** { *; }
-dontwarn androidx.compose.ui.platform.**
-dontwarn org.jetbrains.skiko.MainKt

# 保留序列化相关代码
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# 保留数据模型
-keep class com.wfbarn.models.** { *; }

# 注意：移除了对 material.icons.** 的全量保留
# 让 ProGuard 自动移除未使用的图标类，这是减小体积的关键
