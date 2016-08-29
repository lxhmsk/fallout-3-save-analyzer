package game;

public class PlayerStats {

  public final int strength, perception, endurance, charisma, intelligence, agility, luck;

  public PlayerStats(int strength, int perception, int endurance, int charisma, int intelligence,
      int agility, int luck) {
    this.strength = strength;
    this.perception = perception;
    this.endurance = endurance;
    this.charisma = charisma;
    this.intelligence = intelligence;
    this.agility = agility;
    this.luck = luck;
  }
  
  @Override
  public String toString() {

    return new StringBuilder()
        .append(String.format("Str: %02d\n", strength))
        .append(String.format("Per: %02d\n", perception))
        .append(String.format("End: %02d\n", endurance))
        .append(String.format("Cha: %02d\n", charisma))
        .append(String.format("Int: %02d\n", intelligence))
        .append(String.format("Agi: %02d\n", agility))
        .append(String.format("Luc: %02d\n", luck))
        .toString();
  }
  
  public PlayerStats add(PlayerStats other) {
    return new PlayerStats(
        this.strength + other.strength,
        this.perception + other.perception,
        this.endurance + other.endurance,
        this.charisma + other.endurance,
        this.intelligence + other.intelligence,
        this.agility + other.agility,
        this.luck + other.luck);
  }
}