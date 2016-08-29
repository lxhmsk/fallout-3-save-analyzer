package save;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import save.SaveFile.LoadException;

public class Fo3ByteBuffer {

  private final File file;

  private final ByteBuffer data;
  private int previousPosition;

  public Fo3ByteBuffer(File f) throws IOException {
    this.file = f;
    this.data = ByteBuffer.wrap(Files.readAllBytes(f.toPath()));
    this.data.order(ByteOrder.LITTLE_ENDIAN);
  }

  public Fo3ByteBuffer(Fo3ByteBuffer base) {
    this.file = base.file;
    this.data = ByteBuffer.wrap(base.data.array());
    this.data.order(ByteOrder.LITTLE_ENDIAN);
  }
  
  public File getFile() {
    return file;
  }
  
  public int numBytes() {
    return data.capacity();
  }

  public int remainingBytes() {
    return data.remaining();
  }

  public int position() {
    return data.position();
  }

  public void position(int position) {
    this.data.position(position);
  }
  
  public String positionHex() {
    return "0x" + Integer.toHexString(position()).toUpperCase();
  }

  public int previousPosition() {
    return previousPosition;
  }
  
  private void recordPosition() {
    this.previousPosition = data.position();
  }
  
  /******************************************
   * Generic data types
   ******************************************/

  /**
   * Absolute position, doesn't change position in buffer.
   */
  public byte getByte(int index) {
    recordPosition();
    return this.data.get(index);
  }
  
  /**
   * Reads a byte and a pipe.
   */
  public byte readByte() {
    return readByte(true);
  }

  public byte readByte(boolean readPipe) {
    recordPosition();
    byte b = data.get();
    if (readPipe) {
      assertPipe(false);
    }
    return b;
  }

  public byte[] readBytes(int length) {
    byte[] bytes = new byte[length];
    recordPosition();
    data.get(bytes);
    return bytes;
  }

  /**
   * Reads a byte and asserts that it is the expected value, and reads a pipe.
   */
  public byte assertByte(int expected) throws LoadException {
    return assertByte(expected, true);
  }

  /**
   * Reads a byte and asserts that it is the expected value.
   */
  public byte assertByte(int expected, boolean readPipe) throws LoadException {
    recordPosition();
    byte actual = data.get();
    if (actual != expected) {
      throw new LoadException(
          "Expected 0x%02X got 0x%02X at 0x%08X", expected, actual, previousPosition());
    }
    if (readPipe) {
      assertPipe(false);
    }
    return actual;
  }

  /**
   * Reads a 4 byte int and a pipe.
   */
  public int readInt() {
    return readInt(true);
  }

  public int readInt(boolean readPipe) {
    recordPosition();
    int i = data.getInt();
    if (readPipe) {
      assertPipe(false);
    }
    return i;
  }

  public int assertInt(int expected, boolean readPipe) throws LoadException {
    int actual = readInt(readPipe);
    if (actual != expected) {
      throw new LoadException(
          "Expected 0x%08X at 0x%08X but got 0x%08X ", expected, actual,
          previousPosition());
    }
    return expected;
  }

  /**
   * Reads a short (2 bytes) and a pipe.
   */
  public short readShort() {
    return readShort(true);
  }

  public short readShort(boolean readPipe) {
    recordPosition();
    short s = data.getShort();
    if (readPipe) {
      assertPipe(false);
    }
    return s;
  }

  /**
   * Reads a 4 byte float and a pipe.
   */
  public float readFloat() {
    return readFloat(true);
  }

  public float readFloat(boolean readPipe) {
    recordPosition();
    float f = data.getFloat();
    if (readPipe) {
      assertPipe(false);
    }
    return f;
  }

  /**
   * Reads an 8 byte float and a pipe.
   */
  public double readDouble() {
    return readDouble(true);
  }

  public double readDouble(boolean readPipe) {
    recordPosition();
    double d = data.getDouble();
    if (readPipe) {
      assertPipe(false);
    }
    return d;
  }
  
  public String readString(int length) {
    return readString(length, true);
  }

  public String readString(int length, boolean readPipe) {
    byte[] bytes = new byte[length];
    recordPosition();
    data.get(bytes);
    if (readPipe) {
      assertPipe(false);
    }
    return new String(bytes);
  }

  /*****************************************************
   * Methods specialized for FO3 data
   *****************************************************/

