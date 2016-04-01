package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.ConversionInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC_W;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.util.InstructionFinder;



public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;
  final String constantPush = "(ConstantPushInstruction|LDC|LDC2_W)";

  boolean _DEBUG = true;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}

  private void optimise_method(ClassGen cgen, ConstantPoolGen cpgen, Method method){
    if(method == null){
      return;
    }

    // To manipulate the method we need a MethodGen, and an InstructionList

    MethodGen mGen = new MethodGen(method, cgen.getClassName(), cpgen);
    InstructionList il = new InstructionList(method.getCode().getCode());

    if(_DEBUG){
      System.out.println("Method: " + method);
      System.out.println("================================================");
      System.out.println("Pre-optimisation instructions:");
      printInstructions(il);
      System.out.println("================================================");
    }

    boolean optimised;
    do {
      optimised = false;
      optimised |= simple_fold_optimisation(cpgen, il);
    } while(optimised);

    if(_DEBUG){
      System.out.println("Optimised instructions:");
      printInstructions(il);
      System.out.println("================================================");
    }

    cgen.replaceMethod(method, mGen.getMethod());
  }

  private boolean optimise_conversions(ConstantPoolGen cpgen, InstructionList il){
    // Delete conversions only for constants from the pool
    boolean optimised = false;
    InstructionFinder f = new InstructionFinder(il);
    String conversionRegExp = constantPush + " " + "(ConversionInstruction)*" + " " +
                              constantPush + " " + "(ConversionInstruction)*" + " " +
                              "ArithmeticInstruction";
    Iterator it = f.search(conversionRegExp);
    while(it.hasNext()){
      InstructionHandle[] matches = (InstructionHandle[]) it.next();
      for(InstructionHandle h : matches){
          if(h.getInstruction() instanceof ConversionInstruction){
            try {
              il.delete(h);
            } catch (TargetLostException e){
              e.printStackTrace();
            }
          }
      }
    optimised = true;
    }
    return optimised;
  }

  private boolean optimise_arithmetic(ConstantPoolGen cpgen, InstructionList il){
    boolean optimised = false;
    // Since conversions are removed we should now have input in the
    // form push push op
    // 1. Find values in form push push operation
    // 2. Extract values from stack variables
    // 3. Find type of operation
    // 4. Compute new value of type of operation result
    // 5. Check if constant pool values used elsewhere, if not remove
    // them
    // 6. Remove pushes and operation from instruction list
    // 7. Add new push of constant pool value to instruction list
    // 8. Regenerate InstructionFinder f and repeat (new patterns may
    // have emerged).

    InstructionFinder f = new InstructionFinder(il);
    String simpleRegExp = constantPush + " " +
                          constantPush + " " +
                          "ArithmeticInstruction";

    InstructionHandle[] matches = null;
    for(Iterator it = f.search(simpleRegExp); it.hasNext();){
      optimised = true;
      matches = (InstructionHandle[]) it.next();
      if(_DEBUG){
        printMatches(matches);
      }

      Instruction left_i =  matches[0].getInstruction();
      Instruction right_i =  matches[1].getInstruction();
      Number left_v = get_value(cpgen, left_i);
      Number right_v = get_value(cpgen, right_i);
      ArithmeticInstruction op = (ArithmeticInstruction) matches[2].getInstruction();
      String opSig = op.getType(cpgen).getSignature();
      Double result = evaluate_op(left_v, right_v, op);

      // Change instruction handle to result type (in case there were
      // conversions)
      if(opSig.equals("D"))
        matches[0].setInstruction(new LDC2_W(cpgen.addDouble(result)));
      else if(opSig.equals("F"))
        matches[0].setInstruction(new LDC(cpgen.addFloat(result.floatValue())));
      else if(opSig.equals("J"))
        matches[0].setInstruction(new LDC2_W(cpgen.addLong(result.longValue())));
      else if(opSig.equals("I"))
        matches[0].setInstruction(new LDC(cpgen.addInteger(result.intValue())));

      // Delete unneeded instruction handles
      try {
        il.delete(matches[1], matches[2]);
      } catch (TargetLostException e) {
        e.printStackTrace();
      }
    }
    return optimised;
  }

  private Double evaluate_op(Number l, Number r, ArithmeticInstruction op) throws RuntimeException {
    String op_s = op.getClass().getSimpleName().substring(1, 4);
    if(op_s.equals("ADD")){
      return l.doubleValue() + r.doubleValue();
    }
    else if(op_s.equals("SUB")){
      return l.doubleValue() - r.doubleValue();
    }
    else if(op_s.equals("MUL")){
      return l.doubleValue() * r.doubleValue();
    }
    else if(op_s.equals("DIV")){
      return l.doubleValue() / r.doubleValue();
    }
    else {
      throw new RuntimeException("Operation: " + op.getClass() + " not recognized");
    }
  }

  private Number get_value(ConstantPoolGen cpgen, Instruction instruction){
    if(instruction instanceof ConstantPushInstruction){
      return (Number) ((ConstantPushInstruction) instruction).getValue();
    }
    else if(instruction instanceof LDC){
      return (Number) ((LDC) instruction).getValue(cpgen);
    }
    else if(instruction instanceof LDC2_W){
      return (Number) ((LDC2_W) instruction).getValue(cpgen);
    }
    else{
      System.out.println("Instruction: " + instruction + " not recognised");
      return -1;
    }
  }

  private boolean simple_fold_optimisation(ConstantPoolGen cpgen, InstructionList il){
    boolean optimised = false;
    optimised |= optimise_conversions(cpgen, il);
    optimised |= optimise_arithmetic(cpgen, il);
    return optimised;
  }

  private boolean constant_variable_folding(){
    // Optimise uses of local variables of type int, long, float,
    // double, whose value does not change throughout the scope of the
    // method -> after declaration, variable is not reassigned. Need to
    // propogate THROUGHOUT the method.
    return false;
  }
  private boolean dynamic_variable_folding(){
    // Optimise uses of local variables of int, long,float, and double,
    // whose value will be reassinged with a different constant number
    // during the scope of the method. Value needs to propagate, but for
    // specific intervals. (assignment <-> reassignment)
    return false;
  }

	public void optimize()
	{
    Scanner reader = new Scanner(System.in);
    System.out.println("================================================");
    System.out.println("Optimise class: '" + original.getClassName() + "' ?");
    System.out.println("1 --> yes; 0 --> no");
    System.out.println("================================================");
    int n = reader.nextInt();
		ClassGen cgen = new ClassGen(original);
    if(n == 0){
      this.optimized = cgen.getJavaClass();
      return;
    }


		ConstantPoolGen cpgen = cgen.getConstantPool();
    ConstantPool cp = cpgen.getConstantPool();
    Constant[] constants = cp.getConstantPool();
    Method[] methods = cgen.getMethods();

    System.out.println("================================================");
    System.out.println("Class constant pool");
    for(Constant c : constants){
      // Debugging to print out constants that we care about
      if(c == null) continue;
      else if(c instanceof ConstantString) continue;
      else if(c instanceof ConstantUtf8) continue;
      else System.out.println(c);
    }
    System.out.println("================================================");

    for(Method m : methods){
      // Optimisation body should be in this submethod
      boolean optimised = true;
      optimise_method(cgen, cpgen, m);
    }


		this.optimized = cgen.getJavaClass();
	}

	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
      this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}

  private void printMatches(InstructionHandle[] matches){
    System.out.println("Optimisable instructions found:");
    for(InstructionHandle h : matches){
      System.out.println(h);
    }
    System.out.println("================================================");
  }

  private void printInstructions(InstructionList il){
    InstructionHandle[] handles = il.getInstructionHandles();
    for(InstructionHandle h : handles){
      System.out.println(h);
    }
  }
}
