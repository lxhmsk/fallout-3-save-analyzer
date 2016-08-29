package save;

public class FormIdIndex {

  final int formIdIndex;

  public FormIdIndex(int formIdIndex) {
    this.formIdIndex = formIdIndex;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + formIdIndex;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    FormIdIndex other = (FormIdIndex) obj;
    if (formIdIndex != other.formIdIndex) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    return String.format("0x%06X", formIdIndex);
  }
  
}
