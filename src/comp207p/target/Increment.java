public class Increment {
  public int method_one(){
    int a = 3;
    int b = 4;
    b++;
    b += 10l;
    int c = a + b;
    return c;
  }

  public int method_two(){
    long a = 3;
    int b = 4;
    b++;
    b += 10l;
    int c = (int)a + b;
    return c;
  }
}
