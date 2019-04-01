# NotificationPush
此应用可以通过fcm从一台手机向另一台手机转发通知

This app is for forwarding your notification from one phone to another using the fcm tunnel

## What you need
* 一台旧手机作为服务器(An old phone used as server)
* 一台可以接收fcm推送的主力机(Your daily phone which could receive fcm push)
* 完成！(finish!)

## What you need to do
* 下载最新release(Download the latest release)
* 为两台手机安装(Install for both phones)
* 将主力机上的token（就是那堆乱码）复制到server的输入框中(Copy the token from daily phone to the input box on the server)
* 取消电池优化(It's may be necessary to turn off the battery optimization)
* 在服务端切换start开关并授予相关权限(Switch the 'start' and allow the permission on server)
* 现在，你应该能收到推送了(Now,you should have been able to receive the notification)

### Only for users from China
* 服务端可能必须24小时挂梯！

# if you want to compiler by yourself
* 下载源码(Download the source code)
* 在firebase中建立新项目(Create a new project in firebase)
* 根据Google的firebase文档加入google-services.json文件(Add google-services.json file according to the official document of Google)
* 修改NotificationPush/app/src/main/java/com/RichardLuo/notificationpush/GetNotification.java中的常量Authorization和Sender为自己相应内容,你应该参照旧版fcm推送方法进行修改！
(Change the constant 'Authorization' and 'Sender' to what you have in firebase console(old one))
* 编译(Compiler)
