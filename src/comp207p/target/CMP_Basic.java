public class CMP_Basic {
  public boolean m1(){
    return 3 > 4;
  }
  public boolean m2(){
    int a = 3;
    return a > 4;
  }
  public boolean m3(){
    int a  = 3;
    int b = 4;
    return a > b;
  }
  public boolean m4(){
    int a = 3;
    int b = 4;
    if(a > b){
      a = 5;
    }
    return a > b;
  }
  public boolean m5(){
    long a = 128L;
    long b = 2003L;
    return b > a;
  }
  public boolean m6(){
    int a = 1;
    for(int i = 0; i < 5; i++){
      a = 2;
    }
    return a > 4;
  }

  public boolean m7(){
    double a = 2.03;
    double b = 4.02;
    return b > a;
  }
}
