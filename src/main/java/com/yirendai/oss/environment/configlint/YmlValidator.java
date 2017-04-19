package com.yirendai.oss.environment.configlint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.PropertySourcesLoader;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;

/**
 * Created by melody on 2016/11/9.
 */
public class YmlValidator implements FileValidator {

  private static final Logger log = LoggerFactory.getLogger(PropertyValidator.class);

  public void validate(final String path) {
    // 运行时异常直接抛出，不处理
    try {
      final PropertySourcesLoader psLoader = new PropertySourcesLoader();
      final FileSystemResource res = new FileSystemResource(path);
      psLoader.load(res);
    } catch (final IOException ex) {
      log.warn("error loading yml file {}", path, ex);
    }
  }
}
