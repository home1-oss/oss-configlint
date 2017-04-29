## 简介
检查yml和properties配置文件的合法性。

+ 针对yml的检查，使用SpringBoot的PropertySourcesLoader，与configserver中检查yml的方式一致；
+ 对于properties文件，Spring使用的是jdk自带的Properties类进行加载。由于properties的格式非常宽松自由，这里只简单加入了key冲突检查。

## 使用
+ 编译

```  
    mvn clean package
```  

+ 使用示例

```
    java -jar oss-configlint-1.0.6.OSS.jar $文件名列表
```  

## 推荐使用方式
建议在所有`config项目`中配置`git`的`pre-commit`，在本地提交之前检查配置文件的合法性。

### git的 pre-commit 配置
以 common-config 为例，配置步骤如下：  

+ 在项目根目录加入编译之后的`configlint`的jar包。

```
    $ ls
    application.yml  README.md  oss-configlint-1.0.6.OSS.jar
```  

+ 配置`git`的`pre-commit`。在项目根目录，编辑 `.git/hooks/pre-commit`, 加入如下内容：

```  
    java -jar oss-configlint-1.0.6.OSS.jar application.yml
```  

保存之后，给刚才的`pre-commit`文件加入可执行权限。`chmod +x .git/hooks/pre-commit`。  

+ 配置完成。

### 测试  
+ 我们在`application.yml`中加入非法内容, 在同一段内key重复。  

```
    spring:
      profiles: production.env

    spring:
      resources.cache-period: 86400
      freemarker.cache: true
```  

+ git add添加文件之后，执行commit命令。如下所示，git提交失败，并将错误信息展示出来。  

```
    $ git commit -m"modify application.yml"  

    文件校验错误：application.yml
    while parsing MappingNode
     in 'reader', line 77, column 1:
        spring:
        ^
    Duplicate key: spring
     in 'reader', line 91, column 1:
        ---
        ^
```

### 将git的pre-commit加入版本控制

默认的 git hooks 脚本在 `.git/hooks` 目录，该目录不在版本控制里。如果希望将`pre-commit`的脚本远程共享，有以下两种解决方案：

**git version < 2.9**

+ 对于低版本的git，可以将`pre-commit`放到项目目录，每次从远程下载下来项目时，建立从`项目目录`到`.git/hooks`目录下的软链接。
+ 该方式使用不便，而且 windows 开发平台对软链接的支持很弱。

**git version >= 2.9**

+ 在项目目录下创建用于存放`hook`脚本的目录
+ 使用`git config --local hooksPath ${myHooksPath}`，设置hooks脚本目录即可。

> 建议使用高版本的git。
