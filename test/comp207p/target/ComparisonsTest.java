package comp207p.target;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test comparisons
 */
public class ComparisonsTest {

  CMP_comparisons cmp = new CMP_comparisons();

  @Test
  public void TEST_I_GT_F(){
    assertEquals(false, cmp.I_GT_F());
  }
  @Test
  public void TEST_I_GTE_F(){
    assertEquals(false, cmp.I_GTE_F());
  }
  @Test
  public void TEST_I_LT_T(){
    assertEquals(true, cmp.I_LT_T());
  }
  @Test
  public void TEST_I_LTE_T(){
    assertEquals(true, cmp.I_LTE_T());
  }
  @Test
  public void TEST_I_EQ_F(){
    assertEquals(false, cmp.I_EQ_F());
  }
  @Test
  public void TEST_I_NEQ_T(){
    assertEquals(true, cmp.I_NEQ_T());
  }

  @Test
  public void TEST_L_GT_F(){
    assertEquals(false, cmp.L_GT_F());
  }
  @Test
  public void TEST_L_GTE_F(){
    assertEquals(false, cmp.L_GTE_F());
  }
  @Test
  public void TEST_L_LT_T(){
    assertEquals(true, cmp.L_LT_T());
  }
  @Test
  public void TEST_L_LTE_T(){
    assertEquals(true, cmp.L_LTE_T());
  }
  @Test
  public void TEST_L_EQ_F(){
    assertEquals(false, cmp.L_EQ_F());
  }
  @Test
  public void TEST_L_NEQ_T(){
    assertEquals(true, cmp.L_NEQ_T());
  }

  @Test
  public void TEST_F_GT_F(){
    assertEquals(false, cmp.F_GT_F());
  }
  @Test
  public void TEST_F_GTE_F(){
    assertEquals(false, cmp.F_GTE_F());
  }
  @Test
  public void TEST_F_LT_T(){
    assertEquals(true, cmp.F_LT_T());
  }
  @Test
  public void TEST_F_LTE_T(){
    assertEquals(true, cmp.F_LTE_T());
  }
  @Test
  public void TEST_F_EQ_F(){
    assertEquals(false, cmp.F_EQ_F());
  }
  @Test
  public void TEST_F_NEQ_T(){
    assertEquals(true, cmp.F_NEQ_T());
  }

  @Test
  public void TEST_D_GT_F(){
    assertEquals(false, cmp.D_GT_F());
  }
  @Test
  public void TEST_D_GTE_F(){
    assertEquals(false, cmp.D_GTE_F());
  }
  @Test
  public void TEST_D_LT_T(){
    assertEquals(true, cmp.D_LT_T());
  }
  @Test
  public void TEST_D_LTE_T(){
    assertEquals(true, cmp.D_LTE_T());
  }
  @Test
  public void TEST_D_EQ_F(){
    assertEquals(false, cmp.D_EQ_F());
  }
  @Test
  public void TEST_D_NEQ_T(){
    assertEquals(true, cmp.D_NEQ_T());
  }
}
