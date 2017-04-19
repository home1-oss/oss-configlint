package com.yirendai.oss.environment.configlint;

public class ConfigLintApplication {

  public static void main(final String ...args) {
    if (args.length == 0) {
      System.out.println("请输入要检查的文件列表!");
      return;
    }

    ValidateRunner runner = new ValidateRunner();
    int status = runner.process(args);

    System.exit(status);
  }
}