  /**
   * Reads a byte and asserts that it's a pipe.
   *
   * This is private because it gives the option to not
   * record the* previous position in the data buffer.
   * Other methods in this class call readPipe(), and if
   * readPipe() recorded the pipe's position, then
   * code that checks the values returned from the
   * other methods would get the pipe's position
   * and not the position of the data it's actually
   * interested in.
   */
  private void assertPipe(boolean recordPosition) {
    if (recordPosition) {
      recordPosition();
    }
    byte actual = data.get();
    if (actual != 0x7C) {
      throw new AssertionError(String.format(
          "Expected pipe at 0x%08X but got 0x%02X", actual, data.position() - 1));
    }
  }

  public void assertPipe() {
    assertPipe(true);
  }
  
  /**
   * Reads a form index: a 3-byte big-ending value and a pipe.
   */
  public FormIdIndex readFormIdIndex() {
    return readFormIdIndex(true);
  }
  
  /**
   * Reads a form index: a 3-byte big-ending value.
   */
  public FormIdIndex readFormIdIndex(boolean readPipe) {

    byte b1 = readByte(false);
    byte b2 = readByte(false);
    byte b3 = readByte(false);
    
    int formIdIndex = (0xFF0000 & (b1 << 16)) |
                      (0x00FF00 & (b2 <<  8)) |
                      (0x0000FF & b3);

    if (readPipe) {
      assertPipe(false);
    }
    return new FormIdIndex(formIdIndex);
  }

  /**
   * Reads a number based on the number of bytes given. Does not read a pipe.
   */
  public int readNumber(int numBytes) throws LoadException {
    recordPosition();
    if (numBytes == 1) {
      return (0xFF & data.get());
    } else if (numBytes == 2) {
      return (0xFFFF & data.getShort());
    } else if (numBytes == 4) {
      return data.getInt();
    } else {
      throw new LoadException("numBytes must be 1, 2, or 4, not " + numBytes);
    }
  }

  /**
   * Reads an unsigned varint and a pipe.
   */
  public int readUvarint() throws LoadException {

    // Bottom 2 bits of the first byte indicate how many bytes are in the number
    // overall: 0b00 = 1 byte, 0b01 = 2 bytes, 0b10 = 4 bytes.

    recordPosition(); // Don't record the positions for the other bytes.
    byte byte1 = data.get();
    int byteCount = (1 << (byte1 & 0b11));

    // The varints in the save file appear
    // to all be unsigned, so throw away
    // the sign extensions (if they were
    // signed, then some inventories from
    // save files would have negative
    // counts). 

    int value;
    if (byteCount == 1) {

      value = (byte1 >> 2) & 0x3F;

    } else if (byteCount == 2) {

      byte byte2 = data.get();

      value = ((byte2 << 8) & 0xFF00) |
              ((byte1 << 0) & 0x00FF);

      value = (value >> 2) & 0x3FFF; 

    } else if (byteCount == 4) {

      byte byte2 = data.get();
      byte byte3 = data.get();
      byte byte4 = data.get();

      value = ((byte4 << 24) & 0xFF00_0000) |
              ((byte3 << 16) & 0x00FF_0000) |
              ((byte2 <<  8) & 0x0000_FF00) |
              ((byte1 <<  0) & 0x0000_00FF);

      value = (value >> 2) & 0x3FFF_FFFF;

    } else {
      // Presumably, 0b11 means it's an 8 byte number, but there don't
      // appear to be any in the save file.
      throw new LoadException("Varint size is not 1, 2, or 4 for 0x%02X: %d", byte1, byteCount);
    }

    assertPipe(false);
    return value;
  }

  public String readBString() {
    short length = readShort();
    return readString(length);
  }

  public void skipBytes(int n) {
    data.position(data.position() + n);
  }

  /******************************************
   * Debugging
   ******************************************/

  /**
   * Does not change the position in the buffer.
   */
  public String dumpBytes(int length, boolean spacing) {

    StringBuilder hex = new StringBuilder();

    int start = data.position();
    for (int i = 0; i < length; i++) {

      byte b = data.get(start + i);
      hex.append(String.format("%02X", b));

      if (spacing && i > 0 && i % 4 == 0) {
        hex.append(" ");
      }
    }
    return hex.toString();
  }

  /**
   * Does not change the position in the buffer.
   */
  public String dumpAscii(int length, boolean spacing) {

    StringBuilder ascii = new StringBuilder();

    int start = data.position();
    for (int i = 0; i < length; i++) {
      byte b = data.get(start + i);
      char c = (b > 31 && b < 127) ? (char) b : '.';
      if (spacing) {
        ascii.append(' ');
      }
      ascii.append(c);
    }
    return ascii.toString();
  }

  @Override
  public String toString() {
    return String.format("Position: 0x%08X", data.position());
  }
}
