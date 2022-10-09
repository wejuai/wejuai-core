# wejuai-core
wejuai数据和业务核心

## 结构
- 最外层是gradle构建文件以及 gitlab runner配置
- resources中有初始化的城市信息json、spring cloud config服务器配置和日志配置
- src/java/com/wejuai/core:
   - config: 配置参数和需要初始化数据库的内容以及基础框架配置
   - repository: 连接数据库的jpa配置，包括mongo和mysql
   - service：业务功能，dto中为在service层面使用的数据传输对象
   - support：第三方服务支持以及使用方式
   - web：对外开放的接口
   - Application.java：项目启动类，直接运行mian方法启动

## 外部关联
- 腾讯开放平台的智能聊天
- 阿里云的oss
- wejuai-wx中的gateway服务

## 配置项
- resources/bootstrap.yml config：服务的帐号密码(该配置只在本地使用，在集群中会去环境变量中寻找冒号前面的参数)，详细配置去config-server查看
- config/DataInitConfig.java：控制台使用的帐号和密码
- .gitlab-ci.yml：gitlab runner配置文件，其中参数要在gitlab控制台配置

## 本地运行
1. 配置项以及其中的第三方服务开通
2. gradle build，其中github的仓库必须使用key才可以下载，需要在个人文件夹下的`.gradle/gradle.properties`中添加对应的`key=value`方式配置，如果不行，就去下载对应仓库的代码本地install一下
3. 启动配置项中的数据库
4. 运行Application.java的main方法

## docker build以及运行
- 运行gradle中的docker build task
- 如果配置了其中的第三方仓库可以运行docker push，会先build再push
- 运行方式 docker run {image name:tag}，默认是运行的profile为dev，可以通过环境变量的方式修改，默认启动配置参数在Dockerfile中