# NotificationPush
This app is for forwarding your notification from one phone to another using google's fcm

Currently it is only publishing in [CoolApk](https://www.coolapk.com/apk/223104)

## What do you need
* An old phone used as server
* Your daily phone which could receive fcm push

## What do you need to do
* Download the latest release
* Install for both phones
* Copy the token from daily phone to the input box on the server
* It may be necessary to turn off the battery optimization
* Switch the 'start' button and allow the permission on server
* Now,you should have been able to receive the notification

### Only for users from China
* 服务端可能必须24小时挂梯！

# If you want to compile by yourself
* Download the source code
* Create a new project in firebase
* Add google-services.json file according to the official document of Google
* Create a file local.properties under project root folder. Write
```
FCM_AUTHORIZATION="Your authorization key"
FCM_SENDER="Your sender number"
```
* Compile