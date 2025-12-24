# WFBarn

基于 Jetpack Compose for Desktop 构建的仓位、金融与金钱管理系统，旨在实现对个人财富的宏观把控与经济分析。

## 核心功能

- **多资产管理**：支持股票、基金、现金、货币基金、比特币、债券及可转债的统一追踪。
- **每日复盘**：记录每日盈亏，实时更新各持仓内容，精准把控财富波动。
- **资金流水**：涵盖资金入账（如工资）与日常消费记录。
- **宏观经济曲线**：支持自定义宏观经济指标记录，并提供可视化趋势图表。
- **本地持久化**：数据存储于 Windows 用户的 `Documents/WFBarn` 目录下，确保隐私与安全。

## 环境要求

- JDK 17 或更高版本

## 快速开始

在开发模式下运行应用：

```bash
./gradlew run
```

## 数据存储

应用数据以 JSON 格式持久化存储在：
`C:\Users\<您的用户名>\Documents\WFBarn\state.json`

## 技术栈

- [Kotlin](https://kotlinlang.org/)
- [Jetpack Compose for Desktop](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
- [Kotlinx Datetime](https://github.com/Kotlin/kotlinx.datetime)
