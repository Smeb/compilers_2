package comp207p.target;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test operators
 */
public class ExpressionsTest {

    Expressions exp = new Expressions();

    @Test
    public void testAdd(){
        assertEquals(7, exp.add());
    }
    @Test
    public void testSub(){
        assertEquals(1, exp.sub());
    }
    @Test
    public void testMul(){
        assertEquals(12, exp.mul());
    }
    @Test
    public void testDiv(){
        assertEquals(1, exp.div());
    }
    @Test
    public void testOr(){
        assertEquals(7, exp.or());
    }
    @Test
    public void testXor(){
        assertEquals(7, exp.xor());
    }
    @Test
    public void testAnd(){
        assertEquals(0, exp.and());
    }
    @Test
    public void testShl(){
        assertEquals(32, exp.shl());
    }
    @Test
    public void testShr(){
        assertEquals(0, exp.shr());
    }
}
