# NotificationPush
此应用可以通过fcm从一台手机向另一台手机转发通知

目前仅在酷安发布[传送门](https://www.coolapk.com/apk/223104)

## 需要
* 一台旧手机作为服务器
* 一台可以接收fcm推送的主力机

## 使用
* 下载最新release
* 为两台手机安装
* 将主力机上的token（就是那堆乱码）复制到server的输入框中
* 取消电池优化
* 在服务端切换start开关并授予相关权限
* 现在，你应该能收到推送了

### 对于中国大陆用户
* 服务端可能必须24小时挂梯！

## 自己编译
* 下载源码
* 在firebase中建立新项目
* 根据Google的firebase文档加入google-services.json文件
* 在根目录底下创建一个local.properties文件，然后写入
```
FCM_AUTHORIZATION="你对应的内容"
FCM_SENDER="你对应的内容"
```
* 编译