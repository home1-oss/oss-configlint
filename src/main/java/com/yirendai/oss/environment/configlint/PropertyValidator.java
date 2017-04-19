package com.yirendai.oss.environment.configlint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by melody on 2016/11/9.
 */
public class PropertyValidator implements FileValidator {

  private static final Logger log = LoggerFactory.getLogger(PropertyValidator.class);

  public void validate(final String path) {
    try (final FileReader fileReader = new FileReader(path)) {
      final StrictPropertyLoader spLoader = new StrictPropertyLoader();
      spLoader.load(new BufferedReader(fileReader));
    } catch (final IOException ex) {
      log.warn("error loading property file {}", path, ex);
    }
  }

  private static final class StrictPropertyLoader {
    private StringBuilder sb = new StringBuilder();
    private Set<String> uniqSet = new HashSet<>();

    private void load(final Reader reader) throws IOException {
      final char[] convtBuf = new char[1024];
      int limit;
      int keyLen;
      int valueStart;
      char currentChar;
      boolean hasSep;
      boolean precedingBackslash;

      final LineReader lr = new LineReader(reader);
      while ((limit = lr.readLine()) >= 0) {
        keyLen = 0;
        valueStart = limit;
        hasSep = false;

        //System.out.println("line=<" + new String(lineBuf, 0, limit) + ">");
        precedingBackslash = false;
        while (keyLen < limit) {
          currentChar = lr.lineBuf[keyLen];
          //need check if escaped.
          if ((currentChar == '=' || currentChar == ':') && !precedingBackslash) {
            valueStart = keyLen + 1;
            hasSep = true;
            break;
          } else if ((currentChar == ' ' || currentChar == '\t' || currentChar == '\f') && !precedingBackslash) {
            valueStart = keyLen + 1;
            break;
          }
          if (currentChar == '\\') {
            precedingBackslash = !precedingBackslash;
          } else {
            precedingBackslash = false;
          }
          keyLen++;
        }
        while (valueStart < limit) {
          currentChar = lr.lineBuf[valueStart];
          if (currentChar != ' ' && currentChar != '\t' && currentChar != '\f') {
            if (!hasSep && (currentChar == '=' || currentChar == ':')) {
              hasSep = true;
            } else {
              break;
            }
          }
          valueStart++;
        }
        final String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
        final String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
        if (log.isTraceEnabled()) {
          log.trace("found key: {}, value: {}", key, value);
        }

        if (this.uniqSet.contains(key)) {
          this.sb.append("duplicated key: ").append(key).append("\n");
        } else {
          this.uniqSet.add(key);
        }
      }

      if (this.sb.length() != 0) {
        throw new IllegalArgumentException(this.sb.toString());
      }
    }

    @SuppressWarnings("squid:S1226") // this code block is from JDK, we don't want to modify it.
    private String loadConvert(final char[] in, int off, final int len, char[] convtBuf) {
      if (convtBuf.length < len) {
        int newLen = len * 2;
        if (newLen < 0) {
          newLen = Integer.MAX_VALUE;
        }
        convtBuf = new char[newLen];
      }
      char oneChar;
      char[] out = convtBuf;
      int outLen = 0;
      int end = off + len;

      while (off < end) {
        oneChar = in[off++];
        if (oneChar == '\\') {
          oneChar = in[off++];
          if (oneChar == 'u') {
            // Read the xxxx
            int value = 0;
            for (int i = 0; i < 4; i++) {
              oneChar = in[off++];
              switch (oneChar) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                  value = (value << 4) + oneChar - '0';
                  break;
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                  value = (value << 4) + 10 + oneChar - 'a';
                  break;
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                  value = (value << 4) + 10 + oneChar - 'A';
                  break;
                default:
                  throw new IllegalArgumentException(
                      "Malformed \\uxxxx encoding.");
              }
            }
            out[outLen++] = (char) value;
          } else {
            if (oneChar == 't') {
              oneChar = '\t';
            } else if (oneChar == 'r') {
              oneChar = '\r';
            } else if (oneChar == 'n') {
              oneChar = '\n';
            } else if (oneChar == 'f') {
              oneChar = '\f';
            }
            out[outLen++] = oneChar;
          }
        } else {
          out[outLen++] = oneChar;
        }
      }
      return new String(out, 0, outLen);
    }
  }

  static final class LineReader {

    public LineReader(final InputStream inStream) {
      this.inStream = inStream;
      this.inByteBuf = new byte[8192];
    }

    public LineReader(final Reader reader) {
      this.reader = reader;
      this.inCharBuf = new char[8192];
    }

    byte[] inByteBuf;
    char[] inCharBuf;
    char[] lineBuf = new char[1024];
    int inLimit = 0;
    int inOff = 0;
    InputStream inStream;
    Reader reader;

    int readLine() throws IOException {
      int len = 0;
      char currentChar;

      boolean skipWhiteSpace = true;
      boolean isCommentLine = false;
      boolean isNewLine = true;
      boolean appendedLineBegin = false;
      boolean precedingBackslash = false;
      boolean skipLf = false;

      while (true) {
        if (inOff >= inLimit) {
          inLimit = (inStream == null) ? reader.read(inCharBuf)
              : inStream.read(inByteBuf);
          inOff = 0;
          if (inLimit <= 0) {
            if (len == 0 || isCommentLine) {
              return -1;
            }
            if (precedingBackslash) {
              len--;
            }
            return len;
          }
        }
        if (inStream != null) {
          //The line below is equivalent to calling a
          //ISO8859-1 decoder.
          currentChar = (char) (0xff & inByteBuf[inOff++]);
        } else {
          currentChar = inCharBuf[inOff++];
        }
        if (skipLf) {
          skipLf = false;
          if (currentChar == '\n') {
            continue;
          }
        }
        if (skipWhiteSpace) {
          if (currentChar == ' ' || currentChar == '\t' || currentChar == '\f') {
            continue;
          }
          if (!appendedLineBegin && (currentChar == '\r' || currentChar == '\n')) {
            continue;
          }
          skipWhiteSpace = false;
          appendedLineBegin = false;
        }
        if (isNewLine) {
          isNewLine = false;
          if (currentChar == '#' || currentChar == '!') {
            isCommentLine = true;
            continue;
          }
        }

        if (currentChar != '\n' && currentChar != '\r') {
          lineBuf[len++] = currentChar;
          if (len == lineBuf.length) {
            int newLength = lineBuf.length * 2;
            if (newLength < 0) {
              newLength = Integer.MAX_VALUE;
            }
            char[] buf = new char[newLength];
            System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
            lineBuf = buf;
          }
          //flip the preceding backslash flag
          if (currentChar == '\\') {
            precedingBackslash = !precedingBackslash;
          } else {
            precedingBackslash = false;
          }
        } else {
          // reached EOL
          if (isCommentLine || len == 0) {
            isCommentLine = false;
            isNewLine = true;
            skipWhiteSpace = true;
            len = 0;
            continue;
          }
          if (inOff >= inLimit) {
            inLimit = (inStream == null)
                ? reader.read(inCharBuf)
                : inStream.read(inByteBuf);
            inOff = 0;
            if (inLimit <= 0) {
              if (precedingBackslash) {
                len--;
              }
              return len;
            }
          }
          if (precedingBackslash) {
            len -= 1;
            //skip the leading whitespace characters in following line
            skipWhiteSpace = true;
            appendedLineBegin = true;
            precedingBackslash = false;
            if (currentChar == '\r') {
              skipLf = true;
            }
          } else {
            return len;
          }
        }
      }
    }
  }
}
