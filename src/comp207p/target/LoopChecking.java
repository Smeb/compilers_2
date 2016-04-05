package comp207p.target;

public class LoopChecking {
  public void foldable_loop(){
    int a = 3;
    for(int i = 0; i < 5; i++){
      System.out.println(i);
    }
    int b = 3 + a;
  }

  public void nonfoldable_loop(){
    int a = 3;
    for(int i = 0; i < 5; i++){
      a = 5;
    }
    int b = 3 + a;
  }
}
