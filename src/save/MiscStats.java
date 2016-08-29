package save;

import save.SaveFile.LoadException;

public class MiscStats {

  public final int questsCompleted;
  public final int locationsDiscovered;
  public final int peopleKilled;
  public final int creaturesKilled;
  public final int locksPicked;
  public final int computersHacked;
  public final int stimpacksTaken;
  public final int radXTaken;
  public final int radAwayTaken;
  public final int chemsTaken;
  public final int timesAddicted;
  public final int minesDisarmed;
  public final int speechSuccesses;
  public final int pocketsPicked;
  public final int pantsExploded;
  public final int booksRead;
  public final int bobbleheadsFound;
  public final int weaponsCreated;
  public final int peopleMezzed;
  public final int captivesRescued;
  public final int sandmanKills;
  public final int paralyzingPunches;
  public final int robotsDisabled;
  public final int contractsCompleted;
  public final int corpsesEaten;
  public final int mysteriousStrangerVisits;

  private MiscStats(
      int questsCompleted,
      int locationsDiscovered,
      int peopleKilled,
      int creaturesKilled,
      int locksPicked,
      int computersHacked,
      int stimpacksTaken,
      int radXTaken,
      int radAwayTaken,
      int chemsTaken,
      int timesAddicted,
      int minesDisarmed,
      int speechSuccesses,
      int pocketsPicked,
      int pantsExploded,
      int booksRead,
      int bobbleheadsFound,
      int weaponsCreated,
      int peopleMezzed,
      int captivesRescued,
      int sandmanKills,
      int paralyzingPunches,
      int robotsDisabled,
      int contractsCompleted,
      int corpsesEaten,
      int mysteriousStrangerVisits) {

    this.questsCompleted = questsCompleted;
    this.locationsDiscovered = locationsDiscovered;
    this.peopleKilled = peopleKilled;
    this.creaturesKilled = creaturesKilled;
    this.locksPicked = locksPicked;
    this.computersHacked = computersHacked;
    this.stimpacksTaken = stimpacksTaken;
    this.radXTaken = radXTaken;
    this.radAwayTaken = radAwayTaken;
    this.chemsTaken = chemsTaken;
    this.timesAddicted = timesAddicted;
    this.minesDisarmed = minesDisarmed;
    this.speechSuccesses = speechSuccesses;
    this.pocketsPicked = pocketsPicked;
    this.pantsExploded = pantsExploded;
    this.booksRead = booksRead;
    this.bobbleheadsFound = bobbleheadsFound;
    this.weaponsCreated = weaponsCreated;
    this.peopleMezzed = peopleMezzed;
    this.captivesRescued = captivesRescued;
    this.sandmanKills = sandmanKills;
    this.paralyzingPunches = paralyzingPunches;
    this.robotsDisabled = robotsDisabled;
    this.contractsCompleted = contractsCompleted;
    this.corpsesEaten = corpsesEaten;
    this.mysteriousStrangerVisits = mysteriousStrangerVisits;
  }
  
  static MiscStats load(Fo3ByteBuffer data, FileLocations fileLocations) throws LoadException {

    // See http://www.uesp.net/wiki/Tes5Mod:Save_File_Format#Global_Data

    data.position(fileLocations.globalDataTable1Address);
    
    // The misc stats table seems to always appear first.
    data.assertInt(0, false);
    data.assertInt(0x87, false); // size of struct
    data.assertInt(26, true); // number of stats

    return new MiscStats(
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt(),
        data.readInt());
  }
  
}
