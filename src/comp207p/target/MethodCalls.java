package comp207p.target;

public class MethodCalls
{

  public int methodOne(){
    int a = 3;
    for(int i = 0; i < 10; i++){
      a = a * i;
    }
    return a + 3;
  }
}
