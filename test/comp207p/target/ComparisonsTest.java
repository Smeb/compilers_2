package comp207p.target;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test comparisons
 */
public class ComparisonsTest {

  CMP_comparisons cmp = new CMP_comparisons();

  @Test
  public void TEST_I_GT(){
    assertEquals(false, cmp.I_GT());
  }
  @Test
  public void TEST_I_GTE(){
    assertEquals(false, cmp.I_GTE());
  }
  @Test
  public void TEST_I_LT(){
    assertEquals(true, cmp.I_LT());
  }
  @Test
  public void TEST_I_LTE(){
    assertEquals(true, cmp.I_LTE());
  }
  @Test
  public void TEST_I_EQ(){
    assertEquals(false, cmp.I_EQ());
  }
  @Test
  public void TEST_I_NEQ(){
    assertEquals(true, cmp.I_NEQ());
  }

  @Test
  public void TEST_L_GT(){
    assertEquals(false, cmp.L_GT());
  }
  @Test
  public void TEST_L_GTE(){
    assertEquals(false, cmp.L_GTE());
  }
  @Test
  public void TEST_L_LT(){
    assertEquals(true, cmp.L_LT());
  }
  @Test
  public void TEST_L_LTE(){
    assertEquals(true, cmp.L_LTE());
  }
  @Test
  public void TEST_L_EQ(){
    assertEquals(false, cmp.L_EQ());
  }
  @Test
  public void TEST_L_NEQ(){
    assertEquals(true, cmp.L_NEQ());
  }

  @Test
  public void TEST_F_GT(){
    assertEquals(false, cmp.F_GT());
  }
  @Test
  public void TEST_F_GTE(){
    assertEquals(false, cmp.F_GTE());
  }
  @Test
  public void TEST_F_LT(){
    assertEquals(true, cmp.F_LT());
  }
  @Test
  public void TEST_F_LTE(){
    assertEquals(true, cmp.F_LTE());
  }
  @Test
  public void TEST_F_EQ(){
    assertEquals(false, cmp.F_EQ());
  }
  @Test
  public void TEST_F_NEQ(){
    assertEquals(true, cmp.F_NEQ());
  }

  @Test
  public void TEST_D_GT(){
    assertEquals(false, cmp.D_GT());
  }
  @Test
  public void TEST_D_GTE(){
    assertEquals(false, cmp.D_GTE());
  }
  @Test
  public void TEST_D_LT(){
    assertEquals(true, cmp.D_LT());
  }
  @Test
  public void TEST_D_LTE(){
    assertEquals(true, cmp.D_LTE());
  }
  @Test
  public void TEST_D_EQ(){
    assertEquals(false, cmp.D_EQ());
  }
  @Test
  public void TEST_D_NEQ(){
    assertEquals(true, cmp.D_NEQ());
  }
}
