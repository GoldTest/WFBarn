# Compose Desktop 基础混淆规则
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

# 针对 materialIconsExtended 的优化：只保留被使用的图标
# ProGuard 会自动移除未引用的图标类，极大减小体积
-keep class androidx.compose.material.icons.** {
    public static androidx.compose.ui.graphics.vector.ImageVector get*;
}
