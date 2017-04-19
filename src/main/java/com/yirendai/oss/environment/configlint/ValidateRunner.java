package com.yirendai.oss.environment.configlint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by melody on 2016/11/9.
 */
public class ValidateRunner {

  private static final Logger log = LoggerFactory.getLogger(PropertyValidator.class);

  private Map<String, FileValidator> validators = new HashMap<>();
  private Map<String, String> fileValidateMappings = new HashMap<>();

  public ValidateRunner() {
    // TODO: 修改为从配置文件加载
    this.fileValidateMappings.put("yml", "YmlValidator");
    this.fileValidateMappings.put("yaml", "YmlValidator");
    this.fileValidateMappings.put("property", "PropertyValidator");
    this.fileValidateMappings.put("properties", "PropertyValidator");
  }

  public int process(final String... args) {
    int status = 0;
    for (final String path : args) {
      final String suffix = verify(path);
      if (suffix == null) {
        continue;
      }

      try {
        final FileValidator validator = this.getValidator(suffix);
        if (validator != null) {
          validator.validate(path);
        } else {
          log.warn("validator for {} not found.", path);
          status = 1;
        }
      } catch (final Exception ex) {
        log.warn("文件校验错误 {}, {}", path, ex.getMessage(), ex);
        status = 1;
      }
    }

    return status;
  }

  private String verify(final String path) {
    final String fileSuffix = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
    if (!this.fileValidateMappings.containsKey(fileSuffix)) {
      log.warn("不支持的文件类型: {}", path);
      return null;
    }

    final File file = new File(path);
    if (!file.isFile()) {
      log.warn("指定的path不是文件: {}", path);
      return null;
    }
    if (!file.exists()) {
      log.warn("文件不存在: {}", path);
      return null;
    }

    return fileSuffix;
  }

  private FileValidator getValidator(final String suffix) {
    final String clsName = this.getClass().getPackage().getName() + "." + this.fileValidateMappings.get(suffix);
    FileValidator validator = this.validators.get(clsName);
    if (validator == null) {
      Class<?> cls;
      try {
        cls = Class.forName(clsName);
        validator = (FileValidator) cls.newInstance();
        this.validators.put(clsName, validator);
      } catch (final Exception ex) {
        log.warn("error get validator, suffix {}", suffix, ex);
      }
    }

    return validator;
  }
}
