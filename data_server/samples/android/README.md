# Android HTTP 上报的安全序列化示例

参考 SLANManager 原有 Double 序列化器会输出裸 NaN，无法作为标准 JSON 接收。`SafeExperimentJson.java` 可替代原 GsonBuilder：

```java
String uploadMsg = SafeExperimentJson.serialize(experiment);
// 继续使用原 POST /upload、application/json 和 Experiment 根对象。
```

数值 NaN、正负 Infinity 转成显式 null，保留合法 0、有限数值和文本，不修改实验对象。示例覆盖 float/double 及其包装类型。接收端维持严格 JSON 校验，不把无效数值静默当作 0。

该示例已用 Gson 2.8.9 检查；本轮未改动 Android 工程或重新发布设备软件。移入设备工程时应添加对应 package，并将 HTTP 成功条件改为 `response.isSuccessful()` 且业务 Result 等于 `"1"`，不要把所有 onResponse 当成成功。
