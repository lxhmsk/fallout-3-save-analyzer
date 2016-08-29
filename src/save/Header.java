package save;

public class Header {

  public final String magic;
  public final int headerSize;
  public final int maybeVersion;
  public final int screenshotWidth;
  public final int screenshotHeight;
  public final int saveIndex;
  public final String name;
  public final String karma;
  public final int level;
  public final String location;
  public final String playtime;

  private Header(String magic, int headerSize, int maybeVersion, int screenshotWidth,
      int screenshotHeight, int saveIndex, String name, String karma, int level, String location,
      String playtime) {
    this.magic = magic;
    this.headerSize = headerSize;
    this.maybeVersion = maybeVersion;
    this.screenshotWidth = screenshotWidth;
    this.screenshotHeight = screenshotHeight;
    this.saveIndex = saveIndex;
    this.name = name;
    this.karma = karma;
    this.level = level;
    this.location = location;
    this.playtime = playtime;
  }
  
  static Header load(Fo3ByteBuffer data) {

    // See http://falloutmods.wikia.com/wiki/FOS_file_format

    String magic = data.readString(11, false);
    int headerSize = data.readInt(false);
    int maybeVersion = data.readInt();
    int screenshotWidth = data.readInt();
    int screenshotHeight = data.readInt();
    int saveIndex = data.readInt();
    String name = data.readBString();
    String karma = data.readBString();
    int level = data.readInt();
    String location = data.readBString();
    String playtime = data.readBString();

    return new Header(
        magic,
        headerSize,
        maybeVersion,
        screenshotWidth,
        screenshotHeight,
        saveIndex,
        name,
        karma,
        level,
        location,
        playtime);
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append(String.format("Magic: %s\n", magic))
        .append(String.format("Header Size: %s\n", headerSize))
        .append(String.format("Maybe Version: %s\n", maybeVersion))
        .append(String.format("Screenshot Width: %s\n", screenshotWidth))
        .append(String.format("Screenshot Height: %s\n", screenshotHeight))
        .append(String.format("Save Index: %s\n", saveIndex))
        .append(String.format("Name: %s\n", name))
        .append(String.format("Karma: %s\n", karma))
        .append(String.format("Level: %d\n", level))
        .append(String.format("Location: %s\n", location))
        .append(String.format("Playtime: %s\n", playtime))
        .toString();
  }
}
